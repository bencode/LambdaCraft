<script lang="ts">
  import type { Complex } from '../lib/math'

  export let roots: Complex[] = []
  export let labels: string[] = []

  const width = 400
  const height = 400
  const padding = 40
  const scale = 80  // 1 单位 = 80 像素

  const cx = width / 2
  const cy = height / 2

  const toScreen = (z: Complex) => ({
    x: cx + z.re * scale,
    y: cy - z.im * scale  // y 轴向上为正
  })

  const colors = ['#e74c3c', '#3498db', '#2ecc71', '#f39c12']

  $: screenRoots = roots.map(toScreen)
</script>

<svg {width} {height} class="complex-plane">
  <!-- 坐标轴 -->
  <line x1={padding} y1={cy} x2={width - padding} y2={cy} class="axis" />
  <line x1={cx} y1={padding} x2={cx} y2={height - padding} class="axis" />

  <!-- 轴标签 -->
  <text x={width - padding + 10} y={cy + 5} class="axis-label">Re</text>
  <text x={cx + 5} y={padding - 10} class="axis-label">Im</text>

  <!-- 刻度 -->
  {#each [-2, -1, 1, 2] as tick}
    <line x1={cx + tick * scale} y1={cy - 5} x2={cx + tick * scale} y2={cy + 5} class="tick" />
    <text x={cx + tick * scale} y={cy + 20} class="tick-label">{tick}</text>
    <line x1={cx - 5} y1={cy - tick * scale} x2={cx + 5} y2={cy - tick * scale} class="tick" />
    {#if tick !== 0}
      <text x={cx - 25} y={cy - tick * scale + 5} class="tick-label">{tick}i</text>
    {/if}
  {/each}

  <!-- 连线形成多边形 -->
  {#if screenRoots.length >= 2}
    <polygon
      points={screenRoots.map(p => `${p.x},${p.y}`).join(' ')}
      class="polygon"
    />
  {/if}

  <!-- 根的点 -->
  {#each screenRoots as point, i}
    <circle cx={point.x} cy={point.y} r="8" fill={colors[i % colors.length]} class="root" />
    <text x={point.x + 12} y={point.y - 8} class="root-label" fill={colors[i % colors.length]}>
      {labels[i] || `r${i + 1}`}
    </text>
  {/each}
</svg>

<style>
  .complex-plane {
    background: #1a1a2e;
    border-radius: 8px;
  }

  .axis {
    stroke: #444;
    stroke-width: 1;
  }

  .axis-label {
    fill: #888;
    font-size: 14px;
  }

  .tick {
    stroke: #444;
    stroke-width: 1;
  }

  .tick-label {
    fill: #666;
    font-size: 12px;
    text-anchor: middle;
  }

  .polygon {
    fill: rgba(255, 255, 255, 0.05);
    stroke: rgba(255, 255, 255, 0.3);
    stroke-width: 2;
    stroke-dasharray: 4;
  }

  .root {
    stroke: white;
    stroke-width: 2;
    cursor: pointer;
    transition: r 0.2s;
  }

  .root:hover {
    r: 10;
  }

  .root-label {
    font-size: 14px;
    font-weight: bold;
  }
</style>
