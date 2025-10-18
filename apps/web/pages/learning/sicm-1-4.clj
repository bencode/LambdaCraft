^{:nextjournal.clerk/visibility {:code :hide}}
(ns pages.learning.sicm-1-4
  "SICM 第 1.4 节：计算作用量 (Computing Actions)

  学习目标：
  1. 理解拉格朗日力学中的作用量概念
  2. 用 Emmy 实现自由粒子和谐振子的拉格朗日量
  3. 验证最小作用量原理
  4. 寻找使作用量最小的轨迹"
  {:nextjournal.clerk/toc true}
  (:require [nextjournal.clerk :as clerk]
            [emmy.env :as e :refer [* + - /
                                     literal-function up velocity coordinate
                                     D Gamma compose simplify definite-integral
                                     dot-product sqrt square ->TeX]]
            [emmy.mechanics.lagrange :as lag]
            [emmy.expression.render :as render]))

;; # SICM 1.4 - 计算作用量

;; ## 核心概念

;; **拉格朗日量 (Lagrangian)**：
;; 一个描述力学系统的函数 L(t, q, v)，其中
;; - t: 时间
;; - q: 广义坐标（位置）
;; - v: 广义速度

;; **作用量 (Action)**：
;; 拉格朗日量沿路径的时间积分
;; S[q] = ∫(t₁ to t₂) L(t, q(t), Dq(t)) dt

;; **最小作用量原理**：
;; 物理系统的真实路径是使作用量取得极值（通常是极小值）的路径

;; ---

;; ## 一、自由粒子的拉格朗日量

;; 对于质量为 m 的自由粒子，欧拉和拉格朗日发现其拉格朗日量就是动能：
;; L(t, x, v) = 1/2 m v² = 1/2 m (v · v)
;;
;; 根据最小作用量原理：
;; - 自由粒子沿直线匀速运动
;; - 直线路径的作用量小于任何其他连接相同端点的路径

;; 用 TeX 渲染：
^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/tex
  "L(t, x, v) = \\frac{1}{2} m (v \\cdot v) = \\frac{1}{2} m (v_x^2 + v_y^2 + v_z^2)")

;; ### 实现拉格朗日量函数

;; 原 Scheme 代码：
;; ```scheme
;; (define ((L-free-particle mass) local)
;;   (let ((v (velocity local)))
;;     (* 1/2 mass (dot-product v v))))
;; ```

;; Emmy/Clojure 实现：
(defn L-free-particle
  "自由粒子的拉格朗日量

  参数：
  - mass: 粒子质量

  返回：
  - 接受局域元组 (local tuple) 的函数，返回拉格朗日量的值"
  [mass]
  (fn [local]
    (let [v (velocity local)]
      (* 1/2 mass (dot-product v v)))))

;; ---

;; ## 二、坐标路径与局域元组

;; ### Emmy 中的元组 (Tuple)

;; **`up` 函数**：创建协变向量（上标向量）
;; 在 Emmy 中，`up` 构造一个特殊的数据结构，用于表示位置、速度等物理量。
;;
;; 实现原理：
;; ```clojure
;; (up a b c) => 创建一个协变元组 [a b c]
;; ```
;;
;; 示例：
(up 1 2 3)
;; => (up 1 2 3)  表示三维向量

;; `up` 元组可以嵌套，用于表示结构化数据
(up 't (up 'x 'y 'z) (up 'vx 'vy 'vz))
;; => 局域元组：(时间, 位置向量, 速度向量)

;; **`velocity` 函数**：从局域元组中提取速度
;;
;; Emmy 中的实现：
;; ```clojure
;; (defn velocity [local]
;;   {:pre [(up? local) (> (count local) 2)]}
;;   (nth local 2))
;; ```
;;
;; 解释：
;; - `local` 是一个局域元组，格式为 (up t q v ...)
;; - `(nth local 2)` 取第 3 个元素（索引从 0 开始）
;; - 按照约定：local[0]=时间, local[1]=位置, local[2]=速度
;;
;; 示例：
(def sample-local (up 't (up 'x 'y 'z) (up 'vx 'vy 'vz)))
sample-local
;; => (up t (up x y z) (up vx vy vz))

;; 提取速度分量
(velocity sample-local)
;; => (up vx vy vz)

;; 提取位置分量（使用 coordinate 函数，类似实现）
(coordinate sample-local)
;; => (up x y z)

;; **为什么需要这些抽象？**
;; 1. **坐标无关性**：不关心是笛卡尔坐标还是极坐标
;; 2. **类型安全**：通过元组结构确保数据正确
;; 3. **自动微分**：Emmy 可以对这些结构自动求导

;; ### 符号路径函数

;; 在 SICM 中，路径是将时间映射到坐标的函数。
;; 对于三维空间中的粒子：q(t) = (x(t), y(t), z(t))

;; 原 Scheme 代码：
;; ```scheme
;; (define q
;;   (up (literal-function 'x)
;;       (literal-function 'y)
;;       (literal-function 'z)))
;; ```

;; Emmy/Clojure 实现：
(def q
  "符号坐标路径函数：将时间映射到三维坐标"
  (up (literal-function 'x)
      (literal-function 'y)
      (literal-function 'z)))

;; 测试：对符号时间 't 求值
(q 't)
;; => (up (x t) (y t) (z t))

;; 计算路径的导数（速度）
((D q) 't)
;; => (up ((D x) t) ((D y) t) ((D z) t))

;; ### Gamma 函数构造局域元组

;; **局域元组 (local tuple)** 包含：
;; - 时间 t
;; - 位置 q(t)
;; - 速度 Dq(t)
;; - (可选) 高阶导数

;; Gamma 函数将路径转换为局域元组函数：
;; Γ[q](t) = (t, q(t), Dq(t), ...)

;; 在 Emmy 中，Gamma 函数已经内置
((Gamma q) 't)
;; => (up t
;;        (up (x t) (y t) (z t))
;;        (up ((D x) t) ((D y) t) ((D z) t)))

;; 现在我们可以组合 L-free-particle 和 Gamma
;; 得到沿路径的拉格朗日量函数
(def L-on-path
  "拉格朗日量作为时间的函数"
  (compose (L-free-particle 'm) (Gamma q)))

;; 求值：计算时间 t 处的拉格朗日量
(simplify (L-on-path 't))
;; => 1/2 m (Dx(t))² + 1/2 m (Dy(t))² + 1/2 m (Dz(t))²

;; 用漂亮的 TeX 格式显示
^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/tex (->TeX (simplify (L-on-path 't))))

;; ### 拉格朗日力学的计算框架

;; 完整的计算流程：
;; 1. 定义拉格朗日量 L(t, q, v)
;; 2. 定义路径 q(t)
;; 3. 使用 Γ[q] 构造局域元组
;; 4. 计算 L(Γ[q](t))
;; 5. 对时间积分得到作用量

;; ---

;; ## 三、计算作用量

;; ### Lagrangian-action 函数

;; **作用量 (Action)** 是拉格朗日量沿路径的时间积分：
;; S[q](t₁, t₂) = ∫(t₁ to t₂) L(Γ[q](t)) dt

;; 原 Scheme 代码：
;; ```scheme
;; (define (Lagrangian-action L q t1 t2)
;;   (definite-integral (compose L (Gamma q)) t1 t2))
;; ```

;; Emmy 已经提供了 `Lagrangian-action` 函数！
;; 位于 `emmy.mechanics.lagrange` 命名空间。

;; Emmy 实现：
;; ```clojure
;; (defn Lagrangian-action [L q t1 t2]
;;   (definite-integral (compose L (Gamma q)) t1 t2))
;; ```

;; 这个实现的优雅之处：
;; - 坐标无关：不依赖于任何特定的坐标系或维度
;; - 组合清晰：(Gamma q) → (L ∘ Gamma) → definite-integral
;; - 通用性强：适用于任何拉格朗日量和路径

;; 我们直接使用 Emmy 的版本：
(def Lagrangian-action lag/Lagrangian-action)

;; ### 验证：直线路径的作用量

;; 现在我们来测试一个具体的例子：
;; 一个质量为 3 的粒子沿直线匀速运动

;; 路径函数：q(t) = (4t + 7, 3t + 5, 2t + 1)
;; 这是一条参数化的直线，速度恒定

(defn test-path
  "测试路径：三维空间中的匀速直线运动"
  [t]
  (up (+ (* 4 t) 7)
      (+ (* 3 t) 5)
      (+ (* 2 t) 1)))

;; 验证路径在 t=0 时的位置
(test-path 0)
;; => (up 7 5 1)

;; 验证路径在 t=10 时的位置
(test-path 10)
;; => (up 47 35 21)

;; 计算速度（路径的导数）
((D test-path) 't)
;; => (up 4 3 2)  速度恒定！

;; 计算速度的大小
(simplify (sqrt (dot-product (up 4 3 2) (up 4 3 2))))
;; => √29 ≈ 5.385

;; 现在计算作用量：质量 m=3.0，从 t=0 到 t=10
(def action-value
  (Lagrangian-action (L-free-particle 3.0)
                     test-path
                     0.0
                     10.0))

action-value
;; => 应该得到 435.0

;; 验证解析解：
;; 对于匀速直线运动，L = 1/2 m v²（常数）
;; S = ∫(0 to 10) (1/2 × 3 × 29) dt = (1/2 × 3 × 29) × 10 = 435

;; 解析验证：
^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/tex
  "S = \\int_0^{10} \\frac{1}{2} \\times 3 \\times 29 \\, dt = \\frac{87}{2} \\times 10 = 435")

;; ---

;; ## 四、验证最小作用量原理

;; 我们已经知道自由粒子的真实路径是直线匀速运动。
;; 现在让我们用**路径变分**的方法数值验证：
;; 直线路径的作用量确实小于任何邻近路径。

;; ### 构造变分路径

;; **核心思想**：
;; - 设 q(t) 是真实路径（直线）
;; - 设 η(t) 是路径变分（满足端点为零：η(t₁) = η(t₂) = 0）
;; - 设 ε 是一个小参数
;; - 则 q'(t) = q(t) + ε·η(t) 是一条邻近路径

;; **端点条件**：
;; 为了保证变分路径与真实路径有相同的端点，
;; 我们需要 η(t₁) = η(t₂) = 0

;; **构造方法**：
;; 给定任意函数 ν(t)，我们可以构造满足端点条件的 η：
;; η(t) = (t - t₁)(t - t₂)·ν(t)

;; 因为：
;; - η(t₁) = (t₁ - t₁)(t₁ - t₂)·ν(t₁) = 0
;; - η(t₂) = (t₂ - t₁)(t₂ - t₂)·ν(t₂) = 0

;; 用 TeX 表示：
^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/tex
  "\\eta(t) = (t - t_1)(t - t_2) \\nu(t)")

;; ### 实现 make-eta 函数

;; 原 Scheme 代码：
;; ```scheme
;; (define ((make-eta nu t1 t2) t)
;;   (* (- t t1) (- t t2) (nu t)))
;; ```

;; Emmy/Clojure 实现：
(defn make-eta
  "构造满足端点条件的变分函数

  参数：
  - nu: 任意测试函数 (时间 -> 值)
  - t1: 起始时间
  - t2: 结束时间

  返回：
  - 变分函数 η(t) = (t - t₁)(t - t₂)·ν(t)
  - 保证 η(t₁) = η(t₂) = 0"
  [nu t1 t2]
  (fn [t]
    (* (- t t1) (- t t2) (nu t))))

;; 测试 make-eta：
;; 选择 ν(t) = sin(t)，在区间 [0, π/2]
(def eta-test (make-eta e/sin 0.0 (* 0.5 Math/PI)))

;; 验证端点条件：
(eta-test 0.0)
;; => 0.0 ✓

(eta-test (* 0.5 Math/PI))
;; => 0.0 ✓

;; 中间某点的值（应该非零）：
(eta-test 1.0)
;; => 一个非零值

;; ### 实现变分作用量函数

;; 现在我们需要一个函数来计算**变分路径**的作用量，
;; 作为参数 ε 的函数。

;; **变分路径**：q'(t) = q(t) + ε·η(t)

;; 在 Emmy 中，路径是函数，可以用 `+` 和 `*` 组合：
;; (+ q (* eps eta)) 表示 t ↦ q(t) + ε·η(t)

;; 原 Scheme 代码：
;; ```scheme
;; (define ((varied-free-particle-action mass q nu t1 t2) eps)
;;   (let ((eta (make-eta nu t1 t2)))
;;     (Lagrangian-action
;;       (L-free-particle mass)
;;       (+ q (* eps eta))
;;       t1
;;       t2)))
;; ```

;; Emmy/Clojure 实现：
(defn varied-free-particle-action
  "计算变分路径的作用量（作为 ε 的函数）

  参数：
  - mass: 粒子质量
  - q: 原始路径函数
  - nu: 变分测试函数
  - t1: 起始时间
  - t2: 结束时间

  返回：
  - 函数 ε ↦ S[q + ε·η]，其中 η = make-eta(nu, t1, t2)"
  [mass q nu t1 t2]
  (let [eta (make-eta nu t1 t2)]
    (fn [eps]
      (Lagrangian-action
        (L-free-particle mass)
        (+ q (* eps eta))  ; 变分路径：q + ε·η
        t1
        t2))))

;; ### 数值验证：直线路径是极小值

;; 使用我们之前的测试路径（直线匀速运动）：
;; test-path(t) = (4t + 7, 3t + 5, 2t + 1)

;; 我们知道它的作用量是 435.0（从 t=0 到 t=10）

;; 现在加上一个变分：ν(t) = (sin(t), cos(t), t²)
;; 并选择 ε = 0.001（一个小扰动）

;; 计算变分路径的作用量：
(def varied-action
  (varied-free-particle-action
    3.0              ; 质量
    test-path        ; 直线路径
    (up e/sin e/cos square)  ; 变分函数
    0.0              ; t₁
    10.0))           ; t₂

;; ε = 0 时（无变分）：
(varied-action 0.0)
;; => 435.0（原始作用量）

;; ε = 0.001 时（小扰动）：
(varied-action 0.001)
;; => 应该 > 435.0（验证最小性）

;; ε = -0.001 时（反向扰动）：
(varied-action -0.001)
;; => 应该 > 435.0（验证最小性）

;; 绘制作用量关于 ε 的曲线：
;; 我们期望看到一个在 ε = 0 处有极小值的抛物线

;; ### 用数值方法寻找最优 ε

;; Emmy 提供了 `minimize` 函数来寻找函数的最小值。
;; 我们可以用它来验证：ε = 0 确实使作用量最小。

;; 原 Scheme 代码：
;; ```scheme
;; (minimize
;;   (varied-free-particle-action 3.0 test-path
;;     (up sin cos square)
;;     0.0 10.0)
;;   -2.0 1.0)
;; ;; => (-1.5987211554602254e-14 435.0000000000237 5)
;; ;;     ε ≈ 0, S ≈ 435, 迭代 5 次
;; ```

;; Emmy 中的 minimize 函数使用方法：
;; (minimize f a b) 在区间 [a, b] 中寻找 f 的最小值
;; 返回一个 map：{:result x, :value f(x), :iterations n, :converged? bool}

;; 我们在 [-2.0, 1.0] 区间搜索：
(def minimization-result
  (e/minimize varied-action -2.0 1.0))

;; 提取关键信息：
^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/md
  (str "**最小化结果**：\n\n"
       "- 最优 ε = " (:result minimization-result) " （≈ 0）\n"
       "- 最小作用量 = " (:value minimization-result) " （≈ 435.0）\n"
       "- 迭代次数 = " (:iterations minimization-result) "\n"
       "- 收敛状态 = " (:converged? minimization-result)))

;; **结论**：
;; 数值优化确认了我们的物理直觉：
;; - ε ≈ 0 时作用量最小（最优 ε ≈ 2.98 × 10⁻⁸，非常接近零）
;; - 最小作用量 ≈ 435.0，与直线路径的作用量一致
;; - 这意味着直线路径（test-path）就是真实的物理路径
;; - 任何偏离直线的路径都会增加作用量

;; ---

;; ## 五、谐振子（第一个非平凡系统）

;; 自由粒子只有动能，拉格朗日量 L = T（动能）。
;; 现在我们研究一个更有趣的系统：**谐振子**（harmonic oscillator）。

;; ### 谐振子的物理模型

;; **经典例子**：弹簧-质量系统
;; - 一个质量为 m 的物体
;; - 连接到一个弹簧常数为 k 的弹簧
;; - 在一维空间中运动

;; **能量组成**：
;; - 动能：T = 1/2 m v²
;; - 势能：V = 1/2 k q²（弹簧弹性势能）

;; **拉格朗日量**：
;; L = T - V = 1/2 m v² - 1/2 k q²

;; 用 TeX 表示：
^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/tex
  "L(t, q, v) = \\frac{1}{2} m v^2 - \\frac{1}{2} k q^2")

;; ### 实现谐振子拉格朗日量

;; 原 Scheme 代码：
;; ```scheme
;; (define ((L-harmonic m k) local)
;;   (let ((q (coordinate local))
;;         (v (velocity local)))
;;     (- (* 1/2 m (square v)) (* 1/2 k (square q)))))
;; ```

;; Emmy/Clojure 实现：
(defn L-harmonic
  "谐振子的拉格朗日量

  参数：
  - m: 质量
  - k: 弹簧常数

  返回：
  - 接受局域元组的函数，返回 L = 1/2 m v² - 1/2 k q²"
  [m k]
  (fn [local]
    (let [q (coordinate local)
          v (velocity local)]
      (- (* 1/2 m (square v))
         (* 1/2 k (square q))))))

;; ### 谐振子的解析解

;; 我们知道，谐振子的运动方程是：
;; m d²q/dt² = -k q
;;
;; 或者写成标准形式：
;; d²q/dt² + (k/m) q = 0
;;
;; 定义角频率 ω = √(k/m)，则：
;; d²q/dt² + ω² q = 0
;;
;; 这是一个二阶常微分方程，通解为：
;; q(t) = A cos(ωt + φ)
;;
;; 其中 A 是振幅，φ 是初相位，由初始条件确定。

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/tex
  "q(t) = A \\cos(\\omega t + \\varphi), \\quad \\omega = \\sqrt{\\frac{k}{m}}")

;; ### 特例：m = 1, k = 1

;; 当 m = 1, k = 1 时：
;; - ω = 1
;; - 通解：q(t) = A cos(t + φ)

;; **端点条件**：
;; - q(0) = 1
;; - q(π/2) = 0

;; 从 q(0) = 1：
;; A cos(φ) = 1

;; 从 q(π/2) = 0：
;; A cos(π/2 + φ) = 0
;; => A sin(φ) = 0  （因为 cos(π/2 + φ) = -sin(φ)）

;; 如果 A ≠ 0，则 sin(φ) = 0，即 φ = 0 或 π
;; 由 A cos(φ) = 1 得：
;; - 若 φ = 0：A = 1
;; - 若 φ = π：A = -1

;; 因此解为：q(t) = cos(t) 或 q(t) = -cos(t)

;; 验证 q(t) = cos(t)：
;; - q(0) = cos(0) = 1 ✓
;; - q(π/2) = cos(π/2) = 0 ✓

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/md
  "对于端点条件 q(0) = 1, q(π/2) = 0，解析解为：**q(t) = cos(t)**")

;; ---

;; ## 六、数值求解：寻找使作用量最小的轨迹

;; 前面我们验证了已知路径是否使作用量最小。
;; 现在我们要解决逆问题：**给定端点条件，如何找到真实轨迹？**

;; ### 核心思想：参数化路径族

;; **策略**：
;; 1. 构造一个**参数化的路径族**，所有路径都满足端点条件
;; 2. 将作用量表示为参数的函数
;; 3. 用数值优化方法找到使作用量最小的参数
;; 4. 得到的路径就是对真实轨迹的近似

;; **参数化方法**：拉格朗日插值多项式
;; - 给定端点 (t₀, q₀) 和 (t₁, q₁)
;; - 给定 n 个中间点的坐标 [q₁', q₂', ..., qₙ']
;; - 构造一个多项式，穿过所有这些点
;; - 改变中间点的坐标，就得到不同的路径

;; ### 拉格朗日插值多项式

;; 给定 n+1 个点 (t₀, q₀), (t₁, q₁), ..., (tₙ, qₙ)，
;; 拉格朗日插值多项式为：
;;
;; L(t) = Σᵢ qᵢ · ℓᵢ(t)
;;
;; 其中基函数 ℓᵢ(t) = ∏_{j≠i} (t - tⱼ)/(tᵢ - tⱼ)

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/tex
  "L(t) = \\sum_{i=0}^{n} q_i \\cdot \\ell_i(t), \\quad \\ell_i(t) = \\prod_{j \\neq i} \\frac{t - t_j}{t_i - t_j}")

;; Emmy 在 `emmy.mechanics.lagrange` 命名空间中已经提供了这些函数！
;; 我们直接使用即可，不需要自己实现。

;; ### 使用 Emmy 的 make-path 函数

;; Emmy 提供的 `lag/make-path` 函数：
;; - 输入：(t₀, q₀), (t₁, q₁), 以及中间点的坐标列表
;; - 输出：一个路径函数 t ↦ q(t)
;; - 内部使用拉格朗日插值多项式实现

;; 测试 make-path：
;; 构造一条从 (0, 1) 到 (π/2, 0) 的路径，
;; 中间点选择 [0.8, 0.6]（只是随机选择）

(def test-harmonic-path
  (lag/make-path 0.0 1.0 (* 0.5 Math/PI) 0.0 [0.8 0.6]))

;; 验证端点：
(test-harmonic-path 0.0)
;; => 应该 ≈ 1.0

(test-harmonic-path (* 0.5 Math/PI))
;; => 应该 ≈ 0.0

;; 中间某点：
(test-harmonic-path 0.8)
;; => 某个值

;; ### 使用 Emmy 的 find-path 函数

;; Emmy 已经提供了完整的 `find-path` 实现！
;; 它内部使用了：
;; - `linear-interpolants`：生成初始猜测
;; - `parametric-path-action`：参数化作用量
;; - `multidimensional-minimize`：多维优化

;; **算法流程**：
;; 1. 选择中间点的初始猜测（端点之间的线性插值）
;; 2. 定义参数化作用量函数：qs ↦ S[make-path(..., qs)]
;; 3. 用多维优化算法寻找使作用量最小的 qs
;; 4. 返回对应的路径

;; Emmy 源码中的实现：
;; ```clojure
;; (defn find-path [Lagrangian t0 q0 t1 q1 n]
;;   (let [initial-qs (linear-interpolants q0 q1 n)
;;         minimizing-qs (multidimensional-minimize
;;                         (parametric-path-action Lagrangian t0 q0 t1 q1)
;;                         initial-qs)]
;;     (make-path t0 q0 t1 q1 minimizing-qs)))
;; ```

;; ### 用谐振子验证 find-path

;; 现在我们可以用谐振子来测试数值求解方法：
;; - 已知解析解：q(t) = cos(t)
;; - 端点条件：q(0) = 1, q(π/2) = 0
;; - 参数：m = 1, k = 1

;; 用 3 个中间点寻找路径：
(def harmonic-solution
  (lag/find-path (L-harmonic 1.0 1.0)
                 0.0 1.0
                 (* 0.5 Math/PI) 0.0
                 3))

;; 测试几个点，与解析解 cos(t) 比较：
(def comparison-points
  (for [t [0.0 0.5 1.0 (* 0.5 Math/PI)]]
    {:t t
     :numerical (harmonic-solution t)
     :analytical (e/cos t)
     :error (e/abs (- (harmonic-solution t) (e/cos t)))}))

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/table comparison-points)

;; 计算最大误差：
(def max-error
  (apply max (map :error comparison-points)))

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/md
  (str "**数值解与解析解对比**\n\n"
       "最大误差：" max-error "\n\n"
       "使用 3 个中间点的多项式近似，\n"
       "我们得到了一个与解析解 cos(t) 非常接近的数值解。"))

;; ---

;; ## 总结

;; 通过这一节的学习，我们完成了：

;; ### 1. 基础概念与实现
;; - **拉格朗日量**：能量的表达式 L = T - V
;; - **作用量**：拉格朗日量的时间积分
;; - **最小作用量原理**：真实路径使作用量取极值

;; ### 2. 核心函数实现
;; - `L-free-particle`：自由粒子拉格朗日量（只有动能）
;; - `L-harmonic`：谐振子拉格朗日量（动能 - 势能）
;; - `Lagrangian-action`：计算作用量的通用函数
;; - `make-eta`：构造满足端点条件的变分
;; - `varied-free-particle-action`：计算变分路径的作用量

;; ### 3. 路径变分验证
;; - 数值验证了自由粒子的直线路径使作用量最小
;; - 使用 `minimize` 函数确认 ε ≈ 0 时作用量最小
;; - 理解了变分原理的计算实现

;; ### 4. 数值求解方法
;; - **拉格朗日插值**：`make-path` 构造参数化路径族
;; - **线性插值**：`linear-interpolants` 生成初始猜测
;; - **参数化作用量**：`parametric-path-action` 将作用量表示为参数的函数
;; - **find-path**：通过多维优化寻找使作用量最小的轨迹

;; ### 5. 谐振子验证
;; - 实现了完整的数值求解流程
;; - 用谐振子验证了方法的正确性
;; - 数值解与解析解 cos(t) 高度吻合

;; ### 6. Emmy 工具掌握
;; - `literal-function` - 符号函数
;; - `D` - 自动微分
;; - `Gamma` - 构造局域元组
;; - `definite-integral` - 数值积分
;; - `minimize` - 一维优化
;; - `multidimensional-minimize` - 多维优化
;; - `lagrange-interpolation` - 拉格朗日插值

;; ### 核心收获

;; **计算思想**：
;; - 用高阶函数表达物理概念
;; - 函数组合构建复杂计算
;; - 代码即是数学表达式

;; **数值方法**：
;; - 参数化 + 优化 = 求解轨迹
;; - 变分原理的计算实现
;; - 近似解与精确解的平衡

;; **物理洞察**：
;; - 最小作用量原理是普遍规律
;; - 不同系统只需改变拉格朗日量
;; - 数值方法可以处理无解析解的系统

;; ---

;; ## 参考资料

;; - [SICM 原书 1.4 节 (英文)](https://mitp-content-server.mit.edu/books/content/sectbyfn/books_pres_0/9579/sicm_edition_2.zip/chapter001.html#SEC7)
;; - [本地中文翻译](https://github.com/bencode/LambdaCraft/blob/main/books/sicm/1.4-Computing_Actions.md) - 更详细的中文解释
;; - [Emmy 文档](https://emmy.mentat.org/)
