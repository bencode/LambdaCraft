^{:nextjournal.clerk/visibility {:code :hide}}
(ns notebooks.home
  (:require [nextjournal.clerk :as clerk]))

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/html
 [:link {:rel "stylesheet" :href "/css/style.css"}])

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/html
 [:div
  [:h1 {:class "bcd-logo"} "LambdaCraft"]
  [:p {:class "bcd-slogan"} "() => study(math) => practice(code)"]])

; ## Study
; - [My Reading System](https://in.qijun.io)
;
; ## Math
;
; ## Practice
;
; ## Code
