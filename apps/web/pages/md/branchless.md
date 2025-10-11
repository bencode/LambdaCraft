# 我把分支“藏”进了函数：TypeScript 的 final encoding，顺路聊聊 Clojure

**语言的边界，即思维的边界**
*——一个程序员关于表达式与语言的自言自语*

最近我又在想那个老问题：怎么优雅地表示一棵表达式树？
一个算式，比如：

```
f(x) = (x + 2) * (x + 3)^2 - sin(x)
```

这问题太老了，几乎所有语言的教程都会提。可每次重写，我总觉得哪里不对。我写的不是计算，而是在和**语言本身的抽象能力较劲**。

### 1. 我们都从分支开始

老办法嘛，一棵树，一个 `tag`，一个 `switch`。代码很直觉，也很稳：

```ts
// 传统 AST：判别联合
type Expr =
  | { tag: 'Num'; n: number }
  | { tag: 'Var'; name: string }
  | { tag: 'Add'; a: Expr; b: Expr }
  | { tag: 'Mul'; a: Expr; b: Expr }
  | { tag: 'Neg'; x: Expr }
  | { tag: 'Pow'; a: Expr; b: Expr }
  | { tag: 'Sin'; x: Expr }
  | { tag: 'Cos'; x: Expr };

function evalExpr(e: Expr, env: Record<string, number>): number {
  switch (e.tag) {
    case 'Num': return e.n;
    case 'Var': return env[e.name];
    case 'Add': return evalExpr(e.a, env) + evalExpr(e.b, env);
    case 'Mul': return evalExpr(e.a, env) * evalExpr(e.b, env);
    case 'Neg': return -evalExpr(e.x, env);
    case 'Pow': return Math.pow(evalExpr(e.a, env), evalExpr(e.b, env));
    case 'Sin': return Math.sin(evalExpr(e.x, env));
    case 'Cos': return Math.cos(evalExpr(e.x, env));
  }
}
```

但每当我想加一个新节点，比如 `Div`、`Ln`、`Exp`，我就要在 **所有操作** 里再加一堆分支：`eval`、`print`、`simplify`、`diff`……它们像影子一样紧紧跟着我。

这时候我会想起一个老朋友：**Expression Problem**——

> 想加「新节点」，要改所有「操作」；
> 想加「新操作」，要改所有「节点」。

我在这里卡了很多年。

### 2. 如果换个角度看呢？

有一天，我看到 **final encoding/branchless**。有人说：“**不写分支，也能表达分支**”。听起来有点玄，但核心就一句话——**让表达式自己知道该调用谁**。

我试着在 TypeScript 里照猫画虎。

```ts
// 一个表达式：给我一个“解释器”（一张函数表），我产出一个 R
type ExprF = <R>(alg: Alg<R>) => R;

// 你支持哪些节点，就在解释器表里列出这些处理器
type Alg<R> = {
  const: (n: number) => R;
  var:   (name: string) => R;
  add:   (x: R, y: R) => R;
  mul:   (x: R, y: R) => R;
  neg:   (x: R) => R;
  pow:   (x: R, y: R) => R;
  sin:   (x: R) => R;
  cos:   (x: R) => R;
};

// 构造器：表达式本身就是“等你传入 alg 的函数”
const Const = (n: number): ExprF => alg => alg.const(n);
const VarF  = (name: string): ExprF => alg => alg.var(name);
const AddF  = (x: ExprF, y: ExprF): ExprF => alg => alg.add(x(alg), y(alg));
const MulF  = (x: ExprF, y: ExprF): ExprF => alg => alg.mul(x(alg), y(alg));
const NegF  = (x: ExprF): ExprF => alg => alg.neg(x(alg));
const PowF  = (x: ExprF, y: ExprF): ExprF => alg => alg.pow(x(alg), y(alg));
const SinF  = (x: ExprF): ExprF => alg => alg.sin(x(alg));
const CosF  = (x: ExprF): ExprF => alg => alg.cos(x(alg));

const run = <R>(e: ExprF, alg: Alg<R>): R => e(alg);
```

这时，表达式不再是“数据”，而是一种**等待被解释的行为**。
求值？传入 `evalAlg`。打印？传入 `printAlg`。化简与求导？再各自写一套解释器即可。

**神奇之处在于**：任何时候，我都没有写 `switch`。分支被“吸收”进了高阶函数。

### 3. 我把它跑起来（eval/print/simplify/diff）

**求值解释器：**

```ts
const evalAlg = (env: Record<string, number>): Alg<number> => ({
  const: n => n,
  var  : name => {
    if (!(name in env)) throw new Error(`Unbound var: ${name}`);
    return env[name];
  },
  add  : (x, y) => x + y,
  mul  : (x, y) => x * y,
  neg  : x => -x,
  pow  : (x, y) => Math.pow(x, y),
  sin  : x => Math.sin(x),
  cos  : x => Math.cos(x),
});
```

**打印解释器（带简单优先级）：**

```ts
type Printed = { s: string; p: number };
const paren = (need: boolean, s: string) => (need ? `(${s})` : s);

const printAlg = (): Alg<Printed> => ({
  const: n => ({ s: String(n), p: 100 }),
  var  : name => ({ s: name, p: 100 }),
  neg  : x => ({ s: `-${paren(x.p < 90, x.s)}`, p: 95 }),
  pow  : (x, y) => ({ s: `${paren(x.p < 99, x.s)}^${paren(y.p <= 99, y.s)}`, p: 99 }),
  mul  : (x, y) => ({ s: `${paren(x.p < 98, x.s)} * ${paren(y.p < 98, y.s)}`, p: 98 }),
  add  : (x, y) => ({ s: `${paren(x.p < 97, x.s)} + ${paren(y.p < 97, y.s)}`, p: 97 }),
  sin  : x => ({ s: `sin(${x.s})`, p: 100 }),
  cos  : x => ({ s: `cos(${x.s})`, p: 100 }),
});
const printF = (e: ExprF): string => run(e, printAlg()).s;
```

**化简解释器（常量折叠/零一律）：**

```ts
type S = { e: ExprF; c?: number };
const SConst = (n: number): S => ({ e: Const(n), c: n });
const SExpr  = (e: ExprF): S => ({ e });

const simplifyAlg = (): Alg<S> => ({
  const: n => SConst(n),
  var  : name => SExpr(VarF(name)),
  neg  : x => (x.c !== undefined ? SConst(-x.c) : SExpr(NegF(x.e))),
  add  : (x, y) => {
    if (x.c === 0) return y;
    if (y.c === 0) return x;
    if (x.c !== undefined && y.c !== undefined) return SConst(x.c + y.c);
    return SExpr(AddF(x.e, y.e));
  },
  mul  : (x, y) => {
    if (x.c === 0 || y.c === 0) return SConst(0);
    if (x.c === 1) return y;
    if (y.c === 1) return x;
    if (x.c === -1) return y.c !== undefined ? SConst(-y.c) : SExpr(NegF(y.e));
    if (y.c === -1) return x.c !== undefined ? SConst(-x.c) : SExpr(NegF(x.e));
    if (x.c !== undefined && y.c !== undefined) return SConst(x.c * y.c);
    return SExpr(MulF(x.e, y.e));
  },
  pow  : (x, y) => {
    if (y.c === 0) return SConst(1);
    if (y.c === 1) return x;
    if (x.c === 0) return SConst(0);
    if (x.c === 1) return SConst(1);
    if (x.c !== undefined && y.c !== undefined) return SConst(Math.pow(x.c, y.c));
    return SExpr(PowF(x.e, y.e));
  },
  sin  : x => (x.c !== undefined ? SConst(Math.sin(x.c)) : SExpr(SinF(x.e))),
  cos  : x => (x.c !== undefined ? SConst(Math.cos(x.c)) : SExpr(CosF(x.e))),
});
const simplifyF = (e: ExprF): ExprF => run(e, simplifyAlg()).e;
```

**求导解释器（对变量 `v`；先支持“幂的常数指数”）：**

```ts
type D = { e: ExprF; d: ExprF };
const DPair = (e: ExprF, d: ExprF): D => ({ e, d });

const diffAlg = (v: string): Alg<D> => ({
  const: n => DPair(Const(n), Const(0)),
  var  : name => DPair(VarF(name), Const(name === v ? 1 : 0)),
  neg  : x => DPair(NegF(x.e), NegF(x.d)),
  add  : (x, y) => DPair(AddF(x.e, y.e), AddF(x.d, y.d)),
  mul  : (x, y) => DPair(MulF(x.e, y.e), AddF(MulF(x.d, y.e), MulF(x.e, y.d))),
  pow  : (x, y) => {
    const ySimpl = run(y.e, simplifyAlg()); // 看 y 是否是常量
    if (ySimpl.c !== undefined) {
      const k = ySimpl.c;
      return DPair(
        PowF(x.e, y.e),
        MulF(Const(k), MulF(PowF(x.e, Const(k - 1)), x.d))
      );
    }
    return DPair(PowF(x.e, y.e), Const(Number.NaN)); // 通用规则需要 ln/exp/div 节点
  },
  sin  : x => DPair(SinF(x.e), MulF(CosF(x.e), x.d)),
  cos  : x => DPair(CosF(x.e), NegF(MulF(SinF(x.e), x.d))),
});
const diffF = (e: ExprF, v: string): ExprF => simplifyF(run(e, diffAlg(v)).d);
```

**把开头那个式子撸一遍：**

```ts
const x = VarF('x');
const f = AddF(
  MulF(AddF(x, Const(2)), PowF(AddF(x, Const(3)), Const(2))),
  NegF(SinF(x))
);

console.log('f(x)   =', printF(f));
console.log('simp   =', printF(simplifyF(f)));

const df = diffF(f, 'x');
console.log("f'(x)  =", printF(df));
console.log("simp'  =", printF(simplifyF(df)));

console.log('f(1)   =', run(f, evalAlg({ x: 1 })));
console.log("f'(1)  =", run(df, evalAlg({ x: 1 })));
```

我终于发现：**新增“操作”特别省心**。再来一个 `latexAlg`、`codegenAlg`、`freeVarsAlg`……都只是写一张新的解释器表，表达式完全不用改。

### 4. 这时候我开始怀疑：是不是语言的问题？

同样的思想，放到 Clojure，就像回了家。

```clojure
(defn C-val [x] (fn [alg] ((:const alg) x)))
(defn C-add [a b] (fn [alg] ((:add alg) (a alg) (b alg) alg)))

(def eval-alg {:const identity
               :add   (fn [a b _] (+ a b))})

(def print-alg {:const str
                :add   (fn [a b _] (str "(" a ") + (" b ")"))})

(def e (C-add (C-val 42) (C-add (C-val 32) (C-val 10))))
((e eval-alg))   ;=> 84
((e print-alg))  ;=> "(42) + ((32) + (10))"
```

函数是一等公民，map 是解释器，关键字就是“方法名”。我在 TypeScript 里是**模拟**这种结构；在 Clojure 里，只是**顺手**。

如果我还想让「节点」和「操作」**两头都开放**，Clojure 还有 **multimethods**：
`(defmulti exec (fn [op expr & _] [op (:tag expr)]))` —— 新增节点=新 `:tag`；新增操作=新 `op`；旧代码不用动。这就是语言在帮我思考，而不是和我对抗。

### 5. 回到那句老话

**语言的边界，即思维的边界。**
在 TypeScript，我在**设计模式**；在 Clojure，我在**描述计算**；在 Haskell，我在**证明结构**。
同一道题，不同语言给你不同的**默认思路**。它们不是语法差异，而是**你能抵达的抽象边界**。

我不是在教机器思考；我是在用语言，**教我自己**思考。
