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

; ## Study
;
; - [Reading System](https://in.qijun.io)
; - [Technical Documentation](/docs/index)
;
; ## Math
;
; - [Emmy Computer Algebra System](/docs/emmy)
; - Mathematical Computing and Symbolic Computation
;
; ## Practice
;
; - Interactive Notebooks with Clerk
; - Physics and Mathematics Examples
;
; ## Code
;
; - Clojure Development Environment
; - Functional Programming Demonstrations
