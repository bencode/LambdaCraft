<script lang="ts">
  import katex from 'katex'
  import 'katex/dist/katex.min.css'
  import { link } from 'svelte-spa-router'

  const formulas = [
    {
      degree: 2,
      title: '二次方程',
      equation: 'ax^2 + bx + c = 0',
      formula: String.raw`x = \frac{-b \pm \sqrt{b^2 - 4ac}}{2a}`,
      discriminant: String.raw`\Delta = b^2 - 4ac`,
      notes: '判别式 Δ > 0 时有两个不同实根，Δ = 0 时有重根，Δ < 0 时有共轭复根'
    },
    {
      degree: 3,
      title: '三次方程（卡尔达诺公式）',
      equation: 'x^3 + px + q = 0',
      formula: String.raw`x = \sqrt[3]{-\frac{q}{2} + \sqrt{\Delta}} + \sqrt[3]{-\frac{q}{2} - \sqrt{\Delta}}`,
      discriminant: String.raw`\Delta = \frac{q^2}{4} + \frac{p^3}{27}`,
      notes: '一般三次方程 ax³ + bx² + cx + d = 0 需先换元 x = t - b/(3a) 化为标准形式'
    },
    {
      degree: 4,
      title: '四次方程（费拉里方法）',
      equation: 'x^4 + px^2 + qx + r = 0',
      formula: String.raw`\text{引入辅助变量 } m \text{，解三次方程得 } m`,
      discriminant: String.raw`m^3 + 2pm^2 + (p^2 - 4r)m - q^2 = 0`,
      notes: '利用 m 将四次方程分解为两个二次方程求解'
    }
  ]

  const render = (latex: string): string => {
    try {
      return katex.renderToString(latex, { displayMode: true, throwOnError: false })
    } catch {
      return latex
    }
  }

  const renderInline = (latex: string): string => {
    try {
      return katex.renderToString(latex, { displayMode: false, throwOnError: false })
    } catch {
      return latex
    }
  }
</script>

<div class="max-w-3xl mx-auto">
  <a href="/" use:link class="inline-block mb-4 text-blue-500 hover:text-blue-600 hover:underline">
    ← 返回可视化
  </a>

  <h2 class="text-2xl font-bold text-center mb-8">求根公式参考</h2>

  {#each formulas as f}
    <section class="bg-white dark:bg-slate-800 rounded-xl p-6 mb-4 border border-gray-200 dark:border-slate-700 shadow-sm">
      <h3 class="text-xl font-semibold mb-4 pb-3 border-b border-gray-200 dark:border-slate-700">
        {f.title}
      </h3>

      <div class="flex items-center gap-2 mb-4">
        <span class="text-gray-500 dark:text-gray-400 text-sm">方程：</span>
        <span class="font-serif">{@html renderInline(f.equation)}</span>
      </div>

      <div class="my-6 overflow-x-auto">
        {@html render(f.formula)}
      </div>

      <div class="flex items-center gap-2 mb-4">
        <span class="text-gray-500 dark:text-gray-400 text-sm">判别式：</span>
        <span class="font-serif">{@html renderInline(f.discriminant)}</span>
      </div>

      <p class="text-sm text-gray-500 dark:text-gray-400 leading-relaxed">
        {f.notes}
      </p>
    </section>
  {/each}
</div>
