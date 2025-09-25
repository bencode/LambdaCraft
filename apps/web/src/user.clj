(ns user
  (:require [nextjournal.clerk :as clerk]
            [clojure.string :as str]))

(def serve-opts
  {:browse? false
   :port 7777
   :watch-paths ["src" "pages" "docs"]
   :paths ["pages/*.clj" "docs/*.clj"]
   :index "pages/home.clj"})

(defn start!
  ([] (start! serve-opts))
  ([opts]
   (clerk/serve! opts)))

(defn -main [& _]
  (start!)
  (println "Clerk dev server at http://localhost:7777")
  (flush))
