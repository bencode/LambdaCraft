^{:nextjournal.clerk/visibility {:code :hide}}
(ns pages.dev-notes
  "日常开发总结"
  {:nextjournal.clerk/toc true}
  (:require [nextjournal.clerk :as clerk]))

;; # 日常开发总结

;; 记录日常开发中的实践、思考和总结。

;; ## AI编程的系列

;; 探讨 AI 编程工具的使用方式和最佳实践。

;; - [为什么仅靠 VibeCoding 很难构建复杂系统](/pages/ai-programming/vibe-coding) - 系统结构、设计决策、协作方式 (2025-12-05)

;; ## Agent 上下文工程实践指南

;; 基于 Agent Skills for Context Engineering 项目的深度学习总结，系统化介绍如何通过上下文工程构建可靠的 AI Agent 系统。

;; - [系列导读](/blog/agent/00-index) - 上下文工程 vs 提示工程、核心理念、系列结构 (2025-12-30)
;; - [01. 上下文基础：注意力机制与失败模式](/blog/agent/01-context-fundamentals) - RULER基准、Lost-in-Middle、4个失败模式 (2025-12-30)
;; - [02. 上下文压缩与优化策略](/blog/agent/02-context-optimization) - 锚定迭代总结、观察掩蔽、Token经济学 (2025-12-30)
;; - [03. 多 Agent 架构模式与 Token 经济学](/blog/agent/03-multi-agent-patterns) - Supervisor/P2P/Hierarchical、文件系统状态机 (2025-12-30)
;; - [04. 内存系统与工具设计](/blog/agent/04-memory-tool-design) - 5层内存架构、整合原则、时间知识图 (2025-12-30)
;; - [05. 评估框架：LLM-as-Judge 实践](/blog/agent/05-evaluation-framework) - 多维度评分、配对比较、位置偏见缓解 (2025-12-30)
;; - [06. 实战：X-to-Book 多 Agent 系统设计](/blog/agent/06-x-to-book-case-study) - 完整案例分析、性能数据 (2025-12-30)
;; - [07. Agent 技能系统实现](/blog/agent/07-skill-system) - SKILL.md格式、渐进式披露、语义检索 (2025-12-30)
;; - [08. 总结：8个关键洞察](/blog/agent/08-key-insights) - 核心智慧提炼、实践指南 (2025-12-30)

;; ## Schema驱动的前端渲染引擎

;; 从最简单的功能开始，逐步展示如何构建一个完整的渲染引擎。

;; - [第1节：最简单的渲染引擎](/pages/rendering-engine/ch1-basic-rendering) - Schema到组件的转换、嵌套结构、组件注册 (2025-10-28)
;; - [第2节：支持动态属性](/pages/rendering-engine/ch2-dynamic-props) - 表达式系统、Context注入、属性求值 (2025-10-28)
;; - [第3节：组件交互 - ActionStore](/pages/rendering-engine/ch3-action-store) - ActionProvider、延迟执行、页面隔离 (2025-10-28)

