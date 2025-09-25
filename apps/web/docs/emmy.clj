^{:nextjournal.clerk/visibility {:code :hide}}
(ns docs.emmy
  {:nextjournal.clerk/toc true}
  (:require [nextjournal.clerk :as clerk]
            [emmy.env :as e :refer :all]
            [emmy.expression.render :as render]
            [emmy.viewer :as ev]))

;; # Emmy

;; Emmy is a powerful Clojure(Script) computer algebra system based on MIT's scmutils system.
;; It provides a comprehensive environment for symbolic computation, automatic differentiation,
;; numerical methods, and classical mechanics - particularly suitable for physics and mathematics research.

;; ## Resources

;; - [Emmy Documentation](https://cljdoc.org/d/org.mentat/emmy/CURRENT)
;; - [Emmy GitHub Repository](https://github.com/mentat-collective/emmy)
;; - [Emmy Website](https://emmy.mentat.org/)
;; - [Emmy Viewers](https://emmy-viewers.mentat.org/)

;; ## Key Features

;; Emmy provides:
;; - **Symbolic computation** with expression manipulation and simplification
;; - **Automatic differentiation** for computing derivatives and gradients
;; - **Numerical methods** for integration, optimization, and root finding
;; - **Classical mechanics** tools including Lagrangian and Hamiltonian formulations
;; - **Differential geometry** support for manifolds, tensors, and geometric calculations
;; - **Visualization** capabilities through Emmy Viewers integration

;; ## 1. Basic Symbolic Computation

;; ### 1.1 Creating Symbolic Expressions
(def x 'x)
(def y 'y)
(def z 'z)

;; Create symbolic expressions
(def expr1 (+ (* 3 x) (* 2 y) 5))
expr1

;; ### 1.2 Expression Simplification
(simplify (+ (* x x) (* 2 x x) (* 3 x x)))

;; ### 1.3 Symbolic Operations
(def expr2 (square (+ x y)))
expr2

;; Simplify expression
(simplify expr2)

;; ### 1.4 Trigonometric Simplification
(simplify (+ (square (sin x)) (square (cos x))))

;; ### 1.5 Render as TeX Format
^{:nextjournal.clerk/visibility {:code :show}}
(clerk/tex (->TeX (simplify (square (+ 'a 'b)))))

;; ### 1.6 Infix Notation
(->infix (simplify (cube (+ 'x 3))))

;; ## 2. Automatic Differentiation

;; ### 2.1 Single Variable Differentiation
(def f (fn [x] (* x x x)))  ; f(x) = x³

;; Calculate derivative
((D f) 3)  ; f'(3) = 3x² = 27

;; Symbolic differentiation
(simplify ((D cube) 'x))

;; ### 2.2 Higher Order Derivatives
(simplify ((D (D (D cube))) 'x))  ; Third derivative

;; ### 2.3 Partial Derivatives of Multivariate Functions
(def g (fn [x y] (+ (* x x y) (* y y y))))  ; g(x,y) = x²y + y³

;; Partial derivative with respect to x
(simplify (((partial 0) g) 'x 'y))

;; Partial derivative with respect to y
(simplify (((partial 1) g) 'x 'y))

;; ### 2.4 Gradient Calculation
(def h (fn [[x y z]] (+ (* x y) (* y z) (* z x))))

;; Calculate gradient
((D h) ['x 'y 'z])

;; ### 2.5 Jacobian Matrix
(def vector-fn (fn [[x y]]
                  [(+ (* x x) y)
                   (- (* x y) y)]))

(simplify ((D vector-fn) ['x 'y]))

;; ## 3. Numerical Computation Examples

;; ### 3.1 Function Evaluation
(defn evaluate-polynomial [x]
  (+ (* x x x) (* 2 x x) (- x) 5))

;; Evaluate polynomial at x=2
(evaluate-polynomial 2)

;; ### 3.2 Evaluating Symbolic Expressions
(def poly-expr (+ (cube 'x) (* 2 (square 'x)) (- 'x) 5))
(simplify poly-expr)

;; Evaluate at specific point (manual substitution)
(let [result (+ (cube 2) (* 2 (square 2)) (- 2) 5)]
  result)

;; ## 4. Classical Mechanics Fundamentals

;; ### 4.1 Basic Physical Quantities
;; Kinetic energy expression T = 1/2 * m * v²
(def kinetic-energy (fn [m v] (* 1/2 m (square v))))
(simplify (kinetic-energy 'm 'v))

;; Potential energy expression V = 1/2 * k * x²
(def potential-energy (fn [k x] (* 1/2 k (square x))))
(simplify (potential-energy 'k 'x))

;; Lagrangian L = T - V
(def lagrangian (fn [m k v x]
                  (- (kinetic-energy m v)
                     (potential-energy k x))))
(simplify (lagrangian 'm 'k 'v 'x))

;; ## 5. Differential Geometry

;; ### 5.1 Vectors and Forms
(def v1 (up 1 2 3))
(def v2 (up 4 5 6))

;; Vector addition
(+ v1 v2)

;; Dot product
(dot-product v1 v2)

;; Cross product
(cross-product v1 v2)

;; ### 5.2 Tensor Operations
(def M (up (up 'a 'b)
           (up 'c 'd)))

;; Matrix multiplication
(simplify (* M (up 'x 'y)))

;; ### 5.3 Determinant
(determinant (down (down 1 2)
                   (down 3 4)))

;; ## 6. Visualization Examples

^{:nextjournal.clerk/visibility {:code :show :result :show}}
(clerk/plotly
 {:data [{:x (range -5 5 0.1)
          :y (map #(Math/sin %) (range -5 5 0.1))
          :type "scatter"
          :mode "lines"
          :name "sin(x)"}
         {:x (range -5 5 0.1)
          :y (map #(Math/cos %) (range -5 5 0.1))
          :type "scatter"
          :mode "lines"
          :name "cos(x)"}]
  :layout {:title "Trigonometric Functions"
           :xaxis {:title "x"}
           :yaxis {:title "y"}}})

;; ## 7. Exercises

;; ### Exercise 1: Calculate derivative of composite function
;; Calculate d/dx[sin(x²)]
(def composite-fn (compose sin square))
(simplify ((D composite-fn) 'x))

;; ### Exercise 2: Polynomial Operations
(def poly1 (+ (* 'x 'x) (* 2 'x) 3))
(def poly2 (+ (* 'x 'x) (- 'x) 1))
(simplify (+ poly1 poly2))

;; ### Exercise 3: Solve simple physics problems
;; Projectile motion trajectory
(defn projectile-motion [v0 theta g]
  (fn [t]
    (up (* v0 (cos theta) t)
        (- (* v0 (sin theta) t)
           (* 1/2 g t t)))))

(def trajectory (projectile-motion 10 (/ Math/PI 4) 9.8))
(trajectory 1)  ; Position at t=1 second

;; ## 8. Advanced Topics

;; ### 8.1 Calculus of Variations
;; Brachistochrone problem functional
(defn brachistochrone-lagrangian [[_ [x y] [xdot ydot]]]
  (/ (sqrt (+ (square xdot) (square ydot)))
     (sqrt y)))

;; ### 8.2 Matrix Operations
;; 2x2 rotation matrix
(def rotation-2d
  (fn [theta]
    (up (up (cos theta) (- (sin theta)))
        (up (sin theta) (cos theta)))))

(simplify (rotation-2d 'θ))

;; ### 8.3 Vector Transformations
(def transform-vector
  (fn [matrix vec]
    (* matrix vec)))

;; Apply rotation matrix to vector
(simplify (transform-vector (rotation-2d 'θ) (up 'x 'y)))