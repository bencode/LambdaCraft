^{:nextjournal.clerk/visibility {:code :hide}}
(ns pages.treasure
  (:require [nextjournal.clerk :as clerk]))

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/html
 [:link {:rel "stylesheet" :href "/css/style.css"}])

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/html
 [:div
  [:h1 {:class "bcd-logo"} "百宝箱"]
  [:p {:class "bcd-slogan"} "收藏发现的好东西"]])

; - [Alpine.js](https://alpinejs.dev/) - 轻量级JavaScript框架，通过15个属性为HTML添加交互性
