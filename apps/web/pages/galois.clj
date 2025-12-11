^{:nextjournal.clerk/visibility {:code :hide}}
(ns pages.galois
  "伽罗瓦理论学习笔记"
  (:require [nextjournal.clerk :as clerk]))

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/html [:link {:rel "stylesheet" :href "/css/style.css"}])

;; # 伽罗瓦理论学习笔记
;;
;; 这是我学习伽罗瓦理论的笔记系列，从直觉出发，逐步建立严格的数学理论。
;;
;; ## 章节目录
;;
;; - [第一章：从数的扩张谈起](/books/galois/ch1)
;;
;; ## 关于伽罗瓦理论
;;
;; 伽罗瓦理论是抽象代数中的核心理论，它揭示了多项式方程的根与对称性之间的深刻联系。
;; 这套理论回答了一个古老的问题：哪些方程可以用根式求解？
