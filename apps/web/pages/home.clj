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

;; ## 阅读与写作
;; - [编程技艺周刊](https://in.qijun.io)
;; - [博客](/pages/blog)
;; - [我的知识库](/docs/home)
;;
;; ## 学习笔记
;; - [SICM - 经典力学](/pages/sicm)
;; - [FP - 函数式编程](/pages/fp)
;;
;; ## 开源作品
;; - [codescope](https://github.com/bencode/codescope) - 代码结构分析工具
;; - [increa-reader](https://github.com/bencode/increa-reader) - AI原生阅读器
;; - [increa.el](https://github.com/bencode/increa.el) - Emacs AI 代码补全插件
;; - [chalkpad](https://github.com/bencode/chalkpad) - 增强版代码实验室
;; - [code](https://github.com/bencode/code) - 代码实验室
;; - [logseq-runit-plugin](https://github.com/bencode/logseq-runit-plugin) - Logseq 代码运行插件
