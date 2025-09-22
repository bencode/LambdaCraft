^{:nextjournal.clerk/visibility {:code :hide}}
(ns notebooks.welcome
  (:require [nextjournal.clerk :as clerk]))

;; # Welcome to LambdaCraft Blog
;;
;; **() => study(math) => practice(code)**
;;
;; This is a Clerk-powered blog for exploring mathematics and programming.

;; ## What is this?
;;
;; This blog uses [Clerk](https://clerk.vision/) to create interactive notebooks that combine:
;;
;; - Mathematical notation and visualizations
;; - Live code examples
;; - Interactive explorations
;; - Beautiful presentations

;; ## Getting Started
;;
;; To start the development server:

(comment
  (require '[blog.core :as blog])
  (blog/start-server))

;; ## Blog Structure
;;
;; - `notebooks/` - Blog posts as Clerk notebooks
;; - `src/blog/` - Core blog functionality
;; - `resources/` - Static assets

;; ## Example: Simple Math
;;
;; Let's start with something simple:

(defn factorial [n]
  (if (<= n 1)
    1
    (* n (factorial (dec n)))))

(map factorial (range 1 6))

;; ## Visualization Example

^{:nextjournal.clerk/visibility {:result :show}}
(clerk/plotly
  {:data [{:x (range 10)
           :y (map #(* % %) (range 10))
           :type "scatter"
           :mode "lines+markers"
           :name "y = x²"}]
   :layout {:title "Quadratic Function"
            :xaxis {:title "x"}
            :yaxis {:title "y"}}})

;; Welcome to the journey of learning through interactive computing!