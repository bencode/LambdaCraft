^{:nextjournal.clerk/visibility {:code :hide}}
(ns pages.rendering-engine.ch1-basic-rendering
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

;; [← 返回目录](/pages/rendering-engine) | [下一节 →](/pages/rendering-engine/ch2-dynamic-props)

;; ---

(clerk/md (read-md "rendering-engine-evolution-ch1-v2.md"))

;; ---

;; [← 返回目录](/pages/rendering-engine) | [下一节 →](/pages/rendering-engine/ch2-dynamic-props)
