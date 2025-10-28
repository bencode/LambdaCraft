^{:nextjournal.clerk/visibility {:code :hide}}
(ns pages.blog.rendering-engine-evolution-ch3
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

;; [← 上一节](/pages/blog/rendering-engine-evolution-ch2) | [返回博客](/pages/blog)

;; ---

(clerk/md (read-md "rendering-engine-evolution-ch3.md"))

;; ---

;; [← 上一节](/pages/blog/rendering-engine-evolution-ch2) | [返回博客](/pages/blog)
