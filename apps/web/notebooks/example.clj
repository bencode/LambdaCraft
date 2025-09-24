;; # Clerk Notebook Complete Feature Showcase
;;
;; This file demonstrates all common features and best practices of Clerk notebook

^{:nextjournal.clerk/visibility {:code :hide :result :show}}
(ns notebooks.example
  {:nextjournal.clerk/toc true
   :nextjournal.clerk/width :wide}
  (:require [nextjournal.clerk :as clerk]
            [nextjournal.clerk.viewer :as v]))

;; ## 1. Basic Features

;; ### Code and result display
(+ 1 2 3)

;; ### Hide code, show result only
^{:nextjournal.clerk/visibility {:code :hide :result :show}}
(str "This code is hidden, only result is shown")

;; ### Hide result, show code only
^{:nextjournal.clerk/visibility {:code :show :result :hide}}
(println "This result is hidden")

;; ### Complete hiding (usually for helper functions)
^{:nextjournal.clerk/visibility {:code :hide :result :hide}}
(defn helper-function [x]
  "This is a helper function, completely hidden"
  (* x 2))

;; ## 2. HTML and Styling

;; ### Basic HTML
(clerk/html [:h3 "Hello " [:em "World"] "!"])

;; ### HTML with CSS classes (using Tailwind)
(clerk/html [:div.bg-blue-100.p-4.rounded.border
             [:h4.text-xl.font-bold.text-blue-800 "Blue Card"]
             [:p.text-blue-600 "This is a card using Tailwind CSS styles"]])

;; ### Inline styles
(clerk/html [:div {:style {:background-color "#f0f8ff"
                          :padding "1rem"
                          :border-radius "8px"
                          :border "2px solid #007acc"}}
             [:h4 "Custom Styles"]
             [:p "Example using inline styles"]])

;; ### External CSS import
^{:nextjournal.clerk/visibility {:code :hide :result :show}}
(clerk/html [:style ".highlight { background: yellow; padding: 0.2em; }"])

(clerk/html [:p "Here is a " [:span.highlight "highlighted"] " word"])

;; ## 3. Layout Components

;; ### Horizontal layout
(clerk/row
  (clerk/html [:div.bg-red-100.p-4.text-center.rounded "Box 1"])
  (clerk/html [:div.bg-green-100.p-4.text-center.rounded "Box 2"])
  (clerk/html [:div.bg-blue-100.p-4.text-center.rounded "Box 3"]))

;; ### Vertical layout
(clerk/col
  (clerk/html [:div.bg-purple-100.p-4.text-center.rounded "Top"])
  (clerk/html [:div.bg-pink-100.p-4.text-center.rounded "Middle"])
  (clerk/html [:div.bg-indigo-100.p-4.text-center.rounded "Bottom"]))

;; ## 4. Data Display

;; ### Table display
(def sample-data
  [{:name "Alice" :age 25 :city "New York"}
   {:name "Bob" :age 30 :city "San Francisco"}
   {:name "Charlie" :age 28 :city "Los Angeles"}])

(clerk/table sample-data)

;; ### Large table with pagination
^{:nextjournal.clerk/visibility {:code :hide :result :show}}
(def large-dataset
  (for [i (range 100)]
    {:id i :value (rand-int 1000) :category (rand-nth ["A" "B" "C"])}))

(clerk/table {::clerk/page-size 10} large-dataset)

;; ## 5. Data Visualization

;; ### Vega-Lite scatter plot
(clerk/vl {:width 400 :height 300
           :data {:values (map (fn [i] {:x i :y (+ (* i i) (rand-int 10))})
                              (range 10))}
           :mark "point"
           :encoding {:x {:field :x :type "quantitative" :title "X Value"}
                     :y {:field :y :type "quantitative" :title "Y Value"}}})

;; ### Vega-Lite bar chart
(clerk/vl {:width 300 :height 200
           :data {:values sample-data}
           :mark "bar"
           :encoding {:x {:field :name :type "nominal" :title "Name"}
                     :y {:field :age :type "quantitative" :title "Age"}
                     :color {:field :city :type "nominal"}}})

;; ### Plotly 3D chart
(clerk/plotly {:data [{:type "scatter3d"
                      :mode "markers"
                      :x (range 10)
                      :y (map #(* % %) (range 10))
                      :z (map #(* % % %) (range 10))
                      :marker {:size 5 :color (range 10) :colorscale "Viridis"}}]
               :layout {:title "3D Scatter Plot"
                       :scene {:xaxis {:title "X"}
                              :yaxis {:title "X²"}
                              :zaxis {:title "X³"}}}})

;; ## 6. Text and Formulas

;; ### Markdown text
(clerk/md "## Markdown Support

This is **bold** and *italic* text.

- List item 1
- List item 2
- List item 3

Code block:
```clojure
(defn hello [name]
  (str \"Hello, \" name \"!\"))
```")

;; ### LaTeX math formulas
(clerk/tex "E = mc^2")

(clerk/tex "\\sum_{i=1}^n i = \\frac{n(n+1)}{2}")

;; ### Code highlighting
(clerk/code "(defn fibonacci [n]
  (cond
    (= n 0) 0
    (= n 1) 1
    :else (+ (fibonacci (- n 1)) (fibonacci (- n 2)))))")

;; ## 7. Custom Viewers

;; ### Simple custom rendering
(clerk/with-viewer
  '#(vector :div.text-center.p-6.bg-gradient-to-r.from-purple-400.to-pink-400.text-white.rounded-lg
            [:h2.text-2xl.font-bold "🎉 " % " 🎉"])
  "Welcome to Clerk!")

;; ### Interactive component
^{:nextjournal.clerk/visibility {:code :hide :result :show}}
(clerk/with-viewer
  '(fn [initial-value]
     (reagent.core/with-let [counter (reagent.core/atom initial-value)]
       [:div.text-center.p-6.bg-gray-50.rounded-lg.border
        [:h3.text-xl.mb-4 "Counter: " [:span.font-bold.text-blue-600 @counter]]
        [:div.space-x-4
         [:button.px-4.py-2.bg-green-500.text-white.rounded.hover:bg-green-600
          {:on-click #(swap! counter inc)} "Increment"]
         [:button.px-4.py-2.bg-red-500.text-white.rounded.hover:bg-red-600
          {:on-click #(swap! counter dec)} "Decrement"]
         [:button.px-4.py-2.bg-gray-500.text-white.rounded.hover:bg-gray-600
          {:on-click #(reset! counter initial-value)} "Reset"]]]))
  10)

;; ### Data explorer
^{:nextjournal.clerk/visibility {:code :hide :result :show}}
(def data-explorer-viewer
  {:render-fn
   '(fn [data]
      (reagent.core/with-let [view-type (reagent.core/atom "table")]
        [:div.border.rounded-lg.p-4
         [:div.mb-4
          [:h4.text-lg.font-bold.mb-2 "Data Explorer"]
          [:div.flex.space-x-2
           [:button.px-3.py-1.rounded.text-sm
            {:class (if (= @view-type "table")
                      "bg-blue-500 text-white"
                      "bg-gray-200")
             :on-click #(reset! view-type "table")} "Table View"]
           [:button.px-3.py-1.rounded.text-sm
            {:class (if (= @view-type "raw")
                      "bg-blue-500 text-white"
                      "bg-gray-200")
             :on-click #(reset! view-type "raw")} "Raw Data"]
           [:button.px-3.py-1.rounded.text-sm
            {:class (if (= @view-type "stats")
                      "bg-blue-500 text-white"
                      "bg-gray-200")
             :on-click #(reset! view-type "stats")} "Statistics"]]]
         [:div
          (case @view-type
            "table" (nextjournal.clerk.render/inspect-presented
                     (nextjournal.clerk/table data))
            "raw" [:pre.text-xs.bg-gray-100.p-2.rounded.overflow-auto
                   (pr-str data)]
            "stats" [:div.bg-blue-50.p-3.rounded
                     [:p "Record count: " (count data)]
                     [:p "Fields: " (pr-str (keys (first data)))]])]]))})

(clerk/with-viewer data-explorer-viewer sample-data)

;; ## 8. Images and Media

;; ### Base64 image
^{:nextjournal.clerk/visibility {:code :hide :result :show}}
(def tiny-image-base64
  "data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTAwIiBoZWlnaHQ9IjEwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KICA8Y2lyY2xlIGN4PSI1MCIgY3k9IjUwIiByPSI0MCIgZmlsbD0iIzQyOGJmZiIvPgogIDx0ZXh0IHg9IjUwIiB5PSI1NSIgdGV4dC1hbmNob3I9Im1pZGRsZSIgZmlsbD0id2hpdGUiIGZvbnQtZmFtaWx5PSJBcmlhbCIgZm9udC1zaXplPSIxNCIgZm9udC13ZWlnaHQ9ImJvbGQiPkNMSjwvdGV4dD4KPC9zdmc+")

(clerk/html [:img {:src tiny-image-base64 :alt "Clojure Logo" :width 100}])

;; ## 9. Data Processing Pipeline Demo

;; ### Raw data
^{:nextjournal.clerk/visibility {:code :hide :result :show}}
(def raw-sales-data
  [{:product "Phone" :price 5999 :sold 120 :month "2024-01"}
   {:product "Laptop" :price 8999 :sold 80 :month "2024-01"}
   {:product "Phone" :price 5999 :sold 150 :month "2024-02"}
   {:product "Laptop" :price 8999 :sold 95 :month "2024-02"}
   {:product "Tablet" :price 3999 :sold 60 :month "2024-01"}
   {:product "Tablet" :price 3999 :sold 70 :month "2024-02"}])

;; ### Data processing steps
^{:nextjournal.clerk/visibility {:code :hide :result :show}}
(def processed-sales
  (->> raw-sales-data
       (map #(assoc % :revenue (* (:price %) (:sold %))))
       (group-by :product)
       (map (fn [[product records]]
              {:product product
               :total-sold (reduce + (map :sold records))
               :total-revenue (reduce + (map :revenue records))
               :avg-price (/ (reduce + (map :price records)) (count records))}))))

;; ### Results display
(clerk/table processed-sales)

;; ### Sales chart
(clerk/vl {:width 400 :height 250
           :data {:values processed-sales}
           :mark "bar"
           :encoding {:x {:field :product :type "nominal" :title "Product"}
                     :y {:field :total-revenue :type "quantitative" :title "Total Revenue"}
                     :color {:field :product :type "nominal"}}})

;; ## 10. Performance and Caching Demo

;; ### No-cache computation (re-executes every time)
^::clerk/no-cache
(do
  (println "Execution time:" (java.time.LocalTime/now))
  (Thread/sleep 1000)
  "This computation re-executes every time")

;; ### Cached computation (executes only once)
(do
  (println "Cached computation time:" (java.time.LocalTime/now))
  (Thread/sleep 500)
  "This computation is cached")

;; ## 11. Debugging and Development Tips

;; ### Debug info display
^{:nextjournal.clerk/visibility {:code :fold :result :show}}
(let [data [1 2 3 4 5]
      step1 (map #(* % 2) data)
      step2 (filter even? step1)
      step3 (reduce + step2)]
  {:original-data data
   :step1-double step1
   :step2-filter-even step2
   :step3-sum step3})

;; ### Conditional display (development mode)
^{:nextjournal.clerk/visibility {:code :hide :result :show}}
(def debug-mode? false)

^{:nextjournal.clerk/visibility {:code :hide :result :show}}
(when debug-mode?
  (clerk/html [:div.bg-yellow-100.border.border-yellow-400.text-yellow-700.px-4.py-3.rounded
               [:strong "Debug Mode: "] "This info is only shown in debug mode"]))

;; ## 12. Best Practices Summary

(clerk/html
  [:div.bg-blue-50.border-l-4.border-blue-400.p-4.my-6
   [:h4.text-lg.font-bold.text-blue-800 "💡 Clerk Notebook Best Practices"]
   [:ul.list-disc.list-inside.text-blue-700.space-y-2.mt-2
    [:li "Use " [:code.bg-blue-100.px-1.rounded "^{:nextjournal.clerk/visibility {:code :hide :result :show}}"] " to hide setup code"]
    [:li "Use " [:code.bg-blue-100.px-1.rounded "clerk/row"] " and " [:code.bg-blue-100.px-1.rounded "clerk/col"] " for layout"]
    [:li "Display tabular data with " [:code.bg-blue-100.px-1.rounded "clerk/table"]]
    [:li "Prefer " [:code.bg-blue-100.px-1.rounded "clerk/vl"] " and " [:code.bg-blue-100.px-1.rounded "clerk/plotly"] " for visualization"]
    [:li "Use " [:code.bg-blue-100.px-1.rounded "clerk/with-viewer"] " for custom components"]
    [:li "Use " [:code.bg-blue-100.px-1.rounded "^::clerk/no-cache"] " to disable caching when debugging"]
    [:li "Set " [:code.bg-blue-100.px-1.rounded "::clerk/page-size"] " for large datasets"]
    [:li "Use Tailwind CSS classes for quick styling"]]])

;; ### Complete workflow example
^{:nextjournal.clerk/visibility {:code :hide :result :show}}
(clerk/html [:div.text-center.p-6.bg-gradient-to-r.from-green-400.to-blue-500.text-white.rounded-lg.mt-6
             [:h3.text-2xl.font-bold "🎯 Complete!"]
             [:p.mt-2 "You've learned the main features of Clerk Notebook"]
             [:p "Now you can start creating your own data science and technical documentation!"]])