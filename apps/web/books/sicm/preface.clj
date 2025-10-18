^{:nextjournal.clerk/visibility {:code :hide}}
(ns books.sicm.preface
  (:require [nextjournal.clerk :as clerk]
            [clojure.java.io :as io]))

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/html [:link {:rel "stylesheet" :href "/css/style.css"}])

^{:nextjournal.clerk/visibility {:code :hide}}
(defn read-md [filename]
  (let [project-root (System/getProperty "user.dir")
        parent-dir (-> project-root io/file .getParentFile .getParentFile)
        md-path (io/file parent-dir "books" "sicm" filename)]
    (slurp md-path)))

;; [← 返回目录](/books/sicm/contents)

;; ---

(clerk/md (read-md "0.0-preface.md"))

;; ---

(clerk/md (read-md "0.1-acknowledgments.md"))

;; ---

;; [← 返回目录](/books/sicm/contents) | [下一章：第1章 →](/books/sicm/ch1)
