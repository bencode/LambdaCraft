# ir-rag-labs/

可运行的 IR / RAG 实验集合，按 series 组织。配套站点上 `/ir-rag/` 各章节。

## 结构

```
ir-rag-labs/
├── 00-lexical/
│   ├── pyproject.toml      # uv-managed，独立 Python 环境
│   ├── notebooks/          # .ipynb 源文件，本地用 JupyterLab 调试
│   ├── src/                # 教学版库代码
│   ├── data/               # 演示用小语料
│   └── build.toml          # 标记每个 notebook 的 tier
├── 01-bm25/
├── 02-language-models/
├── 03-learning-to-rank/
├── 04-dense-retrieval/     # Tier 3 only
├── 05-reranking/           # Tier 3 only
└── 06-rag/                 # Tier 3 only
```

## 每个 lab 子项目的约定

- **独立 pyproject**：用 [uv](https://docs.astral.sh/uv/) 管理 Python 依赖，互不干扰
- **小数据集**：演示用语料控制在 < 1000 文档（Pyodide 要在浏览器里跑）
- **build.toml**：声明每个 notebook 的目标 tier
- **README**：每个 lab 一个，说明目标 / 怎么跑 / 对应的文章链接

## build.toml 示例

```toml
[notebooks."01-bm25-from-scratch"]
tier = 1                              # 嵌 LambdaCraft 站点 PyodideRunner
shows-in-article = "01-bm25/02-bim-and-derivation"
embed-cells = ["all"]                 # 全部 cell 嵌入

[notebooks."02-k1-b-sensitivity"]
tier = 1
shows-in-article = "01-bm25/03-in-practice"
embed-cells = ["3-7"]                 # 只嵌选定 cell

[notebooks."04-bert-reranker"]
tier = 3                              # GPU 必需，外链
hosting = ["colab", "self-jupyterlab"]
shows-in-article = "05-reranking/02-bert-cross-encoder"
```

## 双向 dataflow

```
notebook (源)              ← 真实可调试，本地 JupyterLab
   ↓
build script
   ↓                       ← 按 build.toml 派生
   ├→ MDX 嵌入 cells       (Tier 1: PyodideRunner)
   └→ Colab/JupyterLab     (Tier 3: 外链)
```

build script 留待 Phase 0b 之后写（先有 PyodideRunner 组件，再写派生 pipeline）。

## 当前状态

🔴 **待启动**——Phase 0a 只建 README 占位，第一个真实 lab 在 Phase 1 BM25 时启动。
