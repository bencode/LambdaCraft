# Chapter 02 The Term Vocabulary and Postings Lists

Chapter 1 已经把布尔检索（Boolean retrieval）的基本框架搭了起来：词项（term）进入词典，文档编号（docID）进入倒排列表（postings list），查询则在这些列表上做集合运算。但那一章默认了两个前提：什么算一个 document，以及字符序列如何变成可以索引的 term。这两个前提一旦落到真实语料里，就不再是理所当然的。

本章处理的就是更底层的定义问题。系统首先要决定文档边界，然后把字节流解码成字符序列，再把字符序列切成 token，并通过停用词、规范化和词形归并来决定最终词汇表（term vocabulary）。在此基础上，倒排列表本身还会继续演化：一方面可以通过跳表指针（skip pointers）加速合取查询，另一方面还要补充位置信息，才能支持短语查询（phrase queries）和邻近查询（proximity queries）。

本章沿着四个问题展开：

1. 检索系统如何从原始文件得到可索引的 document 和字符序列。
2. token、type、term 分别是什么，以及词汇表如何在规范化过程中被定义出来。
3. 为什么普通 postings list 还可以继续优化，以及 skip pointers 何时有效。
4. 为什么 term presence 还不足以支持 phrase / proximity 查询，以及 positional postings 如何补上这层结构。

## 全局记号

$$
\begin{aligned}
\Sigma^* &: \text{字符序列的集合} \\
\tau &: \Sigma^* \to \text{Token}^* \\
\nu &: \text{Token} \to T \cup \{\bot\} \\
D &: \text{document 集合} \\
T &: \text{term 集合}
\end{aligned}
$$

这里的 $\tau$ 表示 tokenization，$\nu$ 表示从 token 到 term 的规范化映射；当 $\nu(x) = \bot$ 时，表示这个 token 不进入词典。

## 2.1 文档边界与字符序列

### 从字节流到字符序列

索引流程真正接触到的输入，通常是文件里的字节序列，而不是"已经清洗好的文本"。对纯 ASCII 英文文本来说，这一步看上去几乎透明；但只要进入 PDF、Word、HTML、XML、多语言编码或者压缩封装，系统首先面对的就是解码问题，而不是检索问题。

还有一个常被跳过的前提：**文本是字符的线性序列**。大多数时候这没错，但在阿拉伯语、希伯来语这样的右到左文字里，一段夹带数字或英文的句子在视觉上会左右交错——读起来不线性。Unicode 的处理方式是在存储时保留概念上的线性次序，显示系统负责把右到左部分反向渲染。旧编码未必这样做。这提示一件事：**"字符序列"是一个已经内置了语言假设的抽象**。

可以把这一层抽象成一个很简单的流水线：

```text
bytes on disk / network
        |
        v
format detection + decoding
        |
        v
character sequence
        |
        v
document unit selection
```

形式化地，这一步并不直接产生 term，而是先把原始表示映射成字符序列：

$$
\delta : \text{Bytes}^* \to \Sigma^*
$$

这里的 $\delta$ 可能要同时完成几件事：识别编码、解析文档格式、解压内容、抽取正文、解码实体引用。信息检索通常把这些问题视为前置处理，但系统并不能假设它们天然已经解决。

先给这一层定义一个最小接口：

```python
from dataclasses import dataclass


@dataclass(frozen=True)
class RawDocument:
    doc_id: int
    payload: bytes
    encoding: str = "utf-8"


def decode_text(raw: RawDocument) -> str:
    return raw.payload.decode(raw.encoding, errors="replace")
```

这段代码省略了格式识别、正文抽取和元数据利用，但它表达了一个关键边界：倒排索引并不直接建立在字节上，而是建立在解码后的字符序列上。

### 文档单元并不是天然固定的

接下来系统还要决定，什么算一个 document。最朴素的做法是“一文件一文档”，但很多场景下这并不合适。一个 mbox 文件里可能包含多封邮件；一封邮件里可能又包含多个附件；一本书作为一个 document 往往又太大，因为查询词可能分别散落在远离彼此的位置上，却仍然会造成一次表面匹配。

```text
file
 ├─ email 1
 ├─ email 2
 └─ attachment 1

book
 ├─ chapter 1
 ├─ chapter 2
 └─ paragraph ...
```

因此，文档单元本身是检索设计的一部分。文档切得更细，结果通常更聚焦；但切得过细，又可能把本应共同命中的上下文拆散。这是一个典型的粒度权衡：

$$
\text{smaller document units} \Rightarrow
\begin{cases}
\text{precision often increases} \\
\text{recall may decrease}
\end{cases}
$$

对长文档尤其如此。把整本书当成一个 document，查询 `Chinese toys` 时，第一章提到 `Chinese`、最后一章提到 `toys` 也会命中；把单位切到章、节或段落，相关性会更像读者真正想要的结果，但又可能错过跨段落展开的概念。

下面这段代码只是一个粒度示意。document unit 的改变会直接改变后续索引和匹配的边界。

```python
@dataclass(frozen=True)
class Document:
    doc_id: int
    text: str


def paragraph_documents(raw: RawDocument) -> list[Document]:
    text = decode_text(raw)
    paragraphs = [part.strip() for part in text.split("\n\n") if part.strip()]
    base_doc_id = raw.doc_id * 1000
    # 把段落当作 document，用来展示 granularity 改变后的索引边界。
    return [
        Document(doc_id=base_doc_id + offset, text=paragraph)
        for offset, paragraph in enumerate(paragraphs, start=1)
    ]
```

系统在这一步才得到可以索引的对象：确定了边界的 document，以及它对应的字符序列。

## 2.2 词汇表与词项规范化

### token、type、term 不是同一个对象

有了字符序列以后，tokenization 的任务就是把字符流切成可处理的单元。最简单的例子是：

```text
Input:
Friends, Romans, Countrymen, lend me your ears;

Tokens:
Friends Romans Countrymen lend me your ears
```

但 token、type 和 term 不应混为一谈：

```text
token: 某个 document 里出现的一次字符实例
type: 全部相同字符序列 token 的类别
term: 经过规范化后真正进入词典的索引项
```

这三个概念的区分看似迂腐，但在实践中非常重要：很多文献混用 "token" 和 "term"，混用以后就讲不清楚"停用词是在 type 层丢弃还是 term 层丢弃"、"同一个 type 在不同 document 里算几个 token"。Manning 在这本书里严格区分三者，继承的是他语言学家的训练习惯。

如果文档是 `to sleep perchance to dream`，那么它包含 5 个 token、4 个 type；如果 `to` 被当作停用词丢弃，那么进入词典的 term 可能只剩 3 个。用映射写出来：

$$
\tau(\text{text}) = [x_1, x_2, \dots, x_n]
$$

$$
T = \{ \nu(x_i) \mid \nu(x_i) \neq \bot \}
$$

词汇表由 tokenization 与 normalization 共同决定，并非文本天然自带的属性。

### tokenization 是语言相关的设计问题

按空格切词只是最基础的做法，真实文本里立刻会暴露它的不足。英语里立刻会出现撇号、连字符、缩写、专有名词和复合词问题：

```text
O'Neill
aren't
Hewlett-Packard
lower-case
San Francisco
```

不同切分策略会直接影响查询是否匹配。`O'Neill` 可以被切成 `o'neill`、`oneill`、`o` + `neill`，而 `aren't` 可以保留缩写，也可以展开为 `are` + `n't`。这些选择直接决定布尔查询的匹配边界，而不只是排版偏好。

这里引出一条硬约束：**query 和 document 必须用同一个 tokenizer**。如果建索引时把 `O'Neill` 规范成 `oneill`，查询时却保留成 `o'neill`，那整个倒排索引对这个 token 就等于不存在。Boolean 模型的正确性依赖这一点——两侧用不同切法，不是"匹配差一点"，是**系统上不可能命中**。

更困难的情况来自没有显式空格分词的语言，例如中文、日文、泰文。tokenization 本身就依赖语言识别（language identification）与语言特定规则。**语言识别是 tokenization 的前置步骤**——系统不先判断文档用的是什么语言，就无法选对切分策略。Konheim 在 1981 年就用基于字符 n-gram 的分类器做过语言识别，思路源头其实来自密码学，而不是检索。现代方法（Cavnar & Trenkle 1994 等）仍然沿用 n-gram 统计，准确率已经相当高。

token 边界已经包含了语言知识：不存在"先做语言无关切分，再做语言相关后处理"的干净分层。

```text
English:
  whitespace often helps

Chinese / Japanese:
  character stream
      |
      v
  segmentation model
      |
      v
  tokens
```

对英语技术文本，一个常见起点就是这样的正则切分：

```python
import re


TOKEN_PATTERN = re.compile(r"[A-Za-z0-9]+(?:['-][A-Za-z0-9]+)*")


def tokenize(text: str) -> list[str]:
    return TOKEN_PATTERN.findall(text)
```

这类正则策略对英语技术文本常常足够有用，但它并不能覆盖多语言分词、URL、邮箱、日期和复杂复合词。工程上更常见的情况是，tokenization 必须跟着具体语料、语言和查询需求一起设计；不存在万能 tokenizer。

对中文、日文这种无空格语言，除了靠 segmentation 模型还有另一条路：放弃"词"的概念，直接用 **character k-gram**（比如 bigram、trigram）作为索引单元。一个中文字符往往已经是一个音节甚至一个语素，大多数词只有 2 字，bigram 索引能覆盖绝大多数查询匹配需求，不需要"正确分词"这个前置难题。Luk & Kwok 2002、Kishida 2005 对这个思路有系统评估。

### 2.2.2 停用词（stop words）

一个容易被低估的问题：像 `the`、`a`、`of` 这类高频词到底要不要进词典？

从空间角度看，这些词占用了倒排索引中相当大的一部分——在一些早期语料上，删除最常见的 150 个词能减少倒排空间 25%–30%。因此早期 IR 系统广泛使用 **stop list**：按**collection frequency**（词项在整个语料中出现的总次数）排序，取最常见的一批，再人工过滤出语义贫乏的那部分，作为停用词丢弃。典型 stop list 的大小变迁能看出风向：

```text
早期传统 IR：          200–300 词
2000 年前后：           7–12 词
现代 web search：     通常不用 stop list
```

为什么停用词从主流做法变成了边缘选项？两条工程原因：
- **压缩技术改进**，stop words 的 postings 占用代价变得可以接受
- **Term weighting（下一章展开）让高频词自然获得低权重**，不再需要硬删除就能避免主导分数

但彻底去掉停用词也有代价——删 stop words 是在牺牲一部分表达能力：

- **Phrase query**：`"President of the United States"` 保留 `of` 和 `the`，比 `President AND "United States"` 精确得多
- **标题、歌名、诗句**：`To be or not to be`、`Let It Be`、`I Don't Want to Miss a Thing` 几乎全由 stop words 组成
- **法律和代码搜索**：高频词承担了结构性语义

所以现代 IR 系统多半不再删 stop words，而是让 weighting 机制**自然地**降低它们的影响。

### 2.2.3 规范化（normalization）与等价类

切出 token 后，下一件事是决定哪些不同写法应当被当作**同一个 term**。这就是规范化的工作，数学上就是在 token 上构造**等价类**：

```text
USA, usa, U.S.A.        -> usa
Schütze, Schuetze       -> schutze
lower-case, lower case  -> lower case
```

形式化地：

$$
\nu : \text{Token} \to T \cup \{\bot\}
$$

其中 $\bot$ 表示 token 被丢弃，$T$ 中的元素是最终进入索引的 term。规范化改变的不只是词形外观，而是**匹配边界本身**：一旦两个 token 被映射到同一个 term，系统就认为它们在索引层面可互换。

#### 三种实现方式

构造等价类有三条路，各有代价：

1. **隐式等价类（implicit equivalence classing）**：通过一组删除或替换规则让多种写法折叠成同一个 token。例如统一删掉连字符：`anti-discriminatory` 和 `antidiscriminatory` 都变成 `antidiscriminatory`。优点是实现简单；缺点是只擅长"删掉字符"，要做"反向等价"（`antidiscriminatory` → `anti-discriminatory`）就难了。
2. **Query expansion**：索引保留原形，查询时展开——查 `car` 时同时查 `automobile`。好处是**可以非对称**：查 `windows` 可以带上 `Windows` 和 `window`，但查 `window` 不扩展到 `Windows`（因为大概率用户就是想要小写意义）。
3. **Index expansion**：在建索引时就展开——某文档包含 `automobile`，就同时以 `car` 的身份索引它。查询时就不需要展开。

三种各有权衡：隐式规则最省运行时但表达力弱；query expansion 灵活但每次查询多开销；index expansion 前置开销但查询快。

#### 大小写折叠（case-folding）

最常见的隐式规则是把所有字母转小写。句首的 `Automobile` 就能匹配到用户输入的 `automobile`，web 搜索里尤其有用——大多数人打 `ferrari` 的时候其实在找 Ferrari 这个品牌。

但 case-folding 有代价：`General Motors`、`The Associated Press`、`Bush`、`Black` 这些专有名词折叠后会和同拼写的普通词混在一起。

一个更精细的方案是 **truecasing**：用机器学习模型判断某处的大写化是语法必要（句首）还是语义必要（专有名词）。但它的收益并不一定盖过成本，因为用户查询时**自己也常常不打大小写**。实际做法多半还是：全部小写化。

#### 重音与 diacritics

英语里这个问题边缘化，最多涉及 `cliché` / `cliche`、`naïve` / `naive`。其它语言里就复杂：西班牙语 `peña`（悬崖）和 `pena`（悲伤）在字典里是不同词，但用户查询时往往不打重音——这种情况下 IR 系统通常选择统一去掉重音符。

#### 代码示意

一个最小规范化器，把 tokenize 与 normalize 串起来：

```python
STOP_WORDS = {"a", "an", "the", "of", "to"}


def normalize(token: str) -> str | None:
    normalized = token.lower().replace("'", "")
    if normalized in STOP_WORDS:
        return None
    return normalized


def terms(text: str) -> list[str]:
    return [t for token in tokenize(text) if (t := normalize(token)) is not None]
```

term vocabulary 是系统通过 tokenization、normalization 和 stemming / lemmatization 一步一步设计出来的对象。它不会从文本里自然掉出来。

### 2.2.4 Stemming 与 lemmatization

词形归并（stemming / lemmatization）是规范化最常见也最容易被误用的一类操作。它们都试图把不同词形映射到同一个形式上，但目标和代价并不相同。

```text
stemming:
  通过启发式截断词尾形成近似公共词干（stem）

lemmatization:
  通过词汇和形态分析回到词典形态（lemma）
```

典型例子：

$$
\text{am, are, is} \Rightarrow \text{be}
$$

$$
\text{car, cars, car's, cars'} \Rightarrow \text{car}
$$

两者的区别不只在实现，也在产物：**stemming 的结果常常不是真正的词**（`ponies → poni`），只是一个可用于匹配的符号；**lemmatization 的结果是词典里能查到的词形**（`ponies → pony`）。遇到 `saw` 这种多义词，stemmer 可能返回 `s`（单纯截尾），lemmatizer 会根据上下文返回 `see`（动词）或 `saw`（名词）。

#### Porter stemmer

英语 stemming 的事实标准是 **Porter stemmer**，由 Martin Porter 在 1980 年的论文 *An Algorithm for Suffix Stripping* 中提出。它的结构很有意思：分 5 个**顺序应用的阶段**，每个阶段里有一组替换规则，规则按"最长后缀匹配"选一条应用。第一阶段的一小部分规则大致长这样：

```text
SSES  →  SS     caresses  →  caress
IES   →  I      ponies    →  poni
SS    →  SS     caress    →  caress
S     →   ∅     cats      →  cat
```

规则简单到近乎朴素，但这个算法经过几十年仍是英语 IR 的默认 stemmer。Porter 之前有 Lovins 1968 做过 one-pass stemmer；之后有 Paice/Husk 1990 等改进版，但都没能撼动 Porter 的主流地位。Porter 本人后来又做了 Snowball 框架，让同一套 stemmer 思路扩展到多种语言。

#### Stemming 的收益（和代价）

这里需要保持克制：stemming 并不是普遍正确的操作。它通常**提高 recall，却可能伤害 precision**。教科书级的反例是：Porter 会把 `operate`、`operating`、`operation`、`operative`、`operatives`、`operational` 全部归到 `oper`。于是查询 `operating system`（操作系统）和 `operative dentistry`（手术性牙科）的边界就被抹平——两种完全不同主题的文档会互相混入结果。

这里的关键判断是：**哪些词形差异应该被视为同一个检索单元**。这个判断高度依赖语言本身的形态复杂度：

| 语言 | 形态复杂度 | Stemming 对 IR 的实证收益 |
|------|-----------|-------------------------|
| 英语 | 低 | 接近 0，甚至负向（多项 CLEF 评测结果） |
| 西班牙语 | 中 | 约 +10% |
| 德语 | 中高（复合词多） | +15% 左右（加上 compound splitting 更好） |
| 芬兰语 | 极高 | 约 +30% |

Hollink 2004 年在 CLEF 评测上系统对比过 8 种欧洲语言，结论大致是：**语言形态越丰富，stemming 收益越大；英语处于尴尬的低收益区**。这和很多人对 stemming 的直觉（"语言越规则越好做"）相反——因为形态规则的语言里，同一个概念的词形变体数量更多，归并后才有明显的 recall 增长。

这给工程实践的教训是：**stemming 不是 free lunch，而是 equivalence class 扩展，要看是否和你的语言、语料、查询分布匹配**。

## 2.3 用 skip pointers 加速倒排交集

### skip pointers：同一模型上的跳跃结构

Chapter 1 已经给出了合取查询的基本代价：如果两个 postings list 长度分别为 $m$ 和 $n$，普通双指针交集需要

$$
O(m+n)
$$

这已经是一个很好的基线，但当倒排列表很长、且索引相对稳定时，还可以在同一条列表上增加"跳跃边"，把一部分显然无关的区间整体跨过去。这种结构通常称为 skip pointers，它源自 William Pugh 1990 年发表的 *Skip Lists: A Probabilistic Alternative to Balanced Trees*——在更广的数据结构领域里，skip list 原本是平衡树的简化替代，多级 skip 能给出 $O(\log P)$ 期望查找。Moffat 和 Zobel 1996 年把这个结构正式引入 IR 上下文（*Self-Indexing Inverted Files for Fast Text Retrieval*），以后就成了倒排索引常见优化之一。

```text
docIDs:  3 -- 8 -- 12 -- 16 -- 19 -- 23 -- 28 -- 41
            \____________________/
                  skip
```

如果当前指针在 `8`，另一条列表当前值已经到 `24`，那么看到 `skip(8) = 23` 仍不超过 `24` 时，系统就可以直接跳到 `23`，而不必逐个比较 `12`、`16`、`19`。反过来，如果对侧当前值只有 `20`，那么这次跳跃就不安全，因为 `23 > 20`，系统仍然必须继续顺序推进。

skip pointers 改的是 postings merge 的访问路径，不改查询语义。这一点很重要：它只是一个执行优化，不是新的检索模型。

### 如何在交集时利用跳跃

下面用一个简单结构模拟带 skip 的 postings。这里的 `skip_index` 表示“当前 posting 可以跳到的列表下标”，不是目标 `docID` 本身。跳跃成立需要两个条件：skip 目标的 `docID` 仍不超过对侧当前 `docID`，而且这种优化只对原始 postings list 有意义，对中间结果通常没有现成 skip 可用。

```python
from dataclasses import dataclass


@dataclass(frozen=True)
class SkipPosting:
    doc_id: int
    skip_index: int | None = None


def intersect_with_skips(
    left: list[SkipPosting],
    right: list[SkipPosting],
) -> list[int]:
    answer: list[int] = []
    i = 0
    j = 0

    while i < len(left) and j < len(right):
        left_posting = left[i]
        right_posting = right[j]

        if left_posting.doc_id == right_posting.doc_id:
            answer.append(left_posting.doc_id)
            i += 1
            j += 1
        elif left_posting.doc_id < right_posting.doc_id:
            # 只有 skip 目标仍不超过对侧当前 docID，跳跃才不会漏掉交集结果。
            while (
                (skip_index := left[i].skip_index) is not None
                and left[skip_index].doc_id <= right_posting.doc_id
            ):
                i = skip_index
            if left[i].doc_id < right_posting.doc_id:
                i += 1
        else:
            # 对右侧列表做同样的安全跳跃；一旦不能跳，就回到顺序推进。
            while (
                (skip_index := right[j].skip_index) is not None
                and right[skip_index].doc_id <= left_posting.doc_id
            ):
                j = skip_index
            if right[j].doc_id < left_posting.doc_id:
                j += 1

    return answer
```

这里有两个边界必须保持清楚：

- skip pointers 只帮助 `AND` 查询，不帮助 `OR` 查询。
- skip 的收益依赖 postings 足够长，而且索引更新不能过于频繁。

原因也很直接。`OR` 查询本来就需要把双方都保留下来，跨过去并不能减少结果枚举；而如果 postings list 经常增删，预先布好的跳跃结构很容易失效。

### skip 放在哪里

skip pointers 的位置本身也是权衡。放得太密，跳跃跨度短、元数据多、判断开销大；放得太稀，又很少遇到值得跳的机会。一个常用的经验法则是：对长度为 $P$ 的 postings list，使用大约 $\sqrt{P}$ 个均匀分布的 skips。

$$
\text{number of skips} \approx \sqrt{P}
$$

这是一个实践启发式，不保证最优。skip pointers 的收益来自 postings 的几何结构，而不是更复杂的布尔语义。

## 2.4 位置索引、短语与邻近查询

### term presence 还不足以支持短语

用户希望 `"Stanford University"` 作为一个**短语**查询，而不是两个独立词的 AND。这不是小众需求——现代搜索引擎里，大约 **10% 的 web 查询** 明确用双引号表达 phrase query，加上人名、组织名这类隐式 phrase，比例更高。

普通倒排索引只能回答"某个 term 是否在某篇 document 中出现过"。这足以支持一般的 Boolean presence 查询，却不足以支持短语查询。查询 `"stanford university"` 时，文档

```text
The inventor Stanford Ovshinsky never went to university.
```

显然不该命中；但如果 postings 里只有 `stanford -> [d]`、`university -> [d]`，系统看不出两个词之间隔了多远。

这就是 Chapter 2 的第二个关键推进：布尔检索需要的不只是文档级出现信息，还需要更细粒度的结构信息。

### 双词索引（biword index）是一种特例化方案

支持短语的一个直接思路，是把每对相邻词都当成新的 vocabulary term，也就是双词索引（biword index）：

```text
Friends, Romans, Countrymen
    ->
friends romans
romans countrymen
```

这样一来，两词短语查询可以直接查词典；更长短语则拆成多个 biword 做合取。例如：

```text
stanford university palo alto
    ->
"stanford university"
AND "university palo"
AND "palo alto"
```

这种方式的优点是直接，缺点也同样直接：词汇表会迅速膨胀，而且对更长短语仍可能出现 false positives。因此，biword index 更像某类特化优化，而不是一般方案。

### 位置索引（positional index）是通用解法

更标准的结构是位置索引（positional index）：对每个 term，不只记录它出现在哪些 document 中，还记录它在 document 内出现在哪些位置上。可以写成：

$$
I_{\mathrm{pos}} : T \to [(\mathrm{docID}, \langle p_1, p_2, \dots, p_k \rangle)]
$$

其中 $p_i$ 是 token 位置。它和 Chapter 1 的 postings list 只差一步，但这一步决定了 phrase / proximity 查询能否被可靠表达。

```text
to  -> (1, [7, 18, 33, 72]), (2, [1, 17, 74])
be  -> (1, [17, 25]),        (4, [17, 191, 291])
```

位置索引的值已经不再是单个 `docID` 或 `(docID, tf)`，而是位置列表：

```python
PositionalPosting = tuple[int, list[int]]
PositionalIndex = dict[str, list[PositionalPosting]]


def build_positional_index(docs: list[Document]) -> PositionalIndex:
    buckets: dict[str, dict[int, list[int]]] = {}

    for doc in docs:
        # 位置索引的核心是保留每次出现的 token 位置，而不仅仅是记住“出现过”。
        for position, term in enumerate(terms(doc.text)):
            doc_positions = buckets.setdefault(term, {})
            doc_positions.setdefault(doc.doc_id, []).append(position)

    return {
        term: sorted(doc_positions.items())
        for term, doc_positions in sorted(buckets.items())
    }
```

### phrase query 实际上是位置兼容性检查

有了 positional postings，短语查询此时检查的是“这些 term 的位置是否满足相邻或接近的约束”。

对二词短语 `"new york"`，系统在同一篇文档中需要找到：

$$
p_{\text{york}} = p_{\text{new}} + 1
$$

对邻近查询（proximity query）`A NEAR/k B`，条件则变成：

$$
|p_A - p_B| \le k
$$

这个判断过程适合用局部位置归并来理解：

```text
new  -> doc 7: [3, 10, 25]
york -> doc 7: [4, 18, 26]

check:
  4  = 3 + 1   -> match
  18 != 10 + 1
  26 = 25 + 1  -> match
```

短语匹配本质上是在检查位置约束：

```python
def phrase_hits(
    left_positions: list[int],
    right_positions: list[int],
    gap: int = 1,
) -> list[int]:
    hits: list[int] = []
    i = 0
    j = 0

    # 这里检查的是相对位移约束，而不是简单的同文档共现。
    while i < len(left_positions) and j < len(right_positions):
        target = left_positions[i] + gap
        if right_positions[j] == target:
            hits.append(right_positions[j])
            i += 1
            j += 1
        elif right_positions[j] < target:
            j += 1
        else:
            i += 1

    return hits
```

对完整短语，系统会先在 document 层面对齐，再在每个候选 document 内做位置兼容性检查。positional index 因此成了 phrase 与 proximity 查询的通用基础，而 biword index 更多只是针对某些高频短语的额外优化。

#### 从 docID-merge 到 token-merge：复杂度的一次提升

位置索引带来的不只是"多存了一些信息"，而是**查询复杂度的本质变化**。Chapter 1 里普通 Boolean 查询是 $\Theta(N)$，其中 $N$ 是文档总数——每个文档对应 postings 中一个 entry。位置索引里，每**次出现**都对应一个 entry，于是同样的 Boolean 查询变成 $\Theta(T)$，其中 $T$ 是 collection 的**总 token 数**。对一般语料而言，$T \gg N$，典型比例是每文档 100 到 1000 个 token。

这个变化的影响也反映在存储上。粗略经验：

- **Positional index 通常比 non-positional 大 2–4 倍**
- 压缩后的 positional index 约等于原始未压缩文本的 1/3 到 1/2

大多数现代应用仍然接受这个代价，因为短语查询和邻近查询的用户价值太高。

### 组合方案：为什么 `The Who` 值得单独建索引

biword index 和 positional index 并不互斥。更常见的工程做法是：

- 保留单词级倒排索引作为基础结构
- 对部分高价值短语或复合词增加 phrase / biword 索引
- 对一般 phrase / proximity 查询使用 positional postings

"高价值短语"的判据不是直觉上的"查询频率"，而是**评估代价**。一个反直觉的例子来自原书：

| 短语 | 两词单独常见度 | 查询频率 | Phrase index 加速 |
|------|---------------|---------|-------------------|
| `Britney Spears` | 中—中 | 高 | 约 **3×** |
| `The Who` | 极高—极高 | 中 | 约 **1000×** |

`Britney Spears` 虽然查询频率高，但两个词单独都不算超常见，位置 merge 本来就不贵；`The Who` 恰好相反——`the` 和 `who` 单独的 postings 几乎覆盖整个语料，直接做位置 merge 成本爆炸，一旦预先建了 phrase index，加速能到三个数量级。

所以组合索引的选型标准是 **"最贵的那些 phrase 查询"，不是"最常的那些 phrase 查询"**。Williams、Zobel、Anderson 2004 年在 *Fast Phrase Querying with Combined Indexes* 里做了更细的设计：对每个 term 额外维护一个"next word index"（记录它后面紧跟过哪些词），比纯 positional 快约 4 倍，只多 26% 空间。

这说明 Chapter 2 的推进在于让 postings list 变得更像一个可继续增强的查询数据结构。Chapter 1 解决的是 presence-based retrieval 的骨架；本章则把这个骨架推进到了更接近真实文本检索系统的状态。

## 本章总结

- 检索系统并不是直接处理“文本本身”。它先要确定 document unit，再把原始字节流稳定地解码成字符序列。
- 词汇表并非天然存在；tokenization、stop words、normalization、stemming / lemmatization 共同决定了索引边界。
- skip pointers 只是 postings intersection 的执行优化，改的是访问路径，不是 Boolean 查询语义。
- phrase 和 proximity 查询要求 postings 中保留位置信息。
- positional index 和 skip pointers 的加入，让 Chapter 1 的倒排骨架扩展成了真实文本检索系统的基础结构。

## 2.5 参考文献

### 本章主源

- Manning, C.D.; Raghavan, P.; Schütze, H. *Introduction to Information Retrieval*, Chapter 2: "The Term Vocabulary and Postings Lists"（本章改编自该书论证结构与具体例子）, online edition, Cambridge University Press, 2009.

### Stemming

- Lovins, J.B. "Development of a Stemming Algorithm." *Mechanical Translation and Computational Linguistics*, 1968.（Porter 之前的第一代 stemmer）
- Porter, M.F. "An Algorithm for Suffix Stripping." *Program*, 14(3): 130–137, 1980.（Porter stemmer 原论文）
- Paice, C.D.; Husk, G. 1990.（另一系常用 stemmer）
- Salton, G. *Automatic Text Processing*. 1989.（stemming 的早期实证评估）
- Hollink, V. et al. "Monolingual Document Retrieval for European Languages." *IR Journal*, 2004.（CLEF 多语言 stemming 评测）

### Skip lists 与倒排优化

- Pugh, W. "Skip Lists: A Probabilistic Alternative to Balanced Trees." *Communications of the ACM*, 33(6): 668–676, 1990.（skip list 原论文——数据结构视角）
- Moffat, A.; Zobel, J. "Self-Indexing Inverted Files for Fast Text Retrieval." *ACM TOIS*, 1996.（skip pointers 在 IR 的经典应用）

### 短语、邻近、组合索引

- Williams, H.E.; Zobel, J.; Anderson, P. "Fast Phrase Querying with Combined Indexes." *ACM TOIS*, 2004.（next word index）

### 中日韩分词与 language identification

- Sproat, R.; Shih, C.; Gale, W.; Chang, N. "A Stochastic Finite-State Word-Segmentation Algorithm for Chinese." *Computational Linguistics*, 22(3), 1996.（中文分词经典基准）
- Luk, R.W.P.; Kwok, K.L. "A Comparison of Chinese Document Indexing Strategies and Retrieval Models." 2002.（character bigram indexing 评估）
- Cavnar, W.B.; Trenkle, J.M. "N-gram-based Text Categorization." 1994.（language identification 的现代方法）
- Konheim, A.G. *Cryptography: A Primer*. Wiley, 1981.（n-gram language identification 的早期密码学源头）
