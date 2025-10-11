^{:nextjournal.clerk/visibility {:code :hide}}
(ns pages.test-syntax
  (:require [nextjournal.clerk :as clerk]))

^{:nextjournal.clerk/visibility {:code :hide}}
(clerk/html [:h1 "代码高亮测试"])

;; ## Clojure 代码（应该高亮）

(clerk/md "```clojure
(defn hello [name]
  (str \"Hello, \" name \"!\"))
```")

;; ## JavaScript 代码（测试1）

(clerk/md "```javascript
function hello(name) {
  return `Hello, ${name}!`;
}
```")

;; ## JS 代码（测试2）

(clerk/md "```js
const add = (a, b) => a + b;
```")

;; ## TypeScript 代码（测试3）

(clerk/md "```typescript
type User = { name: string; age: number };
const greet = (user: User): string => `Hello, ${user.name}!`;
```")

;; ## 无语言标识符（不应该高亮）

(clerk/md "```
plain text without language
```")
