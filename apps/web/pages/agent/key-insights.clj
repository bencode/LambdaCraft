^{:nextjournal.clerk/visibility {:code :hide}}
(ns pages.agent.key-insights
  (:require [nextjournal.clerk :as clerk]
            [clojure.java.io :as io]))

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/html [:link {:rel "stylesheet" :href "/css/style.css"}])

^{:nextjournal.clerk/visibility {:code :hide}}
(defn read-md [filename]
  (let [project-root (System/getProperty "user.dir")
        parent-dir (-> project-root io/file .getParentFile .getParentFile)
        md-path (io/file parent-dir "blog" "agent" filename)]
    (slurp md-path)))

;; [← 返回目录](/pages/dev-notes)

;; ---

(clerk/md (read-md "08-key-insights.md"))

;; ---

;; [← 返回目录](/pages/dev-notes)
