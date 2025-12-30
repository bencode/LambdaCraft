# 内存系统与工具设计

系列：[Agent 上下文工程实践指南](00-index.md) | 第 4 篇

上一篇：[03. 多 Agent 架构模式与 Token 经济学](03-multi-agent-patterns.md)

## 引言

Agent 的能力不仅取决于模型智能，还取决于内存系统和工具设计。本文探讨如何设计5层内存架构来支持长期运行的 Agent，以及如何应用工具设计的整合原则来优化上下文使用。

本文整合了源项目中 `memory-systems` 和 `tool-design` 两个核心技能的内容。

## 五层内存架构

### 架构概览

```
┌─────────────────────────────────────┐
│  5. 时间知识图                       │  持久化、时间维度
│  (Temporal Knowledge Graph)          │
├─────────────────────────────────────┤
│  4. 实体内存                         │  持久化、结构化
│  (Entity Memory)                    │
├─────────────────────────────────────┤
│  3. 长期内存                         │  持久化、向量检索
│  (Long-term Memory)                 │
├─────────────────────────────────────┤
│  2. 短期内存                         │  会话级、压缩
│  (Short-term Memory)                │
├─────────────────────────────────────┤
│  1. 工作内存                         │  当前上下文窗口
│  (Working Memory)                   │
└─────────────────────────────────────┘
```

### 1. 工作内存（Working Memory）

**定义**：当前上下文窗口中的内容，直接可见于模型。

**特点**：
- 容量：受模型上下文窗口限制（如 100K tokens）
- 持久性：仅在当前请求中存在
- 访问速度：最快（直接可用）

**内容**：
```markdown
<working_memory>
<current_task>
实现用户认证的 JWT 刷新机制
</current_task>

<recent_context>
- 已实现 JWT 生成和验证
- 已创建 /auth/login 端点
- 下一步：实现 /auth/refresh 端点
</recent_context>

<active_files>
- src/auth/jwt.ts
- src/auth/refresh.ts
</active_files>
</working_memory>
```

**管理策略**：
- 优先级最高的信息放在这里
- 当接近容量时，触发压缩到短期内存

### 2. 短期内存（Short-term Memory）

**定义**：当前会话的压缩历史，跨多个请求。

**特点**：
- 容量：10-50K tokens（压缩后）
- 持久性：会话级（几小时到几天）
- 访问速度：快（加载到工作内存）

**实现**：

```typescript
type ShortTermMemory = {
  sessionId: string
  summary: string
  decisions: string[]
  filesModified: string[]
  nextSteps: string[]
  createdAt: Date
  updatedAt: Date
}

class ShortTermMemoryManager {
  async save(memory: ShortTermMemory): Promise<void> {
    const filePath = `/tmp/stm_${memory.sessionId}.json`
    await writeFile(filePath, JSON.stringify(memory, null, 2))
  }

  async load(sessionId: string): Promise<ShortTermMemory | null> {
    const filePath = `/tmp/stm_${sessionId}.json`

    if (!await exists(filePath)) {
      return null
    }

    const content = await readFile(filePath, "utf-8")
    return JSON.parse(content)
  }

  async compress(workingMemory: string): Promise<ShortTermMemory> {
    const prompt = `
Compress the following working memory into a structured summary:

${workingMemory}

Extract:
1. Key decisions made
2. Files modified
3. Next steps
4. Brief summary of discussion
`

    const result = await callLLM(prompt)

    return {
      sessionId: generateSessionId(),
      summary: result.summary,
      decisions: result.decisions,
      filesModified: result.filesModified,
      nextSteps: result.nextSteps,
      createdAt: new Date(),
      updatedAt: new Date()
    }
  }
}
```

**示例**：

```json
{
  "sessionId": "session_20250101_001",
  "summary": "实现了用户认证系统的 JWT 生成和验证功能。选择使用 RS256 算法，刷新令牌有效期为7天。",
  "decisions": [
    "使用 RS256 而非 HS256（更安全）",
    "刷新令牌有效期：7天",
    "将黑名单存储在 Redis"
  ],
  "filesModified": [
    "src/auth/jwt.ts",
    "src/auth/login.ts",
    "src/middleware/auth.ts"
  ],
  "nextSteps": [
    "实现 /auth/refresh 端点",
    "添加黑名单检查中间件",
    "编写单元测试"
  ],
  "createdAt": "2025-01-01T10:00:00Z",
  "updatedAt": "2025-01-01T12:30:00Z"
}
```

### 3. 长期内存（Long-term Memory）

**定义**：跨会话的持久化内存，使用向量检索。

**特点**：
- 容量：无限（外部存储）
- 持久性：永久
- 访问速度：中等（需要检索）

**实现**：

```typescript
type LongTermMemoryEntry = {
  id: string
  content: string
  embedding: number[]
  metadata: {
    timestamp: Date
    sessionId: string
    tags: string[]
  }
}

class LongTermMemoryManager {
  private vectorDB: VectorDatabase

  async store(content: string, metadata: Record<string, any>): Promise<void> {
    const embedding = await this.getEmbedding(content)

    const entry: LongTermMemoryEntry = {
      id: generateId(),
      content,
      embedding,
      metadata: {
        timestamp: new Date(),
        ...metadata
      }
    }

    await this.vectorDB.insert(entry)
  }

  async retrieve(
    query: string,
    limit: number = 5
  ): Promise<LongTermMemoryEntry[]> {
    const queryEmbedding = await this.getEmbedding(query)

    return await this.vectorDB.search(queryEmbedding, limit)
  }

  private async getEmbedding(text: string): Promise<number[]> {
    // 使用 OpenAI embeddings 或其他嵌入模型
    return await openai.embeddings.create({
      model: "text-embedding-3-small",
      input: text
    })
  }
}
```

**使用场景**：

```typescript
// 存储过去的决策
await ltm.store(
  "决定使用 PostgreSQL 作为主数据库，因为需要 ACID 保证和复杂查询支持",
  { tags: ["decision", "database"], sessionId: "session_001" }
)

// 检索相关历史
const relevant = await ltm.retrieve(
  "选择什么数据库存储用户会话？"
)

// 将检索结果加载到工作内存
const context = relevant.map(r => r.content).join("\n\n")
```

### 4. 实体内存（Entity Memory）

**定义**：结构化的实体信息存储（人物、项目、概念等）。

**特点**：
- 容量：无限
- 持久性：永久
- 访问速度：快（结构化查询）

**Schema**：

```typescript
type Entity = {
  id: string
  type: "person" | "project" | "concept" | "file"
  name: string
  attributes: Record<string, any>
  relationships: Relationship[]
  history: HistoryEntry[]
}

type Relationship = {
  targetId: string
  type: string
  metadata: Record<string, any>
}

type HistoryEntry = {
  timestamp: Date
  event: string
  data: Record<string, any>
}
```

**实现**：

```typescript
class EntityMemoryManager {
  async createEntity(entity: Entity): Promise<void> {
    const filePath = `/storage/entities/${entity.type}/${entity.id}.json`
    await writeFile(filePath, JSON.stringify(entity, null, 2))
  }

  async getEntity(id: string): Promise<Entity | null> {
    // 扫描所有类型目录
    const types = ["person", "project", "concept", "file"]

    for (const type of types) {
      const filePath = `/storage/entities/${type}/${id}.json`
      if (await exists(filePath)) {
        const content = await readFile(filePath, "utf-8")
        return JSON.parse(content)
      }
    }

    return null
  }

  async updateEntity(id: string, update: Partial<Entity>): Promise<void> {
    const entity = await this.getEntity(id)
    if (!entity) {
      throw new Error(`Entity ${id} not found`)
    }

    const updated = { ...entity, ...update }

    // 添加历史记录
    updated.history.push({
      timestamp: new Date(),
      event: "update",
      data: update
    })

    const filePath = `/storage/entities/${entity.type}/${id}.json`
    await writeFile(filePath, JSON.stringify(updated, null, 2))
  }

  async query(query: EntityQuery): Promise<Entity[]> {
    // 实现灵活的查询逻辑
    const allEntities = await this.loadAllEntities()

    return allEntities.filter(entity => {
      if (query.type && entity.type !== query.type) return false
      if (query.name && !entity.name.includes(query.name)) return false
      if (query.hasAttribute) {
        const [key, value] = query.hasAttribute
        if (entity.attributes[key] !== value) return false
      }
      return true
    })
  }
}
```

**示例**：

```json
{
  "id": "user_alice",
  "type": "person",
  "name": "Alice Smith",
  "attributes": {
    "role": "Frontend Developer",
    "email": "alice@example.com",
    "preferences": {
      "framework": "React",
      "styling": "Tailwind CSS"
    }
  },
  "relationships": [
    {
      "targetId": "project_webapp",
      "type": "contributes_to",
      "metadata": { "role": "lead" }
    }
  ],
  "history": [
    {
      "timestamp": "2025-01-01T10:00:00Z",
      "event": "created",
      "data": {}
    },
    {
      "timestamp": "2025-01-05T15:30:00Z",
      "event": "update",
      "data": { "attributes": { "role": "Frontend Lead" } }
    }
  ]
}
```

### 5. 时间知识图（Temporal Knowledge Graph）

**定义**：追踪实体和关系随时间的演变。

**特点**：
- 容量：无限
- 持久性：永久
- 访问速度：中等（图查询）

**Schema**：

```typescript
type TemporalNode = {
  entityId: string
  timestamp: Date
  snapshot: Record<string, any>
}

type TemporalEdge = {
  sourceId: string
  targetId: string
  type: string
  validFrom: Date
  validUntil: Date | null
  metadata: Record<string, any>
}

type TemporalKnowledgeGraph = {
  nodes: TemporalNode[]
  edges: TemporalEdge[]
}
```

**实现**：

```typescript
class TemporalKnowledgeGraphManager {
  async addSnapshot(
    entityId: string,
    snapshot: Record<string, any>
  ): Promise<void> {
    const node: TemporalNode = {
      entityId,
      timestamp: new Date(),
      snapshot
    }

    await this.appendToGraph(node)
  }

  async addRelationship(
    sourceId: string,
    targetId: string,
    type: string,
    metadata: Record<string, any> = {}
  ): Promise<void> {
    const edge: TemporalEdge = {
      sourceId,
      targetId,
      type,
      validFrom: new Date(),
      validUntil: null,
      metadata
    }

    await this.appendToGraph(edge)
  }

  async endRelationship(
    sourceId: string,
    targetId: string,
    type: string
  ): Promise<void> {
    const edges = await this.findEdges(sourceId, targetId, type)

    for (const edge of edges) {
      if (edge.validUntil === null) {
        edge.validUntil = new Date()
        await this.updateEdge(edge)
      }
    }
  }

  async getEvolution(
    entityId: string,
    from: Date,
    to: Date
  ): Promise<TemporalNode[]> {
    const nodes = await this.loadNodes()

    return nodes.filter(node =>
      node.entityId === entityId &&
      node.timestamp >= from &&
      node.timestamp <= to
    )
  }

  async queryAtTime(
    entityId: string,
    timestamp: Date
  ): Promise<Record<string, any>> {
    const nodes = await this.getEvolution(
      entityId,
      new Date(0),
      timestamp
    )

    // 返回最接近但不晚于指定时间的快照
    return nodes[nodes.length - 1]?.snapshot || {}
  }
}
```

**使用示例（X-to-Book 案例）**：

```typescript
// 追踪 Elon Musk 对 AI 的观点演变
await tkg.addSnapshot("elon_musk", {
  topic: "AI safety",
  stance: "optimistic",
  evidence: "Tweet: AI will bring prosperity"
})

// 15天后
await tkg.addSnapshot("elon_musk", {
  topic: "AI safety",
  stance: "concerned",
  evidence: "Tweet: We need AI regulation"
})

// 查询演变
const evolution = await tkg.getEvolution(
  "elon_musk",
  new Date("2025-01-01"),
  new Date("2025-01-31")
)

// 生成叙述
const narrative = `
Elon Musk's stance on AI safety evolved significantly:
- Early January: Optimistic, focused on prosperity
- Mid January: Shift to concern, calling for regulation
`
```

## 工具设计原则

### 整合原则（Integration Principle）

**核心思想**：如果开发者不能明确说该用哪个工具，Agent 也不能。

**反例**：15个细粒度工具

```typescript
const tools = [
  "read_file",
  "write_file",
  "append_file",
  "delete_file",
  "list_directory",
  "create_directory",
  "move_file",
  "copy_file",
  // ...
]
```

Agent 在每次文件操作时都需要决策："应该用 read_file 还是 read_file_lines？"这增加了认知负担和上下文占用。

**正例**：整合的文件系统工具

```typescript
const tools = [
  {
    name: "file_system",
    description: "Perform file system operations",
    parameters: {
      operation: {
        type: "enum",
        values: ["read", "write", "append", "delete", "list", "move", "copy"]
      },
      path: { type: "string" },
      content: { type: "string", optional: true },
      options: { type: "object", optional: true }
    }
  }
]
```

单个工具，内部根据 `operation` 参数路由到不同实现。

### 清晰描述（Clear Description）

**原则**：描述应回答三个问题：

1. **做什么**（What）：功能是什么
2. **何时用**（When）：在什么情况下使用
3. **返回什么**（Returns）：输出格式和含义

**示例**：

```typescript
{
  name: "semantic_search",
  description: `
    Searches the codebase for semantically similar code or documentation.

    When to use:
    - When you need to find code that does something similar (not exact text match)
    - When searching for concepts rather than keywords
    - When grep/exact search returns too many or too few results

    Returns:
    - Array of {file_path, similarity_score, excerpt}
    - Sorted by descending similarity
    - Maximum 10 results
  `,
  parameters: {
    query: {
      type: "string",
      description: "Natural language description of what you're looking for"
    },
    limit: {
      type: "number",
      description: "Maximum number of results (default: 5)",
      optional: true
    }
  }
}
```

### 命名空间（Namespacing）

**原则**：使用前缀来组织工具，减少选择复杂度。

**示例**：

```typescript
const tools = [
  // 文件系统命名空间
  "fs:read",
  "fs:write",
  "fs:list",

  // Git 命名空间
  "git:status",
  "git:diff",
  "git:commit",

  // 数据库命名空间
  "db:query",
  "db:insert",
  "db:update",

  // API 命名空间
  "api:get",
  "api:post",
  "api:put"
]
```

Agent 可以先选择命名空间，再选择具体操作，降低决策复杂度。

### 建筑简化（Architectural Simplicity）

**原则**：优先选择少数几个综合工具，而非许多单一工具。

**对比**：

| 策略 | 工具数量 | 工具描述总 Token | 决策复杂度 |
|------|---------|----------------|----------|
| 细粒度 | 15-20 | 3000-4000 | 高 |
| 整合 | 3-5 | 1000-1500 | 低 |

**X-to-Book 案例**：

```typescript
// 不好的做法
const tools = [
  "scrape_profile",
  "scrape_tweets",
  "scrape_replies",
  "scrape_likes",
  "analyze_sentiment",
  "analyze_topics",
  "analyze_network",
  "generate_chapter",
  "generate_summary",
  "generate_title",
  // ... 10 more
]

// 好的做法
const tools = [
  "scrape_user_activity",  // 整合所有抓取
  "analyze_content",        // 整合所有分析
  "generate_book"           // 整合所有生成
]
```

### 错误消息启用恢复（Error Messages Enable Recovery）

**原则**：错误消息应包含如何修复的信息。

**反例**：

```json
{
  "error": "Invalid parameters"
}
```

**正例**：

```json
{
  "error": "Invalid parameters",
  "details": {
    "parameter": "date_range",
    "issue": "start_date must be before end_date",
    "received": {
      "start_date": "2025-01-15",
      "end_date": "2025-01-01"
    },
    "suggestion": "Swap start_date and end_date, or use absolute dates"
  }
}
```

Agent 可以根据错误消息自动修复参数并重试。

## 工具输出优化

### 结构化 vs 自然语言

**原则**：优先使用结构化输出，除非输出是给最终用户的。

**对比**：

```typescript
// 自然语言输出（不好）
{
  output: "Found 3 users: John Doe (age 30, email john@example.com), ..."
}
// Token 数：~50

// 结构化输出（好）
{
  output: {
    count: 3,
    users: [
      { name: "John Doe", age: 30, email: "john@example.com" },
      { name: "Jane Smith", age: 25, email: "jane@example.com" },
      { name: "Bob Johnson", age: 35, email: "bob@example.com" }
    ]
  }
}
// Token 数：~30（节省 40%）
```

### 摘要 + 完整输出

**原则**：返回摘要到上下文，完整输出存储到文件。

**实现**：

```typescript
async function executeToolWithSummary(
  toolName: string,
  args: Record<string, any>
): Promise<ToolResult> {
  const fullOutput = await executeTool(toolName, args)

  if (fullOutput.length > 2000) {
    const summary = await summarize(fullOutput, { maxLength: 200 })
    const filePath = await saveToFile(fullOutput)

    return {
      summary,
      file_ref: filePath,
      full_output_available: true
    }
  }

  return { output: fullOutput }
}
```

## 实践检查清单

### 内存架构检查
- [ ] 是否定义了5层内存的边界和用途？
- [ ] 工作内存是否保持在预算内？
- [ ] 是否有机制将工作内存压缩到短期内存？
- [ ] 长期内存是否使用向量检索？
- [ ] 是否跟踪关键实体的演变？

### 工具设计检查
- [ ] 工具数量是否控制在 10 个以内？
- [ ] 是否应用了整合原则？
- [ ] 工具描述是否回答了"做什么、何时用、返回什么"？
- [ ] 错误消息是否可操作？
- [ ] 工具输出是否结构化？

### 输出优化检查
- [ ] 长输出是否使用摘要+文件引用？
- [ ] 是否优先使用结构化数据？

## 下一步

掌握了内存系统和工具设计后，下一篇将探讨如何评估 Agent 系统的质量，包括 LLM-as-Judge 的实践技巧。

下一篇：[05. 评估框架：LLM-as-Judge 实践](05-evaluation-framework.md)

## 参考资料

- [Agent Skills - Memory Systems](https://github.com/muratcankoylan/Agent-Skills-for-Context-Engineering/tree/main/skills/memory-systems)
- [Agent Skills - Tool Design](https://github.com/muratcankoylan/Agent-Skills-for-Context-Engineering/tree/main/skills/tool-design)
- [MemGPT: Towards LLMs as Operating Systems](https://arxiv.org/abs/2310.08560)
