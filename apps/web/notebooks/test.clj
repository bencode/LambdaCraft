^{:nextjournal.clerk/visibility {:code :hide :result :show}}
(ns notebooks.test
  (:require [nextjournal.clerk :as clerk]))

^{:nextjournal.clerk/visibility {:code :hide :result :show}}
(clerk/html
 [:div.text-center.p-8.bg-gradient-to-br.from-blue-500.to-purple-600.text-white.rounded-lg
  [:h1.text-3xl.font-bold.mb-4 "🚀 Test Page"]
  [:p.text-lg "This is a test notebook to verify deployment works!"]])

;; # Test Notebook

;; This is a simple test to make sure our deployment pipeline works.

;; ## Basic Computation
(+ 1 2 3 4 5)

;; ## Data Visualization
(clerk/plotly
  {:data [{:x ["A" "B" "C" "D"]
           :y [1 4 2 3]
           :type "bar"
           :marker {:color ["red" "green" "blue" "orange"]}}]
   :layout {:title "Test Chart"
            :xaxis {:title "Categories"}
            :yaxis {:title "Values"}}})

;; ## Table Example
(clerk/table
  [{:name "Test 1" :value 100 :status "✅"}
   {:name "Test 2" :value 200 :status "✅"}
   {:name "Test 3" :value 150 :status "⏳"}])

^{:nextjournal.clerk/visibility {:code :hide :result :show}}
(clerk/html
 [:div.mt-8.p-4.bg-green-100.border.border-green-400.text-green-700.rounded
  [:strong "Success! "] "If you can see this page, the deployment is working correctly."])