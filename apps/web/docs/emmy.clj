^{:nextjournal.clerk/visibility {:code :hide}}
(ns docs.emmy
  {:nextjournal.clerk/toc true}
  (:require [nextjournal.clerk :as clerk]
            [emmy.env :as e :refer :all]
            [emmy.expression.render :as render]
            [emmy.viewer :as ev]))

;; # Emmy

;; Emmy 是一个基于 MIT scmutils 系统的强大 Clojure(Script) 计算机代数系统。
;; 它为符号计算、自动微分、数值方法和经典力学提供了全面的环境，
;; 特别适合物理和数学研究。

;; ## 资源链接

;; - [Emmy 官方文档](https://cljdoc.org/d/org.mentat/emmy/CURRENT)
;; - [Emmy GitHub 仓库](https://github.com/mentat-collective/emmy)
;; - [Emmy 官方网站](https://emmy.mentat.org/)
;; - [Emmy 可视化工具](https://emmy-viewers.mentat.org/)

;; ## 核心功能

;; Emmy 提供：
;; - **符号计算** - 表达式操作和简化
;; - **自动微分** - 计算导数和梯度
;; - **数值方法** - 积分、优化和求根
;; - **经典力学** - 拉格朗日和哈密尔顿公式
;; - **微分几何** - 流形、张量和几何计算
;; - **可视化功能** - 通过 Emmy Viewers 集成

;; ## 1. 基础符号计算

;; ### 1.1 创建符号表达式
(def x 'x)
(def y 'y)
(def z 'z)

;; 创建符号表达式
(def expr1 (+ (* 3 x) (* 2 y) 5))
expr1

;; ### 1.2 表达式简化
(simplify (+ (* x x) (* 2 x x) (* 3 x x)))

;; ### 1.3 符号运算
(def expr2 (square (+ x y)))
expr2

;; 简化表达式
(simplify expr2)

;; ### 1.4 三角函数简化
(simplify (+ (square (sin x)) (square (cos x))))

;; ### 1.5 渲染为 TeX 格式
^{:nextjournal.clerk/visibility {:code :show}}
(clerk/tex (->TeX (simplify (square (+ 'a 'b)))))

;; ### 1.6 中缀表示法
(->infix (simplify (cube (+ 'x 3))))

;; ## 2. 自动微分

;; ### 2.1 一元函数微分
(def f (fn [x] (* x x x)))  ; f(x) = x³

;; 计算导数
((D f) 3)  ; f'(3) = 3x² = 27

;; 符号微分
(simplify ((D cube) 'x))

;; ### 2.2 高阶导数
(simplify ((D (D (D cube))) 'x))  ; 三阶导数

;; ### 2.3 多元函数偏导数
(def g (fn [x y] (+ (* x x y) (* y y y))))  ; g(x,y) = x²y + y³

;; 对 x 求偏导
(simplify (((partial 0) g) 'x 'y))

;; 对 y 求偏导
(simplify (((partial 1) g) 'x 'y))

;; ### 2.4 梯度计算
(def h (fn [[x y z]] (+ (* x y) (* y z) (* z x))))

;; 计算梯度
((D h) ['x 'y 'z])

;; ### 2.5 雅可比矩阵
(def vector-fn (fn [[x y]]
                  [(+ (* x x) y)
                   (- (* x y) y)]))

(simplify ((D vector-fn) ['x 'y]))

;; ## 3. 数值计算示例

;; ### 3.1 函数求值
(defn evaluate-polynomial [x]
  (+ (* x x x) (* 2 x x) (- x) 5))

;; 计算多项式在 x=2 处的值
(evaluate-polynomial 2)

;; ### 3.2 符号表达式求值
(def poly-expr (+ (cube 'x) (* 2 (square 'x)) (- 'x) 5))
(simplify poly-expr)

;; 在特定点求值（手动替换）
(let [result (+ (cube 2) (* 2 (square 2)) (- 2) 5)]
  result)

;; ## 4. 经典力学基础

;; ### 4.1 基本物理量
;; 动能表达式 T = 1/2 * m * v²
(def kinetic-energy (fn [m v] (* 1/2 m (square v))))
(simplify (kinetic-energy 'm 'v))

;; 势能表达式 V = 1/2 * k * x²
(def potential-energy (fn [k x] (* 1/2 k (square x))))
(simplify (potential-energy 'k 'x))

;; 拉格朗日量 L = T - V
(def lagrangian (fn [m k v x]
                  (- (kinetic-energy m v)
                     (potential-energy k x))))
(simplify (lagrangian 'm 'k 'v 'x))

;; ## 5. 微分几何

;; ### 5.1 向量和形式
(def v1 (up 1 2 3))
(def v2 (up 4 5 6))

;; 向量加法
(+ v1 v2)

;; 点积
(dot-product v1 v2)

;; 叉积
(cross-product v1 v2)

;; ### 5.2 张量运算
(def M (up (up 'a 'b)
           (up 'c 'd)))

;; 矩阵乘法
(simplify (* M (up 'x 'y)))

;; ### 5.3 行列式
(determinant (down (down 1 2)
                   (down 3 4)))

;; ## 6. 可视化示例

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
  :layout {:title "三角函数"
           :xaxis {:title "x"}
           :yaxis {:title "y"}}})

;; ## 7. 应用

;; ### 1: 计算复合函数的导数
;; 计算 d/dx[sin(x²)]
(def composite-fn (compose sin square))
(simplify ((D composite-fn) 'x))

;; ### 2: 多项式运算
(def poly1 (+ (* 'x 'x) (* 2 'x) 3))
(def poly2 (+ (* 'x 'x) (- 'x) 1))
(simplify (+ poly1 poly2))

;; ### 3: 求解简单的物理问题
;; 抛体运动的轨迹
(defn projectile-motion [v0 theta g]
  (fn [t]
    (up (* v0 (cos theta) t)
        (- (* v0 (sin theta) t)
           (* 1/2 g t t)))))

(def trajectory (projectile-motion 10 (/ Math/PI 4) 9.8))
(trajectory 1)  ; t=1 秒时的位置

;; ## 8. 高级主题

;; ### 8.1 变分法
;; 最速降线问题的泛函
(defn brachistochrone-lagrangian [[_ [x y] [xdot ydot]]]
  (/ (sqrt (+ (square xdot) (square ydot)))
     (sqrt y)))

;; ### 8.2 矩阵运算
;; 2x2 旋转矩阵
(def rotation-2d
  (fn [theta]
    (up (up (cos theta) (- (sin theta)))
        (up (sin theta) (cos theta)))))

(simplify (rotation-2d 'θ))

;; ### 8.3 向量变换
(def transform-vector
  (fn [matrix vec]
    (* matrix vec)))

;; 应用旋转矩阵到向量
(simplify (transform-vector (rotation-2d 'θ) (up 'x 'y)))
