^{:nextjournal.clerk/visibility {:code :hide}}
(ns books.galois.ch1
  {:nextjournal.clerk/toc true}
  (:require [nextjournal.clerk :as clerk]
            [clojure.java.io :as io]))

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/html [:link {:rel "stylesheet" :href "/css/style.css"}])

^{:nextjournal.clerk/visibility {:code :hide}}
(defn read-md [filename]
  (let [project-root (System/getProperty "user.dir")
        parent-dir (-> project-root io/file .getParentFile .getParentFile)
        md-path (io/file parent-dir "ibooks" "galois" filename)]
    (slurp md-path)))

;; [← 返回目录](/pages/galois)

;; ---

(clerk/md (read-md "ch1.md"))

;; ---

;; [← 返回目录](/pages/galois)
