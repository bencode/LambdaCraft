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
            [emmy.expression.render :as render]))

;; # SICM 1.4 - 计算作用量

;; ## 学习任务清单

;; ### 第一部分：基础概念
;; - [ ] 1. 理解自由粒子的拉格朗日量 L(t,x,v) = 1/2 m v²
;; - [ ] 2. 实现 L-free-particle 函数
;; - [ ] 3. 创建坐标路径函数 q(t) = (x(t), y(t), z(t))

;; ### 第二部分：作用量计算
;; - [ ] 4. 使用 Gamma (Γ) 构造局域元组 (t, q(t), Dq(t))
;; - [ ] 5. 实现 Lagrangian-action 函数计算作用量
;; - [ ] 6. 验证直线路径的作用量计算（test-path 示例）

;; ### 第三部分：最小作用量原理
;; - [ ] 7. 实现路径变分 make-eta 验证最小作用量原理

;; ### 第四部分：谐振子应用
;; - [ ] 8. 实现谐振子拉格朗日量 L-harmonic
;; - [ ] 9. 使用 find-path 寻找谐振子的最优轨迹

;; ---

;; ## 上下文信息

;; ### 核心概念

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

;; ### 自由粒子的拉格朗日量

;; 对于质量为 m 的自由粒子，其拉格朗日量是动能：
;; L(t, x, v) = 1/2 m (v · v)

;; 根据欧拉-拉格朗日的发现：
;; - 自由粒子沿直线匀速运动
;; - 直线路径的作用量小于任何其他连接相同端点的路径

;; ---

;; ## 实现开始

;; ### 第 1 步：理解自由粒子的拉格朗日量

;; 自由粒子的拉格朗日量公式：
;; L(t, x, v) = 1/2 m v²
;;
;; 在三维空间中，如果 v = (vₓ, vᵧ, vᵧ)，则
;; L = 1/2 m (vₓ² + vᵧ² + vᵧ²)

;; 用 TeX 渲染：
(clerk/tex
  "L(t, x, v) = \\frac{1}{2} m (v \\cdot v) = \\frac{1}{2} m (v_x^2 + v_y^2 + v_z^2)")

;; ### 第 2 步：实现 L-free-particle 函数

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

;; 测试：创建一个质量为 m 的自由粒子的拉格朗日量
(def L-free (L-free-particle 'm))

;; 符号测试：创建一个质量为 m 的拉格朗日量函数
;; 稍后我们会用真实的局域元组来测试

;; ---

;; ### 第 3 步：创建坐标路径函数

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

;; ---

;; ### 第 4 步：使用 Gamma (Γ) 构造局域元组

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
(clerk/tex (->TeX (simplify (L-on-path 't))))

;; ---

;; ## 第一阶段总结

;; 我们已经完成了基础概念部分：

;; ✅ **第 1 步**：理解了自由粒子的拉格朗日量 L = 1/2 m v²
;;    - 动能作为拉格朗日量
;;    - 最小作用量原理的物理意义

;; ✅ **第 2 步**：实现了 `L-free-particle` 函数
;;    - 高阶函数：接受质量，返回拉格朗日量函数
;;    - 从局域元组提取速度并计算动能

;; ✅ **第 3 步**：创建了符号坐标路径 `q`
;;    - 使用 `literal-function` 创建符号函数
;;    - 用 `up` 构造三维坐标元组
;;    - 可以对路径求导得到速度

;; ✅ **第 4 步**：理解了 Gamma (Γ) 函数
;;    - Γ 将路径转换为局域元组
;;    - 组合 L ∘ Γ 得到时间的函数
;;    - 这就是我们要积分的被积函数

;; ### 关键洞察

;; 拉格朗日力学的计算框架：
;; 1. 定义拉格朗日量 L(t, q, v)
;; 2. 定义路径 q(t)
;; 3. 使用 Γ[q] 构造局域元组
;; 4. 计算 L(Γ[q](t))
;; 5. 对时间积分得到作用量

;; ---

;; ## 学习进度

;; **第一部分：基础概念** ✓
;; - [x] 1. 理解自由粒子的拉格朗日量
;; - [x] 2. 实现 L-free-particle 函数
;; - [x] 3. 创建坐标路径函数 q(t)
;; - [x] 4. 使用 Gamma 构造局域元组

;; **第二部分：作用量计算** (下一步)
;; - [ ] 5. 实现 Lagrangian-action 函数
;; - [ ] 6. 验证直线路径的作用量计算

;; ---

;; ## 参考资料

;; - SICM 书籍：`books/sicm/1.4-Computing_Actions.md`
;; - Emmy 文档示例：`docs/emmy.clj`
;; - Emmy 官方文档：https://emmy.mentat.org/

;; ---

;; ## 下一步

;; 继续实现第 5 步：实现 Lagrangian-action 函数计算作用量
;; 这是核心功能：计算路径的作用量 S[q] = ∫ L(Γ[q](t)) dt

;; ---

;; ## 第二部分：作用量计算

;; ### 第 5 步：实现 Lagrangian-action 函数

;; **作用量 (Action)** 是拉格朗日量沿路径的时间积分：
;; S[q](t₁, t₂) = ∫(t₁ to t₂) L(Γ[q](t)) dt

;; 原 Scheme 代码：
;; ```scheme
;; (define (Lagrangian-action L q t1 t2)
;;   (definite-integral (compose L (Gamma q)) t1 t2))
;; ```

;; Emmy/Clojure 实现：
(defn Lagrangian-action
  "计算拉格朗日作用量

  参数：
  - L: 拉格朗日量函数 (接受局域元组)
  - q: 路径函数 (时间 -> 坐标)
  - t1: 起始时间
  - t2: 结束时间

  返回：
  - 作用量 S[q] = ∫(t₁ to t₂) L(Γ[q](t)) dt"
  [L q t1 t2]
  (definite-integral (compose L (Gamma q)) t1 t2))

;; 这个函数很优雅：
;; 1. (Gamma q) 将路径转换为局域元组函数
;; 2. (compose L (Gamma q)) 得到时间的函数
;; 3. definite-integral 对这个函数积分

;; 注意：这个实现是**坐标无关**的！
;; 它不依赖于任何特定的坐标系或维度

;; ---

;; ### 第 6 步：验证直线路径的作用量计算

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

;; 让我们用 TeX 展示这个计算：
(clerk/tex
  "S = \\int_0^{10} \\frac{1}{2} \\times 3 \\times 29 \\, dt = \\frac{87}{2} \\times 10 = 435")

;; ---

;; ## 第二阶段总结

;; 我们已经实现了作用量计算的核心功能：

;; ✅ **第 5 步**：实现了 `Lagrangian-action` 函数
;;    - 使用 compose 组合 L 和 Gamma
;;    - 使用 definite-integral 计算定积分
;;    - 实现是坐标无关的

;; ✅ **第 6 步**：验证了直线路径的作用量
;;    - 创建了测试路径 test-path
;;    - 计算得到 S = 435.0
;;    - 与解析解一致！

;; ### 物理意义

;; 这个例子验证了：
;; 1. 自由粒子的拉格朗日量是动能
;; 2. 匀速直线运动的作用量可以准确计算
;; 3. 数值积分与解析解一致

;; ---

;; ## 学习进度更新

;; **第一部分：基础概念** ✓
;; - [x] 1. 理解自由粒子的拉格朗日量
;; - [x] 2. 实现 L-free-particle 函数
;; - [x] 3. 创建坐标路径函数 q(t)
;; - [x] 4. 使用 Gamma 构造局域元组

;; **第二部分：作用量计算** ✓
;; - [x] 5. 实现 Lagrangian-action 函数
;; - [x] 6. 验证直线路径的作用量计算

;; **第三部分：最小作用量原理** (下一步)
;; - [ ] 7. 实现路径变分 make-eta 验证最小作用量原理

;; **第四部分：谐振子应用**
;; - [ ] 8. 实现谐振子拉格朗日量 L-harmonic
;; - [ ] 9. 使用 find-path 寻找谐振子的最优轨迹
