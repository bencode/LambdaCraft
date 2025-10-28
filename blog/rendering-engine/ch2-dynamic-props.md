# 第2节：支持动态属性

> 这是《Schema驱动的前端渲染引擎设计和实现》系列的第二篇。上一节我们实现了基础的渲染功能，这一节将引入表达式系统，让属性可以动态求值。

## 2.1 遇到新需求

现在有个新需求：

> 页面顶部要显示 "欢迎，张三"，其中 "张三" 是当前登录用户的名字，每个用户看到的都是自己的名字。

第一版实现遇到了问题：

```typescript
// v0.1 只能这样写
{
  type: 'Text',
  props: { content: '欢迎，张三' }  // 写死了！
}
```

我们需要让属性值变成动态的。

## 2.2 方案探索

### 方案1：模板字符串（amis 的方案）

amis 使用模板语法：

```json
{
  "type": "text",
  "content": "欢迎，${user.name}"
}
```

实现：

```typescript
function evalTemplate(template: string, data: any) {
  // 替换 ${expr} 为实际值
  return template.replace(/\$\{([^}]+)\}/g, (_, expr) => {
    return eval(`data.${expr}`)
  })
}

// 使用
const content = evalTemplate('欢迎，${user.name}', { user: { name: '张三' } })
// => '欢迎，张三'
```

**问题：**
1. 需要解析模板语法
2. 静态文本和动态文本没有区分
3. 如何处理对象类型的属性？

```json
{
  "type": "input",
  "placeholder": "请输入${field.label}",  // 字符串 ✓
  "disabled": "${user.role !== 'admin'}"  // 布尔值 ✗ 会变成字符串
}
```

### 方案2：表达式前缀（formily 的方案）

formily 使用 `{{}}` 包裹表达式：

```typescript
{
  'x-component-props': {
    placeholder: '请输入用户名',    // 静态
    disabled: '{{!$self.value}}'  // 动态，需要求值
  }
}
```

**问题：**
1. 还是需要解析 `{{}}`
2. 区分不够直观

### 方案3：后缀标记

我们用一个比较简单的方案：**属性名加 `$` 后缀表示需要求值**

```typescript
{
  type: 'Text',
  props: {
    content: '欢迎，',           // 静态属性
    content$: 'user.name'        // 动态属性，会覆盖 content
  }
}
```

优势：
1. 不需要解析字符串
2. 静态和动态属性一眼能看出来
3. TypeScript 类型友好
4. 支持所有类型的值

## 2.3 实现表达式系统

### 2.3.1 基础版本

首先定义上下文类型：

```typescript
type Context = {
  user: {
    name: string
    role: string
  }
  // 可以扩展更多上下文数据
}
```

实现求值函数：

```typescript
function evaluate(expr: string, context: Context): unknown {
  // 使用 Function 构造函数安全地执行表达式
  try {
    const fn = new Function('context', `with(context) { return ${expr} }`)
    return fn(context)
  } catch (e) {
    console.error(`表达式执行错误: ${expr}`, e)
    return undefined
  }
}

// 测试
const context = { user: { name: '张三', role: 'admin' } }
console.log(evaluate('user.name', context))           // '张三'
console.log(evaluate('user.role === "admin"', context)) // true
```

### 2.3.2 评估 Props

现在我们需要在渲染时评估带 `$` 的属性：

```typescript
function evaluateProps(
  props: Record<string, unknown>,
  context: Context
): Record<string, unknown> {
  const result: Record<string, unknown> = {}

  for (const key in props) {
    // 检查是否是动态属性
    if (key.endsWith('$')) {
      const actualKey = key.slice(0, -1)  // 去掉 $
      const expr = props[key] as string
      result[actualKey] = evaluate(expr, context)
    } else {
      // 静态属性直接复制
      result[key] = props[key]
    }
  }

  return result
}

// 测试
const props = {
  content: '默认值',
  content$: 'user.name',
  disabled$: 'user.role !== "admin"'
}

const context = { user: { name: '张三', role: 'admin' } }
const evaluated = evaluateProps(props, context)

console.log(evaluated)
// => { content: '张三', disabled: false }
```

### 2.3.3 集成到渲染引擎

更新我们的 createEngine 函数，让 context 在 render 时传入：

```typescript
function createEngine(registry: ReturnType<typeof createRegistry>) {
  const render = (schema: Schema, context: Context): React.ReactElement => {
    const Component = registry.get(schema.type)

    if (!Component) {
      return <div style={{ color: 'red' }}>
        Component "{schema.type}" not found
      </div>
    }

    // 评估 props
    const props = schema.props
      ? evaluateProps(schema.props, context)
      : {}

    // 递归渲染子节点
    if (schema.children && schema.children.length > 0) {
      props.children = schema.children.map((child, index) => (
        <React.Fragment key={index}>
          {render(child, context)}
        </React.Fragment>
      ))
    }

    return createElement(Component, props)
  }

  return render
}
```

## 2.4 完整示例

现在我们可以使用动态属性了：

```typescript
// 模拟当前登录用户
const context = {
  user: {
    name: '张三',
    role: 'admin',
    avatar: 'https://example.com/avatar.jpg'
  }
}

// Schema 配置
const schema: Schema = {
  type: 'Box',
  children: [
    // 静态文本
    {
      type: 'Text',
      props: {
        content: '欢迎，',
        style: { fontSize: 14, color: '#666' }
      }
    },
    // 动态用户名
    {
      type: 'Text',
      props: {
        content$: 'user.name',  // 动态求值
        style: { fontSize: 14, fontWeight: 'bold', color: '#000' }
      }
    },
    // 根据角色显示不同内容
    {
      type: 'Text',
      props: {
        content$: 'user.role === "admin" ? "（管理员）" : "（普通用户）"',
        style: { fontSize: 12, color: '#999' }
      }
    }
  ]
}

// 创建渲染函数
const render = createEngine(registry)

function App() {
  return render(schema, context)
}
```

**渲染结果：**

```
欢迎，张三（管理员）
```

如果 `context.user.role` 是 `'user'`，则显示：

```
欢迎，张三（普通用户）
```

## 2.5 优化：表达式缓存

每次渲染都编译表达式会有性能问题，我们加入缓存：

```typescript
function createEvaluator() {
  const cache = new Map<string, Function>()

  return (expr: string, context: Context): unknown => {
    // 从缓存获取
    let fn = cache.get(expr)

    if (!fn) {
      // 编译并缓存
      try {
        fn = new Function('context', `with(context) { return ${expr} }`)
        cache.set(expr, fn)
      } catch (e) {
        console.error(`表达式编译错误: ${expr}`, e)
        return undefined
      }
    }

    // 执行
    try {
      return fn(context)
    } catch (e) {
      console.error(`表达式执行错误: ${expr}`, e)
      return undefined
    }
  }
}
```

更新 evaluateProps：

```typescript
function evaluateProps(
  props: Record<string, unknown>,
  context: Context,
  evaluate: ReturnType<typeof createEvaluator>
): Record<string, unknown> {
  const result: Record<string, unknown> = {}

  for (const key in props) {
    if (key.endsWith('$')) {
      const actualKey = key.slice(0, -1)
      const expr = props[key] as string
      result[actualKey] = evaluate(expr, context)
    } else {
      result[key] = props[key]
    }
  }

  return result
}
```

更新 createEngine：

```typescript
function createEngine(registry: ReturnType<typeof createRegistry>) {
  const evaluate = createEvaluator()  // 创建求值器

  const render = (schema: Schema, context: Context): React.ReactElement => {
    const Component = registry.get(schema.type)

    if (!Component) {
      return <div style={{ color: 'red' }}>
        Component "{schema.type}" not found
      </div>
    }

    const props = schema.props
      ? evaluateProps(schema.props, context, evaluate)
      : {}

    if (schema.children && schema.children.length > 0) {
      props.children = schema.children.map((child, index) => (
        <React.Fragment key={index}>
          {render(child, context)}
        </React.Fragment>
      ))
    }

    return createElement(Component, props)
  }

  return render
}
```

## 2.6 支持函数表达式

现在我们还需要支持更复杂的场景，比如格式化日期：

```typescript
{
  type: 'Text',
  props: {
    content$: `
      (() => {
        const date = new Date()
        return date.toLocaleDateString()
      })()
    `
  }
}
```

或者提供工具函数：

```typescript
// 扩展 Context
type Context = {
  user: {
    name: string
    role: string
  }
  // 工具函数
  utils: {
    formatDate: (date: Date) => string
    formatCurrency: (amount: number) => string
  }
}

// Schema 中使用
{
  type: 'Text',
  props: {
    content$: 'utils.formatDate(new Date())'
  }
}
```

## 2.7 实战：动态表单

现在我们可以实现一个真实场景：根据用户角色显示不同的表单字段。

```typescript
const formSchema: Schema = {
  type: 'Form',
  children: [
    // 用户名（所有人可见）
    {
      type: 'Box',
      children: [
        { type: 'Text', props: { content: '用户名：' } },
        { type: 'Input', props: { placeholder: '请输入用户名' } }
      ]
    },

    // 邮箱（所有人可见）
    {
      type: 'Box',
      children: [
        { type: 'Text', props: { content: '邮箱：' } },
        { type: 'Input', props: { placeholder: '请输入邮箱' } }
      ]
    },

    // 权限设置（仅管理员可见）
    {
      type: 'Box',
      props: {
        style$: 'user.role === "admin" ? {} : { display: "none" }'
      },
      children: [
        { type: 'Text', props: { content: '权限：' } },
        { type: 'Input', props: { placeholder: '设置权限' } }
      ]
    },

    // 提交按钮（文案根据角色变化）
    {
      type: 'Button',
      props: {
        label$: 'user.role === "admin" ? "保存并审核" : "提交"'
      }
    }
  ]
}
```

当 `user.role === 'admin'` 时显示：
```
用户名：[_______]
邮箱：  [_______]
权限：  [_______]
[保存并审核]
```

当 `user.role === 'user'` 时显示：
```
用户名：[_______]
邮箱：  [_______]
[提交]
```

## 2.8 完整代码

让我们整合所有功能：

```typescript
import React, { createElement } from 'react'

// ============ 类型定义 ============
type Schema = {
  type: string
  props?: Record<string, unknown>
  children?: Schema[]
}

type Context = {
  user: {
    name: string
    role: string
  }
}

// ============ 表达式求值器 ============
function createEvaluator() {
  const cache = new Map<string, Function>()

  return (expr: string, context: Context): unknown => {
    let fn = cache.get(expr)

    if (!fn) {
      try {
        fn = new Function('context', `with(context) { return ${expr} }`)
        cache.set(expr, fn)
      } catch (e) {
        console.error(`表达式编译错误: ${expr}`, e)
        return undefined
      }
    }

    try {
      return fn(context)
    } catch (e) {
      console.error(`表达式执行错误: ${expr}`, e)
      return undefined
    }
  }
}

// ============ Props 评估 ============
function evaluateProps(
  props: Record<string, unknown>,
  context: Context,
  evaluate: ReturnType<typeof createEvaluator>
): Record<string, unknown> {
  const result: Record<string, unknown> = {}

  for (const key in props) {
    if (key.endsWith('$')) {
      const actualKey = key.slice(0, -1)
      const expr = props[key] as string
      result[actualKey] = evaluate(expr, context)
    } else {
      result[key] = props[key]
    }
  }

  return result
}

// ============ 组件注册 ============
type Registry = {
  register: (name: string, component: React.ComponentType<any>) => void
  get: (name: string) => React.ComponentType<any> | undefined
}

function createRegistry(): Registry {
  const components: Record<string, React.ComponentType<any>> = {}

  return {
    register: (name, component) => {
      components[name] = component
    },
    get: (name) => components[name]
  }
}

// ============ 渲染引擎 ============
function createEngine(registry: Registry) {
  const evaluate = createEvaluator()

  const render = (schema: Schema, context: Context): React.ReactElement => {
    const Component = registry.get(schema.type)

    if (!Component) {
      return <div style={{ color: 'red' }}>
        Component "{schema.type}" not found
      </div>
    }

    const props = schema.props
      ? evaluateProps(schema.props, context, evaluate)
      : {}

    if (schema.children && schema.children.length > 0) {
      props.children = schema.children.map((child, index) => (
        <React.Fragment key={index}>
          {render(child, context)}
        </React.Fragment>
      ))
    }

    return createElement(Component, props)
  }

  return render
}

// ============ 使用示例 ============
const registry = createRegistry()
registry.register('Text', ({ content }) => <span>{content}</span>)
registry.register('Button', ({ label }) => <button>{label}</button>)
registry.register('Box', ({ children }) => <div>{children}</div>)

const render = createEngine(registry)

const schema = {
  type: 'Box',
  children: [
    { type: 'Text', props: { content: '欢迎，' } },
    { type: 'Text', props: { content$: 'user.name' } }
  ]
}

function App() {
  const context = { user: { name: '张三', role: 'admin' } }
  return render(schema, context)
}
```

## 2.9 当前实现的局限性

我们已经支持了动态属性，但还有新问题：

### 1. Context 是静态的

```typescript
// Context 在创建 Engine 时就固定了
const render = createEngine(registry, context)

// 如果 context 变化，需要重新创建 render 函数？
```

### 2. 无法响应用户交互

```typescript
// 用户点击按钮后，如何更新 context？
{
  type: 'Button',
  props: {
    label: '刷新',
    onClick: '???' // 如何配置点击事件？
  }
}
```

### 3. 组件之间仍然无法通信

```typescript
// 如何让 Input 的值影响 Button 的 disabled 状态？
[
  {
    type: 'Input',
    // 用户输入的值怎么传递出去？
  },
  {
    type: 'Button',
    props: {
      disabled$: '???' // 如何访问 Input 的值？
    }
  }
]
```

## 2.10 本节小结

这一节我们引入了表达式系统，让属性可以动态求值。

实现的功能：
- 用 `$` 后缀标识动态属性
- 用 Function 构造函数执行表达式
- 表达式缓存
- 支持复杂表达式

核心代码：

```typescript
// 1. 后缀语法
props: {
  label: '静态文本',
  label$: 'user.name'  // 动态求值
}

// 2. 表达式求值器
const evaluate = createEvaluator()
evaluate(expr, context)

// 3. Props 评估
function evaluateProps(props, context, evaluate) {
  const result = {}
  for (const key in props) {
    if (key.endsWith('$')) {
      const actualKey = key.slice(0, -1)
      result[actualKey] = evaluate(props[key], context)
    } else {
      result[key] = props[key]
    }
  }
  return result
}

// 4. 创建引擎
const render = createEngine(registry, context)
```

采用的设计：
- 纯函数，无副作用
- 闭包管理缓存
- 高阶函数组合

还没解决的问题：
- Context 是在 render 时传入的，如何让它可以动态更新？
- 如何处理用户交互（onClick 等事件）？
- 组件之间如何通信？

下一节我们将解决事件处理的问题：如何让组件可以交互。

