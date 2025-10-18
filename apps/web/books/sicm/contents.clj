^{:nextjournal.clerk/visibility {:code :hide}}
(ns books.sicm.contents
  (:require [nextjournal.clerk :as clerk]))

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/html [:link {:rel "stylesheet" :href "/css/style.css"}])

;; # SICM
;;
;; Structure and Interpretation of Classical Mechanics
;;
;; ## 关于本书
;;
;; 这是《Structure and Interpretation of Classical Mechanics》(SICM) 的中文翻译版本。
;;
;; - **原书作者**: Gerald Jay Sussman, Jack Wisdom
;; - **出版社**: MIT Press
;;
;; 本书使用 Scheme 语言和符号计算来教授经典力学，是 SICP 的姊妹篇。
;;
^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/html
 [:div.sicm-toc
  [:h2 "目录"]
  [:ul
   [:li [:a {:href "/books/sicm/preface"} "序言与致谢"]]
   [:li [:a {:href "/books/sicm/ch1"} "第1章 拉格朗日力学"]]]])

;; ## 相关资源
;;
;; - [SICM 原书在线版 (英文)](https://mitp-content-server.mit.edu/books/content/sectbyfn/books_pres_0/9579/sicm_edition_2.zip/toc.html)
;; - [原书代码（Scheme）](https://groups.csail.mit.edu/mac/users/gjs/6946)
;; - [Emmy - Clojure 版 scmutils](https://github.com/mentat-collective/emmy)
;;
