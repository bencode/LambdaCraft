^{:nextjournal.clerk/visibility {:code :hide}}
(ns pages.fp
  "Functional Programming Practice"
  {:nextjournal.clerk/toc true}
  (:require [nextjournal.clerk :as clerk]))

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/html [:link {:rel "stylesheet" :href "/css/style.css"}])

;; # FP - 函数式编程练习场

;; ## 关于
;;
;; 我使用 [JupyterLite](https://github.com/jupyterlite/jupyterlite) 来编写编程练习，主要聚焦于函数式编程技巧。
;;
;; JupyterLite 是一个完全运行在浏览器中的 Jupyter 发行版，无需服务器即可使用。
;; 我集成了自己常用的编程语言：JavaScript、Python、Scheme 和 Clojure。
;;
;; 相关代码和练习内容在 [code 仓库](https://github.com/bencode/code)。

;; ## 文章列表
;;
;; - [Transducer: 一种强大的函数组合模式](https://bencode.github.io/code/tree/?path=transducers.ipynb)
;; - [SICP中的编程技艺 1.1](https://bencode.github.io/code/notebooks/index.html?path=sicp-1-1.ipynb)
;; - [SICP中的编程技艺 1.2](https://bencode.github.io/code/notebooks/index.html?path=sicp-1-2.ipynb)
