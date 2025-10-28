^{:nextjournal.clerk/visibility {:code :hide}}
(ns pages.rendering-engine.ch3-action-store
  (:require [nextjournal.clerk :as clerk]
            [clojure.java.io :as io]))

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/html [:link {:rel "stylesheet" :href "/css/style.css"}])

^{:nextjournal.clerk/visibility {:code :hide}}
(defn read-md [filename]
  (let [project-root (System/getProperty "user.dir")
        parent-dir (-> project-root io/file .getParentFile .getParentFile)
        md-path (io/file parent-dir "blog" filename)]
    (slurp md-path)))

;; [← 上一节](/pages/rendering-engine/ch2-dynamic-props) | [返回目录](/pages/rendering-engine)

;; ---

(clerk/md (read-md "ch3-action-store.md"))

;; ---

;; [← 上一节](/pages/rendering-engine/ch2-dynamic-props) | [返回目录](/pages/rendering-engine)
