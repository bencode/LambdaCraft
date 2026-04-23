# Chapter 01 Boolean Retrieval

信息检索（information retrieval, IR）是一个比搜索引擎更古老的学科。"Information Retrieval" 这个术语是 Calvin Mooers 在 1948–1950 年间提出的；更早的愿景出现在 Vannevar Bush 1945 年那篇著名的 *As We May Think*——他设想过一台叫 memex 的"个人文献机器"，把全部笔记、资料、通信微缩到一个书桌大小的设备里，随时按链接调取。Bush 当时想的是机械微缩胶片，但他提出的问题一直留到今天：**面对大量非结构化文本，一个系统怎样才能根据用户的信息需求找出真正有用的文档？**

这也就是 IR 作为学科的正式定义：**从大规模集合中，为用户的信息需求找到（通常是文本的）非结构化资料**。三个关键词——非结构化、信息需求、大规模——每一个都会在本章反复回来。

布尔检索（Boolean retrieval）是这个问题最早、最简洁的答案：把查询写成 `AND` / `OR` / `NOT` 组成的布尔表达式，系统回答"哪些文档**满足**这个表达式"。简单，但也正因为简单，它先把整个检索链路的骨架立了起来：文档怎么组织成索引，索引怎么接受查询，查询怎么被高效执行。后面的词频、加权、评分函数都是在这个骨架上继续叠加。

本章沿着四个问题展开：

1. 信息检索中的"查询""信息需求""相关结果"分别是什么。
2. 为什么全文扫描不足以成为通用方案，以及倒排索引（inverted index）如何改变问题表示。
3. Boolean 查询如何在倒排列表上高效执行。
4. 布尔检索在真实系统里还留下什么问题，以及这些问题把我们推向后面几章的哪些主题。

## 全局记号

$$
\begin{aligned}
D &: \text{文档集合} \\
T &: \text{词项集合} \\
d &\in D \\
t &\in T \\
q &: \text{查询} \\
R(q) &: \text{对 } q \text{ 真正相关的文档集合} \\
A(q) &: \text{系统返回的文档集合}
\end{aligned}
$$

## 1.1 检索任务、相关性与线性扫描

### 检索任务与相关性

以 Shakespeare 的 *Collected Works* 为例——这本全集大约一百万字，Shakespeare 一生用过大约三万两千个不同的词。考虑这样一个查询：

```text
Brutus AND Caesar AND NOT Calpurnia
```

从逻辑上看，这个问题并不复杂：只要逐篇检查每个文档是否同时包含 `Brutus`、`Caesar`，且不包含 `Calpurnia` 即可。放到 Shakespeare 这种百万字规模上，任何现代笔记本用 `grep` 扫一遍都不成问题。

问题出在规模增长之后。当文档从"一部全集"变成"企业全量邮件"再到"web 上数十亿网页"时，每次查询重新扫描全部文本的成本就会迅速吃不消。单次判断并不困难，成本来自**无法复用的重复工作**。

这时需要先把检索任务里的几个基本对象分清楚：

```text
information need: 用户真正想知道的信息
query: 用户提交给系统的表达式
relevant document: 对 information need 有用的文档
corpus / collection: 被检索的文档集合
```

关键的区分是：$\text{information need} \neq \text{query}$。

系统实际执行的是查询，但系统是否有效，要看它返回的 $A(q)$ 与真正相关集合 $R(q)$ 之间的关系。

```text
information need
      |
      v
query string / Boolean expression
      |
      v
retrieval system
      |
      v
returned set A(q)
      |
      +---- compare with ----> relevant set R(q)
```

如果把词项（term）与文档（document）的出现关系写成一个二值矩阵，那么它看起来会是这样：

```text
            doc1  doc2  doc3  doc4
Brutus        1     1     0     1
Caesar        1     1     1     1
Calpurnia     0     1     0     0
```

形式化地，可以写成：

$$
M : T \times D \to \{0,1\}
$$

$$
M(t,d) = 1 \iff t \text{ occurs in } d
$$

在这种表示下，布尔查询就是**对应行向量的逐位布尔运算**。对上面的矩阵求 `Brutus AND Caesar AND NOT Calpurnia`，相当于：

```text
Brutus       : 1 1 0 1
Caesar       : 1 1 1 1
NOT Calpurnia: 1 0 1 1      (取 Calpurnia 行的补)
-----------------------
逐位 AND     : 1 0 0 1
```

结果 `1 0 0 1` 指向 doc1 和 doc4——也就是同时包含 Brutus 和 Caesar、且不包含 Calpurnia 的文档。查询看上去已经有了清爽的代数形式。

对于未排序的结果集合，两个最基本的评价量是精确率（precision）和召回率（recall）：

$$
\mathrm{precision}(q) = \frac{|A(q) \cap R(q)|}{|A(q)|}
$$

$$
\mathrm{recall}(q) = \frac{|A(q) \cap R(q)|}{|R(q)|}
$$

这两个量衡量的都是结果集合与相关集合之间的重合程度：系统找回了多少相关文档，以及返回结果里有多少真正有用。

### 线性扫描为什么不够

最朴素的实现就是**挨个文档扫一遍**——在 Unix 世界里这就是 `grep` 的工作。代码上看几乎透明：

```python
from dataclasses import dataclass


@dataclass(frozen=True)
class Document:
    doc_id: int
    text: str


def matches(doc: Document, required: set[str], forbidden: set[str]) -> bool:
    tokens = set(doc.text.split())
    return required <= tokens and tokens.isdisjoint(forbidden)


def boolean_scan(
    docs: list[Document],
    required: set[str],
    forbidden: set[str],
) -> list[int]:
    return [
        doc.doc_id
        for doc in docs
        if matches(doc, required=required, forbidden=forbidden)
    ]
```

这段实现是正确的，但它把全部成本都集中在"重复扫描全文"上。实际系统里，这往往是最先崩掉的一层。

线性扫描撑不起通用搜索的原因有三条，它们也直接对应了倒排索引要解决的问题：

1. **规模**：文档集合从 GB 级增长到 TB、PB 级时，每次查询全扫一遍都不可接受。以一个现实的规模假设为例——$N = 10^6$ 篇文档、平均 1000 词、每词约 6 bytes，collection 就是 6 GB，不同词项约 $M = 5 \times 10^5$ 个。
2. **灵活匹配**：`grep` 几乎给不了 `Brutus NEAR Caesar` 这种"两个词在 5 个词之内"或"同一句话里"的查询——而这类查询在真实场景里极常见。
3. **排序检索**：线性扫描只能回答"命中与否"，回答不了"哪个更像答案"。后面会看到，实际搜索系统的核心诉求就是这条。

这三条局限就是后续章节的提纲。本章先只处理前两条里最基础的一半：把"命中与否"从重复扫描改写成**在索引上做集合运算**。

## 1.2 倒排索引的表示与构建

### 从稀疏矩阵到倒排索引

倒排索引（inverted index）的核心思想是：系统不再从"文档里有哪些词"出发，而改为从"某个词出现在了哪些文档里"出发。

$M : T \times D \to \{0,1\}$ 给出了干净的逻辑定义，但在真实语料规模下几乎不可直接存储。承接上一节的规模假设（$M = 5 \times 10^5$ 个不同词项，$N = 10^6$ 篇文档），矩阵有 $5 \times 10^{11}$ 个单元——五千亿个格子。每篇文档平均 1000 词，最多提供 $10^9$ 个 `1`，其余 **99.8% 以上全是 `0`**。

矩阵庞大的真正问题不是访问速度，而是**空间**：直接存 500 GB 的单元，其中 99.8% 毫无信息量。更合理的方向是只保存出现过的位置。

如果只保留值为 `1` 的位置，同样的数据可以改写成：

```text
dictionary
  Brutus     ----> [1, 2, 4]
  Caesar     ----> [1, 2, 3, 4]
  Calpurnia  ----> [2]
```

如果还保留词频，则进一步变成：

```text
dictionary
  Brutus     ----> [(1,1), (2,1), (4,2)]
  Caesar     ----> [(1,2), (2,1), (3,1), (4,1)]
  Calpurnia  ----> [(2,1)]
```

这一变化首先是一种压缩，但更关键的是它改写了查询的访问方向：系统沿着 `term \to documents` 访问，而不是沿着 `document \to terms` 逐篇查看。

这里需要固定几个核心术语：

```text
dictionary: 词典，term -> postings list 的访问入口
posting: 倒排记录，某个 term 出现在某个 document 的记录
postings list: 倒排列表，某个 term 对应的全部 postings
document frequency: 文档频率，某个 term 出现过的 document 数
```

如果用集合映射来表示，那么在最基础的 non-positional Boolean index 中：

$$
I : T \to \mathcal{P}(D)
$$

如果保留词频：

$$
I : T \to [(\mathrm{docID}, tf)]
$$

这里的 `docID` 指文档编号（document identifier）。每个倒排列表都按 `docID` 排序，并定义文档频率：

$$
df(t) = |I(t)|
$$

### 索引是怎样构建出来的

索引构建的完整流程分四个阶段：

1. **Collect**：拿到要索引的文档集合
2. **Tokenize**：把每篇文档切成 token 序列（Shakespeare 的 `So let it be with Caesar.` → `[So, let, it, be, with, Caesar]`）
3. **Linguistic preprocess**：把 token 规范化为 term（小写化、去除停用词、stemming 等——这些是下一章的重点）
4. **Index**：把 (term, docID) 对按 term 聚合 → 构造 dictionary + postings

Chapter 2 会专门讲前三步。本章先假设前三步已经完成，聚焦第四步——**从 (term, docID) 对到倒排索引**。

这一步本身很简单：按 term 聚合，再把 postings 按 docID 排好。

**一个小例子**。假设两篇文档来自同一段 Shakespeare：

```text
Doc 1: "I did enact Julius Caesar: I was killed i' the Capitol; Brutus killed me."
Doc 2: "So let it be with Caesar. The noble Brutus hath told you Caesar was ambitious:"
```

四步跑下来，dictionary 会出现 `brutus`、`caesar`、`enact`、`julius`、`killed` 等 term，对应的 postings 中：

```text
brutus    -> [1, 2]
caesar    -> [(1, 1), (2, 2)]      # 如果带 tf，就把出现次数一起存
enact     -> [1]
julius    -> [1]
killed    -> [(1, 2)]
```

这个最小例子里已经能看到两层结构：**dictionary 按字母序放 term**，**每条 postings list 按 docID 排序**。

```python
from collections import Counter, defaultdict


Posting = tuple[int, int]
InvertedIndex = dict[str, list[Posting]]


def tokenize(text: str) -> list[str]:
    return text.lower().split()


def build_inverted_index(docs: list[Document]) -> InvertedIndex:
    index: defaultdict[str, list[Posting]] = defaultdict(list)

    for doc in docs:
        for term, tf in Counter(tokenize(doc.text)).items():
            index[term].append((doc.doc_id, tf))

    return {
        term: sorted(postings)
        for term, postings in sorted(index.items())
    }
```

这段代码的关键在于两个排序约束：

- `dictionary` 按 `term` 组织
- `postings list` 按 `docID` 排序

## 1.3 Boolean 查询的执行

### 交集是 Boolean 查询的核心操作

有了倒排索引之后，Boolean 查询的中心问题就不再是"扫描哪些文档"，而是"如何在多个倒排列表上做集合运算"。

对最简单的合取查询：

```text
t1 AND t2 AND ... AND tn
```

核心操作就是交集（intersection）。在 IR 文献里，这个操作也叫 **postings merge**——"merge" 只是沿用了多路有序列表合并算法的术语，和布尔 `OR` 的语义没有关系。

当两个倒排列表都按 `docID` 排序时，可以用双指针做线性归并：

```text
p1: 1   2   4   8
    ^
p2: 2   4   6   8   10
    ^

step 1: 1 < 2   -> move p1
step 2: 2 = 2   -> emit 2, move both
step 3: 4 = 4   -> emit 4, move both
step 4: 8 > 6   -> move p2
step 5: 8 = 8   -> emit 8, move both
result: [2, 4, 8]
```

对应的复杂度是：

$$
O(x + y)
$$

其中 $x$ 和 $y$ 是两个倒排列表的长度。这个复杂度成立的前提非常明确：两边都在同一全序下排序，且指针只单调前进、不回退。

从 collection 总规模的角度看，这个复杂度也可以写成 $\Theta(N)$——毕竟两条 postings 的总长最多是文档总数的两倍。换句话说，**索引并没有改变 Boolean 查询的渐近复杂度**，线性扫描本来也是 $\Theta(N)$。索引带来的是一个**巨大的常数因子**：线性扫描要读全部文本（6 GB 级），而索引只需要读两条短 postings。这个"只改常数不改阶"的事实，后面谈压缩和 cache 时会反复出现。

### 交集算法

下面的实现里，关键是两个指针如何单调前进：这正是线性复杂度的来源。

```python
def doc_ids(postings: list[Posting]) -> list[int]:
    return [doc_id for doc_id, _tf in postings]


def intersect_postings(left: list[int], right: list[int]) -> list[int]:
    answer: list[int] = []
    i = 0
    j = 0

    # 两个指针都只向前移动，因此每个 posting 最多被访问一次。
    while i < len(left) and j < len(right):
        if left[i] == right[j]:
            answer.append(left[i])
            i += 1
            j += 1
        elif left[i] < right[j]:
            i += 1
        else:
            j += 1

    return answer
```

### 执行顺序本身就是优化

对多项合取查询，执行计划的关键启发式是：按 increasing $df$ 顺序处理词项。

理由来自一个简单的不等式：

$$
|X \cap Y| \le \min(|X|, |Y|)
$$

先处理短列表，通常能更快缩小中间结果，从而降低后续相交成本。文档频率不仅是一个统计量，它本身也是执行计划信号。

```text
sort terms by increasing df
    |
    v
shortest postings first
    |
    v
iterative intersection
```

这段代码的关键在于执行顺序：它先取最短列表，再逐步与其他列表相交。

```python
def and_query(index: InvertedIndex, terms: list[str]) -> list[int]:
    """返回同时包含所有 terms 的 docID 列表。

    Terms 必须是小写（和 build_inverted_index 存储时一致）；调用方负责
    在查询前做好 normalize，这里不做隐式转换，以保持索引访问路径的一致。
    """
    postings = [
        doc_ids(index.get(term, []))
        for term in sorted(terms, key=lambda term: len(index.get(term, ())))
    ]
    if not postings:
        return []

    # 先从最短 postings list 开始，通常能更快缩小中间结果。
    result, *rest = postings
    for current in rest:
        result = intersect_postings(result, current)
        if not result:
            return []

    return result
```

倒排索引、双指针交集、按 df 排序的执行顺序——这三件事合起来，就是 Boolean 查询执行的骨架。

## 1.4 Boolean 模型、Westlaw 与排序检索

### Boolean 在商业搜索里当过很久的主角

把 Boolean 说成"入门模型"容易让人误以为它只是教学用具。事实上，**从 1970s 到 1990s 早期（大致到 World Wide Web 兴起之前），商业搜索服务几乎全是 Boolean 的天下**。

最典型的例子是法律搜索服务 Westlaw，1975 年开业，到今天仍是全球最大的法律检索平台之一。Westlaw 直到 2005 年，Boolean 查询（它叫 "Terms and Connectors"）依然是默认模式，`natural language` 查询（也就是排序检索）是 1992 年才加进去的选项。一条典型的 Westlaw 法律查询长得像这样：

```text
"trade secret" /s disclos! /s prevent /s employe!
```

它用到好几个 Boolean 扩展语法：

- `/s` 表示"必须出现在同一句话里"（proximity within sentence）
- `/p` 是"同一段"，`/k` 是"k 个词以内"
- `!` 是尾部通配：`disclos!` 会匹配 `disclose`、`disclosed`、`disclosure`...
- 双引号表示严格短语

这种查询可以精确到"起诉前雇员违反商业秘密的案例"。法律、医学、专利这类专业领域里，用户宁愿花十几分钟组织一条精确的 Boolean 查询，也不愿意看 500 条自动排序的候选——因为结果必须**可复核、可解释**。到 2007 年前后，多数法律图书馆员仍会推荐 Boolean 风格。

有意思的是，Turtle 1994 年在 Westlaw 子集上做过一个对照实验：让系统本身的资深 reference librarian 精心编写 Boolean 查询，再和普通用户的 free text 查询比效果。结果是后者在大多数信息需求上**反而胜出**。这个反差说明 Boolean 不是"总比 free text 更精确"，而是"在给专业用户的可控性、可审计性上更精确"——两种精确性并不是同一件事。

### Boolean 模型能做什么，不能做什么

布尔检索（Boolean retrieval）的优势是明确、可控、透明。它回答的问题是：

```text
这个 document 是否满足 query？
```

因此它的输出可以写成：

$$
S(q) = \{ d \in D \mid d \text{ satisfies } q \}
$$

但一般搜索系统最终还要回答另一个问题：

```text
哪些 document 更像答案？
```

这就要求系统至少引入一个评分函数：

$$
s : D \times Q \to \mathbb{R}
$$

然后返回按 $s(d,q)$ 排序的结果。

这标志着系统从 `presence / absence` 转向 `evidence accumulation`。系统开始累积多个词项的匹配信号，用总分衡量相关性，而不再只判断“出现或没出现”。

### 为什么光靠 Boolean 不够

`AND` / `OR` 只能粗粒度地改变结果集合大小：

```text
more AND  -> smaller result set -> usually higher precision, lower recall
more OR   -> larger result set  -> usually higher recall, lower precision
```

它们能够调整集合边界，却不能自然表达“满足程度”。例如，`AND` 可以说明两个词都出现了，却说不清哪个文档出现得更多、哪个文档更接近用户真正想找的内容。这就是布尔检索的根本边界。

系统可以在基础布尔运算上增加更多操作符，例如 phrase、proximity、wildcard。这些扩展会提升表达力，但它们仍然停留在集合语义里，没有进入排序模型。

### 排序检索的基础示意

一个最朴素的排序实现：

```python
from collections import Counter


def ranked_search(
    index: InvertedIndex,
    terms: list[str],
) -> list[tuple[int, float]]:
    scores: Counter[int] = Counter()

    # 只累加最基础的词频信号，展示“集合 -> 分数”的结构变化。
    for term in terms:
        scores.update({doc_id: tf for doc_id, tf in index.get(term, ())})

    return scores.most_common()
```

两种模型的分工：Boolean model 提供索引、集合运算和执行计划的骨架；ranked retrieval 在这个骨架之上累积统计信号，再把集合判断升级为强弱不等的相关性信号。

### 本章止步，后面还差什么

Boolean 模型把一个基本的检索链路跑通了：查询 → 倒排索引 → 集合运算 → 结果集。但一个现代搜索系统要真正好用，还需要后续几个能力——它们也是后面几章的主线：

1. **词汇表更健壮**：用户的 query 和文档的 term 不一定字面一样。拼写错误、大小写、连字符、同词不同形（`color` 和 `colour`），都应当被系统视为等价。Ch 2 专讲这部分。
2. **短语 / 邻近查询**：要真正回答 `"Stanford University"` 这种查询，单靠"两个词是否都出现"不够，还要知道它们在文档里**相邻**。这要求 postings 里记录**位置信息**，也是 Ch 2 的内容。
3. **累积词项证据**：Boolean 模型只看 term 有没有出现。但 `Brutus` 在文档里出现一次和二十次，通常含义不同。要利用这点，索引需要再存一个 **term frequency (tf)**——Ch 6 开始讨论。
4. **按相关度排序**：Boolean 返回一个集合。但用户要的是一个"最像答案的文档排最前"的列表。这需要一个**评分函数** $s(d, q)$——Ch 6 到 Ch 11 围绕这个函数的不同设计展开。

这四件事合起来，就是"从 Boolean 检索到现代排序检索"的完整路线。

## 本章总结

- `information need`、`query`、`relevant documents` 和系统实际返回的集合，必须先分开。
- 倒排索引把访问方向从 `document -> terms` 改写成 `term -> documents`；这一重排本身，就是检索系统开始可扩展的起点。
- Boolean 查询的执行核心很朴素：按 `docID` 排序的 postings list 做线性交集，再用 `df` 调整执行顺序。
- Boolean retrieval 能清楚回答“是否满足条件”，却说不出“哪个结果更像答案”。
- 排序检索就从这里接上来。

## 1.5 参考文献

### 本章主源

- Manning, C.D.; Raghavan, P.; Schütze, H. *Introduction to Information Retrieval*, Chapter 1: "Boolean Retrieval"（本章改编自该书的论证结构与具体例子）, online edition, Cambridge University Press, 2009.

### IR 学科起源

- Bush, V. "As We May Think." *The Atlantic Monthly*, July 1945.（memex 愿景——IR 领域的 foundational vision）
- Mooers, C.N. "Coding, Information Retrieval, and the Rapid Selector." 1950.（"Information Retrieval" 一词首次出现）
- Cleverdon, C. "The Significance of the Cranfield Tests on Index Languages." 1991.（早期实证评测史）

### 倒排索引与数据结构

- Witten, I.H.; Moffat, A.; Bell, T.C. *Managing Gigabytes: Compressing and Indexing Documents and Images*, 2nd ed. Morgan Kaufmann, 1999.（倒排索引和其它数据结构的空间/时间对比经典）
- Zobel, J.; Moffat, A. "Inverted Files for Text Search Engines." *ACM Computing Surveys*, 2006.（更简洁现代的综述）

### Boolean vs free text

- Lee, J.J.; Fox, E.A. "Experimental Comparison of Schemes for Interpreting Boolean Queries." 1988.（AND/OR 粒度 tradeoff 的早期论述）
- Turtle, H. "Natural Language vs. Boolean Query Evaluation: A Comparison of Retrieval Performance." *SIGIR*, 1994.（Westlaw 上 free text 对 Boolean 的对照实验）

### 正则与字符串搜索

- Friedl, J.E.F. *Mastering Regular Expressions*, 3rd ed. O'Reilly, 2006.（regex 实战）
- Hopcroft, J.E.; Motwani, R.; Ullman, J.D. *Introduction to Automata Theory, Languages, and Computation*, 2nd ed. Addison-Wesley, 2000.（regex 的理论基础——自动机视角）
