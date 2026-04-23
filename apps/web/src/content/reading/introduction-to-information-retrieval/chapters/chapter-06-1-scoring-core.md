# Chapter 6-1 从词项权重到向量空间打分

Chapter 1 解决的是匹配是否成立，Chapter 2 解决的是词项（term）和倒排列表（postings list）怎样被组织起来。但一般搜索系统最终还要回答另一个更难的问题：即使多个文档都命中了查询，它们为什么应该按某种顺序排出来。

这一章处理的是这层从“集合检索”到“排序检索”的过渡。实际系统里，用户几乎总是在要求排序，而不是只要一个命中集合。系统不再只关心某个词是否出现，而要开始累积词项在文档中的证据强度：它在文档里出现了多少次，这个词在整个语料里有多常见，查询和文档在整体上有多接近。把这些判断连起来，才会得到向量空间模型（vector space model）和余弦相似度（cosine similarity）这第一套完整的排序机制。

这一章分成五个部分：

1. 为什么 Boolean retrieval 无法自然回答“谁更像答案”。
2. 为什么词频（term frequency）提供了第一层强度信号。
3. 为什么仅有词频还不够，必须再引入文档频率（document frequency）和逆文档频率（inverse document frequency）。
4. 为什么文档和查询都要表示成同一个向量空间里的对象。
5. 为什么 tf-idf + cosine 已经是一套完整打分机制，但仍然留下几个需要继续解决的问题。

## 全局记号

$$
\begin{aligned}
D &: \text{文档集合} \\
T &: \text{词项集合} \\
N &= |D| \\
q &: \text{查询} \\
tf(t, d) &: \text{词项 } t \text{ 在文档 } d \text{ 中的出现次数} \\
df(t) &: \text{包含词项 } t \text{ 的文档数} \\
idf(t) &= \log \frac{N}{df(t)} \\
w(t, d) &: \text{词项 } t \text{ 在文档 } d \text{ 中的权重} \\
\vec{d}, \vec{q} &: \text{文档向量与查询向量}
\end{aligned}
$$

统一结构约定：

```python
from collections import Counter
from dataclasses import dataclass
from math import log, sqrt
import re


SparseVector = dict[str, float]


@dataclass(frozen=True)
class Document:
    doc_id: int
    text: str
```

## 6.1 为什么匹配成立仍不足以排序

在布尔检索（Boolean retrieval）里，系统返回的是一个集合：

$$
S(q) = \{ d \in D \mid d \text{ satisfies } q \}
$$

它能回答的是：

```text
这个 document 是否满足 query？
```

但当用户输入的是自由文本查询（free text query）时，系统更常需要回答的是：

```text
这些 document 里，哪个更像答案？
```

这要求系统至少定义一个评分函数：

$$
s : D \times Q \to \mathbb{R}
$$

然后按照 $s(d, q)$ 排序，而不是只给出一个命中集合。

这个变化看上去很小，实质上却改变了整个检索问题的结构。布尔模型把匹配关系写成离散判断，而排序模型要开始累积多种强弱不一的证据：

```text
query
  |
  v
matched terms
  |
  v
term weights
  |
  v
document score
  |
  v
ranked results
```

一个简单例子就能看出差异。假设查询是：

```text
digital camera battery
```

而三个文档都包含这三个词：

```text
d1: digital camera battery battery battery charger
d2: digital camera battery review
d3: digital battery market camera history
```

如果只看 presence / absence，这三篇文档都会被认为“命中”；但读者几乎不会认为它们同样相关。这里真正缺失的是强度信息。下一节引入的词频，就是这种强度信息的第一层形式。

## 6.2 词频不是相关性，但它提供了第一层强度信号

### 从出现与否到出现多少次

最直接的想法是：一个词在文档中出现得更多，通常意味着这个词和文档主题关系更紧。于是，系统自然会从二值出现关系转向词频（term frequency）：

$$
tf(t, d) = \text{count of } t \text{ in } d
$$

对自由文本查询，一个最基础的打分方式就是把各个查询词在文档中的词频加起来：

$$
\mathrm{score}_{tf}(q, d) = \sum_{t \in q} tf(t, d)
$$

这一步的直觉很直接：如果查询词在文档里反复出现，这通常比只出现一次更像“和查询相关”。

同时，这也意味着系统开始采用词袋模型（bag of words model）：它保留每个词出现的次数，但忽略词序。于是：

```text
Mary is quicker than John
John is quicker than Mary
```

在这一层表示里是一样的。

### 词频怎样进入代码

先把词频统计写出来：

```python
TOKEN_PATTERN = re.compile(r"[A-Za-z0-9]+(?:['-][A-Za-z0-9]+)*")


def tokenize(text: str) -> list[str]:
    return [token.lower() for token in TOKEN_PATTERN.findall(text)]


def term_frequencies(text: str) -> Counter[str]:
    return Counter(tokenize(text))
```

把上一节的三个文档转换成词频表示，大致会得到：

```text
d1:
  digital: 1
  camera: 1
  battery: 3
  charger: 1

d2:
  digital: 1
  camera: 1
  battery: 1
  review: 1

d3:
  digital: 1
  camera: 1
  battery: 1
  market: 1
  history: 1
```

于是 `d1` 会比 `d2` 和 `d3` 获得更高的原始词频分数。这已经比布尔命中更接近排序问题，但它仍然远远不够。

### raw tf 的边界

词频提供了第一层强度信号，但它不能直接等同于相关性。

第一，某个词在文档里出现 20 次，并不一定真的比出现 1 次重要 20 倍。第二，词频完全没有区分“这个词在整个语料里是不是到处都在出现”。如果查询词本身是一个非常常见的词，那么高词频并不一定意味着强区分力。

`tf` 只说明了”这个词在当前文档里有多突出”，至于”这个词对区分文档有没有用”，它给不了答案。

## 6.3 从 df 到 idf：常见词为什么要降权

### 为什么不用 collection frequency

如果只看词频，`camera` 和 `battery` 在很多电子产品文档里都会显得很重要；但真正决定区分力的是它出现在多少篇不同的文档里，而不是它在整个语料里一共出现了多少次。

因此，这里更关键的统计量是文档频率（document frequency）：

$$
df(t) = |\{ d \in D \mid t \in d \}|
$$

它统计的是：有多少篇文档至少出现过词项 $t$。

这一点很关键，因为检索系统最终要区分的是“哪些文档更相关”，而不是“整个语料里总共出现了多少个这个词”。对排序来说，文档级分布通常比纯 collection frequency 更有意义。

### inverse document frequency 的作用

为了降低高频通用词的影响，引入逆文档频率（inverse document frequency）：

$$
idf(t) = \log \frac{N}{df(t)}
$$

这里 $N$ 是文档总数。于是：

- 稀有词：`df(t)` 小，`idf(t)` 高
- 常见词：`df(t)` 大，`idf(t)` 低
- 若一个词出现在所有文档里，$df(t) = N$，则 $idf(t) = 0$

这正好符合排序直觉：越少见的词，通常越能帮助系统识别出真正相关的少数文档。这里讨论的是区分力，而不是词本身“重要不重要”。

反过来，如果查询里的某个词在每篇文档里都出现，那么它的 `idf` 就会变成 `0`。公式在这里给出的恰恰是正确答案：这个词对当前语料已经没有区分文档的能力了。

可以把这种差别直观写成：

```text
term         df      idf intuition
camera       high    low   常见词，区分力弱
battery      medium  mid   有一定区分力
lithium-ion  low     high  稀有词，区分力强
```

### tf-idf 把局部强度和全局区分力合在一起

一旦把 `tf` 和 `idf` 组合起来，就得到这章最核心的基础权重：

$$
tf\text{-}idf(t, d) = tf(t, d) \cdot idf(t)
$$

它编码的是一个非常具体的判断：

- 某个词在当前文档里出现得多
- 同时它在全体文档里又不是到处都出现

那么这个词就应该对该文档的相关性贡献更大的权重。

下面这段代码把 `df`、`idf` 和 `tf-idf` 连接起来。三层对象各有角色：`tf` 是文档内统计，`df` 是文档级全局统计，`tf-idf` 则是最终用于打分的词项权重。

```python
def document_frequencies(docs: list[Document]) -> Counter[str]:
    return Counter(
        term
        for doc in docs
        for term in set(tokenize(doc.text))
    )


def inverse_document_frequency(df: int, total_docs: int) -> float:
    return log(total_docs / df) if df else 0.0


def tf_idf_vector(
    text: str,
    dfs: Counter[str],
    total_docs: int,
) -> SparseVector:
    return {
        term: tf * inverse_document_frequency(dfs[term], total_docs)
        for term, tf in term_frequencies(text).items()
        if term in dfs
    }
```

这样一来，文档就是一个带权重的稀疏向量。

前面那组三文档例子只适合说明 raw `tf` 的直觉，不适合直接拿来演示 `tf-idf`。在那个极小语料里，查询里的三个词出现在每篇文档中，因此它们的 `idf` 都会退化为 `0`。

## 6.4 文档和查询为什么要进入同一个向量空间

### 文档向量与查询向量

有了 `tf-idf` 之后，每篇文档都可以被看成一个向量：

$$
\vec{d} = (w(t_1, d), w(t_2, d), \dots, w(t_M, d))
$$

其中每个维度对应词典里的一个词项。没有出现的词，其权重就是 `0`。

真正让这套方法成立的关键一步，是把查询也写成同一个向量空间里的对象：

$$
\vec{q} = (w(t_1, q), w(t_2, q), \dots, w(t_M, q))
$$

这样一来，排序问题就变成了一个统一的问题：

```text
给定 query vector q
    |
    v
比较 q 与每个 document vector d 的接近程度
    |
    v
按相似度排序
```

这也是向量空间模型（vector space model）的真正价值：它把“文档表示”和“查询表示”放进了同一个数学对象里。

### 从距离到角度：cosine similarity 的由来

有了查询向量和文档向量，下一步自然是：怎么衡量两个向量的相似程度。

对两个向量，最直接的度量是它们之间的**欧氏距离**（Euclidean distance）：

$$
\|\vec{q} - \vec{d}\| = \sqrt{\sum_{t \in T} (q_t - d_t)^2}
$$

距离越小，两个向量越接近。这符合几何直觉，也是 `k-NN`、聚类等一大类方法默认使用的度量。

但欧氏距离在稀疏高维 + 文档长度差异大的场景里，有一个不容易绕开的问题：**距离被向量长度主导**。如果两篇文档的主题分布几乎一样，只是其中一篇更长（词更多、权重总量更大），两个向量在几何位置上就会相距很远——哪怕"方向"几乎一致。排序结果会被长度差主导，而不是被主题差主导。

要绕开这个问题，有两种等价思路：

- **方式 A**：先把向量归一化到单位长度，再计算距离
- **方式 B**：完全忽略长度，只比较**方向**——也就是两个向量之间的夹角

两条路会汇合到同一个量：夹角的余弦。由向量内积的几何定义：

$$
\vec{q} \cdot \vec{d} = \|\vec{q}\| \cdot \|\vec{d}\| \cdot \cos\theta
$$

把长度项移到左边，就得到**余弦相似度**（cosine similarity）：

$$
\mathrm{score}(q, d) = \cos(\vec{q}, \vec{d}) = \frac{\vec{q} \cdot \vec{d}}{\|\vec{q}\| \cdot \|\vec{d}\|}
$$

公式里的点积（dot product）此时作为分子**自然出现**。它不是"一开始就该想到的自然度量"，而是夹角余弦展开后的中间形式。分母是两个向量的欧氏长度，作用正好是把向量归一化到单位长度——方式 A 和方式 B 在这里合流。

```text
q ---->

d1 ---->
  cosine close to 1

d2 ----------->
  same direction, longer length

d3
   \
    \ 
     >
  larger angle, lower cosine
```

还可以补一个几何事实：如果把 $\vec{q}$ 和 $\vec{d}$ 都归一化到单位向量，那么**按欧氏距离排序**与**按余弦相似度排序**给出完全相同的结果。距离路和角度路在归一化之后给出同一种排序，余弦就是这种等价关系的公式形式。

### 向量打分怎样进入代码

向量打分的部件可以直接写成下面这样。这里最重要的两个结构判断是：点积只发生在共同词项上，而 cosine 的归一化项负责抑制纯长度带来的放大效应。

```python
def dot(left: SparseVector, right: SparseVector) -> float:
    # 点积只在共同出现的维度上累积乘积。
    shared_terms = left.keys() & right.keys()
    return sum(left[term] * right[term] for term in shared_terms)


def l2_norm(vector: SparseVector) -> float:
    return sqrt(sum(weight * weight for weight in vector.values()))


def cosine_score(query: SparseVector, document: SparseVector) -> float:
    # 归一化的作用是减少文档长度本身对分数的放大。
    query_norm = l2_norm(query)
    document_norm = l2_norm(document)
    if query_norm == 0.0 or document_norm == 0.0:
        return 0.0
    return dot(query, document) / (query_norm * document_norm)
```

查询和文档不一定非要使用完全相同的权重函数。很多系统会对查询向量和文档向量采用不同的加权方式。但在这一章里，先把两者都放进同一个稀疏向量框架，就已经足够建立 ranking 的第一套完整结构。

## 6.5 从权重到排序：一个基础向量打分器

到这一步，打分系统的形状已经很清楚了：

1. 统计语料上的 `df`
2. 为每篇文档构造 `tf-idf` 向量
3. 为查询构造查询向量
4. 计算每篇文档与查询的 cosine score
5. 按分数排序

```text
documents
   |
   v
df statistics
   |
   +--> document tf-idf vectors
   |
query ------> query vector
                |
                v
         cosine score with each document
                |
                v
            ranked results
```

把这些部件连起来，就是一个最基础的向量打分器：

```python
def rank_documents(query: str, docs: list[Document]) -> list[tuple[int, float]]:
    total_docs = len(docs)
    dfs = document_frequencies(docs)
    query_weights = tf_idf_vector(query, dfs, total_docs)
    doc_vectors = [
        (doc.doc_id, tf_idf_vector(doc.text, dfs, total_docs))
        for doc in docs
    ]

    scored = [
        (doc_id, score)
        for doc_id, document_weights in doc_vectors
        if (score := cosine_score(query_weights, document_weights)) > 0.0
    ]
    return sorted(scored, key=lambda item: item[1], reverse=True)
```

真实系统不会像这里一样对每次查询都重建全部文档向量。Chapter 1 和 Chapter 2 的倒排索引，正是后续高效计算这些分数的基础。但在概念层面上，向量空间模型已经把“文档表示”“查询表示”“相似度计算”“排序输出”这几步统一到了同一个框架里。

## 6.6 tf-idf + cosine 还留下什么问题

这套排序机制完整且可解释，但它仍然留下几个明显问题。

raw `tf` 的增长通常过快：一个词在文档里出现 20 次，并不意味着它真的比出现 2 次重要 10 倍。即使有了 cosine normalization，文档长度带来的偏差也未必被处理得足够好；长文档可能只是更冗长，也可能真的覆盖了更多不同主题。此外，查询向量和文档向量的权重函数并不一定应该完全相同。

这些问题会自然把系统推进到后续的变体权重和长度归一化，例如：

```text
raw tf
  -> sublinear tf
  -> alternative normalization
  -> stronger probabilistic scoring
```

tf-idf + cosine 应当理解为“排序检索的第一套完整语言”，而不是最终形式。工程上更常见的做法，是在保留词频、稀有度和长度补偿这些核心直觉的前提下，继续换用更稳健的打分形式；后续出现的 BM25 正是沿着这条思路继续改进的。

## 本章总结

- Boolean retrieval 返回的是命中集合；排序检索则要求系统为每篇文档定义可比较的分数。
- `tf` 提供文档内的强度信号，但它本身并不表达区分力。
- `idf` 把全局分布信息纳入权重，于是 `tf-idf` 同时编码了“这个词在文档里有多突出”和“这个词在语料里有多稀有”。
- 向量空间模型把文档和查询都表示成同一空间中的稀疏向量。
- 然后才轮到 cosine similarity、长度归一化，以及后面的 BM25。

## 6.7 参考文献

- Manning, Christopher D.; Raghavan, Prabhakar; Schutze, Hinrich. *Introduction to Information Retrieval*, Chapter 6: “Scoring, term weighting and the vector space model”, Sections 6.2-6.4, online edition, 2009.
