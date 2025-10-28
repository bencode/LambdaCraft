^{:nextjournal.clerk/visibility {:code :hide}}
(ns pages.rendering-engine.ch2-dynamic-props
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

;; [← 上一节](/pages/rendering-engine/ch1-basic-rendering) | [返回目录](/pages/rendering-engine) | [下一节 →](/pages/rendering-engine/ch3-action-store)

;; ---

(clerk/md (read-md "ch2-dynamic-props-v2.md"))

;; ---

;; [← 上一节](/pages/rendering-engine/ch1-basic-rendering) | [返回目录](/pages/rendering-engine) | [下一节 →](/pages/rendering-engine/ch3-action-store)
