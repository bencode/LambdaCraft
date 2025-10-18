^{:nextjournal.clerk/visibility {:code :hide}}
(ns pages.sicm.sicm-1-5
  "SICM 第 1.5 节：欧拉-拉格朗日方程 (The Euler-Lagrange Equations)

  学习目标：
  1. 理解如何从最小作用量原理推导出运动方程
  2. 实现变分算子 δ (variation operator)
  3. 实现 Lagrange-equations 函数
  4. 验证不同力学系统的拉格朗日方程"
  {:nextjournal.clerk/toc true}
  (:require [nextjournal.clerk :as clerk]
            [emmy.env :as e :refer [* + - /
                                     literal-function up down velocity coordinate
                                     D Gamma compose simplify partial
                                     dot-product sqrt square ->TeX]]
            [emmy.mechanics.lagrange :as lag]
            [emmy.expression.render :as render]))

;; # SICM 1.5 - 欧拉-拉格朗日方程

;; ## 核心思想

;; 在第 1.4 节，我们学习了**最小作用量原理**：
;; - 真实路径使作用量 S[q] 取平稳值（通常是极小值）
;; - 通过数值方法可以找到这样的路径

;; 但这还不够！物理学家需要一个**微分方程**来描述运动，而不是每次都要做数值优化。

;; **1.5 节的目标**：
;; 从最小作用量原理推导出**欧拉-拉格朗日方程** (Euler-Lagrange equations)
;; 这是一个微分方程，其解就是使作用量平稳的路径。

;; ---

;; ## 一、拉格朗日方程的数学表达

;; ### 符号化表达

;; 如果 L 是拉格朗日量，q 是坐标路径，
;; 并且 q 使作用量 S[q] 平稳，那么 q 满足：

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/tex
  "D(\\partial_2 L \\circ \\Gamma[q]) - \\partial_1 L \\circ \\Gamma[q] = 0")

;; 其中：
;; - ∂₁L：L 对广义坐标（第2个参数）的偏导数
;; - ∂₂L：L 对广义速度（第3个参数）的偏导数
;; - Γ[q]：将路径转换为局域元组的函数
;; - D：对时间求导

;; ### 传统记法

;; 在传统物理教材中，拉格朗日方程写作：

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/tex
  "\\frac{d}{dt} \\frac{\\partial L}{\\partial \\dot{q}_i} - \\frac{\\partial L}{\\partial q_i} = 0, \\quad i=0, \\dots, n-1")

;; 这种记法有歧义：
;; - L 既表示三个变量 (t, q, q̇) 的函数
;; - 又表示一个变量 t 的函数 L∘Γ[q]

;; **函数式记法的优势**：
;; - 明确区分 L（三个参数的函数）和 L∘Γ[q]（一个参数的函数）
;; - 避免符号歧义
;; - 更适合计算机实现

;; ---

;; ## 二、推导拉格朗日方程

;; 我们用两种方法推导：直接法和变分算子法。
;; 这里我们采用**变分算子法**，因为它更系统和优雅。

;; ### 变分算子的定义

;; 对于一个依赖于路径的函数 f[q]，其**变分** δ_η f[q] 定义为：

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/tex
  "\\delta_\\eta f[q] = \\lim_{\\epsilon \\to 0} \\frac{f[q + \\epsilon\\eta] - f[q]}{\\epsilon}")

;; 这是 f 随路径微小变分而变化的线性近似。

;; **关键性质**：
;; 1. δ_η I[q] = η，其中 I[q] = q（恒等路径函数）
;; 2. δ_η g[q] = Dη，其中 g[q] = Dq（导数路径函数）
;; 3. 变分满足乘法法则、加法法则、链式法则
;; 4. D 和 δ 对易：D(δ_η f[q]) = δ_η(Df[q])

;; ### 推导步骤

;; **步骤 1**：作用量的变分

;; 作用量定义为：

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/tex
  "S[q](t_1, t_2) = \\int_{t_1}^{t_2} L \\circ \\Gamma[q] \\, dt")

;; 对于使作用量平稳的路径 q，必须有：

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/tex
  "\\delta_\\eta S[q](t_1, t_2) = 0")

;; 对所有满足 η(t₁) = η(t₂) = 0 的变分 η 成立。

;; **步骤 2**：变分与积分对易

;; 由于变分与积分对易：

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/tex
  "\\delta_\\eta S[q](t_1, t_2) = \\int_{t_1}^{t_2} \\delta_\\eta (L \\circ \\Gamma[q]) \\, dt")

;; **步骤 3**：链式法则

;; Γ[q](t) = (t, q(t), Dq(t))，所以：
;; δ_η Γ[q](t) = (0, η(t), Dη(t))

;; 应用链式法则：

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/tex
  "\\delta_\\eta (L \\circ \\Gamma[q]) = (\\partial_1 L \\circ \\Gamma[q]) \\cdot 0 + (\\partial_2 L \\circ \\Gamma[q]) \\cdot D\\eta")

;; 等等！这里有问题。让我重新推导：

;; L 是一个三参数函数 L(t, q, v)
;; Γ[q](t) = (t, q(t), Dq(t))
;; 所以 L∘Γ[q] 是 t 的函数

;; 当路径变分为 q + εη 时：
;; Γ[q + εη](t) = (t, q(t) + εη(t), Dq(t) + εDη(t))

;; 所以：
;; δ_η Γ[q](t) = (0, η(t), Dη(t))

;; 应用链式法则（多变量函数的微分）：

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/tex
  "\\delta_\\eta (L \\circ \\Gamma[q]) = (\\partial_1 L \\circ \\Gamma[q]) \\eta + (\\partial_2 L \\circ \\Gamma[q]) D\\eta")

;; 注意：
;; - ∂₁L 对应 q 坐标的偏导（这里记法约定：参数索引从 1 开始）
;; - ∂₂L 对应速度 v 的偏导

;; **步骤 4**：分部积分

;; 现在作用量的变分为：

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/tex
  "\\delta_\\eta S[q] = \\int_{t_1}^{t_2} \\left[ (\\partial_1 L \\circ \\Gamma[q]) \\eta + (\\partial_2 L \\circ \\Gamma[q]) D\\eta \\right] dt")

;; 对第二项分部积分：

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/tex
  "\\int_{t_1}^{t_2} (\\partial_2 L \\circ \\Gamma[q]) D\\eta \\, dt = \\left[ (\\partial_2 L \\circ \\Gamma[q]) \\eta \\right]_{t_1}^{t_2} - \\int_{t_1}^{t_2} D(\\partial_2 L \\circ \\Gamma[q]) \\eta \\, dt")

;; 由于 η(t₁) = η(t₂) = 0，边界项为零。

;; **步骤 5**：推出拉格朗日方程

;; 所以：

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/tex
  "\\delta_\\eta S[q] = \\int_{t_1}^{t_2} \\left[ (\\partial_1 L \\circ \\Gamma[q]) - D(\\partial_2 L \\circ \\Gamma[q]) \\right] \\eta \\, dt = 0")

;; 这对**任意** η（只要端点为零）都成立。
;; 用反证法：如果积分中的方括号不恒为零，
;; 我们可以选择一个"凸起"函数 η 使积分非零。

;; 因此必须有：

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/tex
  "D(\\partial_2 L \\circ \\Gamma[q]) - (\\partial_1 L \\circ \\Gamma[q]) = 0")

;; 这就是**欧拉-拉格朗日方程**！

;; ---

;; ## 三、实现变分算子

;; 虽然在推导中我们需要理解变分算子的概念，
;; 但在计算拉格朗日方程时，我们不需要显式实现 δ。
;; 因为拉格朗日方程的最终形式不包含 δ。

;; 不过，为了完整性和理解练习 1.8，我们还是实现一下：

;; ### 练习 1.8：实现 delta 算子

;; 任务：实现一个 delta 函数，计算路径依赖函数的变分。

;; 定义：
;; ((delta eta) f)[q](t) = δ_η f[q](t)
;;                       = lim_{ε→0} (f[q + εη](t) - f[q](t)) / ε

;; 根据定义，这等价于对 ε 在 0 处求导：
;; δ_η f[q](t) = D_ε(f[q + εη](t))|_{ε=0}

(defn delta
  "变分算子

  参数：
  - eta: 路径变分函数

  返回：
  - 一个算子，作用于路径依赖函数 f
  - 结果是 ((delta eta) f)[q]，这是一个新的路径依赖函数
  - 其在时间 t 的值为 δ_η f[q](t)

  实现原理：
  δ_η f[q] = D_ε(f[q + εη])|_{ε=0}
  即对 ε 求导，然后令 ε = 0"
  [eta]
  (fn [f]
    (fn [q]
      ;; 构造 g(ε) = f[q + εη]
      ;; 计算 Dg(0)
      (fn [t]
        (let [g (fn [eps]
                  ((f (+ q (* eps eta))) t))]
          ((D g) 0))))))

;; 测试 delta 算子：

;; 定义一个简单的路径依赖函数：f[q](t) = F(Γ[q](t))
;; 其中 F 是一个字面函数

;; 注意：这里的 delta 算子实现主要用于理解概念
;; 在实际计算拉格朗日方程时，我们不需要显式使用 delta
;; 因为拉格朗日方程的最终形式已经不包含 delta 算子

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/md
  "**练习 1.8 说明**：

  delta 算子的实现已经完成（见上面的 `delta` 函数）。

  该实现遵循定义：δ_η f[q] = D_ε(f[q + εη])|_{ε=0}

  在实际应用中，我们通常不直接使用 delta 算子，
  而是直接使用已经推导出的拉格朗日方程形式。")

;; ---

;; ## 四、实现 Lagrange-equations 函数

;; 现在我们来实现核心函数：从拉格朗日量计算拉格朗日方程。

;; ### Lagrange-equations 的实现

;; 根据方程：
;; D(∂₂L ∘ Γ[q]) - (∂₁L ∘ Γ[q]) = 0

;; 实现思路：
;; 1. 输入：拉格朗日量 L
;; 2. 输出：一个函数，作用于路径 q
;; 3. 结果：一个时间函数，计算拉格朗日方程的左侧（残差）

(defn Lagrange-equations
  "计算拉格朗日方程的左侧（残差）

  参数：
  - Lagrangian: 拉格朗日量函数 L(t, q, v)

  返回：
  - 一个函数，作用于路径 q
  - 结果是一个时间函数 t ↦ residual(t)
  - residual(t) = D(∂₂L ∘ Γ[q])(t) - (∂₁L ∘ Γ[q])(t)
  - 如果 q 是真实轨迹，则 residual(t) ≈ 0

  实现步骤：
  1. 计算 ∂₁L（对坐标的偏导）
  2. 计算 ∂₂L（对速度的偏导）
  3. 组合：D(∂₂L ∘ Γ[q]) - (∂₁L ∘ Γ[q])"
  [Lagrangian]
  (fn [q]
    (- (D (compose ((partial 2) Lagrangian) (Gamma q)))
       (compose ((partial 1) Lagrangian) (Gamma q)))))

;; 注意：
;; - (partial 1) 表示对第2个参数（索引从0开始）求偏导，即坐标 q
;; - (partial 2) 表示对第3个参数求偏导，即速度 v
;; - 这里的索引约定与数学记法 ∂₁, ∂₂ 对应（从1开始）

;; ---

;; ## 五、验证示例

;; ### 示例 1：自由粒子

;; 自由粒子的拉格朗日量（从 1.4 节复用）：

(defn L-free-particle
  "自由粒子的拉格朗日量：L = (1/2) m v²"
  [mass]
  (fn [local]
    (let [v (velocity local)]
      (* 1/2 mass (dot-product v v)))))

;; 测试直线路径：
(defn test-path-linear
  "一维直线路径：q(t) = at + a₀"
  [t]
  (+ (* 'a t) 'a0))

;; 计算残差：
(def residual-free-particle
  (((Lagrange-equations (L-free-particle 'm))
    test-path-linear)
   't))

residual-free-particle
;; => 应该为 0（因为直线是自由粒子的真实轨迹）

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/md
  (str "**自由粒子验证**：直线路径的残差 = " residual-free-particle))

;; 对于任意路径呢？

(def x-arbitrary (literal-function 'x))

(def residual-free-particle-general
  (simplify
    (((Lagrange-equations (L-free-particle 'm))
      x-arbitrary)
     't)))

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/md
  "**任意路径的拉格朗日方程**：")

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/tex (->TeX residual-free-particle-general))

;; 结果应该是 m D²x(t)
;; 方程为：m D²x(t) = 0，即 D²x = 0
;; 这正是自由粒子的运动方程（加速度为零）！

;; ### 示例 2：谐振子

;; 谐振子拉格朗日量（从 1.4 节复用）：

(defn L-harmonic
  "谐振子的拉格朗日量：L = (1/2) m v² - (1/2) k q²"
  [m k]
  (fn [local]
    (let [q (coordinate local)
          v (velocity local)]
      (- (* 1/2 m (square v))
         (* 1/2 k (square q))))))

;; 提议解：q(t) = A cos(ωt + φ)

(defn proposed-harmonic-solution
  "谐振子的提议解：q(t) = A cos(ωt + φ)"
  [t]
  (* 'A (e/cos (+ (* 'omega t) 'phi))))

;; 计算残差：

(def residual-harmonic
  (simplify
    (((Lagrange-equations (L-harmonic 'm 'k))
      proposed-harmonic-solution)
     't)))

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/md
  "**谐振子验证**：提议解的残差")

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/tex (->TeX residual-harmonic))

;; 结果应该是：A cos(ωt + φ) (k - m ω²)
;; 要使残差为零，必须 k - m ω² = 0
;; 即 ω = √(k/m)

;; 这告诉我们：
;; - 只有当 ω = √(k/m) 时，q(t) = A cos(ωt + φ) 才是解
;; - 这是谐振子的固有频率！

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/md
  "**结论**：提议解是真实轨迹，当且仅当 ω² = k/m")

;; ---

;; ## 六、练习 1.9：推导三个系统的拉格朗日方程

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/md
  "### 练习 1.9a：平面摆

  **物理系统**：
  - 质量为 m 的摆锤
  - 通过长度为 l 的无质量杆连接到枢轴
  - 重力加速度 g
  - 角度 θ 测量摆杆与铅垂线的夹角

  **拉格朗日量**：
  L(t, θ, θ̇) = (1/2) m l² θ̇² + m g l cos(θ)")

;; 实现拉格朗日量：

(defn L-pendulum
  "平面摆的拉格朗日量"
  [m l g]
  (fn [local]
    (let [theta (coordinate local)
          theta-dot (velocity local)]
      (+ (* 1/2 m (square l) (square theta-dot))
         (* m g l (e/cos theta))))))

;; 对任意路径求拉格朗日方程：

(def theta-path (literal-function 'theta))

(def pendulum-equation
  (simplify
    (((Lagrange-equations (L-pendulum 'm 'l 'g))
      theta-path)
     't)))

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/md
  "**平面摆的拉格朗日方程**：")

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/tex (->TeX pendulum-equation))

;; 手工推导对比：
;; ∂₁L = -m g l sin(θ)
;; ∂₂L = m l² θ̇
;; D(∂₂L ∘ Γ[θ]) = m l² D²θ
;; 拉格朗日方程：m l² D²θ + m g l sin(θ) = 0
;; 即：D²θ + (g/l) sin(θ) = 0

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/md
  "**预期结果**：m l² D²θ(t) + m g l sin(θ(t)) = 0

  简化后：D²θ + (g/l) sin(θ) = 0

  这是非线性单摆的经典运动方程。")

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/md
  "### 练习 1.9b：二维势场中的粒子

  **势能**：V(x, y) = (x² + y²)/2 + x²y - y³/3

  **拉格朗日量**：L = (1/2) m (vₓ² + vᵧ²) - V(x, y)")

;; 定义势能：

(defn V-2d-potential
  "二维势能函数"
  [x y]
  (+ (/ (+ (square x) (square y)) 2)
     (* (square x) y)
     (- (/ (e/expt y 3) 3))))

;; 定义拉格朗日量：

(defn L-2d-potential
  "二维势场中粒子的拉格朗日量"
  [m]
  (fn [local]
    (let [q (coordinate local)
          v (velocity local)
          x (nth q 0)
          y (nth q 1)
          vx (nth v 0)
          vy (nth v 1)]
      (- (* 1/2 m (+ (square vx) (square vy)))
         (V-2d-potential x y)))))

;; 对任意路径求方程

(def xy-path (up (literal-function 'x)
                 (literal-function 'y)))

(def potential-2d-equation
  (simplify
    (((Lagrange-equations (L-2d-potential 'm))
      xy-path)
     't)))

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/md
  "**二维势场的拉格朗日方程**：")

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/tex (->TeX potential-2d-equation))

;; 预期结果：
;; 对 x：m D²x = -∂V/∂x = -(x + 2xy)
;; 对 y：m D²y = -∂V/∂y = -(y + x² - y²)

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/md
  "**预期结果**：

  x 分量：m D²x(t) = -x(t) - 2x(t)y(t)

  y 分量：m D²y(t) = -y(t) - x²(t) + y²(t)")

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/md
  "### 练习 1.9c：球面约束运动

  **约束**：粒子在半径为 R 的球面上运动

  **坐标**：
  - θ：余纬（与北极的夹角）
  - φ：经度

  **拉格朗日量**：
  L = (1/2) m R² (θ̇² + (φ̇ sin θ)²)")

;; 实现拉格朗日量：

(defn L-sphere
  "球面约束运动的拉格朗日量"
  [m R]
  (fn [local]
    (let [q (coordinate local)
          qdot (velocity local)
          theta (nth q 0)
          phi (nth q 1)
          theta-dot (nth qdot 0)
          phi-dot (nth qdot 1)]
      (* 1/2 m (square R)
         (+ (square theta-dot)
            (square (* phi-dot (e/sin theta))))))))

;; 对任意路径求方程：

(def sphere-path (up (literal-function 'theta)
                     (literal-function 'phi)))

(def sphere-equation
  (simplify
    (((Lagrange-equations (L-sphere 'm 'R))
      sphere-path)
     't)))

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/md
  "**球面约束运动的拉格朗日方程**：")

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/tex (->TeX sphere-equation))

;; 预期结果：
;; θ 方程：m R² D²θ - m R² φ̇² sin θ cos θ = 0
;; φ 方程：d/dt(m R² φ̇ sin² θ) = 0

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/md
  "**预期结果**：

  θ 方程：m R² D²θ(t) - m R² [Dφ(t)]² sin(θ(t)) cos(θ(t)) = 0

  φ 方程：d/dt[m R² Dφ(t) sin²(θ(t))] = 0

  第二个方程表明 φ̇ sin² θ 是守恒量（角动量守恒）。")

;; ---


^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/md
  "### 练习 1.11：验证开普勒第三定律

  **系统**：两个粒子在引力作用下的相对运动

  **拉格朗日量**（极坐标形式）：
  L = (1/2) μ (ṙ² + r² φ̇²) + G m₁ m₂ / r

  其中 μ = m₁m₂/(m₁+m₂) 是折合质量")

;; 引力势能：

(defn gravitational-energy
  "引力势能"
  [G m1 m2]
  (fn [r]
    (- (/ (* G m1 m2) r))))

;; 中心力场拉格朗日量（极坐标）：

(defn L-central-polar
  "中心力场下粒子的拉格朗日量（极坐标）"
  [m V]
  (fn [local]
    (let [q (coordinate local)
          qdot (velocity local)
          r (nth q 0)
          phi (nth q 1)
          rdot (nth qdot 0)
          phidot (nth qdot 1)]
      (- (* 1/2 m
            (+ (square rdot)
               (square (* r phidot))))
         (V r)))))

;; 圆轨道路径：
;; r(t) = a（常数）
;; φ(t) = n·t（匀速转动）
;; 其中 n 是角频率，a 是轨道半径

(defn circular-orbit
  "圆轨道路径"
  [a n]
  (fn [t]
    (up a (* n t))))

;; 代入拉格朗日方程：

(let [G 'G
      m1 'm_1
      m2 'm_2
      mu (/ (* m1 m2) (+ m1 m2))
      a 'a
      n 'n
      V (gravitational-energy G m1 m2)
      L (L-central-polar mu V)
      orbit (circular-orbit a n)]
  (simplify
    (((Lagrange-equations L) orbit) 't)))

;; 结果应该给出两个方程：
;; r 方程：-μ a n² + G m₁ m₂ / a² = 0
;; φ 方程：0 = 0（自动满足）

;; 从 r 方程得到：
;; μ a n² = G m₁ m₂ / a²
;; n² a³ = G m₁ m₂ / μ = G (m₁ + m₂)

;; 这就是开普勒第三定律

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/md
"从圆轨道的拉格朗日方程，我们得到：

  n² a³ = G (m₁ + m₂)

  其中：
  - n：轨道角频率
  - a：轨道半径
  - G：引力常数
  - m₁, m₂：两个粒子的质量

  这正是开普勒第三定律的数学表达！")
