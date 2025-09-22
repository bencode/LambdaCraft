^{:nextjournal.clerk/visibility {:code :hide}}
(ns notebooks.math-exploration
  (:require [nextjournal.clerk :as clerk]))

;; # Mathematical Explorations
;;
;; Exploring mathematical concepts through interactive computing.

;; ## Fibonacci Sequence
;;
;; The classic recursive definition:

^{:nextjournal.clerk/visibility {:result :show}}
(defn fib [n]
  (if (<= n 1)
    n
    (+ (fib (- n 1)) (fib (- n 2)))))

(map fib (range 10))

;; Let's visualize the growth:

^{:nextjournal.clerk/visibility {:result :show}}
(let [fibs (map fib (range 15))]
  (clerk/plotly
    {:data [{:x (range 15)
             :y fibs
             :type "scatter"
             :mode "lines+markers"
             :name "Fibonacci"}]
     :layout {:title "Fibonacci Sequence Growth"
              :xaxis {:title "n"}
              :yaxis {:title "fib(n)"}}}))

;; ## Prime Numbers

^{:nextjournal.clerk/visibility {:result :show}}
(defn prime? [n]
  (and (> n 1)
       (every? #(not= 0 (mod n %))
               (range 2 (inc (Math/sqrt n))))))

(defn primes-up-to [n]
  (filter prime? (range 2 (inc n))))

(primes-up-to 50)