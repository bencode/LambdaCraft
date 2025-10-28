# 第3节：组件交互 - ActionStore

> 这是《低代码渲染引擎演化》系列的第三篇。前两节我们实现了基础渲染和动态属性，这一节将解决组件之间如何交互的问题。

## 3.1 遇到新需求

假设我们遇到这样的需求：

> 页面上有一个表单和一个提交按钮，点击按钮时要先验证表单，验证通过才能提交

这个需求让我们遇到了新问题：

```typescript
// 问题1：如何配置 onClick？
{
  type: 'Button',
  props: {
    label: '提交',
    onClick: '???' // 不能直接写函数
  }
}

// 问题2：Button 如何调用 Form 的验证方法？
{
  type: 'Form',  // 需要提供 validate() 方法
}
{
  type: 'Button', // 需要调用 Form.validate()
}
```

## 3.2 问题分析

我们需要解决两个核心问题：

### 问题1：组件如何暴露方法？

Form 组件需要提供 `validate()` 和 `getData()` 等方法，但在 Schema 配置中，组件只是一个 JSON 对象，如何让它有方法？

### 问题2：组件之间如何调用方法？

Button 需要调用 Form 的方法，但它们是两个独立的组件，如何建立联系？

## 3.3 解决方案：ActionStore

### 核心思路

我们需要一个**全局的 Action 注册表**：
- 组件可以向注册表注册自己的方法
- 其他组件可以通过组件名查找并调用这些方法

### 类型定义

```typescript
// Action 就是一个函数
type Action = (...args: any[]) => any

// ActionStore 存储所有注册的 Action
type ActionStore = {
  actions: Record<string, Action>  // key: "componentName.actionName"
  register: (componentName: string, actionName: string, handler: Action) => void
  unregister: (componentName: string, actionName: string) => void
  get: (componentName: string, actionName: string) => Action | undefined
}
```

### 实现 ActionStore

我们使用 Zustand 来管理 Action 注册表：

```typescript
import { createStore } from 'zustand'

function createActionStore() {
  return createStore<ActionStore>((set, get) => ({
    actions: {},

    register: (componentName, actionName, handler) => {
      const key = `${componentName}.${actionName}`
      set(state => ({
        actions: {
          ...state.actions,
          [key]: handler
        }
      }))
    },

    unregister: (componentName, actionName) => {
      const key = `${componentName}.${actionName}`
      set(state => {
        const { [key]: _, ...rest } = state.actions
        return { actions: rest }
      })
    },

    get: (componentName, actionName) => {
      const key = `${componentName}.${actionName}`
      return get().actions[key]
    }
  }))
}
```

### 提供 Context

```typescript
import { createContext, useContext, useRef } from 'react'
import type { StoreApi } from 'zustand'

const ActionStoreContext = createContext<StoreApi<ActionStore> | null>(null)

function ActionStoreProvider({ children }: { children: React.ReactNode }) {
  const storeRef = useRef<StoreApi<ActionStore>>()

  if (!storeRef.current) {
    storeRef.current = createActionStore()
  }

  return (
    <ActionStoreContext.Provider value={storeRef.current}>
      {children}
    </ActionStoreContext.Provider>
  )
}

function useActionStore() {
  const store = useContext(ActionStoreContext)
  if (!store) {
    throw new Error('useActionStore must be used within ActionStoreProvider')
  }
  return store
}
```

### ActionProvider - 注册 Action 的组件

我们使用一个特殊的组件来注册 Action：

```typescript
type ActionProviderProps = {
  name: string      // 组件名
  action: string    // action 名
  handler: Action   // 处理函数
}

function ActionProvider({ name, action, handler }: ActionProviderProps) {
  const store = useActionStore()

  useEffect(() => {
    const { register, unregister } = store.getState()
    register(name, action, handler)
    return () => {
      unregister(name, action)
    }
  }, [store, name, action, handler])

  return null  // 不渲染任何内容
}
```

**核心思想**：
- ActionProvider 是一个纯副作用组件
- 当组件挂载时注册 Action
- 当组件卸载时移除 Action
- 不渲染任何内容（返回 null）

**关键优势**：
1. **动态能力监听**：Action 只在组件渲染时可用
2. **条件注册**：可以根据条件决定是否渲染 ActionProvider

### 获取 Action 的 Hook

```typescript
function useGetAction() {
  const store = useActionStore()

  return useCallback((componentName: string, actionName: string) => {
    const { get } = store.getState()
    return get(componentName, actionName)
  }, [store])
}
```

## 3.4 条件注册 Action

ActionProvider 的一个重要优势是可以根据条件决定是否注册 Action：

```typescript
// 示例：打印功能只在某些条件下可用
const PrintButton = ({ enabled }: { enabled: boolean }) => {
  const handlePrint = () => {
    console.log('打印...')
  }

  return (
    <>
      <button disabled={!enabled}>打印</button>
      {/* 只有在 enabled 时才注册 print action */}
      {enabled && (
        <ActionProvider name="printBtn" action="print" handler={handlePrint} />
      )}
    </>
  )
}
```

**工作原理：**
1. 当 `enabled = false` 时，ActionProvider 不渲染，print action 不可用
2. 当 `enabled = true` 时，ActionProvider 渲染，print action 注册
3. 其他组件通过 `getAction('printBtn', 'print')` 获取时：
   - enabled = false → 返回 undefined
   - enabled = true → 返回 handlePrint 函数

**使用场景：**
- 根据权限动态开启/关闭功能
- 根据表单状态决定是否允许提交
- 根据数据加载状态决定功能可用性

## 3.5 实现表单组件

现在我们可以实现一个带方法的 Form 组件：

```typescript
type FormProps = {
  name: string  // 组件名，用于注册 Action
  children?: React.ReactNode
}

const Form = ({ name, children }: FormProps) => {
  const [values, setValues] = useState<Record<string, any>>({})

  // 验证方法
  const validate = useCallback(() => {
    // 简单验证：检查是否有空值
    const emptyFields = Object.entries(values)
      .filter(([_, value]) => !value)
      .map(([key]) => key)

    if (emptyFields.length > 0) {
      return {
        success: false,
        message: `请填写: ${emptyFields.join(', ')}`
      }
    }

    return { success: true }
  }, [values])

  // 获取数据方法
  const getData = useCallback(() => {
    return values
  }, [values])

  // 提供 form context 给子组件
  const handleFieldChange = (fieldName: string, value: any) => {
    setValues(prev => ({ ...prev, [fieldName]: value }))
  }

  return (
    <form style={{ padding: 20, border: '1px solid #ccc' }}>
      {/* 使用 ActionProvider 注册方法 */}
      <ActionProvider name={name} action="validate" handler={validate} />
      <ActionProvider name={name} action="getData" handler={getData} />

      <FormContext.Provider value={{ values, onChange: handleFieldChange }}>
        {children}
      </FormContext.Provider>
    </form>
  )
}
```

实现 Input 组件：

```typescript
type InputProps = {
  name: string
  placeholder?: string
}

const FormContext = createContext<{
  values: Record<string, any>
  onChange: (name: string, value: any) => void
} | null>(null)

const Input = ({ name, placeholder }: InputProps) => {
  const ctx = useContext(FormContext)
  const value = ctx?.values[name] || ''

  return (
    <div style={{ marginBottom: 10 }}>
      <input
        value={value}
        placeholder={placeholder}
        onChange={e => ctx?.onChange(name, e.target.value)}
        style={{ padding: 5, border: '1px solid #ccc' }}
      />
    </div>
  )
}
```

## 3.6 Button 调用 Form 的方法

现在我们需要让 Button 可以配置 onClick，并在点击时调用 Form 的方法。

### 扩展 Context

在渲染上下文中注入 `getAction`：

```typescript
type Context = {
  user: {
    name: string
    role: string
  }
  getAction: (componentName: string, actionName: string) => Action | undefined
}
```

### 更新 createEngine

```typescript
function createEngine(
  registry: Registry,
  getAction: (componentName: string, actionName: string) => Action | undefined
) {
  const evaluate = createEvaluator()

  const render = (schema: Schema, context: Context): React.ReactElement => {
    const Component = registry.get(schema.type)

    if (!Component) {
      return <div style={{ color: 'red' }}>
        Component "{schema.type}" not found
      </div>
    }

    // 注入 getAction 到 context
    const enhancedContext = {
      ...context,
      getAction
    }

    const props = schema.props
      ? evaluateProps(schema.props, enhancedContext, evaluate)
      : {}

    if (schema.children && schema.children.length > 0) {
      props.children = schema.children.map((child, index) => (
        <React.Fragment key={index}>
          {render(child, enhancedContext)}
        </React.Fragment>
      ))
    }

    return createElement(Component, props)
  }

  return render
}
```

### 实现带事件的 Button

```typescript
type ButtonProps = {
  label: string
  onClick?: () => void | Promise<void>
}

const Button = ({ label, onClick }: ButtonProps) => {
  const [loading, setLoading] = useState(false)

  const handleClick = async () => {
    if (!onClick) return

    setLoading(true)
    try {
      await onClick()
    } catch (e) {
      console.error(e)
    } finally {
      setLoading(false)
    }
  }

  return (
    <button
      onClick={handleClick}
      disabled={loading}
      style={{ padding: '5px 15px', cursor: 'pointer' }}
    >
      {loading ? '处理中...' : label}
    </button>
  )
}
```

### 使用示例

```typescript
const schema = {
  type: 'Box',
  children: [
    {
      type: 'Form',
      props: { name: 'userForm' },
      children: [
        {
          type: 'Input',
          props: { name: 'username', placeholder: '用户名' }
        },
        {
          type: 'Input',
          props: { name: 'email', placeholder: '邮箱' }
        }
      ]
    },
    {
      type: 'Button',
      props: {
        label: '提交',
        onClick$: `async ctx => {
          // 调用表单验证
          const validate = ctx.getAction('userForm', 'validate')
          if (!validate) {
            alert('表单未就绪')
            return
          }

          const result = validate()
          if (!result.success) {
            alert(result.message)
            return
          }

          // 获取表单数据
          const getData = ctx.getAction('userForm', 'getData')
          const data = getData()
          console.log('提交数据:', data)
          alert('提交成功！')
        }`
      }
    }
  ]
}
```

**运行效果：**
1. 用户输入用户名和邮箱
2. 点击提交按钮
3. 调用表单验证，如果有空字段则提示
4. 验证通过后获取数据并提交

## 3.7 延迟执行问题

在实际使用中，我们可能遇到这样的场景：

```typescript
{
  type: 'Button',
  props: {
    label: '初始化',
    onClick$: `ctx => {
      // 页面加载时立即调用
      ctx.getAction('myForm', 'reset')()
    }`
  }
}
```

**问题**：如果 Button 比 Form 先渲染，调用时 Form 的 Action 还没注册，`getAction` 会返回 `undefined`。

### 解决方案：Deferred 机制

我们扩展 ActionStore，支持延迟执行：

```typescript
type ActionStore = {
  actions: Record<string, Action>
  defers: Record<string, Array<(handler: Action) => any>>  // 新增：延迟队列
  register: (componentName: string, actionName: string, handler: Action) => void
  unregister: (componentName: string, actionName: string) => void
  get: (componentName: string, actionName: string, deferred?: boolean) => Action | undefined
}

function createActionStore() {
  return createStore<ActionStore>((set, get) => ({
    actions: {},
    defers: {},

    register: (componentName, actionName, handler) => {
      const key = `${componentName}.${actionName}`
      set(state => ({
        actions: { ...state.actions, [key]: handler }
      }))

      // 执行延迟的调用
      const state = get()
      const deferList = state.defers[key]
      if (deferList && deferList.length > 0) {
        deferList.forEach(fn => fn(handler))
        set(state => {
          const { [key]: _, ...rest } = state.defers
          return { defers: rest }
        })
      }
    },

    unregister: (componentName, actionName) => {
      const key = `${componentName}.${actionName}`
      set(state => {
        const { [key]: _, ...rest } = state.actions
        return { actions: rest }
      })
    },

    get: (componentName, actionName, deferred = false) => {
      const key = `${componentName}.${actionName}`
      const handler = get().actions[key]

      if (handler) {
        return handler
      }

      if (deferred) {
        // 返回代理函数
        return (...args: any[]) => {
          console.log('deferred call:', key, args)
          set(state => {
            const list = state.defers[key] || []
            return {
              defers: {
                ...state.defers,
                [key]: [...list, (handler: Action) => handler(...args)]
              }
            }
          })
        }
      }

      return undefined
    }
  }))
}
```

### 使用方式

```typescript
const getAction = useGetAction()

// 不使用 deferred，如果 Action 不存在会返回 undefined
const fn = getAction('myForm', 'validate')
if (fn) {
  fn()
}

// 使用 deferred，即使 Action 不存在也返回函数
// 当 Action 注册后会自动执行
const fn = getAction('myForm', 'validate', true)
fn()  // 安全，不会报错
```

### 工作流程

1. **调用时 Action 未注册**：
   - `getAction('myForm', 'validate', true)` 返回代理函数
   - 调用代理函数时，参数被存储到 defers 队列

2. **Action 注册时**：
   - ActionProvider 调用 `register('myForm', 'validate', handler)`
   - 检查 defers 队列，发现有延迟调用
   - 执行所有延迟的调用：`handler(...args)`
   - 清空该 Action 的延迟队列

## 3.8 页面级隔离

目前我们的 ActionStore 是全局的，如果页面上有多个独立的区域（比如多个 Modal），它们会互相干扰。

### 解决方案

每个页面创建独立的 Store：

```typescript
function Page({ schema }: { schema: Schema }) {
  // 每个页面创建独立的 ActionStore
  const actionStoreRef = useRef<StoreApi<ActionStore>>()

  if (!actionStoreRef.current) {
    actionStoreRef.current = createActionStore()
  }

  const getAction = useCallback((componentName: string, actionName: string, deferred?: boolean) => {
    return actionStoreRef.current!.getState().get(componentName, actionName, deferred)
  }, [])

  const render = useMemo(() => {
    return createEngine(registry, getAction)
  }, [getAction])

  return (
    <ActionStoreContext.Provider value={actionStoreRef.current}>
      {render(schema, { user: { name: '张三', role: 'admin' } })}
    </ActionStoreContext.Provider>
  )
}
```

现在每个 Page 组件都有独立的 ActionStore，互不干扰。

## 3.9 完整示例

让我们整合所有功能：

```typescript
import React, {
  createElement,
  createContext,
  useContext,
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState
} from 'react'
import { createStore } from 'zustand'
import type { StoreApi } from 'zustand'

// ============ 类型定义 ============
type Schema = {
  type: string
  props?: Record<string, unknown>
  children?: Schema[]
}

type Action = (...args: any[]) => any

type ActionStore = {
  actions: Record<string, Action>
  defers: Record<string, Array<(handler: Action) => any>>
  register: (componentName: string, actionName: string, handler: Action) => void
  unregister: (componentName: string, actionName: string) => void
  get: (componentName: string, actionName: string, deferred?: boolean) => Action | undefined
}

type Context = {
  user: {
    name: string
    role: string
  }
  getAction: (componentName: string, actionName: string, deferred?: boolean) => Action | undefined
}

type Registry = {
  register: (name: string, component: React.ComponentType<any>) => void
  get: (name: string) => React.ComponentType<any> | undefined
}

// ============ ActionStore ============
function createActionStore() {
  return createStore<ActionStore>((set, get) => ({
    actions: {},
    defers: {},

    register: (componentName, actionName, handler) => {
      const key = `${componentName}.${actionName}`
      set(state => ({
        actions: { ...state.actions, [key]: handler }
      }))

      const state = get()
      const deferList = state.defers[key]
      if (deferList && deferList.length > 0) {
        deferList.forEach(fn => fn(handler))
        set(state => {
          const { [key]: _, ...rest } = state.defers
          return { defers: rest }
        })
      }
    },

    unregister: (componentName, actionName) => {
      const key = `${componentName}.${actionName}`
      set(state => {
        const { [key]: _, ...rest } = state.actions
        return { actions: rest }
      })
    },

    get: (componentName, actionName, deferred = false) => {
      const key = `${componentName}.${actionName}`
      const handler = get().actions[key]

      if (handler) {
        return handler
      }

      if (deferred) {
        return (...args: any[]) => {
          set(state => {
            const list = state.defers[key] || []
            return {
              defers: {
                ...state.defers,
                [key]: [...list, (handler: Action) => handler(...args)]
              }
            }
          })
        }
      }

      return undefined
    }
  }))
}

const ActionStoreContext = createContext<StoreApi<ActionStore> | null>(null)

function useActionStore() {
  const store = useContext(ActionStoreContext)
  if (!store) throw new Error('useActionStore must be used within ActionStoreProvider')
  return store
}

function ActionProvider({ name, action, handler }: {
  name: string
  action: string
  handler: Action
}) {
  const store = useActionStore()
  useEffect(() => {
    const { register, unregister } = store.getState()
    register(name, action, handler)
    return () => unregister(name, action)
  }, [store, name, action, handler])
  return null
}

// ============ 表达式系统 ============
function createEvaluator() {
  const cache = new Map<string, Function>()

  return (expr: string, context: Context): unknown => {
    let fn = cache.get(expr)
    if (!fn) {
      try {
        fn = new Function('ctx', `with(ctx) { return ${expr} }`)
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
function createEngine(
  registry: Registry,
  getAction: (componentName: string, actionName: string, deferred?: boolean) => Action | undefined
) {
  const evaluate = createEvaluator()

  const render = (schema: Schema, userContext: Omit<Context, 'getAction'>): React.ReactElement => {
    const context: Context = { ...userContext, getAction }
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
          {render(child, userContext)}
        </React.Fragment>
      ))
    }

    return createElement(Component, props)
  }

  return render
}

// ============ 组件实现 ============
const Box = ({ children }: { children?: React.ReactNode }) => (
  <div style={{ marginBottom: 10 }}>{children}</div>
)

const Button = ({ label, onClick }: {
  label: string
  onClick?: () => void | Promise<void>
}) => {
  const [loading, setLoading] = useState(false)

  const handleClick = async () => {
    if (!onClick) return
    setLoading(true)
    try {
      await onClick()
    } catch (e) {
      console.error(e)
    } finally {
      setLoading(false)
    }
  }

  return (
    <button
      onClick={handleClick}
      disabled={loading}
      style={{ padding: '5px 15px', cursor: 'pointer' }}
    >
      {loading ? '处理中...' : label}
    </button>
  )
}

const FormContext = createContext<{
  values: Record<string, any>
  onChange: (name: string, value: any) => void
} | null>(null)

const Form = ({ name, children }: {
  name: string
  children?: React.ReactNode
}) => {
  const [values, setValues] = useState<Record<string, any>>({})

  const validate = useCallback(() => {
    const emptyFields = Object.entries(values)
      .filter(([_, value]) => !value)
      .map(([key]) => key)

    if (emptyFields.length > 0) {
      return {
        success: false,
        message: `请填写: ${emptyFields.join(', ')}`
      }
    }

    return { success: true }
  }, [values])

  const getData = useCallback(() => {
    return values
  }, [values])

  const handleFieldChange = (fieldName: string, value: any) => {
    setValues(prev => ({ ...prev, [fieldName]: value }))
  }

  return (
    <form style={{ padding: 20, border: '1px solid #ccc', marginBottom: 10 }}>
      <ActionProvider name={name} action="validate" handler={validate} />
      <ActionProvider name={name} action="getData" handler={getData} />

      <FormContext.Provider value={{ values, onChange: handleFieldChange }}>
        {children}
      </FormContext.Provider>
    </form>
  )
}

const Input = ({ name, placeholder }: {
  name: string
  placeholder?: string
}) => {
  const ctx = useContext(FormContext)
  const value = ctx?.values[name] || ''

  return (
    <div style={{ marginBottom: 10 }}>
      <input
        value={value}
        placeholder={placeholder}
        onChange={e => ctx?.onChange(name, e.target.value)}
        style={{ padding: 5, border: '1px solid #ccc' }}
      />
    </div>
  )
}

// ============ 使用示例 ============
const registry = createRegistry()
registry.register('Box', Box)
registry.register('Button', Button)
registry.register('Form', Form)
registry.register('Input', Input)

function Page({ schema }: { schema: Schema }) {
  const actionStoreRef = useRef<StoreApi<ActionStore>>()

  if (!actionStoreRef.current) {
    actionStoreRef.current = createActionStore()
  }

  const getAction = useCallback((componentName: string, actionName: string, deferred?: boolean) => {
    return actionStoreRef.current!.getState().get(componentName, actionName, deferred)
  }, [])

  const render = useMemo(() => {
    return createEngine(registry, getAction)
  }, [getAction])

  return (
    <ActionStoreContext.Provider value={actionStoreRef.current}>
      {render(schema, { user: { name: '张三', role: 'admin' } })}
    </ActionStoreContext.Provider>
  )
}

const schema: Schema = {
  type: 'Box',
  children: [
    {
      type: 'Form',
      props: { name: 'userForm' },
      children: [
        {
          type: 'Input',
          props: { name: 'username', placeholder: '用户名' }
        },
        {
          type: 'Input',
          props: { name: 'email', placeholder: '邮箱' }
        }
      ]
    },
    {
      type: 'Button',
      props: {
        label: '提交',
        onClick$: `async ctx => {
          const validate = ctx.getAction('userForm', 'validate')
          if (!validate) {
            alert('表单未就绪')
            return
          }

          const result = validate()
          if (!result.success) {
            alert(result.message)
            return
          }

          const getData = ctx.getAction('userForm', 'getData')
          const data = getData()
          alert('提交成功: ' + JSON.stringify(data))
        }`
      }
    }
  ]
}

function App() {
  return (
    <div style={{ padding: 20 }}>
      <h1>ActionStore 示例</h1>
      <Page schema={schema} />
    </div>
  )
}

export default App
```

## 3.10 本节小结

这一节我们引入了 ActionStore，解决了组件间方法调用的问题。

实现的功能：
- ActionProvider：组件方法注册
- useGetAction：方法调用
- 条件注册：根据条件动态开启/关闭能力
- 延迟执行：deferred 机制解决时序问题
- 页面级隔离：独立的 Store 实例

核心代码：

```typescript
// 1. 注册 Action
<ActionProvider name="myForm" action="validate" handler={validateFn} />

// 2. 调用 Action（普通）
const fn = ctx.getAction('myForm', 'validate')
if (fn) {
  fn()
}

// 3. 调用 Action（延迟）
const fn = ctx.getAction('myForm', 'validate', true)
fn()  // 安全，Action 注册后会自动执行

// 4. 条件注册
{enabled && <ActionProvider name="btn" action="print" handler={printFn} />}
```

采用的设计：
- ActionProvider 作为组件，实现动态能力监听
- 使用 Zustand 管理 Action 注册表
- deferred 机制解决异步注册问题
- 页面级隔离避免冲突

还没解决的问题：
- 组件间如何共享数据？
- 数据变化如何自动更新 UI？
- 如何避免通过组件名耦合？

下一节我们将引入 StateStore，解决组件间的数据共享和响应式更新问题。
