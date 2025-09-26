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

; # Study
; - [每周阅读](https://in.qijun.io)
; - [给自己写的文档](/docs/home)
;
; # Code
; - [chalkpad](https://github.com/bencode/chalkpad)
; - [logseq-runit-plugin](https://github.com/bencode/logseq-runit-plugin)
