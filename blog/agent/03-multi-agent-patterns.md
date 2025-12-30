# 多 Agent 架构模式与 Token 经济学

系列：[Agent 上下文工程实践指南](/pages/agent/00-index) | 第 3 篇

上一篇：[02. 上下文压缩与优化策略](/pages/agent/context-optimization)

## 引言

单 Agent 系统在面对复杂任务时会遇到上下文爆炸问题。多 Agent 架构通过**上下文隔离**来解决这个问题，但同时也带来了 Token 成本的显著增加。本文探讨三种主要的多 Agent 架构模式、Token 经济学，以及如何做出正确的架构选择。

本文整合了源项目中 `multi-agent-patterns` 技能的内容。

## 多 Agent 的真正价值

多 Agent 系统的核心价值不是"模拟团队协作"，而是**上下文隔离**。

### 单 Agent 的上下文问题

```
任务：构建一个完整的 Web 应用

单 Agent 上下文需求：
- 前端框架知识
- 后端 API 设计
- 数据库 schema
- 部署配置
- 测试策略
- 所有代码文件
- 所有工具输出
→ 上下文爆炸，超过 200K tokens
```

### 多 Agent 的上下文隔离

```
任务分割：

Frontend Agent (30K tokens):
- React 组件实现
- UI/UX 相关上下文
- 前端工具输出

Backend Agent (40K tokens):
- API 端点实现
- 数据库 schema
- 后端工具输出

DevOps Agent (20K tokens):
- 部署配置
- CI/CD 设置
- 基础设施代码

→ 每个 Agent 上下文独立，总计 90K (而非 200K)
```

## 三种主要架构模式

### 1. Supervisor/Orchestrator 模式

#### 结构

```
       Orchestrator
      /      |      \
Frontend  Backend  DevOps
```

Orchestrator 负责：
- 任务分解
- 子 Agent 调度
- 结果聚合
- 冲突解决

子 Agent 负责：
- 执行具体任务
- 返回结果
- 不直接通信

#### 实现示例

```typescript
type AgentConfig = {
  name: string
  role: string
  contextBudget: number
  tools: string[]
}

type TaskAssignment = {
  agentName: string
  task: string
  dependencies: string[]
  outputPath: string
}

class Orchestrator {
  private agents: Map<string, AgentConfig>

  async executeTask(task: string): Promise<string> {
    // 1. 分解任务
    const subtasks = await this.decompose(task)

    // 2. 创建任务分配
    const assignments = this.createAssignments(subtasks)

    // 3. 执行任务（考虑依赖关系）
    const results = await this.executeAssignments(assignments)

    // 4. 聚合结果
    return this.aggregateResults(results)
  }

  private async decompose(task: string): Promise<string[]> {
    const prompt = `
Decompose the following task into subtasks for these agents:
${Array.from(this.agents.values()).map(a => `- ${a.name}: ${a.role}`).join('\n')}

Task: ${task}

Return a list of subtasks with the agent assignment.
`

    return await this.callLLM(prompt)
  }

  private async executeAssignments(
    assignments: TaskAssignment[]
  ): Promise<Map<string, string>> {
    const results = new Map<string, string>()

    // 构建依赖图
    const graph = this.buildDependencyGraph(assignments)

    // 拓扑排序
    const sorted = this.topologicalSort(graph)

    // 按顺序执行（可并行化独立任务）
    for (const assignment of sorted) {
      const agentConfig = this.agents.get(assignment.agentName)

      // 加载依赖的输出
      const dependencies = await this.loadDependencies(assignment.dependencies)

      // 执行任务
      const result = await this.executeAgentTask(
        agentConfig,
        assignment.task,
        dependencies
      )

      // 保存结果到文件系统
      await writeFile(assignment.outputPath, result)
      results.set(assignment.agentName, result)
    }

    return results
  }

  private async executeAgentTask(
    config: AgentConfig,
    task: string,
    dependencies: Map<string, string>
  ): Promise<string> {
    const context = this.buildAgentContext(config, task, dependencies)

    return await this.callLLM(context, {
      model: config.model,
      tools: config.tools
    })
  }

  private buildAgentContext(
    config: AgentConfig,
    task: string,
    dependencies: Map<string, string>
  ): string {
    return `
<role>${config.role}</role>

<task>${task}</task>

<dependencies>
${Array.from(dependencies.entries()).map(([name, output]) => `
<${name}>
${output}
</${name}>
`).join('\n')}
</dependencies>
`
  }
}
```

#### 优点
- 清晰的控制流
- 易于调试和监控
- Orchestrator 可以全局优化任务分配

#### 缺点
- Orchestrator 成为瓶颈
- Orchestrator 的上下文可能仍然很大
- 单点故障

#### 适用场景
- 任务有清晰的依赖关系
- 需要集中式决策
- 子任务相对独立

### 2. Peer-to-Peer/Swarm 模式

#### 结构

```
Frontend <--> Backend <--> DevOps
    \           /\          /
     \         /  \        /
      Database     Testing
```

Agent 之间直接通信，无中央协调者。

#### 实现示例

```typescript
type Message = {
  from: string
  to: string
  content: string
  timestamp: number
}

class SwarmAgent {
  private name: string
  private peers: Set<string>
  private messageQueue: Message[]

  async run() {
    while (true) {
      // 1. 处理消息队列
      const messages = this.getMessages()

      // 2. 决定下一步行动
      const action = await this.decide(messages)

      if (action.type === "send_message") {
        await this.sendMessage(action.to, action.content)
      } else if (action.type === "execute_task") {
        const result = await this.executeTask(action.task)
        await this.broadcastResult(result)
      } else if (action.type === "complete") {
        break
      }

      await this.sleep(1000)
    }
  }

  private async decide(messages: Message[]): Promise<Action> {
    const context = this.buildContext(messages)

    const prompt = `
You are ${this.name}.
Your peers: ${Array.from(this.peers).join(', ')}

Recent messages:
${messages.map(m => `${m.from}: ${m.content}`).join('\n')}

Decide your next action:
1. Send a message to a peer (if you need information or coordination)
2. Execute a task (if you have enough context)
3. Complete (if your work is done)
`

    return await this.callLLM(prompt)
  }

  private async sendMessage(to: string, content: string) {
    const message: Message = {
      from: this.name,
      to,
      content,
      timestamp: Date.now()
    }

    await this.deliverMessage(message)
  }

  private async broadcastResult(result: string) {
    for (const peer of this.peers) {
      await this.sendMessage(peer, `Task completed: ${result}`)
    }
  }
}
```

#### 优点
- 无单点故障
- 可动态适应任务变化
- 高度并行

#### 缺点
- 难以调试（无全局视图）
- 可能出现死锁或活锁
- 消息爆炸（N² 复杂度）
- Token 成本极高

#### 适用场景
- 任务高度动态、难以预先分解
- 需要高容错性
- 有充足的 Token 预算

### 3. Hierarchical 模式

#### 结构

```
         RootOrchestrator
        /                \
  FrontendOrchestrator  BackendOrchestrator
      /       \              /       \
  UI      State         API      Database
 Agent    Agent        Agent     Agent
```

多层次的协调结构，结合了 Supervisor 和 Swarm 的特点。

#### 实现示例

```typescript
class HierarchicalAgent {
  private role: "leaf" | "intermediate" | "root"
  private children: HierarchicalAgent[]
  private parent: HierarchicalAgent | null

  async executeTask(task: string): Promise<string> {
    if (this.role === "leaf") {
      return await this.executeLeafTask(task)
    } else {
      return await this.orchestrateSubtasks(task)
    }
  }

  private async orchestrateSubtasks(task: string): Promise<string> {
    // 1. 分解任务
    const subtasks = await this.decompose(task)

    // 2. 分配给子 Agent
    const results = await Promise.all(
      subtasks.map((subtask, i) =>
        this.children[i].executeTask(subtask)
      )
    )

    // 3. 聚合结果
    return this.aggregate(results)
  }

  private async executeLeafTask(task: string): Promise<string> {
    return await this.callLLM(task)
  }
}
```

#### 优点
- 分层的上下文隔离
- 可扩展到大规模任务
- 中间层可以做局部优化

#### 缺点
- 复杂度高
- 层次设计需要仔细规划
- Token 成本增加

#### 适用场景
- 大规模、复杂任务
- 任务有自然的层次结构（如前端/后端/数据库）

## Token 经济学分析

### Token 成本对比

基于源项目的实测数据：

| 架构 | Token 倍数 | 成本基准 |
|------|-----------|---------|
| 单 Agent | 1× | $1.00 |
| 单 Agent + 工具 | 4× | $4.00 |
| 多 Agent (Supervisor) | 15× | $15.00 |
| 多 Agent (Swarm) | 30-50× | $30-50 |

**关键发现**：多 Agent 系统的 Token 成本是单 Agent 的 **15×**（Supervisor）到 **50×**（Swarm）。

### 成本分解

以 Supervisor 模式为例：

```
单次任务完成：

Orchestrator:
- 任务分解：5K tokens
- 调度决策：3K tokens
- 结果聚合：4K tokens
= 12K tokens

3个子 Agent:
- Agent 1: 20K tokens
- Agent 2: 25K tokens
- Agent 3: 18K tokens
= 63K tokens

总计：75K tokens

相比单 Agent 的 20K tokens，增加了 3.75×
```

但这还不是全部成本：

```
实际成本：

初始调用：75K tokens

Orchestrator 发现 Agent 2 的输出有问题，重新调用：
- Orchestrator 重新决策：5K tokens
- Agent 2 重新执行：25K tokens
= 30K tokens

总计：105K tokens → 5.25× vs 单 Agent
```

加上多次迭代和协调：

```
3次迭代后的总成本：
- Orchestrator: 12K × 3 = 36K
- 子 Agent: 63K × 3 = 189K
- 协调开销: 20K
= 245K tokens → 12×

考虑失败重试：
- 失败重试: 50K
= 295K tokens → 15× ✓
```

### 模型升级 vs Token 增加 vs 架构复杂性

源项目的关键洞察：**模型升级 > Token 增加 > 架构复杂性**

#### 对比实验

任务：实现一个完整的用户认证系统

| 策略 | 成本 | 完成率 | 质量评分 |
|------|------|-------|---------|
| GPT-4 + 单 Agent + 小上下文 | $2 | 60% | 6.5/10 |
| GPT-4 + 单 Agent + 大上下文 | $5 | 75% | 7.2/10 |
| GPT-4 + 多 Agent | $20 | 80% | 7.5/10 |
| **Sonnet 4.5 + 单 Agent + 中等上下文** | **$8** | **90%** | **8.5/10** |
| Sonnet 4.5 + 多 Agent | $35 | 95% | 8.8/10 |

**结论**：
1. 升级到更好的模型（GPT-4 → Sonnet 4.5）比增加上下文或使用多 Agent 架构更有效
2. 对于大多数任务，Sonnet 4.5 + 单 Agent 是最佳性价比选择
3. 只有在单 Agent 明确无法处理的超大任务时，才考虑多 Agent

### BrowseComp 的 95% 规则

BrowseComp 是一个浏览器自动化任务的评估基准，研究显示：

**性能方差来源**：
- Token 使用：**80%**
- 工具调用数：~10%
- 模型选择：~5%
- 其他：~5%

**启示**：
1. Token 优化是性能优化的关键（80% 方差）
2. 工具设计优化（减少调用次数）有中等收益（10%）
3. 模型选择的影响相对较小（5%），但升级到更智能的模型可以减少 Token 使用

## 架构选择决策树

### 何时使用单 Agent

```
任务特点：
✓ 上下文 < 80K tokens
✓ 任务逻辑连贯，难以分解
✓ 需要频繁的上下文共享
✓ 成本敏感

→ 使用单 Agent + 上下文优化
```

### 何时使用 Supervisor 模式

```
任务特点：
✓ 上下文 > 100K tokens
✓ 可明确分解为独立子任务
✓ 子任务之间依赖关系清晰
✓ 需要集中式监控和质量控制

→ 使用 Supervisor 模式 + 文件系统协调
```

### 何时使用 Swarm 模式

```
任务特点：
✓ 任务高度动态，难以预先分解
✓ 需要 Agent 之间频繁协商
✓ 容错性要求极高
✓ 有充足的 Token 预算

→ 使用 Swarm 模式（谨慎）
```

### 何时使用 Hierarchical 模式

```
任务特点：
✓ 超大规模任务（> 500K tokens）
✓ 有自然的层次结构
✓ 需要分层的质量控制
✓ 长期运行的项目

→ 使用 Hierarchical 模式
```

## 文件系统作为状态机

### 为什么用文件系统而非数据库

多 Agent 协调的核心挑战是状态同步。源项目推荐使用**文件系统作为状态机**。

**文件系统 vs 数据库**：

| 特性 | 文件系统 | 数据库 |
|------|---------|--------|
| 简单性 | 极简，易调试 | 复杂，需要 schema |
| 可见性 | 直接查看文件内容 | 需要查询工具 |
| 原子性 | 文件级原子写入 | 事务支持 |
| 并发 | 文件锁 | 复杂的并发控制 |
| 可恢复性 | 文件即状态，易恢复 | 需要备份策略 |

**结论**：对于 Agent 协调，文件系统足够且更简单。

### 实现示例

```typescript
class FileSystemStateManager {
  private baseDir: string

  async writeState(agentName: string, state: any): Promise<void> {
    const filePath = `${this.baseDir}/${agentName}_state.json`

    // 原子写入：先写到临时文件，再重命名
    const tempPath = `${filePath}.tmp`
    await writeFile(tempPath, JSON.stringify(state, null, 2))
    await rename(tempPath, filePath)
  }

  async readState(agentName: string): Promise<any> {
    const filePath = `${this.baseDir}/${agentName}_state.json`

    if (!await exists(filePath)) {
      return null
    }

    const content = await readFile(filePath, "utf-8")
    return JSON.parse(content)
  }

  async writeSharedDecision(decision: string): Promise<void> {
    const filePath = `${this.baseDir}/decisions.log`

    // 追加模式
    await appendFile(filePath, `${new Date().toISOString()}: ${decision}\n`)
  }

  async getSharedContext(): Promise<string> {
    const files = await readdir(this.baseDir)

    const context = await Promise.all(
      files.map(async (file) => {
        const content = await readFile(`${this.baseDir}/${file}`, "utf-8")
        return `<${file}>\n${content}\n</${file}>`
      })
    )

    return context.join("\n\n")
  }
}
```

### 状态文件结构

```
/tmp/multi_agent_state/
├── decisions.log              # 全局决策日志
├── frontend_state.json        # Frontend Agent 状态
├── backend_state.json         # Backend Agent 状态
├── devops_state.json          # DevOps Agent 状态
├── shared_context/
│   ├── api_spec.json          # 共享的 API 规格
│   └── database_schema.json   # 共享的数据库 schema
└── outputs/
    ├── frontend_output.txt
    ├── backend_output.txt
    └── devops_output.txt
```

## X-to-Book 案例架构简析

X-to-Book 是源项目中的完整多 Agent 系统案例，监控 Twitter 账户并生成日合成书籍。

### 架构

```
Orchestrator (20K context budget)
       |
       ├── Scraper (20K)
       ├── Analyzer (80K)
       ├── Writer (80K)
       └── Editor (60K)
```

### 关键设计

**1. 原始数据不通过 Orchestrator**

```
Scraper → 直接写入文件 → Analyzer 读取

而非：

Scraper → Orchestrator → Analyzer
```

这避免了 Orchestrator 的上下文爆炸。

**2. 时间知识图**

跟踪人物观点的演变：

```json
{
  "entity": "@elonmusk",
  "topic": "AI safety",
  "timeline": [
    {"date": "2025-01-01", "stance": "optimistic", "evidence": "..."},
    {"date": "2025-01-15", "stance": "concerned", "evidence": "..."}
  ]
}
```

**3. 观察掩蔽**

100K+ tokens 的推文数据不进入 Orchestrator：

```
Scraper 输出：
- 摘要：找到 1,234 条推文，关键主题：AI, 太空探索
- 完整数据：/tmp/tweets_20250101.json

Orchestrator 只看到摘要
```

**4. 整合工具**

3个整合工具而非 15+ 单一工具：

```typescript
// 不好的做法：15个细粒度工具
tools: [
  "get_user_profile",
  "get_user_tweets",
  "get_tweet_replies",
  "get_tweet_likes",
  "get_user_followers",
  // ... 10 more
]

// 好的做法：3个整合工具
tools: [
  "scrape_user_activity",  // 整合所有抓取操作
  "analyze_content",        // 整合所有分析操作
  "generate_book"           // 整合所有生成操作
]
```

**5. 5维评估框架**

```typescript
type Evaluation = {
  accuracy: number      // 事实准确性
  completeness: number  // 覆盖完整性
  coherence: number     // 叙事连贯性
  insight: number       // 洞察深度
  style: number         // 写作质量
}
```

详细的案例分析见[第6篇](/pages/agent/x-to-book-case-study)。

## 实践检查清单

### 架构选择检查
- [ ] 任务是否真的需要多 Agent？（单 Agent + 优化是否足够？）
- [ ] 选择的模式是否匹配任务特点？
- [ ] Token 成本增加是否可接受？

### Token 经济学检查
- [ ] 是否考虑了模型升级作为替代方案？
- [ ] 是否量化了多 Agent 的 Token 成本？
- [ ] 是否有成本监控机制？

### 协调机制检查
- [ ] 是否使用文件系统作为状态机？
- [ ] 状态文件结构是否清晰？
- [ ] 是否避免了大量数据通过 Orchestrator？

### 上下文隔离检查
- [ ] 每个 Agent 的上下文是否独立？
- [ ] 共享上下文是否最小化？
- [ ] 是否使用了观察掩蔽？

## 下一步

理解了多 Agent 架构后，下一篇将探讨如何设计内存系统和工具，以支持复杂的 Agent 行为。

下一篇：[04. 内存系统与工具设计](/pages/agent/memory-tool-design)

## 参考资料

- [Agent Skills - Multi-Agent Patterns](https://github.com/muratcankoylan/Agent-Skills-for-Context-Engineering/tree/main/skills/multi-agent-patterns)
- [X-to-Book System Example](https://github.com/muratcankoylan/Agent-Skills-for-Context-Engineering/tree/main/examples/x-to-book-system)
- [BrowseComp Benchmark](https://arxiv.org/abs/2408.01869)
