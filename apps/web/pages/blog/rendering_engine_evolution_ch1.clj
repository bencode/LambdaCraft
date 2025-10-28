^{:nextjournal.clerk/visibility {:code :hide}}
(ns pages.blog.rendering-engine-evolution-ch1
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

;; [← 返回博客](/pages/blog) | [下一节 →](/pages/blog/rendering-engine-evolution-ch2)

;; ---

(clerk/md (read-md "rendering-engine-evolution-ch1-v2.md"))

;; ---

;; [← 返回博客](/pages/blog) | [下一节 →](/pages/blog/rendering-engine-evolution-ch2)
