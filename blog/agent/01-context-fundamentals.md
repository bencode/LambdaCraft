# 上下文基础：注意力机制与失败模式

系列：[Agent 上下文工程实践指南](/pages/agent/index) | 第 1 篇

## 引言

在 Agent 系统中，上下文不仅仅是 Token 的集合，而是模型注意力的载体。理解上下文的本质、注意力机制的限制，以及已知的失效模式，是设计可靠 Agent 系统的基础。

本文整合了源项目中 `context-fundamentals` 和 `context-degradation` 两个核心技能的内容。

## 上下文的组成部分

Agent 系统的上下文由多个部分构成：

```
完整上下文 = 系统提示 + 工具定义 + 消息历史 + 检索文档 + 工具输出
```

### 各部分占比（实测数据）

| 组成部分 | 典型占比 | 特点 |
|---------|---------|------|
| 系统提示 | 5-15% | 静态，关键指令 |
| 工具定义 | 10-20% | 半静态，随技能扩展增长 |
| 消息历史 | 15-30% | 动态增长，需压缩 |
| 检索文档 | 10-25% | 动态注入，可能包含噪声 |
| 工具输出 | **30-50%** | 动态增长最快，83.9% 的 Agent 轨迹 Token |

关键发现：工具输出是上下文增长的主要来源，优化此处收益最大。

## 注意力机制的本质

### 上下文窗口 ≠ 有效容量

虽然模型声称支持大上下文窗口，但注意力质量随位置和密度变化：

```
声称窗口：100K tokens
有效容量：取决于信息密度、位置、模型架构
```

### RULER 基准测试数据

RULER（Rule-based Length Understanding Evaluation）是评估长上下文性能的基准测试。

**核心发现**：
- 只有 **50%** 的声称支持 32K+ 上下文的模型在 32K Token 时保持令人满意的性能
- 性能降级并非线性，而是存在**阈值效应**

**主流模型阈值**（2025年数据）：

| 模型 | 声称窗口 | 令人满意阈值 | 严重降级阈值 |
|------|---------|-------------|-------------|
| Claude Opus 4.5 | 200K | ~100K | ~180K |
| Gemini 3 Pro | 1M | ~500K | ~800K |
| GPT-5.2 (thinking) | 128K | ~100K | ~120K |
| GPT-4 Turbo | 128K | ~60K | ~100K |

**启示**：
1. 不要假设模型能有效利用全部声称的窗口
2. 设计时留出安全边际（建议使用声称窗口的 50-70%）
3. Thinking 模式虽然窗口小，但在有效范围内表现最佳

## 四个主要失败模式

### 1. Lost-in-Middle（中间遗失）

#### 现象

模型对上下文中间位置的信息召回率显著低于开头和结尾。

**量化数据**：
- 开头位置召回率：85-95%
- 中间位置召回率：**45-55%**（降低 10-40%）
- 结尾位置召回率：80-90%

#### 原理

Transformer 注意力机制的位置编码和因果掩码导致中间位置的注意力权重衰减。

#### 应对策略

**策略1：关键信息放在注意力有利位置**

```markdown
<!-- 系统提示结构 -->
<core_instructions>
关键指令1
关键指令2
</core_instructions>

<context>
大量背景信息...
</context>

<critical_reminder>
<!-- 重复关键指令 -->
记住：关键指令1、关键指令2
</critical_reminder>
```

**策略2：分段处理长文档**

```python
def process_long_document(doc: str, chunk_size: int = 4000):
    chunks = split_into_chunks(doc, chunk_size)
    results = []

    for i, chunk in enumerate(chunks):
        context = f"""
        Processing part {i+1}/{len(chunks)}

        Previous summary: {results[-1] if results else 'N/A'}

        Current chunk:
        {chunk}
        """
        results.append(process_chunk(context))

    return merge_results(results)
```

**策略3：显式引用关键信息**

```markdown
在回答问题时，请参考以下关键事实（第3段）：
- 事实1
- 事实2

[大量其他信息...]

问题：基于第3段的事实，分析...
```

### 2. 上下文毒化（Context Poisoning）

#### 现象

错误或矛盾的信息污染整个上下文，导致模型输出不可靠。

**常见来源**：
- 检索系统返回的无关文档
- 早期对话中的错误陈述
- 工具输出中的格式错误或异常值

#### 应对策略

**策略1：信息源标注**

```markdown
<retrieved_docs confidence="high">
文档1（来源：官方文档，最后更新：2025-01）
</retrieved_docs>

<retrieved_docs confidence="low">
文档2（来源：社区博客，未验证）
</retrieved_docs>

<user_statement verified="false">
用户声称：...
</user_statement>
```

**策略2：矛盾检测与解决**

```python
type ContextEntry = {
    content: str
    source: str
    confidence: float
    timestamp: str
}

def detect_contradictions(entries: list[ContextEntry]) -> list[str]:
    contradictions = []

    for i, entry1 in enumerate(entries):
        for entry2 in entries[i+1:]:
            if is_contradictory(entry1.content, entry2.content):
                # 保留高置信度来源
                if entry1.confidence > entry2.confidence:
                    contradictions.append(entry2.content)
                else:
                    contradictions.append(entry1.content)

    return contradictions
```

**策略3：错误纠正日志**

```markdown
<correction_log>
- [轮次3] 纠正：API端点应为 /v2/users 而非 /v1/users
- [轮次5] 纠正：参数类型为 integer 而非 string
</correction_log>
```

### 3. 分心（Distraction）

#### 现象

无关信息占用注意力预算，降低对关键任务的聚焦度。

**信噪比量化**：

```
信噪比 = 任务相关 Token / 总 Token
```

目标：保持信噪比 > 0.7

#### 应对策略

**策略1：任务相关性过滤**

```python
def filter_relevant_context(
    query: str,
    context_items: list[str],
    relevance_threshold: float = 0.7
) -> list[str]:
    scored_items = [
        (item, compute_relevance(query, item))
        for item in context_items
    ]

    return [
        item for item, score in scored_items
        if score >= relevance_threshold
    ]
```

**策略2：上下文分层**

```markdown
<essential>
核心任务：实现用户认证
关键约束：使用 JWT，支持刷新令牌
</essential>

<helpful>
相关背景：当前使用 session-based 认证
技术栈：Node.js + Express
</helpful>

<optional>
团队偏好：优先使用 TypeScript
代码风格：遵循 Airbnb 规范
</optional>
```

**策略3：渐进式信息披露**

```markdown
# 初始提示
任务：优化数据库查询性能

# 第一轮响应后，根据需要添加
<additional_context>
数据库：PostgreSQL 14
当前查询时间：2.3秒
表大小：500万行
</additional_context>
```

### 4. 冲突（Clash）

#### 现象

不同来源的指令或信息相互矛盾，导致模型行为不确定。

**典型冲突**：
- 系统提示 vs 用户指令
- 早期指令 vs 后续指令
- 工具文档 vs 实际行为

#### 应对策略

**策略1：优先级明确声明**

```markdown
<instruction_priority>
1. 安全规则（不可覆盖）
2. 系统提示
3. 项目配置
4. 用户当前轮次指令
5. 历史对话中的指令
</instruction_priority>

<safety_rules>
- 绝不执行删除生产数据库的操作
- 绝不暴露 API 密钥
</safety_rules>
```

**策略2：冲突显式处理**

```markdown
用户在第3轮要求使用 REST API，但第7轮要求使用 GraphQL。
检测到冲突。

请确认：
A. 使用 GraphQL（最新指令）
B. 使用 REST API（早期指令）
C. 同时支持两者
```

**策略3：配置文件作为单一真相来源**

```typescript
// project-config.json
{
  "architecture": "microservices",
  "api_style": "REST",
  "auth_method": "JWT",
  "database": "PostgreSQL"
}
```

```markdown
所有架构决策以 project-config.json 为准。
用户临时指令如与配置冲突，需显式确认是否更新配置。
```

## 失败模式的组合效应

实际系统中，多个失败模式常同时出现：

```
案例：检索增强生成（RAG）

Lost-in-Middle：检索的10个文档中，第5-7个文档召回率低
↓
分心：10个文档中有3个不相关
↓
毒化：不相关文档包含错误信息
↓
冲突：文档3和文档8对同一问题有不同答案
```

**综合应对**：
1. 检索阶段：提高召回精度，减少无关文档（应对分心）
2. 排序阶段：相关文档放在开头/结尾位置（应对 Lost-in-Middle）
3. 标注阶段：标记来源和置信度（应对毒化和冲突）
4. 提示阶段：显式指导如何处理矛盾（应对冲突）

## 设计原则

基于上述失败模式，总结出以下设计原则：

### 1. 结构强制原则

不依赖模型记住信息，而是通过结构强制保留。

```markdown
<!-- 不好的做法 -->
记住以下信息：项目使用 TypeScript，数据库是 PostgreSQL...

<!-- 好的做法 -->
<project_config>
language: TypeScript
database: PostgreSQL
</project_config>

在每次响应中，必须引用 <project_config> 以确保一致性。
```

### 2. 冗余放置原则

关键信息在开头和结尾都放置。

```markdown
<core_task>
实现用户认证
</core_task>

[大量背景信息和工具定义...]

<reminder>
核心任务：实现用户认证
当前步骤：设计 JWT 刷新机制
</reminder>
```

### 3. 信噪比优先原则

优先减少无关信息，而非增加相关信息。

```python
# 优先级
1. 移除无关内容（提高信噪比）
2. 压缩相关内容（保留信号）
3. 添加新内容（最后考虑）
```

### 4. 分层加载原则

根据任务阶段按需加载上下文。

```markdown
# 规划阶段
<context_mode="planning">
高层需求和架构约束
</context_mode>

# 实现阶段
<context_mode="implementation">
详细的 API 文档和代码示例
</context_mode>
```

### 5. 显式优先级原则

当存在多个信息源时，显式声明优先级。

```markdown
信息优先级：
1. 系统提示中的安全规则
2. project-config.json
3. 用户当前轮次指令
4. 工具文档
5. 历史对话
```

## 实践检查清单

在设计 Agent 系统时，使用以下检查清单：

### Lost-in-Middle 检查
- [ ] 关键指令是否放在开头或结尾？
- [ ] 长文档是否分段处理？
- [ ] 是否显式引用了中间位置的关键信息？

### 上下文毒化检查
- [ ] 外部信息是否标注了来源和置信度？
- [ ] 是否有机制检测矛盾信息？
- [ ] 是否有错误纠正日志？

### 分心检查
- [ ] 上下文的信噪比是否 > 0.7？
- [ ] 是否过滤了无关信息？
- [ ] 是否使用了渐进式披露？

### 冲突检查
- [ ] 是否显式声明了指令优先级？
- [ ] 配置文件是否作为单一真相来源？
- [ ] 冲突发生时是否有解决机制？

## 下一步

理解了上下文的基础和失败模式后，下一篇将探讨如何通过压缩和优化策略来管理有限的上下文预算。

下一篇：[02. 上下文压缩与优化策略](/pages/agent/context-optimization)

## 参考资料

- [RULER: What's the Real Context Size of Your Long-Context Language Models?](https://arxiv.org/abs/2404.06654)
- [Lost in the Middle: How Language Models Use Long Contexts](https://arxiv.org/abs/2307.03172)
- [Agent Skills - Context Fundamentals](https://github.com/muratcankoylan/Agent-Skills-for-Context-Engineering/tree/main/skills/context-fundamentals)
- [Agent Skills - Context Degradation](https://github.com/muratcankoylan/Agent-Skills-for-Context-Engineering/tree/main/skills/context-degradation)
