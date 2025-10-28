^{:nextjournal.clerk/visibility {:code :hide}}
(ns pages.home
  (:require [nextjournal.clerk :as clerk]))

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/html
 [:link {:rel "stylesheet" :href "/css/style.css"}])

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/html
 [:div
  [:h1 {:class "bcd-logo"} "LambdaCraft"]
  [:p {:class "bcd-slogan"} "() => study(math) => practice(code)"]])

;; ## Study
;; - [编程技艺周刊](https://in.qijun.io)
;; - [我的知识库](/docs/home)
;; - [博客](/pages/blog)
;;
;; ## Math
;; - [SICM](/pages/sicm)
;;
;; ## Practice
;;
;; - [FP](/pages/fp)
;;
;; ## Code
;; - [code](https://github.com/bencode/code) - 代码实验室
;; - [chalkpad](https://github.com/bencode/chalkpad) - 代码实验室+
;; - [codescope](https://github.com/bencode/codescope) - 代码结构分析工具，用于理解复杂度和依赖关系
;; - [logseq-runit-plugin](https://github.com/bencode/logseq-runit-plugin) - 在 Logseq 笔记中运行代码片段
;; - [increa.el](https://github.com/bencode/increa.el) - Emacs 智能代码补全插件，提供类似 GitHub Copilot 的幽灵文本补全
;; - [increa-reader](https://github.com/bencode/increa-reader) - AI原生阅读器
