(ns user
  (:require [nextjournal.clerk :as clerk]
            [clojure.string :as str]))

(def serve-opts
  {:browse? false
   :port 7777
   :watch-paths ["notebooks" "src"]
   :paths ["notebooks/*.clj"]
   :index "notebooks/home.clj"})

(defn start!
  ([] (start! serve-opts))
  ([opts]
   (clerk/serve! opts)))

(defn -main [& _]
  (start!)
  (println "Clerk dev server at http://localhost:7777")
  (flush))
