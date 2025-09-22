(ns blog.core
  (:require [nextjournal.clerk :as clerk]))

(defn start-server
  "Start Clerk server for development"
  []
  (clerk/serve! {:browse? true :port 7777}))

(defn build-static
  "Build static site"
  []
  (clerk/build! {:git/sha "main"}))

(comment
  ;; Start development server
  (start-server)

  ;; Show a specific notebook
  (clerk/show! "notebooks/welcome.clj")

  ;; Build static site
  (build-static))