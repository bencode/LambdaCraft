^{:nextjournal.clerk/visibility {:code :hide}}
(ns pages.sicm
  "SICM - Structure and Interpretation of Classical Mechanics"
  (:require [nextjournal.clerk :as clerk]))

;; # SICM - 经典力学的结构与解释

;; **Structure and Interpretation of Classical Mechanics**

;; 这是一本由 Gerald Jay Sussman 和 Jack Wisdom 编写的经典力学教材，
;; 使用计算机程序（Scheme/Clojure）来表达和探索物理概念。

;; ## 📚 关于本书

;; SICM 将经典力学的数学形式化与计算机程序相结合，提供了一种全新的学习视角：
;; - 使用函数式编程表达物理定律
;; - 通过符号计算验证物理理论
;; - 用数值方法求解实际问题
;;
;;
;;
;; ## 📖 学习笔记
;;
;; 我使用 [Emmy](/docs/emmy)（Clojure 版 scmutils）来学习本书。
;; 以下是学习过程中整理的笔记：
;;
;; ### 第一章：拉格朗日力学
;; - [1.3 平稳作用量原理](/pages/sicm/sicm-1-3)
;; - [1.4 计算作用量](/pages/sicm/sicm-1-4)
;; - [1.5 欧拉-拉格朗日方程](/pages/sicm/sicm-1-5)
;; - [变分 (Variation) - 核心概念](/pages/sicm/variation)
;;
;; ## 📕 中文翻译
;; - [在线阅读](/books/sicm/contents)
;; - [Markdown 源文件](https://github.com/bencode/LambdaCraft/tree/main/books/sicm)
;;
;; ### 官方资源
;; - [SICM 原书 (英文)](https://mitp-content-server.mit.edu/books/content/sectbyfn/books_pres_0/9579/sicm_edition_2.zip/toc.html)
;; - [原书代码（Scheme）](https://groups.csail.mit.edu/mac/users/gjs/6946)
;;
;; ### 工具和库 
;; - [Emmy - Clojure 版 scmutils](https://github.com/mentat-collective/emmy)
