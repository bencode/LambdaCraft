# Chapter 11-1 从概率排序原则到 BM25

Chapter 6-1 已经把排序检索写成了一个向量空间问题：文档和查询被表示成带权重的稀疏向量，系统按照 `tf-idf + cosine` 对它们的接近程度排序。但这条路线回答的实际是“哪个文档和查询更相似”。

这一章换一个角度看同一个问题。概率排序（probabilistic retrieval）直接面向的问题是：面对查询 `q`，文档 `d` 为真的相关答案的概率有多大。Probability Ranking Principle（PRP）、Binary Independence Model（BIM）和 Okapi BM25 就沿着这条思路，把“排序目标”“词项权重”“词频饱和”和“长度补偿”整合成一套面向相关概率的打分框架。

这一章围绕五个问题展开：

1. 为什么还要从向量空间继续走向概率排序。
2. Probability Ranking Principle 到底把排序目标改成了什么。
3. Binary Independence Model 怎样把这个目标落到词项级打分上。
4. 为什么二值出现关系不足以处理全文检索。
5. BM25 怎样把 `idf`、词频和文档长度重新组织成一个实用的排序函数。

## 全局记号

$$
\begin{aligned}
D &: \text{文档集合} \\
N &= |D| \\
q &: \text{查询} \\
d &: \text{某篇文档} \\
R_{d,q} &\in \{0,1\}: \text{文档 } d \text{ 对查询 } q \text{ 是否相关} \\
tf(t, d) &: \text{词项 } t \text{ 在文档 } d \text{ 中的出现次数} \\
df(t) &: \text{包含词项 } t \text{ 的文档数} \\
|d| &: \text{文档 } d \text{ 的长度} \\
avgdl &: \text{语料中的平均文档长度} \\
k_1, b &: \text{BM25 的两个主参数}
\end{aligned}
$$

统一结构约定：

```python
from collections import Counter
from dataclasses import dataclass
from math import log
import re


TOKEN_PATTERN = re.compile(r"[A-Za-z0-9]+(?:['-][A-Za-z0-9]+)*")


@dataclass(frozen=True)
class Document:
    doc_id: int
    text: str


@dataclass(frozen=True)
class CorpusStats:
    total_docs: int
    doc_freqs: Counter[str]
    avg_doc_len: float
```

## 11.1 为什么还要从向量空间继续走向概率排序

向量空间模型已经给出了一套完整排序机制，但它的语言是“相似度”。文档和查询都被写成向量，然后系统计算它们的接近程度。这个框架很强，却没有正面回答排序问题的核心：

```text
面对查询 q，哪篇文档最可能相关？
```

概率排序把这个问题直接写成目标：

```text
query q
  |
  v
estimate P(relevant | d, q) for each document
  |
  v
rank documents by that probability
```

向量空间模型把排序写成几何相似度，概率排序把排序写成相关概率。两条路线都在利用词频、稀有度和长度信息，但它们对“分数”的解释并不一样。

BM25 用相关概率——而不是相似度——来定义分数，这种建模思路后来成了搜索工程里最常用的基线。

## 11.2 PRP：排序目标从相似度变成相关概率

### 排序目标应该是相关概率

Probability Ranking Principle（PRP）：如果系统能尽可能准确地估计每篇文档对查询的相关概率，把文档按这个概率从高到低排序，就是当前信息条件下最好的排序方式。

对某个查询 `q` 和文档 `d`，令

$$
P(R_{d,q} = 1 \mid d, q)
$$

表示文档相关的概率。那么最直接的排序规则就是：

$$
\text{rank documents by decreasing } P(R_{d,q} = 1 \mid d, q)
$$

在最简单的 `1/0 loss` 条件下，PRP 还有一个等价判断：如果相关的概率大于不相关的概率，这篇文档就应该被返回。

$$
d \text{ is returned iff } P(R_{d,q}=1 \mid d, q) > P(R_{d,q}=0 \mid d, q)
$$

Chapter 6-1 里的 cosine score 是一个相似度分数；PRP 则给出了一个更直接的目标函数语义：系统想优化的是相关概率，不是几何距离。

### 概率比值比原概率更适合推导

推导 ranking function 时，实践上通常转向相关与不相关的 odds：

$$
\frac{P(R_{d,q}=1 \mid d, q)}{P(R_{d,q}=0 \mid d, q)}
$$

因为这个比值和相关概率单调对应，所以它给出的排序和原始概率完全一致。对数之后，乘积会变成求和，后面的词项权重也会更容易解释。

PRP 规定排序目标，但不提供概率的估计方法。下一节的 Binary Independence Model 从这里接手，给出第一套可操作的估计框架。

## 11.3 Binary Independence Model：把相关概率拆成词项证据

### 二值表示与独立性假设

Binary Independence Model（BIM）先做了两个强约束。

第一，文档和查询都表示成二值词项向量：词项只区分“出现”或“不出现”，不记录出现多少次。若词项 `t` 在文档 `d` 中出现，则对应维度 `x_t = 1`；否则 `x_t = 0`。

第二，模型假设词项之间条件独立。也就是说，在给定查询和相关性状态之后，一个词是否出现，不依赖其他词是否出现。

假设很强，收益也是直接的：原本难以估计的整篇文档概率，开始能够被拆成逐词项的因子。

$$
\mathbf{x} = (x_1, x_2, \dots, x_M), \qquad x_t \in \{0,1\}
$$

```text
document d
  -> binary incidence vector x

query q
  -> binary incidence vector q

score(d, q)
  -> combine evidence from matched query terms
```

### 从 odds 推出 RSV

BIM 最终关心的对象是相关 odds：

$$
O(R \mid \mathbf{x}, \mathbf{q})
=
\frac{P(R=1 \mid \mathbf{x}, \mathbf{q})}{P(R=0 \mid \mathbf{x}, \mathbf{q})}
$$

利用 Bayes 公式和条件独立假设，可以把这个对象拆成逐词项的乘积，再经过两步处理——只保留查询词项、对乘积取对数——得到 Retrieval Status Value（RSV）：

$$
RSV(d, q) = \sum_{t: x_t = q_t = 1} c_t
$$

其中每个词项权重都是一个 log-odds ratio：

$$
c_t
=
\log \frac{p_t(1-u_t)}{u_t(1-p_t)}
$$

这里：

$$
p_t = P(x_t = 1 \mid R = 1, q), \qquad
u_t = P(x_t = 1 \mid R = 0, q)
$$

`p_t` 表示词项在相关文档中出现的概率，`u_t` 表示词项在不相关文档中出现的概率。一个词若更倾向出现在相关文档里，它的 `c_t` 就更大。

文档命中的每个查询词项，都在累积一份”支持相关”的 log-odds 证据。

### BIM 在无反馈时怎样退化到 idf

如果没有 relevance feedback，`p_t` 很难直接估计。在”相关文档占整个语料很小一部分”的近似下，不相关文档上的统计量可以用全语料统计代替：

$$
u_t \approx \frac{df(t)}{N}
$$

进一步得到：

$$
\log \frac{1-u_t}{u_t}
=
\log \frac{N-df(t)}{df(t)}
\approx
\log \frac{N}{df(t)}
$$

这就把 BIM 和 Chapter 6-1 里的 `idf` 直接接上了。即使不做 relevance feedback，概率模型也会自然推出一个近似的 `idf` 解释：越少见的词项，越能帮助区分相关和不相关文档。

下面这段代码把这种“二值出现 + idf 近似”的 RSV 写成一个最基础的实现。它不是 BM25，只是让 BIM 的打分形状先落地。

```python
def tokenize(text: str) -> list[str]:
    return [token.lower() for token in TOKEN_PATTERN.findall(text)]


def term_frequencies(text: str) -> Counter[str]:
    return Counter(tokenize(text))


def document_frequencies(docs: list[Document]) -> Counter[str]:
    return Counter(
        term
        for doc in docs
        for term in set(tokenize(doc.text))
    )


def build_corpus_stats(docs: list[Document]) -> CorpusStats:
    doc_term_counts = [term_frequencies(doc.text) for doc in docs]
    return CorpusStats(
        total_docs=len(docs),
        doc_freqs=document_frequencies(docs),
        avg_doc_len=sum(counts.total() for counts in doc_term_counts) / len(doc_term_counts),
    )


def approximate_bim_score(query: str, doc: Document, stats: CorpusStats) -> float:
    query_terms = set(tokenize(query))
    document_terms = set(tokenize(doc.text))
    # BIM 在这一层只看查询词是否出现；命中的词项累积 idf 式证据。
    return sum(log(stats.total_docs / stats.doc_freqs[term]) for term in query_terms & document_terms)
```

## 11.4 二值独立模型还留下什么问题

### 二值出现关系丢掉了词频

BIM 的第一层边界：它只记录词项是否出现，不记录出现多少次。

于是，下面两篇文档在 BIM 里会被当成一样：

```text
d1: battery camera battery battery charger
d2: battery camera review manual
```

只要 `battery` 和 `camera` 都出现，它们对二值模型来说贡献相同。但全文检索里，词项出现一次和出现五次，通常不应该完全没有区别。

### 文档长度也没有被纳入

第二个问题是文档长度。短摘要、标题和全文页面不是一类对象；如果系统完全忽略长度，那么长文档往往既更容易命中更多词，又更容易包含重复词项。

BIM 最初更适合短记录或长度较一致的文档集合。到了现代全文语料，Chapter 6-1 强调过的两个量必须重新纳入：

- 词频（term frequency）
- 文档长度（document length）

这一步就是 BM25 的出发点。

## 11.5 BM25：把词频和长度重新纳入概率排序

### 从 idf-only RSV 到非二值模型

在 BM25 的建构路径里，最简单的文档分数可以先写成查询词项的 `idf` 求和：

$$
RSV(d, q) = \sum_{t \in q \cap d} \log \frac{N}{df(t)}
$$

这一步和上一节的 BIM 近似是同一方向：词项越稀有，命中它的文档越值得加分。

但 BM25 不止于此。它把 `tf(t,d)` 和文档长度也纳入 scoring function，得到最常用的形式：

$$
\mathrm{BM25}(q,d)=
\sum_{t \in q}
idf(t)\cdot
\frac{tf(t,d)(k_1+1)}
{tf(t,d)+k_1\left(1-b+b\cdot \frac{|d|}{avgdl}\right)}
$$

如果采用带 `0.5` 平滑的 idf 形式，上式里的 `idf(t)` 常写成：

$$
idf(t)=\log \frac{N-df(t)+0.5}{df(t)+0.5}
$$

对 short keyword query，通常不再额外引入 query term frequency 的缩放项；这一章也保持这个版本。

### BM25 的三层结构

BM25 把三个核心判断统一放进同一个词项打分里：

```text
term score
  =
  idf(t)
  x
  tf saturation(tf, k1)
  x
  length normalization(|d|, avgdl, b)
```

第一层是 `idf(t)`。它保留了 BIM 到 Chapter 6-1 这一整条线上都没有丢掉的判断：稀有词更有区分力。

第二层是词频饱和（tf saturation）。`tf` 增大时，词项贡献仍然增大，但增幅会逐步变小。于是，出现 10 次不会比出现 1 次高 10 倍，这比 raw `tf` 更符合检索直觉。

第三层是长度归一化。分母中的

$$
1 - b + b \cdot \frac{|d|}{avgdl}
$$

把文档长度显式纳入模型。若 `|d| > avgdl`，分母变大，词频的收益会被压低；若文档比平均长度更短，这个压制就更弱。

### 参数 `k1` 和 `b` 控制什么

`k_1` 控制的是词频饱和速度：

- `k_1 = 0` 时，模型退化到接近二值出现关系
- `k_1` 越大，词频越接近 raw `tf`

`b` 控制的是长度归一化强度：

- `b = 0` 时，不做长度归一化
- `b = 1` 时，长度补偿完全按 `|d| / avgdl` 生效

没有专门调参时，`k_1` 常取 `1.2` 到 `2` 之间，`b` 常取 `0.75`。下面的代码直接使用这组常见教学值：

$$
k_1 = 1.2, \qquad b = 0.75
$$

## 11.6 一个基础 BM25 scorer

代码里三层分别对应：`idf` 负责区分力，分子里的 `tf` 负责累积强度，分母负责饱和与长度补偿。下面把这条打分函数拆成四段，每段只承担一个职责。

### idf：带 0.5 平滑的稀有度项

先是 `idf` 的定义。采用带 `0.5` 平滑的形式之后，公式里的分子在某些退化情况下可能 `≤ 0`（例如 `df` 比 `N` 还大的边界语料），因此需要一次兜底。

```python
def bm25_idf(df: int, total_docs: int) -> float:
    numerator = total_docs - df + 0.5
    denominator = df + 0.5
    if numerator <= 0.0:
        return 0.0
    return max(0.0, log(numerator / denominator))
```

### 词项打分：三层结构合在一起

这一步是 BM25 的核心。三层判断——`idf` 的区分力、`tf` 的饱和增长、文档长度归一化——全部合并进一个词项分数里。分母中的 `normalization` 同时承担 `k1` 的饱和速度和 `b` 的长度补偿强度。

```python
def bm25_term_score(
    tf: int,
    doc_len: int,
    avg_doc_len: float,
    idf: float,
    k1: float = 1.2,
    b: float = 0.75,
) -> float:
    if tf == 0 or avg_doc_len == 0.0 or idf == 0.0:
        return 0.0

    # 这一项同时承担 tf 饱和和文档长度归一化。
    normalization = k1 * (1 - b + b * (doc_len / avg_doc_len))
    return idf * (tf * (k1 + 1)) / (tf + normalization)
```

### 文档级打分：把查询词项的证据累加

在一个文档内，系统遍历查询词项，把每个词项的 `bm25_term_score` 累加起来。未在语料中出现的词直接跳过，它对排序没有信息量。

```python
def bm25_score(
    query_terms: set[str],
    frequencies: Counter[str],
    stats: CorpusStats,
    k1: float = 1.2,
    b: float = 0.75,
) -> float:
    doc_len = frequencies.total()

    score = 0.0
    for term in query_terms:
        tf = frequencies.get(term, 0)
        df = stats.doc_freqs.get(term, 0)
        if df == 0:
            continue

        idf = bm25_idf(df, stats.total_docs)
        score += bm25_term_score(tf, doc_len, stats.avg_doc_len, idf, k1, b)
    return score
```

### 全流程：统计、打分、排序

最外层把前面的部件接起来：先算出语料统计量，再逐文档打分，最后按分数倒序排列。注意 `doc_term_counts` 被提前物化一次，避免每篇文档的词频在排序里被重复计算。

```python
def rank_bm25(
    query: str,
    docs: list[Document],
    k1: float = 1.2,
    b: float = 0.75,
) -> list[tuple[int, float]]:
    stats = build_corpus_stats(docs)
    query_terms = set(tokenize(query))
    doc_term_counts = [
        (doc.doc_id, term_frequencies(doc.text))
        for doc in docs
    ]
    scored = [
        (doc_id, score)
        for doc_id, frequencies in doc_term_counts
        if (score := bm25_score(query_terms, frequencies, stats, k1=k1, b=b)) > 0.0
    ]
    return sorted(scored, key=lambda item: item[1], reverse=True)
```

用一个很小的语料试一下，就能看到 BM25 和纯词频的区别。这里专门选择了能够让 `idf`、词频和文档长度都发生作用的例子：

```python
docs = [
    Document(1, "mirrorless camera battery battery charger"),
    Document(2, "mirrorless review lens sensor"),
    Document(3, "battery charger safety guide"),
    Document(4, "compact camera photography autofocus tips"),
    Document(5, "garden soil watering plants"),
]


rank_bm25("camera battery", docs)
```

在这个例子里：

- `camera` 和 `battery` 都只出现在一部分文档中，所以 `idf` 不会退化为 `0`
- `battery` 在 `d1` 和 `d3` 的词频不同，于是词频饱和会开始起作用
- `d4` 只命中 `camera`，因此会落后于同时命中两个查询词的文档

## 11.7 BM25 与 tf-idf + cosine 的关系

### 两条路线在解决同一个问题

Chapter 6-1 和这一章并不是互相否定的关系。它们都在回答“为什么这些命中文档不能并列返回”，也都依赖三个核心直觉：

- 词频有信息量
- 常见词应降权
- 文档长度会影响分数

差别在于它们怎样解释这些量。

```text
Chapter 6-1
  -> vector space
  -> tf-idf
  -> cosine similarity

Chapter 11-1
  -> probabilistic ranking
  -> BIM
  -> BM25
```

向量空间路线把文档和查询表示成同一空间中的向量，再用 cosine 衡量方向接近程度。BM25 则把分数写成逐词项累积的相关证据，其中 `idf`、词频饱和和长度补偿都直接作用在每个查询词项上。

### BM25 比“纯词频统计”多解决了什么

一个常见的误解是：”BM25 不就是加了一点修正的词频吗？”这种说法会抹掉模型结构。

BM25 至少同时解决了三件事：

- 它用 `idf` 区分“常见词”和“真正有区分力的词”
- 它让词频增长逐步饱和，而不是线性放大
- 它把文档长度写进同一个词项贡献公式里

所以 BM25 的关键在于它把排序问题从“统计几个词出现了多少次”推进到了“这些词项对相关性的证据有多强”。

这也是它能长期成为检索系统基线模型的原因。

## 本章总结

- PRP 先固定了排序目标：系统应该按文档对查询的相关概率排序，而不是只按几何相似度排序。
- BIM 把这个目标拆成逐词项的 log-odds 证据，并自然导向 `idf` 一类稀有度权重。
- 但二值独立模型不记录词频，也不处理全文长度差异；一旦语料从短记录变成全文页面，这两个缺口就会立刻暴露出来。
- BM25 保留了 `idf` 的区分力判断，同时把词频饱和和长度归一化放进同一个词项贡献函数里。
- `tf-idf + cosine` 和 BM25 共享同一批统计直觉，只是分别属于向量空间和概率排序两条建模路线。

## 11.8 参考文献

- Manning, Christopher D.; Raghavan, Prabhakar; Schutze, Hinrich. *Introduction to Information Retrieval*, Chapter 11: “Probabilistic information retrieval”, Sections 11.2, 11.3, and 11.4.3, online edition, 2009.
