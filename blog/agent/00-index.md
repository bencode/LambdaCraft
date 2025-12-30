# Agent 上下文工程实践指南

本系列是对开源项目 [Agent Skills for Context Engineering](https://github.com/muratcankoylan/Agent-Skills-for-Context-Engineering) 的深度学习总结。

## 什么是上下文工程

**上下文工程**（Context Engineering）是一门通过有效管理语言模型有限的注意力预算，最大化 AI Agent 效能的学科。

它与**提示工程**（Prompt Engineering）的区别：

| 维度 | 提示工程 | 上下文工程 |
|------|---------|-----------|
| 关注点 | 单个提示词的设计 | 完整信息流的管理 |
| 优化目标 | 提升单次响应质量 | 优化整个任务生命周期 |
| 涉及范围 | 用户输入 | 系统提示、工具定义、检索文档、消息历史、工具输出 |
| 核心约束 | Token 限制 | 注意力预算的有限性 |

## 核心理念

### 注意力是约束，不是 Token 数量

更大的上下文窗口不一定提升性能。RULER 基准测试显示，只有 50% 声称支持 32K+ 上下文的模型在 32K Token 时保持令人满意的性能。

关键洞察：性能取决于**有限高信号 Token 集合的最优策展**，而非贪婪式填充上下文。

### 已知的失效模式是可预测的

- **Lost-in-Middle**：中间位置信息召回率比开头/结尾低 10-40%
- **上下文毒化**：错误信息污染整个上下文
- **分心**：无关信息占用注意力预算
- **冲突**：矛盾信息导致模型混淆

这些失效模式都有已证实的缓解策略。

### 结构强制质量

不依赖 LLM 记住一切，而是通过专用结构部分（文件、决策日志、下一步行动）强制保留关键信息。

## 系列结构

本系列包含8篇文章，涵盖上下文工程的完整知识体系：

### 基础篇

**01. 上下文基础：注意力机制与失败模式**
- 上下文的组成部分与注意力机制
- RULER 基准与模型阈值数据
- 4个失败模式及应对策略

**02. 上下文压缩与优化策略**
- 优化目标：tokens-per-task vs tokens-per-request
- 3种压缩方法对比
- 工具观察占比 83.9% 的启示
- 上下文预算管理框架

### 架构篇

**03. 多 Agent 架构模式与 Token 经济学**
- 3种架构模式：Supervisor/P2P/Hierarchical
- Token 成本分析：多 Agent 系统 15× vs 单 Agent
- 模型升级 > Token 增加 > 架构复杂性
- 上下文隔离的价值

**04. 内存系统与工具设计**
- 5层内存架构
- 工具设计的整合原则
- 文件系统作为状态机
- 观察掩蔽技术

### 实践篇

**05. 评估框架：LLM-as-Judge 实践**
- 多维度评分框架
- 3种评估方法：直接评分、配对比较、标准生成
- 位置偏见缓解
- 端状态评估 vs 过程评估

**06. 实战：X-to-Book 多 Agent 系统设计**
- 完整的多 Agent 系统案例分析
- Agent 配置与上下文预算分配
- 文件系统协调机制
- 5维评估框架

### 工程篇

**07. Agent 技能系统实现**
- 渐进式披露实现
- SKILL.md 格式规范
- 语义知识注册表
- 项目集成最佳实践

**08. 总结：8个关键洞察**
- 提炼源项目的核心智慧
- 实践建议与未来方向

## 如何阅读本系列

### 推荐路径

**快速理解**（2篇）：
- 01. 上下文基础
- 08. 关键洞察

**系统学习**（按序阅读）：
- 01 → 02 → 03 → 04 → 05 → 06 → 07 → 08

**问题导向**：
- 如何避免上下文失效？→ 01
- 如何降低 Token 成本？→ 02
- 多 Agent 如何设计？→ 03
- 如何设计工具和内存？→ 04
- 如何评估 Agent 质量？→ 05
- 完整案例参考？→ 06
- 如何实现技能系统？→ 07

### 前置知识

- 基础的 LLM 概念（Token、提示词、上下文窗口）
- Agent 基本概念（工具调用、推理循环）
- 可选：TypeScript/Python 基础（阅读代码示例）

## 源项目介绍

[Agent Skills for Context Engineering](https://github.com/muratcankoylan/Agent-Skills-for-Context-Engineering) 是一个综合性的开源 Agent Skills 集合，由 Muratcan Koylan 及社区贡献者维护。

项目包含：
- **10个核心技能**：涵盖基础理论、架构设计、运营实践
- **3个完整示例**：X-to-Book 系统、LLM-as-Judge、Book SFT Pipeline
- **语义知识注册表**：基于 PostgreSQL + pgvector 的技能索引
- **2,674 行技能文档**：详细的理论、实践和参考资料

## 数据来源

本系列引用的数据和研究来自：
- RULER 基准测试（上下文窗口性能评估）
- Lost-in-Middle 现象研究
- BrowseComp 评估研究
- 源项目的实战案例数据

## 版本说明

- 源项目最后更新：2025-12-20
- 本系列创建时间：2025-12-30
- 基于模型：Claude Opus 4.5、Gemini 3 Pro 等最新模型的研究数据

## 参考资源

- [源项目 GitHub](https://github.com/muratcankoylan/Agent-Skills-for-Context-Engineering)
- [Agent Skills 开放标准](https://github.com/skills)
- [RULER 基准测试](https://arxiv.org/abs/2404.06654)

下一篇：[01. 上下文基础：注意力机制与失败模式](01-context-fundamentals.md)
