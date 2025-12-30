# 实战：X-to-Book 多 Agent 系统设计

系列：[Agent 上下文工程实践指南](/pages/agent/00-index) | 第 6 篇

上一篇：[05. 评估框架：LLM-as-Judge 实践](/pages/agent/evaluation-framework)

## 引言

前面5篇文章介绍了上下文工程的理论和技术。本文通过 X-to-Book 案例，展示如何将这些技术综合应用到实际系统中。X-to-Book 是一个监控 Twitter 账户并生成日合成书籍的多 Agent 系统，它体现了上下文工程的所有核心原则。

## 系统概述

### 功能

监控指定的 Twitter 账户（如 @elonmusk、@sama 等），抓取每日推文和互动，分析内容和观点演变，生成一本连贯的日合成书籍。

### 输入输出

**输入**：
- Twitter 账户列表
- 监控时间范围（如过去24小时）
- 书籍风格偏好（如技术分析、叙事风格）

**输出**：
- 结构化书籍（章节、小节）
- 每章包含：
  - 核心主题总结
  - 关键观点
  - 推文引用和分析
  - 观点演变时间线
- 格式：Markdown + PDF

### 复杂性

- **数据量**：1,000-5,000 条推文/天（取决于账户）
- **上下文需求**：200K+ tokens（原始数据 + 分析 + 生成）
- **质量要求**：事实准确、叙事连贯、洞察深度

这明显超出单 Agent 的能力范围，需要多 Agent 架构。

## 架构设计

### 选择 Supervisor 模式

回顾第3篇的架构决策树：

```
任务特点：
✓ 上下文 > 100K tokens
✓ 可明确分解为独立子任务（抓取、分析、写作、编辑）
✓ 子任务之间依赖关系清晰（抓取 → 分析 → 写作 → 编辑）
✓ 需要集中式监控和质量控制

→ 使用 Supervisor 模式
```

### Agent 配置

```
       Orchestrator (20K)
      /       |       |      \
Scraper  Analyzer  Writer  Editor
 (20K)    (80K)    (80K)   (60K)
```

**上下文预算分配**：

| Agent | 预算 | 理由 |
|-------|------|------|
| Orchestrator | 20K | 仅需任务协调和结果聚合，不处理原始数据 |
| Scraper | 20K | 工具调用和元数据处理，原始推文直接写文件 |
| Analyzer | 80K | 需要加载推文数据和历史知识图进行分析 |
| Writer | 80K | 需要分析结果 + 写作上下文生成连贯文本 |
| Editor | 60K | 需要完整草稿进行编辑和润色 |

总预算：260K（分布式），而单 Agent 需要 500K+

## 应用的上下文工程技术

### 1. 避免 Lost-in-Middle（第1篇）

**问题**：Analyzer 需要处理数千条推文，中间部分容易被忽略。

**解决方案**：

```typescript
// 不好的做法：将所有推文放在一起
const context = `
Analyze these tweets:
${tweets.join('\n\n')}  // 1000+ tweets, 中间部分会被遗忘
`

// 好的做法：分批处理 + 摘要聚合
async function analyzeTweets(tweets: Tweet[]): Promise<Analysis> {
  const batches = chunkArray(tweets, 50)  // 每批50条
  const batchAnalyses = []

  for (const batch of batches) {
    const analysis = await analyzeBatch(batch)
    batchAnalyses.push(analysis)
  }

  // 聚合分析结果
  return await aggregateAnalyses(batchAnalyses)
}

async function analyzeBatch(tweets: Tweet[]): Promise<BatchAnalysis> {
  const context = `
Analyze this batch of ${tweets.length} tweets:

${tweets.map((t, i) => `Tweet ${i+1}: ${t.text}`).join('\n\n')}

Extract:
- Key themes
- Sentiment
- Notable quotes
`

  return await callLLM(context)
}
```

### 2. 观察掩蔽（第2篇）

**问题**：Scraper 输出的原始推文数据达 100K+ tokens，如果传回 Orchestrator 会爆炸。

**解决方案**：

```typescript
class ScraperAgent {
  async scrape(account: string, timeRange: TimeRange): Promise<ScrapeResult> {
    // 1. 调用 Twitter API 获取推文
    const tweets = await this.fetchTweets(account, timeRange)

    // 2. 保存原始数据到文件
    const dataPath = `/tmp/scraped_data/${account}_${Date.now()}.json`
    await writeFile(dataPath, JSON.stringify(tweets, null, 2))

    // 3. 生成摘要（仅传回 Orchestrator）
    const summary = {
      account,
      count: tweets.length,
      timeRange,
      topTopics: this.extractTopTopics(tweets, 5),
      dataPath  // 文件引用
    }

    return {
      summary,  // 传回 Orchestrator（<1K tokens）
      fullDataPath: dataPath  // Analyzer 直接读取文件
    }
  }

  private extractTopTopics(tweets: Tweet[], limit: number): string[] {
    // 简单的主题提取（基于关键词频率）
    const keywords = tweets.flatMap(t => this.extractKeywords(t.text))
    const frequency = this.countFrequency(keywords)
    return Object.entries(frequency)
      .sort((a, b) => b[1] - a[1])
      .slice(0, limit)
      .map(([keyword]) => keyword)
  }
}
```

Orchestrator 看到的：

```json
{
  "summary": {
    "account": "@elonmusk",
    "count": 1234,
    "timeRange": "2025-01-01 to 2025-01-02",
    "topTopics": ["AI safety", "SpaceX", "Tesla", "X platform", "Neuralink"],
    "dataPath": "/tmp/scraped_data/elonmusk_1234567890.json"
  }
}
```

### 3. 锚定迭代总结（第2篇）

**问题**：Writer 生成多个章节后，需要压缩早期章节以腾出空间。

**解决方案**：

```typescript
type ChapterSummary = {
  // 锚点部分（不压缩）
  chapterNumber: number
  title: string
  keyPoints: string[]
  quotes: string[]

  // 内容部分（可压缩）
  fullText: string
  analysis: string
}

class WriterAgent {
  private chapters: ChapterSummary[] = []

  async writeChapter(topic: string, analysis: Analysis): Promise<string> {
    // 1. 生成章节
    const chapter = await this.generateChapter(topic, analysis)

    // 2. 保存锚点和全文
    this.chapters.push({
      chapterNumber: this.chapters.length + 1,
      title: chapter.title,
      keyPoints: chapter.keyPoints,
      quotes: chapter.quotes,
      fullText: chapter.text,
      analysis: chapter.analysis
    })

    // 3. 检查上下文预算
    if (this.getCurrentTokens() > this.budget * 0.75) {
      await this.compressOldChapters()
    }

    return chapter.text
  }

  private async compressOldChapters() {
    // 仅压缩 fullText 和 analysis，保留锚点
    for (let i = 0; i < this.chapters.length - 2; i++) {  // 保留最近2章
      const chapter = this.chapters[i]

      if (chapter.fullText.length > 500) {
        chapter.fullText = await this.summarize(chapter.fullText, { maxLength: 200 })
        chapter.analysis = ""  // 完全删除旧分析
      }
    }
  }

  private buildContext(): string {
    return `
<previous_chapters>
${this.chapters.map(ch => `
<chapter number="${ch.chapterNumber}">
<title>${ch.title}</title>
<key_points>
${ch.keyPoints.map(p => `- ${p}`).join('\n')}
</key_points>
<quotes>
${ch.quotes.map(q => `- "${q}"`).join('\n')}
</quotes>
<summary>${ch.fullText}</summary>
</chapter>
`).join('\n')}
</previous_chapters>
`
  }
}
```

锚点（key_points、quotes）永远保留，确保一致性。

### 4. 时间知识图（第4篇）

**问题**：需要追踪人物观点的演变，识别立场变化。

**解决方案**：

```typescript
type Stance = {
  topic: string
  position: string  // "supportive" | "neutral" | "critical"
  evidence: string
  confidence: number
}

type TemporalProfile = {
  entity: string
  timeline: Array<{
    timestamp: Date
    stances: Stance[]
  }>
}

class AnalyzerAgent {
  private tkg: TemporalKnowledgeGraphManager

  async analyzeTweets(tweets: Tweet[]): Promise<Analysis> {
    // 1. 提取当前立场
    const currentStances = await this.extractStances(tweets)

    // 2. 加载历史立场
    const historicalProfile = await this.tkg.getEntity(tweets[0].author)

    // 3. 检测变化
    const changes = this.detectStanceChanges(
      historicalProfile?.timeline || [],
      currentStances
    )

    // 4. 更新知识图
    await this.tkg.addSnapshot(tweets[0].author, {
      timestamp: new Date(),
      stances: currentStances
    })

    return {
      currentStances,
      changes,
      narrative: this.generateNarrative(changes)
    }
  }

  private async extractStances(tweets: Tweet[]): Promise<Stance[]> {
    const prompt = `
Analyze these tweets and extract the author's stances on key topics:

${tweets.map(t => `- ${t.text}`).join('\n')}

For each topic mentioned:
1. Topic name
2. Position (supportive/neutral/critical)
3. Key evidence (quote from tweet)
4. Confidence (0-1)

Format as JSON array.
`

    return await callLLM(prompt)
  }

  private detectStanceChanges(
    history: TemporalProfile['timeline'],
    current: Stance[]
  ): StanceChange[] {
    const changes: StanceChange[] = []

    for (const currentStance of current) {
      // 找到相同主题的历史立场
      const historical = this.findHistoricalStance(history, currentStance.topic)

      if (historical && historical.position !== currentStance.position) {
        changes.push({
          topic: currentStance.topic,
          from: historical.position,
          to: currentStance.position,
          evidence: {
            before: historical.evidence,
            after: currentStance.evidence
          }
        })
      }
    }

    return changes
  }

  private generateNarrative(changes: StanceChange[]): string {
    if (changes.length === 0) {
      return "No significant stance changes detected."
    }

    return changes.map(change => `
**${change.topic}**: Shifted from ${change.from} to ${change.to}
- Previously: "${change.evidence.before}"
- Now: "${change.evidence.after}"
`).join('\n\n')
  }
}
```

### 5. 整合工具（第4篇）

**问题**：15+ 个细粒度的 Twitter API 工具会占用大量上下文。

**解决方案**：

```typescript
// 不好的做法：15个细粒度工具
const tools = [
  "get_user_profile",
  "get_user_tweets",
  "get_tweet_by_id",
  "get_tweet_replies",
  "get_tweet_likes",
  "get_tweet_retweets",
  "get_user_followers",
  "get_user_following",
  "search_tweets",
  "get_trends",
  // ... 5 more
]

// 好的做法：3个整合工具
const tools = [
  {
    name: "scrape_user_activity",
    description: `
Scrapes all activity for a Twitter user within a time range.

When to use:
- When you need comprehensive data about a user's Twitter activity

Returns:
- tweets: Array of tweet objects
- interactions: Replies, likes, retweets
- profile: User profile snapshot
- metadata: Scraping stats

Automatically handles:
- Rate limiting
- Pagination
- Data deduplication
`,
    parameters: {
      username: { type: "string" },
      timeRange: {
        type: "object",
        properties: {
          start: { type: "string", format: "date-time" },
          end: { type: "string", format: "date-time" }
        }
      },
      includeReplies: { type: "boolean", default: true },
      includeRetweets: { type: "boolean", default: false }
    }
  },
  {
    name: "analyze_content",
    description: "Analyzes tweet content for themes, sentiment, and stances...",
    // ...
  },
  {
    name: "generate_book",
    description: "Generates book chapters from analyzed content...",
    // ...
  }
]
```

工具定义从 3000+ tokens 降至 1000 tokens。

### 6. 5维评估框架（第5篇）

**问题**：如何评估生成书籍的质量？

**解决方案**：

```typescript
type BookEvaluation = {
  accuracy: number       // 0-10: 事实准确性
  completeness: number   // 0-10: 覆盖完整性
  coherence: number      // 0-10: 叙事连贯性
  insight: number        // 0-10: 洞察深度
  style: number          // 0-10: 写作质量
  overall: number        // 加权平均
}

const evaluator = new EvaluatorAgent({
  method: "rubric",
  model: "claude-sonnet-4-5",
  temperature: 0.3,
  dimensions: ["accuracy", "completeness", "coherence", "insight", "style"],
  enableCoT: true,
  mitigateBias: false
})

async function evaluateBook(
  book: Book,
  sourceTweets: Tweet[]
): Promise<BookEvaluation> {
  const rubric = await generateBookRubric(sourceTweets)

  const evaluation = await evaluator.evaluate(
    "Generate a comprehensive book from Twitter activity",
    book.fullText,
    JSON.stringify(sourceTweets)  // 作为参考
  )

  return {
    ...evaluation,
    overall: calculateWeightedScore(evaluation, {
      accuracy: 0.3,      // 最重要：事实准确性
      completeness: 0.2,
      coherence: 0.2,
      insight: 0.15,
      style: 0.15
    })
  }
}

async function generateBookRubric(tweets: Tweet[]): Promise<Rubric> {
  const prompt = `
Generate an evaluation rubric for a book synthesized from Twitter activity.

Sample tweets:
${tweets.slice(0, 10).map(t => `- ${t.text}`).join('\n')}

The rubric should have these dimensions:
1. Accuracy: Does the book accurately represent the tweets?
2. Completeness: Does it cover all major topics and themes?
3. Coherence: Is the narrative coherent across chapters?
4. Insight: Does it identify patterns and evolution?
5. Style: Is it well-written and engaging?

For each dimension, define:
- Excellent (8-10)
- Adequate (5-7)
- Poor (0-4)
`

  return await callLLM(prompt)
}
```

## 完整系统流程

### 阶段1：任务分解（Orchestrator）

```typescript
class OrchestratorAgent {
  async orchestrate(request: BookRequest): Promise<Book> {
    // 1. 分解任务
    const plan = await this.createPlan(request)

    // 2. 执行任务
    const scrapeResult = await this.executeScrape(plan.scrapeTask)
    const analysisResult = await this.executeAnalysis(plan.analysisTask, scrapeResult)
    const draftResult = await this.executeWriting(plan.writingTask, analysisResult)
    const finalResult = await this.executeEditing(plan.editingTask, draftResult)

    // 3. 评估质量
    const evaluation = await this.evaluate(finalResult, scrapeResult.tweets)

    if (evaluation.overall < 7.0) {
      // 质量不达标，重新生成
      return await this.retry(plan, evaluation.feedback)
    }

    return finalResult
  }

  private async createPlan(request: BookRequest): Promise<Plan> {
    const prompt = `
Create an execution plan for generating a book from Twitter activity.

Request:
- Accounts: ${request.accounts.join(', ')}
- Time Range: ${request.timeRange.start} to ${request.timeRange.end}
- Style: ${request.style}

Decompose into tasks for:
1. Scraper: What data to collect?
2. Analyzer: What to analyze?
3. Writer: What structure to use?
4. Editor: What to focus on?

Return a detailed plan.
`

    return await callLLM(prompt)
  }

  private async executeScrape(task: ScrapeTask): Promise<ScrapeResult> {
    const summary = await this.callAgent("Scraper", task)

    return {
      summary,  // 摘要进入 Orchestrator 上下文
      tweetsPath: summary.dataPath  // 文件路径传给下一个 Agent
    }
  }

  private async executeAnalysis(
    task: AnalysisTask,
    scrapeResult: ScrapeResult
  ): Promise<AnalysisResult> {
    // Analyzer 直接从文件加载推文
    const analysisTask = {
      ...task,
      dataPath: scrapeResult.tweetsPath
    }

    const analysis = await this.callAgent("Analyzer", analysisTask)

    return analysis
  }

  private async executeWriting(
    task: WritingTask,
    analysisResult: AnalysisResult
  ): Promise<Draft> {
    const draft = await this.callAgent("Writer", {
      ...task,
      analysis: analysisResult
    })

    return draft
  }

  private async executeEditing(
    task: EditingTask,
    draft: Draft
  ): Promise<Book> {
    const finalBook = await this.callAgent("Editor", {
      ...task,
      draft
    })

    return finalBook
  }
}
```

### 阶段2：数据抓取（Scraper）

```typescript
class ScraperAgent {
  async execute(task: ScrapeTask): Promise<ScrapeResult> {
    const allTweets: Tweet[] = []

    for (const account of task.accounts) {
      const tweets = await this.scrapeAccount(account, task.timeRange)
      allTweets.push(...tweets)
    }

    // 保存到文件
    const dataPath = `/tmp/tweets_${Date.now()}.json`
    await writeFile(dataPath, JSON.stringify(allTweets, null, 2))

    // 生成摘要
    const summary = this.createSummary(allTweets)

    return {
      summary: {
        totalTweets: allTweets.length,
        accounts: task.accounts,
        topTopics: summary.topTopics,
        dataPath
      },
      fullDataPath: dataPath
    }
  }

  private async scrapeAccount(
    account: string,
    timeRange: TimeRange
  ): Promise<Tweet[]> {
    // 调用 Twitter API 或整合工具
    return await this.callTool("scrape_user_activity", {
      username: account,
      timeRange,
      includeReplies: true,
      includeRetweets: false
    })
  }
}
```

### 阶段3：内容分析（Analyzer）

```typescript
class AnalyzerAgent {
  private tkg: TemporalKnowledgeGraphManager

  async execute(task: AnalysisTask): Promise<AnalysisResult> {
    // 1. 加载推文数据
    const tweets = JSON.parse(await readFile(task.dataPath, "utf-8"))

    // 2. 分批分析（避免 Lost-in-Middle）
    const batches = chunkArray(tweets, 50)
    const batchAnalyses = await Promise.all(
      batches.map(batch => this.analyzeBatch(batch))
    )

    // 3. 聚合分析
    const aggregated = this.aggregateAnalyses(batchAnalyses)

    // 4. 检测观点演变
    const changes = await this.detectEvolution(tweets)

    // 5. 组织为章节结构
    const chapterOutline = this.createChapterOutline(aggregated, changes)

    return {
      themes: aggregated.themes,
      sentiment: aggregated.sentiment,
      evolution: changes,
      chapterOutline,
      keyQuotes: aggregated.keyQuotes
    }
  }

  private async analyzeBatch(tweets: Tweet[]): Promise<BatchAnalysis> {
    const prompt = `
Analyze this batch of ${tweets.length} tweets:

${tweets.map((t, i) => `${i+1}. ${t.text}`).join('\n')}

Extract:
1. Key themes (3-5)
2. Overall sentiment
3. Notable quotes (top 3)
4. Topic areas

Return as JSON.
`

    return await callLLM(prompt)
  }

  private async detectEvolution(tweets: Tweet[]): Promise<Evolution[]> {
    // 使用时间知识图检测观点演变
    const currentStances = await this.extractStances(tweets)

    const changes = []
    for (const author of new Set(tweets.map(t => t.author))) {
      const history = await this.tkg.getEvolution(
        author,
        new Date(Date.now() - 30 * 24 * 60 * 60 * 1000),  // 过去30天
        new Date()
      )

      const authorChanges = this.detectStanceChanges(history, currentStances)
      changes.push(...authorChanges)
    }

    return changes
  }
}
```

### 阶段4：书籍写作（Writer）

```typescript
class WriterAgent {
  private chapters: ChapterSummary[] = []
  private budget: number = 80000
  private currentTokens: number = 0

  async execute(task: WritingTask): Promise<Draft> {
    const chapters = []

    for (const outline of task.analysis.chapterOutline) {
      const chapter = await this.writeChapter(outline, task.analysis)
      chapters.push(chapter)

      // 压缩旧章节以控制上下文
      if (this.currentTokens > this.budget * 0.75) {
        await this.compressOldChapters()
      }
    }

    return {
      title: this.generateTitle(task.analysis),
      chapters,
      metadata: {
        generatedAt: new Date(),
        sourceCount: task.analysis.themes.tweetCount
      }
    }
  }

  private async writeChapter(
    outline: ChapterOutline,
    analysis: AnalysisResult
  ): Promise<Chapter> {
    const context = this.buildWritingContext(outline, analysis)

    const prompt = `
${context}

Write a compelling chapter based on the outline:

Title: ${outline.title}
Key Points:
${outline.keyPoints.map(p => `- ${p}`).join('\n')}

Requirements:
1. Start with an engaging opening
2. Weave in relevant quotes naturally
3. Analyze patterns and trends
4. End with a transition to the next chapter

Target length: 1500-2000 words
Style: ${this.getStyleGuide()}
`

    const chapterText = await callLLM(prompt)

    // 保存锚点
    this.chapters.push({
      chapterNumber: this.chapters.length + 1,
      title: outline.title,
      keyPoints: outline.keyPoints,
      quotes: this.extractQuotes(chapterText),
      fullText: chapterText,
      analysis: ""
    })

    this.currentTokens = this.estimateTokens(this.buildWritingContext(outline, analysis))

    return {
      title: outline.title,
      content: chapterText
    }
  }

  private buildWritingContext(
    outline: ChapterOutline,
    analysis: AnalysisResult
  ): string {
    return `
<book_context>
<previous_chapters>
${this.chapters.map(ch => `
<chapter number="${ch.chapterNumber}">
<title>${ch.title}</title>
<key_points>${ch.keyPoints.join('; ')}</key_points>
<quotes>${ch.quotes.join('; ')}</quotes>
</chapter>
`).join('\n')}
</previous_chapters>

<current_chapter_outline>
<title>${outline.title}</title>
<key_points>
${outline.keyPoints.map(p => `- ${p}`).join('\n')}
</key_points>
<relevant_quotes>
${outline.quotes.map(q => `- "${q}"`).join('\n')}
</relevant_quotes>
</current_chapter_outline>

<overall_themes>
${analysis.themes.map(t => `- ${t.name}: ${t.description}`).join('\n')}
</overall_themes>

<evolution_insights>
${analysis.evolution.map(e => `- ${e.narrative}`).join('\n')}
</evolution_insights>
</book_context>
`
  }
}
```

### 阶段5：编辑润色（Editor）

```typescript
class EditorAgent {
  async execute(task: EditingTask): Promise<Book> {
    const { draft } = task

    // 1. 全局一致性检查
    const consistency = await this.checkConsistency(draft)

    // 2. 章节衔接优化
    const improved = await this.improveTransitions(draft)

    // 3. 语言润色
    const polished = await this.polishLanguage(improved)

    // 4. 生成目录和索引
    const final = await this.addMetadata(polished)

    return final
  }

  private async checkConsistency(draft: Draft): Promise<ConsistencyReport> {
    const prompt = `
Review the following book draft for consistency:

${draft.chapters.map((ch, i) => `
Chapter ${i+1}: ${ch.title}
${ch.content.substring(0, 500)}...
`).join('\n\n')}

Check for:
1. Narrative consistency (does the story flow logically?)
2. Factual consistency (are claims consistent across chapters?)
3. Style consistency (is the tone consistent?)

Identify any inconsistencies and suggest fixes.
`

    return await callLLM(prompt)
  }

  private async improveTransitions(draft: Draft): Promise<Draft> {
    const improvedChapters = []

    for (let i = 0; i < draft.chapters.length; i++) {
      const chapter = draft.chapters[i]
      const nextChapter = draft.chapters[i + 1]

      if (nextChapter) {
        // 优化章节结尾以过渡到下一章
        const improvedEnding = await this.improveChapterEnding(
          chapter,
          nextChapter
        )

        improvedChapters.push({
          ...chapter,
          content: chapter.content.replace(
            /\n\n([^\n]+)$/,  // 最后一段
            `\n\n${improvedEnding}`
          )
        })
      } else {
        improvedChapters.push(chapter)
      }
    }

    return { ...draft, chapters: improvedChapters }
  }
}
```

### 阶段6：质量评估

```typescript
async function evaluateAndIterate(
  orchestrator: OrchestratorAgent,
  request: BookRequest,
  maxIterations: number = 2
): Promise<Book> {
  let bestBook: Book | null = null
  let bestScore: number = 0

  for (let i = 0; i < maxIterations; i++) {
    const book = await orchestrator.orchestrate(request)

    const evaluation = await evaluateBook(book, book.metadata.sourceTweets)

    console.log(`Iteration ${i + 1} score: ${evaluation.overall}`)

    if (evaluation.overall > bestScore) {
      bestBook = book
      bestScore = evaluation.overall
    }

    if (evaluation.overall >= 8.0) {
      // 达到目标质量，停止迭代
      break
    }

    // 根据评估反馈调整请求
    request = adjustRequest(request, evaluation)
  }

  return bestBook
}
```

## 性能数据

基于源项目的实测数据：

| 指标 | 单 Agent (假设) | X-to-Book 多 Agent | 改进 |
|------|----------------|-------------------|------|
| 总 Token 消耗 | 500K+ | 295K | 41% ↓ |
| 任务完成率 | 60% | 95% | 58% ↑ |
| 平均质量评分 | 6.2/10 | 8.4/10 | 35% ↑ |
| 执行时间 | 45 min | 32 min | 29% ↓ |
| 成本 | $12 | $8 | 33% ↓ |

## 关键设计决策

### 为什么不让所有数据通过 Orchestrator

```
# 差的设计
Scraper → Orchestrator (100K tweets) → Analyzer
                ↓
        上下文爆炸

# 好的设计
Scraper → 文件系统 (100K tweets)
       ↓                    ↓
Orchestrator (摘要)    Analyzer (直接读文件)
```

Orchestrator 只需要知道"抓取了多少条推文，关键主题是什么"，不需要看到所有推文。

### 为什么用时间知识图

观点演变是书籍的核心价值。时间知识图让 Analyzer 能够：

1. 检测立场变化（如对 AI 从乐观到担忧）
2. 识别转折点（如某个事件后观点大变）
3. 生成"演变叙事"（而非静态快照）

这是单纯的主题分析做不到的。

### 为什么需要 5 维评估

单一评分无法指导改进。5 维评估告诉我们：

- **accuracy 低**：Analyzer 需要更仔细地验证事实
- **completeness 低**：Scraper 可能遗漏了重要推文
- **coherence 低**：Writer 的章节衔接有问题
- **insight 低**：Analyzer 的观点演变检测不够深入
- **style 低**：Editor 需要更好的润色

## 实践检查清单

### 架构检查
- [ ] 是否选择了合适的架构模式？
- [ ] 上下文预算分配是否合理？
- [ ] 是否避免了大数据通过 Orchestrator？

### 上下文工程检查
- [ ] 是否应用了观察掩蔽？
- [ ] 是否使用了锚定迭代总结？
- [ ] 是否避免了 Lost-in-Middle？

### 内存和工具检查
- [ ] 是否使用了时间知识图追踪演变？
- [ ] 工具是否整合？
- [ ] 工具输出是否结构化？

### 评估检查
- [ ] 是否使用多维度评估？
- [ ] 评估标准是否与任务目标对齐？
- [ ] 是否有迭代改进机制？

## 下一步

通过 X-to-Book 案例，我们看到了如何综合应用上下文工程的所有技术。下一篇将回到工程实现，探讨如何构建 Agent 技能系统。

下一篇：[07. Agent 技能系统实现](/pages/agent/skill-system)

## 参考资料

- [X-to-Book System Example](https://github.com/muratcankoylan/Agent-Skills-for-Context-Engineering/tree/main/examples/x-to-book-system)
- [Agent Skills for Context Engineering](https://github.com/muratcankoylan/Agent-Skills-for-Context-Engineering)
