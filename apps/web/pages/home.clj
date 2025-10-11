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
;; - [每周阅读](https://in.qijun.io)
;; - [我的知识库](/docs/home)
;;
;; ## Code
;; - [chalkpad](https://github.com/bencode/chalkpad)
;; - [logseq-runit-plugin](https://github.com/bencode/logseq-runit-plugin) - 在 Logseq 笔记中直接运行代码片段，支持 JavaScript、Python、Scheme 和 Clojure
;; - [increa.el](https://github.com/bencode/increa.el) - Emacs 智能代码补全插件，提供类似 GitHub Copilot 的幽灵文本补全
;; - [increa-reader](https://github.com/bencode/increa-reader)
