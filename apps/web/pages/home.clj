^{:nextjournal.clerk/visibility {:code :hide}}
(ns pages.home
  (:require [nextjournal.clerk :as clerk]))

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/html
 [:link {:rel "stylesheet" :href "/css/style.css"}])

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/html
 [:script "document.title = 'LambdaCraft';"])

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/html
 [:div
  [:h1 {:class "bcd-logo"} "LambdaCraft"]
  [:p {:class "bcd-slogan"} "() => study(math) => practice(code)"]])

; ## Study
;
; - [Reading System](https://in.qijun.io)
; - [Technical Documentation](/docs/home)
;
; ## Math
;
; ## Practice
;
; ## Code

^{:nextjournal.clerk/visibility {:code :hide :result :show}}
(clerk/html
 [:div.space-y-4
  [:div.bg-gray-50.border.border-gray-200.rounded-lg.p-4
   [:div.flex.items-center.justify-between.mb-2
    [:h3.text-lg.font-semibold.text-gray-800 "logseq-runit-plugin"]
    [:a.text-blue-600.hover:text-blue-800.text-sm.font-medium
     {:href "https://github.com/bencode/logseq-runit-plugin"
      :target "_blank"}
     "GitHub →"]]
   [:p.text-gray-600.text-sm "一个 Logseq 插件，用于在笔记中直接运行代码块"]]])
;
