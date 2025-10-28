# 低代码渲染引擎实现详解

## 一、引言

### 1.1 背景与目标
- 低代码平台的核心诉求
- Engine-Shell 的设计定位
- DSL 驱动 UI 的优势

### 1.2 核心概念
- Schema 驱动渲染
- 表达式系统
- 组件联动机制

## 二、整体架构

### 2.1 分层设计
```
┌──────────────────────────────┐
│   Workbench 层 (视图管理)      │
├──────────────────────────────┤
│   Meta 层 (上下文和元组件)     │
├──────────────────────────────┤
│   Engine 层 (核心渲染引擎)     │
├──────────────────────────────┤
│   Registry 层 (组件注册)       │
└──────────────────────────────┘
```

### 2.2 目录结构
- engine/ - 核心渲染逻辑
- meta/ - 上下文管理和元组件
- workbench/ - 视图加载和路由
- registry/ - 组件注册系统

### 2.3 数据流
```
Schema → ComponentFactory → EngineView → React Components
         ↓
    Expression Evaluator
         ↓
    RenderContext (state/action/connector)
```

## 三、核心渲染引擎

### 3.1 渲染流程

#### Schema 节点类型
- Widget - 可视化组件
- Layout - 布局容器
- Container - 逻辑容器
- Meta - 元信息节点

#### 渲染过程
```typescript
create() → EngineView → InternalEngineView → renderNode()
```

### 3.2 组件工厂模式

实现组件的层级查找:
1. 容器特定组件库
2. 全局组件库

代码示例: `/engine/component-factory.ts:12-28`

### 3.3 渲染上下文

RenderContext 提供的能力:
- `setState/getState` - 全局状态管理
- `getAction` - Action 调用
- `$()` - 组件联动
- `$new()` - 创建嵌套上下文

## 四、表达式系统

### 4.1 设计原理

使用 Function 构造函数动态编译表达式:
```typescript
new Function('$context', `with($context) { return (${expr}); }`)
```

### 4.2 Props 评估机制

表达式后缀语法:
```typescript
{
  "label$": "ctx.user.name",     // 动态求值
  "label": "Static Label"         // 静态值
}
```

### 4.3 缓存优化

避免重复编译相同表达式

代码位置: `/engine/expression.ts:45-65`

## 五、状态管理系统

### 5.1 基于 Zustand 的实现

每个 WorkbenchPage 独立 store,实现页面级隔离

### 5.2 API 设计

```typescript
useSetState() // 支持函数/key-value/对象三种方式
useGetState() // 获取完整状态
useStoreState() // 响应式订阅
```

### 5.3 状态隔离策略

通过 Provider 实现多层嵌套隔离

代码示例: `/meta/context/state.ts:25-72`

## 六、Action 管理

### 6.1 注册机制

ActionProvider 组件注册方法:
```typescript
<ActionProvider name="form" action="submit" handler={fn} />
```

### 6.2 延迟执行

代理模式处理 Action 注册前的调用

### 6.3 调用方式

```typescript
const getAction = useGetAction()
getAction('form', 'submit', true)(data)
```

代码位置: `/meta/context/action.ts:88-147`

## 七、组件联动系统

### 7.1 Observable/Observer 模式

发布-订阅机制实现组件间通信

### 7.2 Connector 设计

通过 `$()` 函数访问组件:
```typescript
ctx.$('myInput').get('value')
ctx.$('myInput').set('disabled', true)
ctx.$('myForm').action('submit')
```

### 7.3 实现原理

基于全局 State Store 的 key 命名约定:
```
$$observable/componentName
```

代码示例: `/meta/widgets/connect/index.ts:145-198`

## 八、元组件 (Meta Widgets)

### 8.1 UseState
封装 React useState

### 8.2 UseSelfState
表单控件自身状态管理

### 8.3 UseStoreState
响应全局状态

### 8.4 Show
条件渲染

### 8.5 Bridge
桥接手写 React 组件

代码位置: `/meta/widgets/`

## 九、工作台层

### 9.1 WorkbenchPage

整合所有 Provider:
```
ViewStackProvider
  → ActionStoreProvider
    → StateStoreProvider
      → WorkbenchPageBody
```

### 9.2 视图加载

异步加载 Schema:
```typescript
useLoadView(route) → { loading, viewInfo }
```

### 9.3 上下文创建

支持嵌套上下文的 `$new()` 机制

代码示例: `/workbench/page.tsx:45-78`

## 十、组件注册系统

### 10.1 Registry API

```typescript
registry.component(MyComponent, {
  type: 'Widget',
  name: 'MyWidget',
  container: 'Page'
})
```

### 10.2 转换为 ComponentLibrary

两步转换:
1. 注册所有 Container
2. 注册 Widget 和 Layout 到对应位置

代码位置: `/registry/registry.ts:85-134`

## 十一、设计模式总结

### 11.1 工厂模式
ComponentFactory 的层级查找

### 11.2 观察者模式
Observable/Observer 组件联动

### 11.3 代理模式
Action 延迟执行

### 11.4 策略模式
Props 多种求值策略

### 11.5 Context 模式
多层上下文隔离

## 十二、性能优化

### 12.1 表达式编译缓存
### 12.2 Zustand selector 优化
### 12.3 引用相等性检查
### 12.4 MemoNode 包装

## 十三、最佳实践

### 13.1 Schema 设计规范
### 13.2 组件注册建议
### 13.3 状态管理策略
### 13.4 表达式使用指南

## 十四、扩展性

### 14.1 自定义 Meta 组件
### 14.2 扩展 RenderContext
### 14.3 自定义容器

## 附录

### A. 核心类型定义
### B. 关键文件路径索引
### C. 完整示例
