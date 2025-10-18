^{:nextjournal.clerk/visibility {:code :hide}}
(ns pages.sicm.sicm-1-3
  "SICM 第 1.3 节：平稳作用量原理 (The Principle of Stationary Action)

  学习重点：
  1. 理解作用量的构造原理：为什么是积分形式？
  2. **核心亮点**：F[q] = L ∘ Γ[q] 的优雅分解
  3. 掌握局域元组 (local tuple) 的概念
  4. 理解函数式编程如何表达物理理论"
  {:nextjournal.clerk/toc true}
  (:require [nextjournal.clerk :as clerk]
            [emmy.env :as e :refer [* + - /
                                     literal-function up down velocity coordinate
                                     D Gamma compose simplify
                                     dot-product sqrt square ->TeX]]
            [emmy.expression.render :as render]))

;; # SICM 1.3 - 平稳作用量原理

;; ## 引言：从物理直觉到数学形式

;; 物理学的一个深刻洞察：
;; > 自然界中的真实运动，可以通过某个量的"极值"来刻画。

;; 这个量就是**作用量** (Action)。

;; 但作用量应该长什么样？SICM 用函数式思维给出了优雅的答案。

;; ---

;; ## 一、从经验出发：运动的三个基本性质

;; ### 1. 光滑性 (Smoothness)

;; 物理运动是**连续且光滑**的：
;; - 杂耍别针不会瞬间移动
;; - 速度不会突然跳变
;; - 轨迹可以用光滑函数描述

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/md
  "**数学表达**：路径 q(t) 是可微的，甚至是无穷次可微的")

;; ### 2. 无记忆性 (Memorylessness)

;; 物理运动**不依赖于整个历史**：
;; - 我们中途进入房间，看到杂耍别针正在下落
;; - 我们无法判断它是何时被抛出的
;; - 但我们能预测它的未来轨迹

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/md
  "**数学表达**：运动方程是**局域的** (local)，只依赖于当前状态")

;; ### 3. 决定性 (Determinism)

;; 少数几个参数就能决定未来：
;; - 位置 q(t)
;; - 速度 Dq(t)
;; - （可能还有更高阶导数）

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/md
  "**数学表达**：给定初始条件 (q₀, v₀)，系统的演化是唯一确定的")

;; ---

;; ## 二、作用量的构造原理

;; ### 可实现路径的性质

;; **关键观察 1**：如果整条路径是可实现的，那么它的任何一段也是可实现的。

;; **关键观察 2**：路径段的可实现性是**局域性质** (local property)。
;; - 只依赖于该段内的点
;; - 路径上每一时刻的贡献是平等的

;; ### 积分形式的必然性

;; 要把路径段上每一时刻的贡献组合起来，有什么方法？

;; **要求**：
;; 1. 每个时刻平等对待
;; 2. 不相交子段的贡献相互独立
;; 3. 整段 = 各部分之和

;; **答案**：积分！

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/tex
  "S[q](t_1, t_2) = \\int_{t_1}^{t_2} F[q](t) \\, dt")

;; 其中 F[q](t) 测量路径在时刻 t 的某个局域性质。

;; ---

;; ## 三、核心亮点：F[q] = L ∘ Γ[q] 的优雅分解

;; ### 问题的提出

;; F[q](t) 需要做两件事：
;; 1. 从路径 q 中提取时刻 t 的局域信息
;; 2. 测量这个局域信息的某个性质（系统相关）

;; 传统做法：把这两件事混在一起写。

;; SICM 的做法：**分解成两个函数的复合**！

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/tex
  "F[q] = L \\circ \\Gamma[q]")

;; ### Γ（Gamma）：通用的局域信息提取器

;; **定义**：Γ 将路径转换为"局域元组函数"

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/tex
  "\\Gamma[q](t) = (t, q(t), Dq(t), D^2q(t), \\dots)")

;; **特点**：
;; - **与系统无关**：任何系统都用同一个 Γ
;; - **纯数学操作**：只是提取路径的导数
;; - **通用接口**：提供标准化的数据格式

;; 在 Emmy 中，Gamma 已经内置：

;; 示例：一维路径
(def q-1d (literal-function 'x))

((Gamma q-1d) 't)
;; => (up t (x t) ((D x) t))

;; 示例：二维路径
(def q-2d (up (literal-function 'x)
              (literal-function 'y)))

((Gamma q-2d) 't)
;; => (up t (up (x t) (y t)) (up ((D x) t) ((D y) t)))

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/md
  "**局域元组** (local tuple) 的结构：

  - 第 0 个元素：时间 t
  - 第 1 个元素：坐标 q(t)
  - 第 2 个元素：速度 Dq(t)
  - 第 3 个元素（可选）：加速度 D²q(t)
  - ...")

;; ### L（Lagrangian）：系统特定的性质测量器

;; **定义**：L 接受局域元组，返回一个实数

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/tex
  "L : \\text{LocalTuple} \\to \\mathbb{R}")

;; **特点**：
;; - **与系统相关**：不同系统有不同的 L
;; - **物理的本质**：包含系统的所有物理信息
;; - **标准输入**：总是接受局域元组

;; 示例：自由粒子的拉格朗日量

(defn L-free-particle
  "自由粒子：L = (1/2) m v²"
  [mass]
  (fn [local]
    (let [v (velocity local)]  ; 从局域元组提取速度
      (* 1/2 mass (dot-product v v)))))

;; 测试：给定一个局域元组
(def sample-local-1d (up 't 'x 'v))

((L-free-particle 'm) sample-local-1d)
;; => (1/2) m v²

(simplify ((L-free-particle 'm) sample-local-1d))

;; 示例：谐振子的拉格朗日量

(defn L-harmonic
  "谐振子：L = (1/2) m v² - (1/2) k q²"
  [m k]
  (fn [local]
    (let [q (coordinate local)  ; 从局域元组提取坐标
          v (velocity local)]   ; 从局域元组提取速度
      (- (* 1/2 m (square v))
         (* 1/2 k (square q))))))

((L-harmonic 'm 'k) sample-local-1d)

(simplify ((L-harmonic 'm 'k) sample-local-1d))

;; ---

;; ## 四、为什么这个分解如此精妙？

;; ### 1. 关注点分离 (Separation of Concerns)

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/md
  "```
  F[q] = L ∘ Γ[q]
         ↑   ↑
         │   └─ Γ：提取局域信息（数学）
         └───── L：测量物理性质（物理）
  ```

  - **Γ 回答**：从路径怎么得到局域描述？（数学问题）
  - **L 回答**：这个系统的局域性质是什么？（物理问题）")

;; ### 2. 代码的可重用性

;; **传统方法**：每个系统都要重写全部逻辑

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/code
  "(defn action-free-particle [q t1 t2]
  (integral (fn [t]
              (let [v ((D q) t)]  ; 提取速度
                (* 1/2 m (square v))))
            t1 t2))

(defn action-harmonic [q t1 t2]
  (integral (fn [t]
              (let [x (q t)
                    v ((D q) t)]  ; 又要提取一次
                (- (* 1/2 m (square v))
                   (* 1/2 k (square x)))))
            t1 t2))")

;; **SICM 方法**：Γ 只写一次，作用量定义通用

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/code
  ";; Γ 对所有系统通用（Emmy 已提供）
(defn Gamma [q]
  (fn [t]
    (up t (q t) ((D q) t))))

;; 每个系统只需定义 L
(defn L-free-particle [m]
  (fn [local]
    (* 1/2 m (square (velocity local)))))

(defn L-harmonic [m k]
  (fn [local]
    (- (* 1/2 m (square (velocity local)))
       (* 1/2 k (square (coordinate local))))))

;; 作用量的定义对所有系统都一样！
(defn Lagrangian-action [L q t1 t2]
  (integral (compose L (Gamma q)) t1 t2))")

;; ### 3. 类型系统的清晰性

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/md
  "**类型签名**：

  ```
  Γ : Path → (Time → LocalTuple)
  L : LocalTuple → Real
  F : Path → (Time → Real)

  F = L ∘ Γ
  ```

  这是函数式编程的管道思维：

  ```
  Path → [Γ] → (Time → LocalTuple) → [L] → (Time → Real)
         提取                          评估
  ```")

;; ### 4. 实际演示：组合的威力

;; 定义一条具体路径：匀速直线运动
(defn straight-path
  "一维匀速直线运动：x(t) = 3t + 7"
  [t]
  (+ (* 3 t) 7))

;; 提取局域信息
((Gamma straight-path) 5.0)
;; => (up 5.0 22.0 3.0)
;; 时间 t=5, 位置 x=22, 速度 v=3

;; 计算自由粒子的拉格朗日量
((compose (L-free-particle 2.0) (Gamma straight-path)) 5.0)
;; => 9.0
;; L = (1/2) * 2 * 3² = 9

;; 
;; 计算谐振子的拉格朗日量
((compose (L-harmonic 2.0 1.5) (Gamma straight-path)) 5.0)
;; => 9.0 - (1/2) * 1.5 * 22² = -354.0

;; **关键**：无论什么系统，组合模式都是 `(compose L (Gamma q))`！
