# 评估框架：LLM-as-Judge 实践

系列：[Agent 上下文工程实践指南](/pages/agent/index) | 第 5 篇

上一篇：[04. 内存系统与工具设计](/pages/agent/memory-tool-design)

## 引言

构建 Agent 系统后，如何评估其质量？人工评估成本高且难以扩展，LLM-as-Judge 提供了一个可扩展的解决方案。本文探讨多维度评分框架、三种评估方法、偏见缓解技术，以及生产级的 TypeScript 实现。

本文整合了源项目中 `evaluation` 和 `advanced-evaluation` 两个核心技能，以及 `llm-as-judge-skills` 示例的内容。

## 为什么需要 LLM-as-Judge

### 传统评估方法的局限

| 方法 | 优点 | 缺点 |
|------|------|------|
| 人工评估 | 准确、可信 | 成本高、速度慢、难以扩展 |
| 规则评估 | 快速、确定 | 无法评估创造性、复杂性 |
| 指标评估 | 客观、可重复 | 无法捕捉质量的细微差别 |

### LLM-as-Judge 的优势

- **可扩展**：自动化评估，无需人工干预
- **一致性**：评分标准统一（相比多个人类评估者）
- **细粒度**：可以评估复杂的质量维度
- **可定制**：根据任务调整评估标准

### 适用场景

- 快速迭代开发，需要持续评估
- 大规模 A/B 测试
- 回归测试（确保更新没有降低质量）
- 初步筛选（人工评估前的过滤）

## 多维度评分框架

### 单一评分的问题

```
评分：7/10
```

这无法回答：
- 哪些方面做得好？
- 哪些方面需要改进？
- 为什么是 7 而不是 6 或 8？

### 多维度评分

```typescript
type MultiDimensionalScore = {
  accuracy: number       // 事实准确性
  completeness: number   // 覆盖完整性
  efficiency: number     // Token 和步骤效率
  quality: number        // 输出质量（代码、文本）
  process: number        // 推理过程质量
  overall: number        // 加权总分
}
```

### X-to-Book 的 5 维评估

```typescript
type BookEvaluation = {
  accuracy: number       // 事实准确性（与原推文一致性）
  completeness: number   // 覆盖完整性（是否包含关键观点）
  coherence: number      // 叙事连贯性（章节衔接）
  insight: number        // 洞察深度（是否发现模式/趋势）
  style: number          // 写作质量（可读性、引人入胜）

  overall: number        // 加权平均
  weights: {
    accuracy: 0.3
    completeness: 0.2
    coherence: 0.2
    insight: 0.15
    style: 0.15
  }
}
```

### 加权计算

```typescript
function calculateOverall(scores: MultiDimensionalScore): number {
  const weights = {
    accuracy: 0.3,
    completeness: 0.2,
    efficiency: 0.15,
    quality: 0.2,
    process: 0.15
  }

  return Object.entries(weights).reduce((total, [dim, weight]) => {
    return total + scores[dim] * weight
  }, 0)
}
```

## 三种评估方法

### 1. 直接评分（Direct Scoring）

#### 原理

要求 LLM 直接对输出进行评分。

#### 提示模板

```typescript
const directScoringPrompt = `
You are an expert evaluator for AI Agent systems.

Task: ${task}
Agent Output: ${output}
Reference (if available): ${reference}

Evaluate the agent output across these dimensions:
1. Accuracy (0-10): Is the output factually correct?
2. Completeness (0-10): Does it fully address the task?
3. Quality (0-10): Is the output well-structured and clear?

For each dimension:
- Provide a score (0-10)
- Explain your reasoning (2-3 sentences)

Use this format:
<evaluation>
<accuracy>
<score>8</score>
<reasoning>The output correctly identifies...</reasoning>
</accuracy>
<completeness>
<score>7</score>
<reasoning>The output addresses most requirements but misses...</reasoning>
</completeness>
<quality>
<score>9</score>
<reasoning>The output is well-structured with...</reasoning>
</quality>
</evaluation>
`
```

#### 实现

```typescript
import { z } from "zod"

const DimensionScoreSchema = z.object({
  score: z.number().min(0).max(10),
  reasoning: z.string().min(10)
})

const DirectScoringSchema = z.object({
  accuracy: DimensionScoreSchema,
  completeness: DimensionScoreSchema,
  quality: DimensionScoreSchema
})

type DirectScoringResult = z.infer<typeof DirectScoringSchema>

async function directScoring(
  task: string,
  output: string,
  reference?: string
): Promise<DirectScoringResult> {
  const prompt = buildDirectScoringPrompt(task, output, reference)

  const response = await callLLM(prompt, {
    model: "claude-sonnet-4-5",
    temperature: 0.3  // 低温度保证一致性
  })

  const parsed = parseXMLResponse(response)

  // Zod 验证
  return DirectScoringSchema.parse(parsed)
}
```

#### 优点
- 实现简单
- 获得具体的评分和理由

#### 缺点
- 容易受绝对评分标准的主观性影响
- 不同批次的评分可能不一致（即使同一输出）

### 2. 配对比较（Pairwise Comparison）

#### 原理

要求 LLM 比较两个输出，选择更好的一个。相比直接评分，配对比较更稳定。

#### 提示模板

```typescript
const pairwiseComparisonPrompt = `
You are comparing two AI Agent outputs for the same task.

Task: ${task}

Output A:
${outputA}

Output B:
${outputB}

Which output is better overall? Consider:
- Accuracy
- Completeness
- Quality

Think step-by-step, then choose:
A. Output A is better
B. Output B is better
C. They are roughly equal

Provide your reasoning (3-5 sentences), then state your choice.

Format:
<comparison>
<reasoning>...</reasoning>
<choice>A</choice>
</comparison>
`
```

#### 实现

```typescript
type PairwiseResult = {
  winner: "A" | "B" | "tie"
  reasoning: string
  confidence: number  // 0-1
}

const PairwiseSchema = z.object({
  reasoning: z.string(),
  choice: z.enum(["A", "B", "C"])
})

async function pairwiseComparison(
  task: string,
  outputA: string,
  outputB: string,
  swapPositions: boolean = false
): Promise<PairwiseResult> {
  // 位置偏见缓解：交换位置评估两次
  const [first, second] = swapPositions ? [outputB, outputA] : [outputA, outputB]

  const prompt = buildPairwisePrompt(task, first, second)

  const response = await callLLM(prompt, {
    model: "claude-sonnet-4-5",
    temperature: 0.3
  })

  const parsed = PairwiseSchema.parse(parseXMLResponse(response))

  let winner: "A" | "B" | "tie"
  if (parsed.choice === "C") {
    winner = "tie"
  } else if (swapPositions) {
    // 交换回来
    winner = parsed.choice === "A" ? "B" : "A"
  } else {
    winner = parsed.choice
  }

  return {
    winner,
    reasoning: parsed.reasoning,
    confidence: 0.8  // 可通过多次评估计算真实置信度
  }
}
```

#### 位置偏见缓解

LLM 存在**位置偏见**：倾向于选择第一个或最后一个选项。

**解决方案**：交换位置评估两次

```typescript
async function pairwiseComparisonWithBiasMitigation(
  task: string,
  outputA: string,
  outputB: string
): Promise<PairwiseResult> {
  // 第一次：A在前，B在后
  const result1 = await pairwiseComparison(task, outputA, outputB, false)

  // 第二次：B在前，A在后
  const result2 = await pairwiseComparison(task, outputA, outputB, true)

  // 一致性检查
  if (result1.winner === result2.winner) {
    return {
      ...result1,
      confidence: 0.95  // 两次一致，高置信度
    }
  } else if (result1.winner === "tie" || result2.winner === "tie") {
    return {
      winner: "tie",
      reasoning: "Inconsistent results suggest outputs are roughly equal",
      confidence: 0.5
    }
  } else {
    return {
      winner: "tie",
      reasoning: "Position bias detected - conflicting results",
      confidence: 0.3
    }
  }
}
```

#### 优点
- 比直接评分更稳定
- 适合偏好排序（如选择最佳提示）

#### 缺点
- 需要成对比较，O(n²) 复杂度
- 不能提供绝对评分

### 3. 标准生成（Rubric Generation）

#### 原理

先让 LLM 生成评分标准（rubric），再基于标准评分。这使评分更一致和可解释。

#### 第一步：生成标准

```typescript
const rubricGenerationPrompt = `
You are creating an evaluation rubric for the following task:

Task: ${task}
Sample Outputs:
${sampleOutputs}

Generate a detailed rubric with 3-5 dimensions.
For each dimension, provide:
1. Name and description
2. 3 levels: Excellent (8-10), Adequate (5-7), Poor (0-4)
3. Specific criteria for each level

Format:
<rubric>
<dimension name="accuracy">
<description>...</description>
<excellent>...</excellent>
<adequate>...</adequate>
<poor>...</poor>
</dimension>
...
</rubric>
`
```

#### 第二步：基于标准评分

```typescript
const rubricBasedScoringPrompt = `
Using the following rubric:
${rubric}

Evaluate this output:
Task: ${task}
Output: ${output}

For each dimension in the rubric:
1. Determine which level the output falls into
2. Assign a specific score within that level's range
3. Explain your reasoning

Format:
<evaluation>
<dimension name="accuracy">
<level>excellent</level>
<score>9</score>
<reasoning>...</reasoning>
</dimension>
...
</evaluation>
`
```

#### 完整实现

```typescript
type RubricDimension = {
  name: string
  description: string
  levels: {
    excellent: string
    adequate: string
    poor: string
  }
}

type Rubric = RubricDimension[]

async function generateRubric(
  task: string,
  sampleOutputs: string[]
): Promise<Rubric> {
  const prompt = buildRubricGenerationPrompt(task, sampleOutputs)

  const response = await callLLM(prompt, {
    model: "claude-sonnet-4-5",
    temperature: 0.7  // 略高温度鼓励创造性
  })

  return parseRubric(response)
}

async function scoreWithRubric(
  rubric: Rubric,
  task: string,
  output: string
): Promise<MultiDimensionalScore> {
  const prompt = buildRubricBasedScoringPrompt(rubric, task, output)

  const response = await callLLM(prompt, {
    model: "claude-sonnet-4-5",
    temperature: 0.3
  })

  return parseScores(response)
}

// 完整流程
async function evaluateWithRubric(
  task: string,
  outputs: string[]
): Promise<MultiDimensionalScore[]> {
  // 1. 用前几个样本生成标准
  const samples = outputs.slice(0, 3)
  const rubric = await generateRubric(task, samples)

  // 2. 用标准评估所有输出
  const scores = await Promise.all(
    outputs.map(output => scoreWithRubric(rubric, task, output))
  )

  return scores
}
```

#### 优点
- 评分标准明确、可解释
- 一致性高（同一标准评估所有输出）
- 标准可复用和迭代

#### 缺点
- 需要额外的 LLM 调用生成标准
- 标准质量取决于样本质量

## 链式思维提示（Chain-of-Thought）

### 原理

要求 LLM 在评分前先解释推理过程，可显著提高评分质量。

### 对比

**无 CoT**：

```
Output: [agent output]
Score it from 0-10.
```

**有 CoT**：

```
Output: [agent output]

Think step-by-step:
1. What are the key requirements of the task?
2. Which requirements does the output fulfill?
3. Which requirements does it miss?
4. Are there any errors or issues?

Based on your analysis, assign a score from 0-10 and explain.
```

### 实现

```typescript
const chainOfThoughtPrompt = `
Task: ${task}
Output: ${output}

Evaluate step-by-step:

Step 1: Identify Key Requirements
List the 3-5 most important requirements for this task.

Step 2: Analyze Fulfillment
For each requirement, does the output fulfill it? (Yes/Partial/No)

Step 3: Identify Issues
List any errors, omissions, or quality issues.

Step 4: Determine Score
Based on the above analysis, what score (0-10) does this output deserve?

Format:
<evaluation>
<requirements>
- Requirement 1
- Requirement 2
...
</requirements>

<fulfillment>
- Requirement 1: Yes/Partial/No - explanation
- Requirement 2: Yes/Partial/No - explanation
...
</fulfillment>

<issues>
- Issue 1
- Issue 2
...
</issues>

<score>8</score>
<reasoning>Based on the analysis above...</reasoning>
</evaluation>
`
```

### 性能对比

基于源项目的实验数据：

| 方法 | 与人类评分的相关性 | 一致性（同一输出重复评估）|
|------|-------------------|----------------------|
| 无 CoT | 0.65 | 0.72 |
| 有 CoT | 0.82 | 0.88 |

链式思维显著提高了评分质量和一致性。

## 端状态评估 vs 过程评估

### 端状态评估（End-State Evaluation）

**定义**：仅评估最终输出，不考虑过程。

**适用场景**：
- 最终结果质量是唯一关心的
- 过程难以评估或不可见
- 效率优先（端状态评估更快）

**示例**：

```typescript
async function endStateEvaluation(
  task: string,
  finalOutput: string
): Promise<number> {
  const prompt = `
Task: ${task}
Final Output: ${finalOutput}

Evaluate only the final output quality (0-10).
Ignore how it was produced.
`

  const response = await callLLM(prompt)
  return parseScore(response)
}
```

### 过程评估（Process Evaluation）

**定义**：评估达成最终输出的过程质量。

**适用场景**：
- 过程本身很重要（如教育、可解释性）
- 需要识别低效模式
- 优化 Agent 行为

**示例**：

```typescript
type ProcessEvaluation = {
  planning_quality: number      // 规划质量
  tool_usage_efficiency: number // 工具使用效率
  error_recovery: number         // 错误恢复能力
  final_output: number           // 最终输出质量
}

async function processEvaluation(
  task: string,
  agentTrace: AgentTrace
): Promise<ProcessEvaluation> {
  const prompt = `
Task: ${task}

Agent Trace:
${formatTrace(agentTrace)}

Evaluate the agent's process:
1. Planning Quality: Did it make a good plan before acting?
2. Tool Usage: Did it use tools efficiently?
3. Error Recovery: How well did it handle errors?
4. Final Output: Quality of the end result

Provide scores (0-10) for each dimension.
`

  const response = await callLLM(prompt)
  return parseProcessScores(response)
}
```

### 组合评估

最佳实践是同时评估端状态和过程：

```typescript
type ComprehensiveEvaluation = {
  end_state: MultiDimensionalScore
  process: ProcessEvaluation
  overall: number
}

async function comprehensiveEvaluation(
  task: string,
  agentTrace: AgentTrace,
  finalOutput: string
): Promise<ComprehensiveEvaluation> {
  const [endState, process] = await Promise.all([
    endStateEvaluation(task, finalOutput),
    processEvaluation(task, agentTrace)
  ])

  const overall = endState.overall * 0.7 + process.final_output * 0.3

  return { end_state: endState, process, overall }
}
```

## 生产级实现：EvaluatorAgent

基于源项目的 `llm-as-judge-skills` 示例：

```typescript
type EvaluationMethod = "direct" | "pairwise" | "rubric"

type EvaluatorConfig = {
  method: EvaluationMethod
  model: string
  temperature: number
  dimensions: string[]
  enableCoT: boolean
  mitigateBias: boolean
}

class EvaluatorAgent {
  private config: EvaluatorConfig

  constructor(config: EvaluatorConfig) {
    this.config = config
  }

  async evaluate(
    task: string,
    output: string,
    reference?: string
  ): Promise<MultiDimensionalScore> {
    switch (this.config.method) {
      case "direct":
        return await this.directScoring(task, output, reference)
      case "pairwise":
        throw new Error("Pairwise requires two outputs")
      case "rubric":
        return await this.rubricBasedScoring(task, output)
      default:
        throw new Error(`Unknown method: ${this.config.method}`)
    }
  }

  async compare(
    task: string,
    outputA: string,
    outputB: string
  ): Promise<PairwiseResult> {
    if (this.config.method !== "pairwise") {
      throw new Error("Method must be pairwise for comparison")
    }

    if (this.config.mitigateBias) {
      return await pairwiseComparisonWithBiasMitigation(task, outputA, outputB)
    } else {
      return await pairwiseComparison(task, outputA, outputB)
    }
  }

  async evaluateBatch(
    task: string,
    outputs: string[]
  ): Promise<MultiDimensionalScore[]> {
    if (this.config.method === "rubric") {
      return await evaluateWithRubric(task, outputs)
    } else {
      return await Promise.all(
        outputs.map(output => this.evaluate(task, output))
      )
    }
  }

  private async directScoring(
    task: string,
    output: string,
    reference?: string
  ): Promise<MultiDimensionalScore> {
    const prompt = this.buildPrompt(task, output, reference)

    const response = await this.callLLM(prompt)

    return this.parseResponse(response)
  }

  private buildPrompt(
    task: string,
    output: string,
    reference?: string
  ): string {
    let prompt = `Task: ${task}\nOutput: ${output}\n`

    if (reference) {
      prompt += `Reference: ${reference}\n`
    }

    if (this.config.enableCoT) {
      prompt += this.getCoTInstructions()
    }

    prompt += this.getDimensionInstructions()

    return prompt
  }

  private getCoTInstructions(): string {
    return `
Think step-by-step:
1. What are the key requirements?
2. Which requirements are fulfilled?
3. What issues exist?
4. What score is justified?
`
  }

  private getDimensionInstructions(): string {
    return `
Evaluate across these dimensions (0-10 each):
${this.config.dimensions.map(d => `- ${d}`).join('\n')}

Provide score and reasoning for each.
`
  }

  private async callLLM(prompt: string): Promise<string> {
    return await callLLM(prompt, {
      model: this.config.model,
      temperature: this.config.temperature
    })
  }

  private parseResponse(response: string): MultiDimensionalScore {
    const parsed = parseXMLResponse(response)

    const scores: Record<string, number> = {}
    for (const dim of this.config.dimensions) {
      scores[dim] = parsed[dim].score
    }

    return {
      ...scores,
      overall: calculateOverall(scores)
    } as MultiDimensionalScore
  }
}
```

### 使用示例

```typescript
// 配置评估器
const evaluator = new EvaluatorAgent({
  method: "direct",
  model: "claude-sonnet-4-5",
  temperature: 0.3,
  dimensions: ["accuracy", "completeness", "quality"],
  enableCoT: true,
  mitigateBias: false
})

// 评估单个输出
const score = await evaluator.evaluate(
  "Implement user authentication",
  agentOutput
)

console.log(score)
// {
//   accuracy: 8,
//   completeness: 7,
//   quality: 9,
//   overall: 8.0
// }

// 批量评估
const allScores = await evaluator.evaluateBatch(
  "Implement user authentication",
  [output1, output2, output3]
)

// 配对比较
const pairwiseEvaluator = new EvaluatorAgent({
  method: "pairwise",
  model: "claude-sonnet-4-5",
  temperature: 0.3,
  dimensions: [],
  enableCoT: true,
  mitigateBias: true  // 启用位置偏见缓解
})

const winner = await pairwiseEvaluator.compare(
  "Implement user authentication",
  output1,
  output2
)

console.log(winner)
// {
//   winner: "A",
//   reasoning: "Output A provides more complete error handling...",
//   confidence: 0.95
// }
```

## 实践检查清单

### 评估设计检查
- [ ] 是否使用多维度评分而非单一评分？
- [ ] 评估维度是否与任务目标对齐？
- [ ] 是否选择了合适的评估方法（direct/pairwise/rubric）？

### 质量保证检查
- [ ] 是否使用链式思维提示？
- [ ] 是否缓解了位置偏见（如果使用配对比较）？
- [ ] 是否使用 Zod 等工具验证评估输出格式？

### 效率检查
- [ ] 评估是否自动化？
- [ ] 是否支持批量评估？
- [ ] 是否记录评估历史以便分析？

## 下一步

理解了评估框架后，下一篇将深入分析 X-to-Book 完整案例，展示如何综合应用前面学到的所有技术。

下一篇：[06. 实战：X-to-Book 多 Agent 系统设计](/pages/agent/x-to-book-case-study)

## 参考资料

- [Agent Skills - Evaluation](https://github.com/muratcankoylan/Agent-Skills-for-Context-Engineering/tree/main/skills/evaluation)
- [Agent Skills - Advanced Evaluation](https://github.com/muratcankoylan/Agent-Skills-for-Context-Engineering/tree/main/skills/advanced-evaluation)
- [LLM-as-Judge Skills (TypeScript)](https://github.com/muratcankoylan/Agent-Skills-for-Context-Engineering/tree/main/examples/llm-as-judge-skills)
- [Judging LLM-as-a-Judge with MT-Bench and Chatbot Arena](https://arxiv.org/abs/2306.05685)
