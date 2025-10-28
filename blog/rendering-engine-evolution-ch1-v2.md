# 第1节：最简单的渲染引擎

> 这是《低代码渲染引擎演化》系列的第一篇。我们将从最基础的功能开始，逐步构建出一个完整的渲染引擎。每一步都会展示遇到的问题和解决思路。

## 1.1 从一个需求开始

假设我们要做一个低代码平台，第一个需求是：

> 用户通过拖拽添加一个按钮，配置按钮的文本，然后能在页面上看到这个按钮。

要实现这个功能，需要两部分：
1. 用 JSON 配置来描述按钮
2. 写一个函数把 JSON 配置渲染成 React 组件

## 1.2 设计 Schema

首先，我们需要设计一个 JSON 格式来描述按钮：

```typescript
const schema = {
  type: 'Button',
  props: {
    label: '点击我'
  }
}
```

这个格式很简单：
- `type` 表示组件类型
- `props` 是组件的属性

渲染时，这段 JSON 应该变成：

```tsx
<Button label="点击我" />
```

## 1.3 实现 v0.1：最简渲染函数

让我们写出第一个版本：

```typescript
// types.ts
type Schema = {
  type: string
  props?: Record<string, unknown>
}

// components.ts
const Button = ({ label }: { label: string }) => (
  <button>{label}</button>
)

// engine.ts
import { createElement } from 'react'

const componentMap = {
  Button
}

function render(schema: Schema) {
  const Component = componentMap[schema.type]
  if (!Component) {
    throw new Error(`Component ${schema.type} not found`)
  }
  return createElement(Component, schema.props)
}

// 使用
function App() {
  const schema = {
    type: 'Button',
    props: { label: '点击我' }
  }
  return render(schema)
}
```

**运行效果：**

```html
<button>点击我</button>
```

这样就完成了最基本的 Schema 到组件的转换。

## 1.4 支持更多组件

接下来需要支持更多组件类型：输入框、文本、图片等。

扩展 componentMap：

```typescript
const Input = ({ placeholder }: { placeholder?: string }) => (
  <input placeholder={placeholder} />
)

const Text = ({ content }: { content: string }) => (
  <span>{content}</span>
)

const Image = ({ src, alt }: { src: string; alt?: string }) => (
  <img src={src} alt={alt} />
)

const componentMap = {
  Button,
  Input,
  Text,
  Image
}
```

现在可以渲染多种组件了：

```typescript
const schemas = [
  { type: 'Text', props: { content: '用户名：' } },
  { type: 'Input', props: { placeholder: '请输入用户名' } },
  { type: 'Button', props: { label: '提交' } }
]

function App() {
  return (
    <div>
      {schemas.map((schema, index) => (
        <div key={index}>{render(schema)}</div>
      ))}
    </div>
  )
}
```

## 1.5 支持嵌套结构

现在遇到新问题：如何实现表单这样的容器组件？容器里面可以放多个子组件。

Schema 需要支持 children：

```typescript
type Schema = {
  type: string
  props?: Record<string, unknown>
  children?: Schema[]  // 新增：子节点
}
```

更新渲染函数：

```typescript
function render(schema: Schema): React.ReactElement {
  const Component = componentMap[schema.type]
  if (!Component) {
    throw new Error(`Component ${schema.type} not found`)
  }

  const props = { ...schema.props }

  // 递归渲染子节点
  if (schema.children) {
    props.children = schema.children.map((child, index) => (
      <React.Fragment key={index}>
        {render(child)}
      </React.Fragment>
    ))
  }

  return createElement(Component, props)
}
```

添加容器组件：

```typescript
const Form = ({ children }: { children?: React.ReactNode }) => (
  <form style={{ padding: 20, border: '1px solid #ccc' }}>
    {children}
  </form>
)

const Box = ({ children }: { children?: React.ReactNode }) => (
  <div style={{ marginBottom: 10 }}>
    {children}
  </div>
)

const componentMap = {
  Button,
  Input,
  Text,
  Image,
  Form,
  Box
}
```

现在可以描述嵌套结构了：

```typescript
const schema = {
  type: 'Form',
  children: [
    {
      type: 'Box',
      children: [
        { type: 'Text', props: { content: '用户名：' } },
        { type: 'Input', props: { placeholder: '请输入用户名' } }
      ]
    },
    {
      type: 'Box',
      children: [
        { type: 'Text', props: { content: '密码：' } },
        { type: 'Input', props: { placeholder: '请输入密码' } }
      ]
    },
    {
      type: 'Button',
      props: { label: '登录' }
    }
  ]
}
```

**渲染结果：**

```html
<form>
  <div>
    <span>用户名：</span>
    <input placeholder="请输入用户名" />
  </div>
  <div>
    <span>密码：</span>
    <input placeholder="请输入密码" />
  </div>
  <button>登录</button>
</form>
```

## 1.6 优化：组件注册系统

目前 componentMap 写死在代码里，如果要动态注册组件会比较麻烦。我们可以用函数来创建一个注册表：

```typescript
// 类型定义
type ComponentMap = Record<string, React.ComponentType<any>>

// 创建注册表
function createRegistry() {
  const components: ComponentMap = {}

  return {
    register: (name: string, component: React.ComponentType<any>) => {
      components[name] = component
    },
    get: (name: string) => {
      return components[name]
    },
    getAll: () => components
  }
}

// 使用
const registry = createRegistry()

// 注册基础组件
registry.register('Button', Button)
registry.register('Input', Input)
registry.register('Text', Text)
registry.register('Form', Form)
registry.register('Box', Box)
```

定义 Registry 类型：

```typescript
type Registry = {
  register: (name: string, component: React.ComponentType<any>) => void
  get: (name: string) => React.ComponentType<any> | undefined
}
```

更新渲染函数，让它接受 registry：

```typescript
function createEngine(registry: Registry) {
  // 递归渲染函数
  const render = (schema: Schema): React.ReactElement => {
    const Component = registry.get(schema.type)

    if (!Component) {
      return <div style={{ color: 'red' }}>
        Component "{schema.type}" not found
      </div>
    }

    const props = { ...schema.props }

    if (schema.children && schema.children.length > 0) {
      props.children = schema.children.map((child, index) => (
        <React.Fragment key={index}>
          {render(child)}
        </React.Fragment>
      ))
    }

    return createElement(Component, props)
  }

  return render
}

// 使用
const registry = createRegistry()
registry.register('Button', Button)
registry.register('Input', Input)
// ... 注册其他组件

const render = createEngine(registry)

function App() {
  const schema = { type: 'Button', props: { label: '点击' } }
  return render(schema)
}
```

现在可以动态注册组件了：

```typescript
// 用户可以注册自己的组件
registry.register('MyCustomButton', ({ label, icon }) => (
  <button>
    <i className={icon} />
    {label}
  </button>
))
```

## 1.7 完整示例

让我们整合一下，写一个完整的可运行示例：

```typescript
import React, { createElement } from 'react'
import ReactDOM from 'react-dom'

// ============ 类型定义 ============
type Schema = {
  type: string
  props?: Record<string, unknown>
  children?: Schema[]
}

// ============ 组件定义 ============
const Form = ({ children }: { children?: React.ReactNode }) => (
  <form style={{ padding: 20, border: '1px solid #ccc', borderRadius: 4 }}>
    {children}
  </form>
)

const Box = ({ children }: { children?: React.ReactNode }) => (
  <div style={{ marginBottom: 10 }}>
    {children}
  </div>
)

const Text = ({ content }: { content: string }) => (
  <span style={{ marginRight: 10 }}>{content}</span>
)

const Input = ({ placeholder }: { placeholder?: string }) => (
  <input
    placeholder={placeholder}
    style={{ padding: 5, border: '1px solid #ccc' }}
  />
)

const Button = ({ label }: { label: string }) => (
  <button style={{ padding: '5px 15px', cursor: 'pointer' }}>
    {label}
  </button>
)

// ============ 注册系统 ============
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
  const render = (schema: Schema): React.ReactElement => {
    const Component = registry.get(schema.type)

    if (!Component) {
      return <div style={{ color: 'red' }}>
        Component "{schema.type}" not found
      </div>
    }

    const props = { ...schema.props }

    if (schema.children && schema.children.length > 0) {
      props.children = schema.children.map((child, index) => (
        <React.Fragment key={index}>
          {render(child)}
        </React.Fragment>
      ))
    }

    return createElement(Component, props)
  }

  return render
}

// ============ 初始化 ============
const registry = createRegistry()

registry.register('Form', Form)
registry.register('Box', Box)
registry.register('Text', Text)
registry.register('Input', Input)
registry.register('Button', Button)

const render = createEngine(registry)

// ============ Schema 配置 ============
const schema: Schema = {
  type: 'Form',
  children: [
    {
      type: 'Box',
      children: [
        { type: 'Text', props: { content: '用户名：' } },
        { type: 'Input', props: { placeholder: '请输入用户名' } }
      ]
    },
    {
      type: 'Box',
      children: [
        { type: 'Text', props: { content: '密码：' } },
        { type: 'Input', props: { placeholder: '请输入密码' } }
      ]
    },
    {
      type: 'Button',
      props: { label: '登录' }
    }
  ]
}

// ============ 渲染 ============
function App() {
  return (
    <div style={{ padding: 20 }}>
      <h1>最简渲染引擎 v0.1</h1>
      {render(schema)}
    </div>
  )
}

ReactDOM.render(<App />, document.getElementById('root'))
```

## 1.8 当前实现的局限性

到这里，我们已经有了一个可以工作的渲染引擎。但它还有一些明显的限制：

### 1. 属性都是静态的

```typescript
// 只能写死
{ type: 'Text', props: { content: '用户名：' } }

// 无法做到：根据登录用户显示 "欢迎，张三"
```

### 2. 无法处理事件

```typescript
// onClick 怎么配置？
{
  type: 'Button',
  props: {
    label: '提交',
    onClick: '???' // 无法配置函数
  }
}
```

### 3. 组件之间无法通信

```typescript
// 如何让 Input 的值传递给 Button？
[
  { type: 'Input' },
  { type: 'Button' } // 怎么获取 Input 的值？
]
```

### 4. 没有状态管理

```typescript
// 用户输入的数据存在哪里？
// 如何在多个组件间共享数据？
```

## 1.9 本章小结

这一节我们从最简单的需求出发，一步步实现了一个基础的渲染引擎。

实现的功能：
- Schema 到 React 组件的转换
- 嵌套结构的渲染
- 组件注册系统

核心代码：

```typescript
// 1. Schema 定义
type Schema = {
  type: string
  props?: Record<string, unknown>
  children?: Schema[]
}

// 2. 创建注册表
const registry = createRegistry()
registry.register(name, Component)

// 3. 创建渲染引擎
const render = createEngine(registry)

// 4. 渲染
render(schema)
```

采用的设计：
- 使用闭包管理内部状态
- 函数式风格，避免副作用
- 通过组合而非继承来扩展功能

还没解决的问题：
- 属性都是静态的，无法根据数据动态变化
- 无法处理事件（如 onClick）
- 组件之间无法通信
- 没有状态管理

下一节我们将解决第一个问题：如何让属性可以动态求值。

