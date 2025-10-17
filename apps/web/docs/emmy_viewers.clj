^{:nextjournal.clerk/visibility {:code :hide}}
(ns docs.emmy-viewers
  {:nextjournal.clerk/toc true}
  (:refer-clojure :exclude [+ - * / zero? compare divide numerator
                            denominator infinite? abs ref partial =])
  (:require [emmy.clerk :as ec]
            [emmy.env :as e :refer :all]
            [emmy.mafs :as mafs]
            [emmy.mathbox.plot :as plot]
            [emmy.viewer :as ev]
            [emmy.leva :as leva]
            [nextjournal.clerk :as clerk]))

;; 注意：Emmy-Viewers 的交互式组件在静态构建或某些环境中可能无法正常工作
;; 建议在开发服务器环境中查看此页面

^{::clerk/visibility {:code :hide :result :hide}}
(ec/install!)

;; # Emmy-Viewers 可视化指南

;; Emmy-Viewers 是 Emmy 计算代数系统的可视化层，提供 2D/3D 交互式可视化组件。
;; 它基于 Reagent + D3/Three.js，可以直接展示 Emmy 计算出的符号/数值结果。

;; ## 资源链接

;; - [Emmy-Viewers 官方网站](https://emmy-viewers.mentat.org/)
;; - [Emmy-Viewers GitHub](https://github.com/mentat-collective/emmy-viewers)
;; - [Emmy-Viewers 文档](https://cljdoc.org/d/org.mentat/emmy-viewers/CURRENT)

;; ## 核心功能

;; Emmy-Viewers 提供：
;; - **2D 可视化 (Mafs)** - 函数图形、参数曲线、向量场
;; - **3D 可视化 (MathBox)** - 三维曲面、参数曲线、向量场
;; - **交互控件 (Leva)** - 实时参数调节
;; - **物理模拟** - 动力学系统、相图、哈密顿流

;; ## 1. 二维可视化 (Mafs)

;; **注意**：以下示例需要在开发服务器环境（`clojure -M:dev`）中查看。
;; 如果遇到 "Could not resolve symbol" 错误，请确保：
;; 1. 使用开发服务器而非静态构建版本
;; 2. `(ec/install!)` 已正确执行
;; 3. 页面完全加载后再查看可视化内容

;; ### 1.1 基础函数绘图

;; 最简单的用法：直接把 Emmy 函数传给 Mafs

^{::clerk/visibility {:code :show :result :show}}
(mafs/of-x sin {:color :blue})

;; ### 1.2 多函数叠加

^{::clerk/visibility {:code :show :result :show}}
(mafs/mafs
  {:height 400}
  (mafs/of-x sin {:color :blue})
  (mafs/of-x cos {:color :green})
  (mafs/of-x (fn [x] (/ (sin x) x)) {:color :red}))

;; ### 1.3 参数方程曲线

;; 绘制一个圆的参数方程

^{::clerk/visibility {:code :show :result :show}}
(mafs/mafs
  {:height 400}
  (mafs/parametric
    {:t [0 (* 2 Math/PI)]
     :xy (fn [t] [(* 2 (cos t)) (* 2 (sin t))])
     :color :purple}))

;; ### 1.4 复合函数

;; 使用 Emmy 的函数组合能力

^{::clerk/visibility {:code :show :result :show}}
(let [f (compose square sin)]
  (mafs/of-x f {:color :orange}))

;; ### 1.5 向量场可视化

^{::clerk/visibility {:code :show :result :show}}
(mafs/mafs
  {:height 400}
  (mafs/vector-field
    {:xy (fn [[x y]] [(- y) x])
     :step 0.5
     :color :teal}))

;; ## 2. 三维可视化 (MathBox)

;; ### 2.1 基础三维函数图

^{::clerk/visibility {:code :show :result :show}}
(plot/scene
  (plot/of-x {:z sin :color :blue}))

;; ### 2.2 三维参数曲线

;; 绘制螺旋线

^{::clerk/visibility {:code :show :result :show}}
(plot/scene
  (plot/parametric-curve
    {:f (up (comp (partial * 2) cos)
            (comp (partial * 2) sin)
            (fn [t] (/ t 3)))
     :t [-10 10]
     :color :green}))

;; ### 2.3 三维曲面

^{::clerk/visibility {:code :show :result :show}}
(plot/scene
  (plot/parametric-surface
    {:f (fn [[u v]]
          [(* u (cos v))
           (* u (sin v))
           u])
     :u [-2 2]
     :v [0 (* 2 Math/PI)]
     :color :purple}))

;; ### 2.4 多图层场景

^{::clerk/visibility {:code :show :result :show}}
(plot/scene
  (plot/of-x {:z sin :color :blue})
  (plot/parametric-curve
    {:f (up sin cos (fn [t] (/ t 3)))
     :t [-10 10]
     :color :red}))

;; ## 3. 交互参数控件

;; ### 3.1 基础滑块控制

^{::clerk/visibility {:code :show :result :show}}
(ev/with-let [!phase {:phase 0}]
  (mafs/mafs
    {:height 400}
    (leva/controls
      {:folder {:name "相位控制"}
       :schema {:phase {:min -4 :max 4 :step 0.01}}
       :atom !phase})
    (mafs/of-x
      (ev/with-params {:atom !phase :params [:phase]}
        (fn [phase]
          (fn [x] (sin (+ x phase)))))
      {:color :blue})))

;; ### 3.2 多参数控制

^{::clerk/visibility {:code :show :result :show}}
(ev/with-let [!params {:amplitude 1 :frequency 1 :phase 0}]
  (mafs/mafs
    {:height 400}
    (leva/controls
      {:folder {:name "波形参数"}
       :schema {:amplitude {:min 0.1 :max 5 :step 0.1}
                :frequency {:min 0.1 :max 5 :step 0.1}
                :phase {:min -4 :max 4 :step 0.01}}
       :atom !params})
    (mafs/of-x
      (ev/with-params {:atom !params :params [:amplitude :frequency :phase]}
        (fn [amplitude frequency phase]
          (fn [x] (* amplitude (sin (+ (* frequency x) phase))))))
      {:color :purple})))

;; ### 3.3 三维交互控制

^{::clerk/visibility {:code :show :result :show}}
(ev/with-let [!params {:phase 0}]
  (plot/scene
    (leva/controls
      {:folder {:name "3D 相位控制"}
       :schema {:phase {:min -4 :max 4 :step 0.01}}
       :atom !params})
    (plot/of-y
      {:z (ev/with-params {:atom !params :params [:phase]}
            (fn [phase]
              (fn [y] (* phase (sin (- y phase))))))
       :color :cyan})))

;; ## 4. 数学应用示例

;; ### 4.1 多项式函数

^{::clerk/visibility {:code :show :result :show}}
(mafs/mafs
  {:height 400}
  (mafs/of-x (fn [x] (+ x (* x x))) {:color :blue})
  (mafs/of-x (fn [x] (- (* x x x) x)) {:color :red})
  (mafs/of-x (fn [x] (* x x x x)) {:color :green}))

;; ### 4.2 导数可视化

^{::clerk/visibility {:code :show :result :show}}
(let [f (fn [x] (/ (sin x) x))]
  (mafs/mafs
    {:height 400}
    (mafs/of-x f {:color :blue})
    (mafs/of-x (D f) {:color :red})))

;; ### 4.3 积分区域可视化

^{::clerk/visibility {:code :show :result :show}}
(mafs/mafs
  {:height 400}
  (mafs/of-x square {:color :blue})
  (mafs/polygon
    {:points [[-1 0] [-1 1] [1 1] [1 0]]
     :color :lightblue
     :fill-opacity 0.3}))

;; ## 5. 物理模拟示例

;; ### 5.1 简谐振动

^{::clerk/visibility {:code :show :result :show}}
(ev/with-let [!params {:omega 1 :amplitude 2}]
  (mafs/mafs
    {:height 400}
    (leva/controls
      {:folder {:name "简谐振动参数"}
       :schema {:omega {:min 0.1 :max 5 :step 0.1 :label "角频率 ω"}
                :amplitude {:min 0.5 :max 5 :step 0.1 :label "振幅 A"}}
       :atom !params})
    (mafs/of-x
      (ev/with-params {:atom !params :params [:omega :amplitude]}
        (fn [omega amplitude]
          (fn [t] (* amplitude (cos (* omega t))))))
      {:color :blue})))

;; ### 5.2 阻尼振动

^{::clerk/visibility {:code :show :result :show}}
(ev/with-let [!params {:omega 2 :gamma 0.1}]
  (mafs/mafs
    {:height 400}
    (leva/controls
      {:folder {:name "阻尼振动参数"}
       :schema {:omega {:min 0.1 :max 5 :step 0.1 :label "角频率 ω"}
                :gamma {:min 0 :max 1 :step 0.01 :label "阻尼系数 γ"}}
       :atom !params})
    (mafs/of-x
      (ev/with-params {:atom !params :params [:omega :gamma]}
        (fn [omega gamma]
          (fn [t] (* (exp (* (- gamma) t))
                     (cos (* omega t))))))
      {:color :red})))

;; ### 5.3 李萨如图形

^{::clerk/visibility {:code :show :result :show}}
(ev/with-let [!params {:a 3 :b 2 :delta 0}]
  (mafs/mafs
    {:height 400}
    (leva/controls
      {:folder {:name "李萨如图形参数"}
       :schema {:a {:min 1 :max 5 :step 1 :label "频率比 a"}
                :b {:min 1 :max 5 :step 1 :label "频率比 b"}
                :delta {:min 0 :max 6.28 :step 0.1 :label "相位差 δ"}}
       :atom !params})
    (mafs/parametric
      {:t [0 (* 2 Math/PI)]
       :xy (ev/with-params {:atom !params :params [:a :b :delta]}
             (fn [a b delta]
               (fn [t] [(sin (+ (* a t) delta))
                        (sin (* b t))])))
       :color :purple})))

;; ## 6. 高级主题

;; ### 6.1 复合场景

^{::clerk/visibility {:code :show :result :show}}
(plot/scene
  (plot/of-x {:z (fn [x] (* (sin x) (cos x))) :color :blue})
  (plot/of-y {:z sin :color :green})
  (plot/parametric-curve
    {:f (up (fn [t] t)
            (fn [t] (sin t))
            (fn [t] (cos t)))
     :t [0 (* 2 Math/PI)]
     :color :red}))

;; ### 6.2 极坐标图形

^{::clerk/visibility {:code :show :result :show}}
(mafs/mafs
  {:height 400}
  (mafs/polar
    {:r (fn [theta] (+ 1 (cos theta)))
     :theta [0 (* 2 Math/PI)]
     :color :pink}))

;; ### 6.3 参数化曲面（鞍面）

^{::clerk/visibility {:code :show :result :show}}
(plot/scene
  (plot/parametric-surface
    {:f (fn [[u v]]
          [u v (- (* u u) (* v v))])
     :u [-2 2]
     :v [-2 2]
     :color :teal}))

;; ## 7. 最佳实践

;; ### 使用 Emmy 函数构造数学对象
;; Emmy-Viewers 期望接收 Emmy 的函数（可组合、可微、可符号化），
;; 而非普通的 Clojure 函数。因此需要使用 `emmy.env` 中的函数来构建可绘制对象。

;; ### 控制采样密度
;; 在渲染复杂曲面时，注意控制采样密度（`t`/`dt`、网格步长），
;; 避免构造过大的对象导致性能问题。

;; ### 保持函数纯粹
;; 优先让数学函数保持纯函数形式，这样可以更好地利用 Emmy 的符号计算能力。

;; ## 8. 常见问题

;; **Q：为什么普通函数画不出来？**
;; A：需要用 `emmy.env` 里的函数组合出"可微/可符号化"的表达式，
;; Viewer 才能正确采样/渲染。

;; **Q：3D 场景卡顿怎么办？**
;; A：检查是否在渲染环中构造了大对象；优先让数学函数保持纯函数形式，
;; 控制采样密度。

;; **Q：如何调试可视化效果？**
;; A：使用 `simplify` 和 `->infix` 查看符号表达式，确保数学逻辑正确。
