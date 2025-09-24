^{:nextjournal.clerk/visibility {:code :hide}}
(ns notebooks.home
  (:require [nextjournal.clerk :as clerk]))

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/html
 [:link {:rel "stylesheet" :href "/css/style.css"}])

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/html
 [:div
  [:h1 {:class "my-logo"} "LambdaCraft"]
  [:p {:class "my-slogan"} "() => study(math) => practice(code)"]])
