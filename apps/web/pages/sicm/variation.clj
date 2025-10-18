^{:nextjournal.clerk/visibility {:code :hide}}
(ns pages.sicm.variation
  "变分 (Variation) - SICM核心概念学习

  学习目标：
  1. 理解什么是变分及其数学意义
  2. 掌握路径变分的构造方法
  3. 理解变分与微分的关系
  4. 用 Emmy 实现变分计算"
  {:nextjournal.clerk/toc true}
  (:require [nextjournal.clerk :as clerk]
            [emmy.env :as e :refer [* + - /
                                     literal-function up velocity coordinate
                                     D Gamma compose simplify definite-integral
                                     dot-product sqrt square ->TeX]]
            [emmy.mechanics.lagrange :as lag]))

;; # 变分 (Variation) - 路径空间中的微分

;; ## 一、什么是变分?

;; ### 1.1 从普通函数的微分说起

;; 在微积分中,我们熟悉**函数的微分**:
;; - 对于函数 f: ℝ → ℝ
;; - 微分 df 描述函数值随自变量的变化
;; - df = f'(x) dx

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/tex
  "f: \\mathbb{R} \\to \\mathbb{R}, \\quad df = f'(x) \\, dx")

;; **例子**: f(x) = x²
;; - f'(x) = 2x
;; - df = 2x dx

;; ### 1.2 泛函与变分

;; **泛函 (Functional)**: 将函数映射到实数的"函数的函数"
;; - 输入: 一个函数 (路径)
;; - 输出: 一个实数

;; **变分 (Variation)**: 泛函关于路径的"微分"
;; - 描述泛函值随路径的变化
;; - 类似于普通函数的微分,但作用在函数空间上

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/tex
  "S: \\text{(路径空间)} \\to \\mathbb{R}, \\quad \\delta S[q] = \\text{作用量的变分}")

;; ### 1.3 物理中的例子: 作用量

;; **作用量 S[q]** 就是一个泛函:
;; - 输入: 路径函数 q(t)
;; - 输出: 实数 S = ∫L(t, q, v) dt

;; **变分 δS**: 当路径发生微小改变时,作用量的改变量

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/tex
  "S[q] = \\int_{t_1}^{t_2} L(t, q(t), \\dot{q}(t)) \\, dt")

;; ---

;; ## 二、路径变分的数学构造

;; ### 2.1 变分路径的定义

;; 给定一条**参考路径** q(t),我们构造一族**变分路径**:
;; - q'(t) = q(t) + ε·η(t)
;; 其中:
;; - ε: 小参数 (类似于 dx)
;; - η(t): 变分函数 (描述路径的"方向")

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/tex
  "q'(t) = q(t) + \\varepsilon \\eta(t)")

;; **几何直觉**:
;; - q(t) 是路径空间中的一个点
;; - η(t) 是路径空间中的一个"切向量"
;; - ε·η(t) 是沿着η方向移动ε的距离

;; ### 2.2 端点条件

;; 在力学问题中,路径的端点通常是固定的:
;; - q(t₁) = q₁ (起点固定)
;; - q(t₂) = q₂ (终点固定)

;; 因此变分函数必须满足**端点为零**的条件:
;; - η(t₁) = 0
;; - η(t₂) = 0

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/tex
  "\\eta(t_1) = 0, \\quad \\eta(t_2) = 0")

;; 这样可以保证变分路径与原路径有相同的端点:
;; - q'(t₁) = q(t₁) + ε·η(t₁) = q(t₁) + 0 = q₁ ✓
;; - q'(t₂) = q(t₂) + ε·η(t₂) = q(t₂) + 0 = q₂ ✓

;; ### 2.3 构造满足端点条件的变分函数

;; **核心技巧**: 用 (t - t₁)(t - t₂) 乘以任意函数

;; 给定任意函数 ν(t),定义:
;; - η(t) = (t - t₁)(t - t₂)·ν(t)

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/tex
  "\\eta(t) = (t - t_1)(t - t_2) \\nu(t)")

;; **验证端点条件**:
;; - η(t₁) = (t₁ - t₁)(t₁ - t₂)·ν(t₁) = 0 ✓
;; - η(t₂) = (t₂ - t₁)(t₂ - t₂)·ν(t₂) = 0 ✓

;; **优点**:
;; - ν(t) 可以是任意函数 (sin, cos, 多项式等)
;; - 自动满足端点条件
;; - 灵活构造不同"方向"的变分

;; ### 2.4 Emmy 实现: make-eta 函数

(defn make-eta
  "构造满足端点条件的变分函数

  参数:
  - nu: 任意测试函数 ν(t)
  - t1: 起始时间
  - t2: 结束时间

  返回:
  - 变分函数 η(t) = (t - t₁)(t - t₂)·ν(t)
  - 满足 η(t₁) = η(t₂) = 0"
  [nu t1 t2]
  (fn [t]
    (* (- t t1) (- t t2) (nu t))))

;; ### 2.5 测试 make-eta

;; **例1**: 使用 ν(t) = sin(t)

(def eta-sin
  "用 sin(t) 构造的变分函数,区间 [0, π]"
  (make-eta e/sin 0.0 Math/PI))

;; 验证端点:
(def test-endpoints-sin
  {:t0 0.0
   :eta-t0 (eta-sin 0.0)                ; 应该 = 0
   :t1 Math/PI
   :eta-t1 (eta-sin Math/PI)            ; 应该 = 0
   :t-middle (/ Math/PI 2)
   :eta-middle (eta-sin (/ Math/PI 2))})  ; 应该非零

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/table [test-endpoints-sin])

;; **例2**: 使用 ν(t) = 1 (常数函数)

(def eta-const
  "用常数1构造的变分函数,区间 [0, 2]"
  (make-eta (constantly 1.0) 0.0 2.0))

;; η(t) = (t - 0)(t - 2)·1 = t(t - 2) = t² - 2t
;; 这是一个开口向上的抛物线,在 t=0 和 t=2 处为零

(def test-const-variation
  (for [t [0.0 0.5 1.0 1.5 2.0]]
    {:t t
     :eta (eta-const t)
     :analytical (- (* t t) (* 2 t))}))

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/table test-const-variation)

;; **例3**: 向量值变分 (多维路径)

;; 对于三维空间中的路径 q(t) = (x(t), y(t), z(t))
;; 变分也是向量: η(t) = (ηₓ(t), ηᵧ(t), ηᵤ(t))

(def eta-3d
  "三维变分函数,每个分量用不同的测试函数"
  (make-eta (up e/sin e/cos square) 0.0 Math/PI))

;; 在 t = π/2 处的值
(def eta-3d-value
  (eta-3d (/ Math/PI 2)))

eta-3d-value
;; => (up ηₓ(π/2) ηᵧ(π/2) ηᵤ(π/2))

;; ---

;; ## 三、变分的应用: 测试路径的极值性

;; ### 3.1 变分作用量

;; **问题**: 给定一条路径 q(t),如何判断它是否使作用量最小?

;; **方法**: 计算**变分作用量** S[q + ε·η] 作为 ε 的函数
;; - 如果 ε = 0 时 S 取极小值,则 q 是真实路径
;; - 对于所有满足端点条件的 η,这个性质都应成立

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/tex
  "S[q + \\varepsilon \\eta] \\text{ 在 } \\varepsilon = 0 \\text{ 处取极小值}")

;; ### 3.2 实现: varied-action 函数

(defn varied-action
  "计算变分路径的作用量 (作为 ε 的函数)

  参数:
  - L: 拉格朗日量函数
  - q: 参考路径
  - nu: 变分测试函数
  - t1: 起始时间
  - t2: 结束时间

  返回:
  - 函数 ε ↦ S[q + ε·η],其中 η = make-eta(nu, t1, t2)"
  [L q nu t1 t2]
  (let [eta (make-eta nu t1 t2)]
    (fn [eps]
      (lag/Lagrangian-action L (+ q (* eps eta)) t1 t2))))

;; ### 3.3 例子: 自由粒子的直线路径

;; **已知**: 自由粒子的真实路径是匀速直线
;; **验证**: 直线路径在任意变分下作用量最小

;; 定义拉格朗日量
(defn L-free
  "自由粒子拉格朗日量 L = 1/2 m v²"
  [m]
  (fn [local]
    (* 1/2 m (dot-product (velocity local) (velocity local)))))

;; 定义测试路径: 一维匀速直线 q(t) = 2t + 1
(defn straight-path [t]
  (* 2 t))

;; 计算变分作用量 (使用 ν(t) = sin(t))
(def varied-S
  (varied-action (L-free 1.0) straight-path e/sin 0.0 Math/PI))

;; 在不同 ε 值下计算作用量
(def variation-test-data
  (for [eps (range -0.1 0.11 0.02)]
    {:epsilon eps
     :action (varied-S eps)}))

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/table variation-test-data)

;; **观察**:
;; - ε = 0 时作用量最小
;; - ε 偏离0时,作用量增加
;; - 这验证了直线路径是真实路径

;; ### 3.4 用 minimize 函数寻找最优 ε

;; Emmy 提供 minimize 函数,可以自动找到使作用量最小的 ε

(def optimization-result
  (e/minimize varied-S -1.0 1.0))

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/md
  (str "**优化结果**:\n\n"
       "- 最优 ε = " (:result optimization-result) "\n"
       "- 最小作用量 = " (:value optimization-result) "\n"
       "- 迭代次数 = " (:iterations optimization-result)))

;; **结论**: ε ≈ 0,确认直线路径使作用量最小

;; ---

;; ## 四、变分与欧拉-拉格朗日方程

;; ### 4.1 变分的极值条件

;; **平稳作用量原理**: 真实路径使作用量的**一阶变分为零**

;; 数学表达:
;; - δS = (dS/dε)|_{ε=0} = 0

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/tex
  "\\delta S = \\left. \\frac{dS[q + \\varepsilon \\eta]}{d\\varepsilon} \\right|_{\\varepsilon=0} = 0")

;; 这个条件对**所有**满足端点条件的 η 都成立

;; ### 4.2 从变分推导欧拉-拉格朗日方程

;; 通过变分原理 δS = 0,可以推导出欧拉-拉格朗日方程:

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/tex
  "\\frac{d}{dt}\\left(\\frac{\\partial L}{\\partial \\dot{q}}\\right) - \\frac{\\partial L}{\\partial q} = 0")

;; **推导思路** (详细推导见 SICM 1.5节):
;; 1. 写出 S[q + ε·η] 的表达式
;; 2. 对 ε 求导,令 ε = 0
;; 3. 使用分部积分,移除 η' 项
;; 4. 因为对所有 η 都成立,得到微分方程

;; ### 4.3 数值验证: 检查路径是否满足变分条件

;; 对于真实路径,dS/dε 在 ε=0 处应该为零

(defn check-variation-zero
  "检查路径是否满足变分条件 (数值近似)

  返回 dS/dε 在 ε≈0 处的值,应该接近0"
  [L q nu t1 t2]
  (let [varied-S (varied-action L q nu t1 t2)
        epsilon-small 1e-6]
    (/ (- (varied-S epsilon-small) (varied-S 0.0))
       epsilon-small)))

;; 测试直线路径
(def variation-derivative
  (check-variation-zero (L-free 1.0) straight-path e/sin 0.0 Math/PI))

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/md
  (str "**变分检验**:\n\n"
       "dS/dε ≈ " variation-derivative "\n\n"
       (if (< (e/abs variation-derivative) 1e-4)
         "✓ 接近零,路径满足变分条件"
         "✗ 不接近零,路径不满足变分条件")))

;; ---

;; ## 五、变分的几何意义

;; ### 5.1 路径空间的"切空间"

;; **类比有限维空间**:
;; - 在 ℝⁿ 中,曲线 x(ε) 在 ε=0 处的切向量是 dx/dε
;; - 在路径空间中,"曲线" q(t, ε) 在 ε=0 处的"切向量"是 η(t)

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/tex
  "\\text{有限维}: \\quad \\vec{v} = \\frac{dx}{d\\varepsilon}\\Big|_{\\varepsilon=0}")

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/tex
  "\\text{无限维}: \\quad \\eta(t) = \\frac{\\partial q(t, \\varepsilon)}{\\partial \\varepsilon}\\Big|_{\\varepsilon=0}")

;; ### 5.2 变分 δq 的记号

;; 在物理文献中,常用记号:
;; - δq = ε·η
;; - "δ" 表示路径的无穷小变化

;; 变分作用量可以写成:
;; - δS = S[q + δq] - S[q]

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/tex
  "\\delta q = \\varepsilon \\eta, \\quad \\delta S = S[q + \\delta q] - S[q]")

;; ### 5.3 Gateaux 导数

;; 变分可以严格定义为**Gateaux 导数**:

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/tex
  "\\delta S[q; \\eta] = \\lim_{\\varepsilon \\to 0} \\frac{S[q + \\varepsilon \\eta] - S[q]}{\\varepsilon}")

;; 这是泛函微积分的基础概念

;; ---

;; ## 六、实战练习

;; ### 练习1: 构造不同的变分函数

;; 尝试用不同的 ν(t) 构造变分函数,并验证端点条件

;; 练习: ν(t) = t²
(def eta-square
  (make-eta square 0.0 1.0))

;; 验证
(def practice-1
  {:eta-0 (eta-square 0.0)
   :eta-0.5 (eta-square 0.5)
   :eta-1 (eta-square 1.0)})

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/code practice-1)

;; ### 练习2: 谐振子的变分

;; 对谐振子 L = 1/2 m v² - 1/2 k q²
;; 已知解 q(t) = cos(t)
;; 验证 cos(t) 满足变分条件

(defn L-harmonic [m k]
  (fn [local]
    (- (* 1/2 m (square (velocity local)))
       (* 1/2 k (square (coordinate local))))))

;; 定义余弦路径
(def cosine-path e/cos)

;; 计算变分作用量
(def varied-S-harmonic
  (varied-action (L-harmonic 1.0 1.0) cosine-path e/sin 0.0 (/ Math/PI 2)))

;; 检查在 ε = 0 附近是否为极小值
(def practice-2-data
  (for [eps (range -0.05 0.06 0.01)]
    {:epsilon eps
     :action (varied-S-harmonic eps)}))

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/table practice-2-data)

;; ### 练习3: 计算变分导数

;; 实现一个函数,数值计算 dS/dε

(defn variation-derivative
  "数值计算变分导数 dS/dε 在 ε=0 处的值"
  [L q nu t1 t2]
  (let [varied-S (varied-action L q nu t1 t2)
        h 1e-7]
    (/ (- (varied-S h) (varied-S (- h)))
       (* 2 h))))

;; 对余弦路径计算
(def practice-3-result
  (variation-derivative (L-harmonic 1.0 1.0) cosine-path e/sin 0.0 (/ Math/PI 2)))

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/md
  (str "**练习3结果**:\n\n"
       "dS/dε = " practice-3-result "\n\n"
       "理论值应该为 0 (因为 cos(t) 是真实路径)"))

;; ---

;; ## 七、总结

;; ### 核心概念

;; 1. **变分**: 路径空间中的"微分",描述泛函随路径的变化
;; 2. **变分函数 η(t)**: 满足端点条件的路径扰动
;; 3. **变分路径**: q'(t) = q(t) + ε·η(t)
;; 4. **平稳作用量**: dS/dε = 0 for all η

;; ### 构造技巧

;; **端点条件**: η(t) = (t - t₁)(t - t₂)·ν(t)
;; - 自动满足 η(t₁) = η(t₂) = 0
;; - ν(t) 可以任意选择

;; ### 计算流程

;; 1. 定义参考路径 q(t)
;; 2. 构造变分函数 η(t)
;; 3. 计算 S[q + ε·η] 作为 ε 的函数
;; 4. 检查 ε = 0 是否为极值点

;; ### 物理意义

;; - **变分原理**: 自然界选择使作用量平稳的路径
;; - **欧拉-拉格朗日方程**: 变分条件的微分形式
;; - **数值求解**: 通过优化变分作用量找到真实轨迹

;; ### Emmy 工具

;; - `make-eta`: 构造变分函数
;; - `Lagrangian-action`: 计算作用量
;; - `minimize`: 寻找极值
;; - 函数算术: `(+ q (* eps eta))`

;; ### 与微积分的类比

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/table
  [{:概念 "自变量" :微积分 "x ∈ ℝ" :变分法 "q(t) (路径)"}
   {:概念 "函数" :微积分 "f: ℝ → ℝ" :变分法 "S: (路径) → ℝ (泛函)"}
   {:概念 "微分" :微积分 "dx" :变分法 "δq = ε·η"}
   {:概念 "导数" :微积分 "df/dx" :变分法 "δS/δq"}
   {:概念 "极值条件" :微积分 "f'(x) = 0" :变分法 "δS = 0"}])

;; ### 进一步学习

;; - SICM 1.5节: 欧拉-拉格朗日方程的推导
;; - SICM 1.6节: 如何求解微分方程
;; - 泛函分析: 变分法的严格数学理论

;; ---

;; ## 参考资料

;; - [SICM 1.4节 - Computing Actions](https://mitp-content-server.mit.edu/books/content/sectbyfn/books_pres_0/9579/sicm_edition_2.zip/chapter001.html#SEC7)
;; - [变分法 (Calculus of Variations)](https://en.wikipedia.org/wiki/Calculus_of_variations)
;; - [Emmy 文档](https://emmy.mentat.org/)
;; - 本项目 SICM 学习笔记: `pages/sicm/sicm-1-4.clj`
