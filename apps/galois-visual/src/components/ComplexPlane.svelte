<script lang="ts">
  import type { Complex } from '../lib/math'

  export let roots: Complex[] = []
  export let labels: string[] = []

  const width = 400
  const height = 400
  const padding = 40
  const scale = 80

  const cx = width / 2
  const cy = height / 2

  const toScreen = (z: Complex) => ({
    x: cx + z.re * scale,
    y: cy - z.im * scale
  })

  const colors = ['#ef4444', '#3b82f6', '#22c55e', '#f59e0b']

  $: screenRoots = roots.map(toScreen)
</script>

<div class="bg-white dark:bg-slate-800 rounded-xl p-4 border border-gray-200 dark:border-slate-700 shadow-sm">
  <svg {width} {height} class="block">
    <!-- 坐标轴 -->
    <line x1={padding} y1={cy} x2={width - padding} y2={cy} class="stroke-gray-300 dark:stroke-slate-600" stroke-width="1" />
    <line x1={cx} y1={padding} x2={cx} y2={height - padding} class="stroke-gray-300 dark:stroke-slate-600" stroke-width="1" />

    <!-- 轴标签 -->
    <text x={width - padding + 10} y={cy + 5} class="fill-gray-400 dark:fill-gray-500 text-sm">Re</text>
    <text x={cx + 5} y={padding - 10} class="fill-gray-400 dark:fill-gray-500 text-sm">Im</text>

    <!-- 刻度 -->
    {#each [-2, -1, 1, 2] as tick}
      <line x1={cx + tick * scale} y1={cy - 5} x2={cx + tick * scale} y2={cy + 5} class="stroke-gray-300 dark:stroke-slate-600" stroke-width="1" />
      <text x={cx + tick * scale} y={cy + 20} class="fill-gray-400 dark:fill-gray-500 text-xs" text-anchor="middle">{tick}</text>
      <line x1={cx - 5} y1={cy - tick * scale} x2={cx + 5} y2={cy - tick * scale} class="stroke-gray-300 dark:stroke-slate-600" stroke-width="1" />
      {#if tick !== 0}
        <text x={cx - 25} y={cy - tick * scale + 5} class="fill-gray-400 dark:fill-gray-500 text-xs">{tick}i</text>
      {/if}
    {/each}

    <!-- 连线形成多边形 -->
    {#if screenRoots.length >= 2}
      <polygon
        points={screenRoots.map(p => `${p.x},${p.y}`).join(' ')}
        class="fill-blue-500/10 stroke-blue-400/50 dark:fill-blue-400/10 dark:stroke-blue-300/50"
        stroke-width="2"
        stroke-dasharray="6"
      />
    {/if}

    <!-- 根的点 -->
    {#each screenRoots as point, i}
      <circle
        cx={point.x}
        cy={point.y}
        r="8"
        fill={colors[i % colors.length]}
        class="stroke-white dark:stroke-slate-800 cursor-pointer hover:r-10 transition-all"
        stroke-width="2"
      />
      <text
        x={point.x + 12}
        y={point.y - 8}
        fill={colors[i % colors.length]}
        class="text-sm font-bold"
      >
        {labels[i] || `r${i + 1}`}
      </text>
    {/each}
  </svg>
</div>
