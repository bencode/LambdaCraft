^{:nextjournal.clerk/visibility {:code :hide}}
(ns notebooks.home
  (:require [nextjournal.clerk :as clerk]))

;; # LambdaCraft
;;
;; **() => study(math) => practice(code)**

(defn factorial [n]
  (if (<= n 1)
    1
    (* n (factorial (dec n)))))

(map factorial (range 1 6))

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
