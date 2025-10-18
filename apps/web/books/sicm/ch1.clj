^{:nextjournal.clerk/visibility {:code :hide}}
(ns books.sicm.ch1
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

;; [← 返回目录](/books/sicm/contents) | [← 上一章：序言](/books/sicm/preface)

;; ---

(clerk/md (read-md "1.0-Lagrangian_Mechanics.md"))

;; ---

(clerk/md (read-md "1.1-Configuration_Spaces.md"))

;; ---

(clerk/md (read-md "1.2-Generalized_Coordinates.md"))

;; ---

(clerk/md (read-md "1.3-The_Principle_of_Stationary_Action.md"))

;; ---

(clerk/md (read-md "1.4-Computing_Actions.md"))

;; ---

(clerk/md (read-md "1.5-The_Euler–Lagrange_Equations.md"))

;; ---

;; [← 返回目录](/books/sicm/contents) | [← 上一章：序言](/books/sicm/preface)
