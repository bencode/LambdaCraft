<script lang="ts">
  import { createEventDispatcher } from 'svelte'
  import { unitRoots, generateRandomRoots, rootsToCoefficients, format } from '../lib/math'
  import type { Complex } from '../lib/math'

  export let degree: 2 | 3 | 4 = 4
  export let roots: Complex[] = []

  const dispatch = createEventDispatcher<{
    generate: { roots: Complex[] }
    degreeChange: 2 | 3 | 4
  }>()

  const degreeOptions: (2 | 3 | 4)[] = [2, 3, 4]

  const handleUnitRoots = () => {
    const newRoots = unitRoots(degree)
    dispatch('generate', { roots: newRoots })
  }

  const handleRandom = () => {
    const newRoots = generateRandomRoots(degree)
    dispatch('generate', { roots: newRoots })
  }

  $: coefficients = roots.length > 0 ? rootsToCoefficients(roots) : []
  $: equationStr = formatEquation(coefficients, degree)

  const formatEquation = (coeffs: number[], deg: number): string => {
    if (coeffs.length === 0) return ''
    const terms = coeffs.map((c, i) => {
      const power = deg - i
      const coeff = Math.abs(c) < 0.01 ? 0 : c
      if (Math.abs(coeff) < 0.01 && power > 0) return ''
      const sign = coeff >= 0 ? (i === 0 ? '' : ' + ') : ' - '
      const absCoeff = Math.abs(coeff).toFixed(2).replace(/\.?0+$/, '')
      if (power === 0) return `${sign}${absCoeff}`
      if (power === 1) return `${sign}${absCoeff === '1' ? '' : absCoeff}x`
      return `${sign}${absCoeff === '1' ? '' : absCoeff}x${superscript(power)}`
    }).filter(Boolean)
    return terms.join('') + ' = 0'
  }

  const superscript = (n: number): string => {
    const map: Record<string, string> = { '2': '²', '3': '³', '4': '⁴' }
    return map[String(n)] || `^${n}`
  }
</script>

<div class="equation-panel">
  <div class="degree-selector">
    <span class="label">方程次数：</span>
    {#each degreeOptions as d}
      <button
        class="degree-btn"
        class:active={degree === d}
        on:click={() => dispatch('degreeChange', d)}
      >
        {d}次
      </button>
    {/each}
  </div>

  <div class="actions">
    <button class="action-btn" on:click={handleUnitRoots}>
      单位根 (x{superscript(degree)} = 1)
    </button>
    <button class="action-btn" on:click={handleRandom}>
      随机方程
    </button>
  </div>

  {#if equationStr}
    <div class="equation-display">
      <div class="equation">{equationStr}</div>
      <div class="roots">
        根：{roots.map((r, i) => `r${i + 1} = ${format(r, 2)}`).join(', ')}
      </div>
    </div>
  {/if}
</div>

<style>
  .equation-panel {
    background: #16213e;
    border-radius: 8px;
    padding: 16px;
    color: #eee;
  }

  .degree-selector {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 16px;
  }

  .label {
    color: #888;
  }

  .degree-btn {
    padding: 8px 16px;
    border: 1px solid #444;
    background: transparent;
    color: #eee;
    border-radius: 4px;
    cursor: pointer;
    transition: all 0.2s;
  }

  .degree-btn:hover {
    border-color: #666;
  }

  .degree-btn.active {
    background: #3498db;
    border-color: #3498db;
  }

  .actions {
    display: flex;
    gap: 8px;
    margin-bottom: 16px;
  }

  .action-btn {
    padding: 10px 20px;
    background: #2ecc71;
    border: none;
    border-radius: 4px;
    color: white;
    font-weight: bold;
    cursor: pointer;
    transition: background 0.2s;
  }

  .action-btn:hover {
    background: #27ae60;
  }

  .equation-display {
    background: rgba(0, 0, 0, 0.2);
    padding: 12px;
    border-radius: 4px;
  }

  .equation {
    font-size: 18px;
    font-family: 'Times New Roman', serif;
    margin-bottom: 8px;
  }

  .roots {
    font-size: 12px;
    color: #888;
  }
</style>
