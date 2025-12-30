# 上下文压缩与优化策略

系列：[Agent 上下文工程实践指南](00-index.md) | 第 2 篇

上一篇：[01. 上下文基础：注意力机制与失败模式](01-context-fundamentals.md)

## 引言

在上一篇中，我们理解了上下文的限制和失败模式。本文探讨如何通过压缩和优化策略来管理有限的上下文预算，实现更高效的 Agent 系统。

本文整合了源项目中 `context-compression` 和 `context-optimization` 两个核心技能的内容。

## 优化目标的重新思考

### 常见错误：优化 tokens-per-request

许多开发者关注单次请求的 Token 数量：

```
错误目标：最小化单次请求的 Token 数
```

这导致频繁的压缩和重新加载，反而增加总成本。

### 正确目标：优化 tokens-per-task

应关注完成整个任务的总 Token 消耗：

```
正确目标：最小化完成任务的总 Token 数
```

**对比示例**：

| 策略 | 单次请求 Token | 总请求次数 | 总 Token | 任务完成率 |
|------|--------------|-----------|---------|----------|
| 激进压缩 | 5K | 20 | 100K | 85% |
| 适度压缩 | 15K | 8 | 120K | 98% |
| 最优策略 | 12K | 7 | 84K | 98% |

激进压缩虽然单次请求小，但因信息丢失导致重复工作，总成本更高。

## 工具观察占比的启示

### 83.9% 的数据

研究显示，Agent 轨迹中**工具输出占 83.9%** 的 Token。

**分解**：

```
Agent 轨迹 Token 分布：
- 推理步骤：8.2%
- 工具调用参数：7.9%
- 工具输出：83.9%
```

**启示**：
1. 优化工具输出是收益最大的方向
2. 压缩推理步骤的收益有限
3. 工具设计应考虑输出简洁性

### 优化策略

**策略1：观察掩蔽（Observation Masking）**

不将完整工具输出传回上下文，而是只传递摘要。

```typescript
type ToolResult = {
  summary: string       // 传回上下文
  full_output: string   // 存储到文件系统，不传回
  file_ref: string      // 文件引用路径
}

async function executeTool(
  toolName: string,
  args: Record<string, any>
): Promise<ToolResult> {
  const fullOutput = await callTool(toolName, args)

  const summary = await summarize(fullOutput, maxLength: 200)

  const fileRef = await saveToFile(fullOutput)

  return { summary, full_output: fullOutput, file_ref: fileRef }
}
```

**示例**：

```markdown
# 不好的做法
Tool: search_codebase
Output: [15K tokens of code search results...]

# 好的做法
Tool: search_codebase
Summary: Found 23 matches across 8 files. Top results:
- src/auth/jwt.ts: 5 matches (token validation logic)
- src/auth/session.ts: 3 matches (session management)
Full results saved to: /tmp/search_results_20250101_001.txt
```

**策略2：结构化输出**

工具返回结构化数据，而非自然语言。

```typescript
// 不好的做法
{
  output: "The API returned 3 users. The first user is John Doe, age 30..."
}

// 好的做法
{
  output: {
    count: 3,
    users: [
      { name: "John Doe", age: 30 },
      { name: "Jane Smith", age: 25 },
      { name: "Bob Johnson", age: 35 }
    ]
  }
}
```

结构化数据更紧凑，且易于后续处理。

**策略3：增量输出**

对于长时间运行的工具，使用增量输出而非等待完整结果。

```python
def process_large_dataset(data: list[dict]) -> Iterator[dict]:
    """增量处理，而非等待全部完成"""
    for batch in chunk_data(data, batch_size=100):
        result = process_batch(batch)
        yield {"progress": f"{len(batch)}/{len(data)}", "result": result}
```

## 三种压缩方法对比

### 1. 锚定迭代总结（Anchored Iterative Summarization）

**原理**：保留结构化部分作为"锚点"，仅压缩内容部分。

**实现**：

```typescript
type ConversationState = {
  // 锚点部分（不压缩）
  decisions: string[]
  next_steps: string[]
  files_modified: string[]

  // 内容部分（可压缩）
  discussion: string
}

function compressConversation(
  state: ConversationState
): ConversationState {
  return {
    ...state,
    discussion: summarize(state.discussion, maxLength: 500)
  }
}
```

**结构示例**：

```markdown
<conversation_summary>
<decisions>
- 使用 JWT 进行认证
- 刷新令牌有效期设为 7 天
- 使用 Redis 存储黑名单
</decisions>

<files_modified>
- src/auth/jwt.ts
- src/auth/refresh.ts
- src/middleware/auth.ts
</files_modified>

<next_steps>
- 实现令牌刷新端点
- 添加黑名单检查中间件
- 编写单元测试
</next_steps>

<discussion_summary>
讨论了 JWT vs Session 的权衡。最终选择 JWT 因为需要支持移动端...
</discussion_summary>
</conversation_summary>
```

**优点**：
- 关键结构信息不丢失
- 可验证完整性（检查决策列表）
- 可逆性高

**缺点**：
- 压缩率中等（约 60-70%）
- 需要设计良好的结构

**性能数据**：

| 指标 | 数值 |
|------|------|
| 压缩率 | 65% |
| 结构化总结质量 | 2.2-2.5/5.0 |
| 信息保留率 | 80-85% |

### 2. 不透明压缩（Opaque Compression）

**原理**：使用模型的内部压缩（如 prompt caching），对开发者不可见。

**示例**（OpenAI 方法）：

```typescript
// OpenAI 的压缩 API（假设）
const compressed = await openai.compress({
  messages: conversationHistory,
  targetRatio: 0.01  // 压缩到 1%
})

// compressed 是不透明的内部表示
```

**优点**：
- 压缩率极高（99%+）
- 实现简单

**缺点**：
- 无法验证内容
- 不可调试
- 依赖特定模型

**性能数据**：

| 指标 | 数值 |
|------|------|
| 压缩率 | 99%+ |
| 信息保留率 | 未知（黑盒） |
| 调试难度 | 极高 |

### 3. 完整重新总结（Full Re-summarization）

**原理**：每次压缩时重新总结全部内容，不保留结构。

**实现**（Anthropic 方法）：

```typescript
async function summarizeConversation(
  messages: Message[]
): Promise<string> {
  const prompt = `
Summarize the following conversation, focusing on:
- Key decisions made
- Current progress
- Next steps

Conversation:
${messages.map(m => `${m.role}: ${m.content}`).join('\n')}
`

  const summary = await model.generate(prompt)
  return summary
}
```

**优点**：
- 可读性强
- 灵活，可根据需要调整总结重点
- 可调试

**缺点**：
- 压缩率低（约 40-50%）
- 多次压缩后信息累积丢失
- 计算成本高（需要额外的 LLM 调用）

**性能数据**：

| 指标 | 数值 |
|------|------|
| 压缩率 | 45% |
| 可读性 | 高 |
| 多次压缩后信息丢失 | 显著 |

### 方法对比总结

| 方法 | 压缩率 | 信息保留 | 可验证性 | 适用场景 |
|------|-------|---------|---------|---------|
| 锚定迭代总结 | 65% | 高 | 高 | 长期对话、复杂任务 |
| 不透明压缩 | 99%+ | 未知 | 低 | 短期对话、成本敏感 |
| 完整重新总结 | 45% | 中 | 高 | 短期对话、可读性优先 |

**推荐**：对于生产级 Agent 系统，优先使用**锚定迭代总结**，因其平衡了压缩率、信息保留和可验证性。

## 上下文预算管理框架

### 预算分配

定义各部分的上下文预算：

```typescript
type ContextBudget = {
  total: number
  reserved: {
    system_prompt: number      // 10%
    tool_definitions: number   // 15%
  }
  dynamic: {
    conversation: number       // 30%
    tool_outputs: number       // 35%
    retrieved_docs: number     // 10%
  }
}

const budget: ContextBudget = {
  total: 100000,
  reserved: {
    system_prompt: 10000,
    tool_definitions: 15000
  },
  dynamic: {
    conversation: 30000,
    tool_outputs: 35000,
    retrieved_docs: 10000
  }
}
```

### 触发策略

定义何时触发压缩：

```typescript
type CompressionTrigger = {
  utilization_threshold: number  // 利用率阈值
  segment_age: number            // 段落年龄（轮次数）
  importance_threshold: number   // 重要性阈值
}

const trigger: CompressionTrigger = {
  utilization_threshold: 0.75,  // 75% 利用率时触发
  segment_age: 10,               // 10轮之前的对话可压缩
  importance_threshold: 0.3      // 重要性 < 0.3 的可删除
}

function shouldCompress(context: Context): boolean {
  const utilization = context.currentTokens / context.budget.total
  return utilization >= trigger.utilization_threshold
}
```

### 压缩优先级

定义压缩的优先级：

```typescript
const compressionPriority = [
  "old_tool_outputs",        // 最优先：旧的工具输出
  "low_relevance_docs",      // 低相关性检索文档
  "old_conversation",        // 旧的对话历史
  "repeated_information"     // 重复信息
]

const neverCompress = [
  "system_prompt",           // 永不压缩：系统提示
  "latest_conversation",     // 最近的对话（最近3轮）
  "current_task_context"     // 当前任务上下文
]
```

### 完整实现示例

```typescript
class ContextManager {
  private budget: ContextBudget
  private trigger: CompressionTrigger

  async manageContext(context: Context): Promise<Context> {
    if (!this.shouldCompress(context)) {
      return context
    }

    const compressed = await this.compress(context)

    return compressed
  }

  private shouldCompress(context: Context): boolean {
    const utilization = context.currentTokens / this.budget.total
    return utilization >= this.trigger.utilization_threshold
  }

  private async compress(context: Context): Promise<Context> {
    // 1. 识别可压缩段落
    const compressible = this.identifyCompressible(context)

    // 2. 按优先级排序
    const sorted = this.sortByPriority(compressible)

    // 3. 逐步压缩直到满足预算
    let compressed = context
    for (const segment of sorted) {
      if (compressed.currentTokens <= this.budget.total * 0.7) {
        break
      }
      compressed = await this.compressSegment(compressed, segment)
    }

    return compressed
  }

  private async compressSegment(
    context: Context,
    segment: ContextSegment
  ): Promise<Context> {
    if (segment.type === "tool_output") {
      return this.compressToolOutput(context, segment)
    } else if (segment.type === "conversation") {
      return this.compressConversation(context, segment)
    }
    // ...更多类型
    return context
  }

  private async compressToolOutput(
    context: Context,
    segment: ContextSegment
  ): Promise<Context> {
    const summary = await this.summarize(segment.content, { maxLength: 200 })

    const fileRef = await this.saveToFile(segment.content)

    return context.replace(segment, {
      summary,
      file_ref: fileRef
    })
  }

  private async compressConversation(
    context: Context,
    segment: ContextSegment
  ): Promise<Context> {
    // 使用锚定迭代总结
    const anchored = this.extractAnchors(segment)

    const discussionSummary = await this.summarize(
      segment.content,
      { maxLength: 500 }
    )

    return context.replace(segment, {
      ...anchored,
      discussion: discussionSummary
    })
  }

  private extractAnchors(segment: ContextSegment): Record<string, any> {
    return {
      decisions: this.extractDecisions(segment),
      files_modified: this.extractFilesModified(segment),
      next_steps: this.extractNextSteps(segment)
    }
  }
}
```

## KV-Cache 优化

### 什么是 KV-Cache

Transformer 模型在生成时会缓存键值（Key-Value）对，避免重新计算已处理的 Token。

```
第一次请求：计算全部 100K Token 的 KV
第二次请求：复用 KV Cache，仅计算新 Token
```

### 缓存效率优化

**策略1：静态前缀复用**

将不变的部分放在前面，最大化缓存复用。

```markdown
<!-- 好的结构：静态前缀 -->
<system_prompt>
[5K tokens, 永不改变]
</system_prompt>

<tool_definitions>
[10K tokens, 偶尔更新]
</tool_definitions>

<conversation>
[动态内容]
</conversation>
```

**策略2：避免频繁修改前缀**

```typescript
// 不好的做法：每次都修改系统提示
const systemPrompt = `Current time: ${new Date().toISOString()}...`

// 好的做法：时间信息放在后面
const context = `
<system_prompt>
[静态内容]
</system_prompt>

<metadata>
Current time: ${new Date().toISOString()}
</metadata>
`
```

**性能影响**：

| 策略 | 缓存命中率 | 延迟降低 | 成本降低 |
|------|-----------|---------|---------|
| 无优化 | 20-30% | 0% | 0% |
| 静态前缀 | 60-70% | 40% | 50% |
| 完全静态化 | 90%+ | 70% | 75% |

## 上下文分割（Context Sharding）

对于超大任务，将上下文分割到多个 Agent。

### 分割策略

```typescript
type Shard = {
  id: string
  focus: string
  context: Context
  coordination: {
    shared_state_path: string
    dependencies: string[]
  }
}

const shards: Shard[] = [
  {
    id: "frontend",
    focus: "React 组件实现",
    context: { /* 仅包含前端相关上下文 */ },
    coordination: {
      shared_state_path: "/tmp/shared_state.json",
      dependencies: ["backend"]  // 依赖后端 API
    }
  },
  {
    id: "backend",
    focus: "API 端点实现",
    context: { /* 仅包含后端相关上下文 */ },
    coordination: {
      shared_state_path: "/tmp/shared_state.json",
      dependencies: []
    }
  }
]
```

### 协调机制

使用文件系统作为状态机：

```typescript
// backend shard 写入 API 规格
await writeFile("/tmp/shared_state.json", JSON.stringify({
  api_endpoints: [
    { path: "/api/users", method: "GET" },
    { path: "/api/users/:id", method: "GET" }
  ]
}))

// frontend shard 读取 API 规格
const sharedState = JSON.parse(
  await readFile("/tmp/shared_state.json", "utf-8")
)

// 基于 API 规格实现前端调用
```

**优点**：
- 每个 shard 的上下文独立，不互相污染
- 可并行处理
- 文件系统状态易于调试和验证

## 实践检查清单

### 优化目标检查
- [ ] 优化目标是 tokens-per-task 而非 tokens-per-request？
- [ ] 压缩策略考虑了任务完成率？

### 工具输出检查
- [ ] 是否使用了观察掩蔽？
- [ ] 工具输出是否结构化？
- [ ] 长输出是否保存到文件系统？

### 压缩策略检查
- [ ] 选择的压缩方法是否适合任务类型？
- [ ] 是否使用了锚定结构保留关键信息？
- [ ] 是否有机制验证压缩后的完整性？

### 预算管理检查
- [ ] 是否定义了各部分的预算分配？
- [ ] 压缩触发阈值是否合理（建议 70-80%）？
- [ ] 是否有永不压缩的部分清单？

### KV-Cache 检查
- [ ] 静态内容是否放在前缀？
- [ ] 是否避免了频繁修改前缀？

## 下一步

掌握了上下文压缩和优化后，下一篇将探讨多 Agent 架构模式，以及如何通过上下文隔离进一步优化 Token 经济学。

下一篇：[03. 多 Agent 架构模式与 Token 经济学](03-multi-agent-patterns.md)

## 参考资料

- [Agent Skills - Context Compression](https://github.com/muratcankoylan/Agent-Skills-for-Context-Engineering/tree/main/skills/context-compression)
- [Agent Skills - Context Optimization](https://github.com/muratcankoylan/Agent-Skills-for-Context-Engineering/tree/main/skills/context-optimization)
- [Prompt Caching with Claude](https://docs.anthropic.com/claude/docs/prompt-caching)
