^{:nextjournal.clerk/visibility {:code :hide}}
(ns docs.cider-shortcuts
  "Cider 常用快捷键参考"
  {:nextjournal.clerk/toc true}
  (:require [nextjournal.clerk :as clerk]))

;; # Cider 快捷键参考

;; ## 🔥 日常必备（核心 6 个）

;; ### 1️⃣ 求值代码
;; - `C-c C-k` - **加载整个文件**（最常用！）
;; - `C-c C-e` - 求值光标前的表达式
;; - `C-c C-c` - 求值当前顶层表达式

;; ### 2️⃣ 查看帮助
;; - `C-c C-d d` - 查看文档 (doc)
;; - `C-c C-d a` - 搜索文档 (apropos)

;; ### 3️⃣ 导航
;; - `M-.` - 跳转到定义
;; - `M-,` - 跳回来

;; ### 4️⃣ REPL 切换
;; - `C-c C-z` - 在代码和 REPL 之间切换

;; ---

;; ## 📝 典型工作流程

;; ```
;; 编辑文件 → C-c C-k (加载) → C-c C-z (切到REPL) → 测试 → C-c C-z (切回代码)
;; ```

;; ---

;; ## 📚 进阶快捷键

;; ### 求值相关
;; - `C-c C-p` - 求值并在 minibuffer 显示结果
;; - `C-c M-n` - 切换到当前文件的命名空间
;; - `C-u C-c C-c` - 调试模式求值

;; ### 文档查看
;; - `C-c C-d j` - 在浏览器中查看 Javadoc
;; - `C-c C-d r` - 查看函数源码

;; ### 导航
;; - `C-c M-.` - 跳转到资源
;; - `C-c C-t n` - 跳转到下一个测试
;; - `C-c C-t p` - 跳转到上一个测试

;; ### 测试
;; - `C-c C-t t` - 运行当前测试
;; - `C-c C-t n` - 运行当前命名空间的所有测试
;; - `C-c C-t p` - 运行项目所有测试
;; - `C-c C-t r` - 重新运行失败的测试

;; ### 调试
;; - `C-u C-c C-c` - 调试模式求值
;; - `C-c C-b` - 中断执行
;; - `n` - 下一步
;; - `c` - 继续
;; - `o` - 跳出
;; - `i` - 进入

;; ### REPL 管理
;; - `C-c M-o` - 清空 REPL buffer
;; - `C-c C-o` - 清空最后一个输出
;; - `C-c C-q` - 退出 REPL

;; ### 重构
;; - `C-c C-r r` - 重命名符号
;; - `C-c C-r a` - 添加缺失的 require
;; - `C-c C-r n` - 清理命名空间

;; ### 检查
;; - `C-c M-i` - 检查符号（inspect）
;; - `C-c M-t v` - 追踪变量
;; - `C-c M-t n` - 追踪命名空间

;; ---

;; ## 💡 实用技巧

;; ### 快速测试表达式
;; 1. 写一个表达式，比如 `(q 't)`
;; 2. 光标放在表达式后面
;; 3. 按 `C-c C-e` 立即求值

;; ### 查看函数用法
;; 1. 光标放在函数名上，比如 `literal-function`
;; 2. 按 `C-c C-d d` 查看文档
;; 3. 按 `M-.` 查看源码

;; ### 调试技巧
;; 1. 在要调试的表达式上按 `C-u C-c C-c`
;; 2. 使用 `n`/`c`/`o`/`i` 控制执行
;; 3. 查看变量值

;; ---

;; ## 🎯 记忆口诀

;; **必记的 6 个：**
;; ```
;; C-c C-k  加载文件
;; C-c C-e  求值表达式
;; C-c C-d d  看文档
;; M-.      跳定义
;; M-,      跳回来
;; C-c C-z  切 REPL
;; ```

;; ---

;; ## 📖 更多资源

;; - [Cider 官方文档](https://docs.cider.mx/)
;; - [Cider 快捷键速查表](https://github.com/clojure-emacs/cider#keyboard-shortcuts)
