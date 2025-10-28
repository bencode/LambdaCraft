^{:nextjournal.clerk/visibility {:code :hide}}
(ns pages.blog
  "技术文章索引"
  {:nextjournal.clerk/toc true}
  (:require [nextjournal.clerk :as clerk]))

;; # 技术文章

;; ## Schema驱动的前端渲染引擎设计和实现

;; 这个系列从最简单的功能开始，逐步展示如何构建一个完整的渲染引擎。

;; - [第1节：最简单的渲染引擎](/pages/blog/rendering-engine-evolution-ch1) - Schema到组件的转换、嵌套结构、组件注册
;; - [第2节：支持动态属性](/pages/blog/rendering-engine-evolution-ch2) - 表达式系统、Context注入、属性求值
;; - [第3节：组件交互 - ActionStore](/pages/blog/rendering-engine-evolution-ch3) - ActionProvider、延迟执行、页面隔离
