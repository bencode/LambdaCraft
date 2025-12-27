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

<div class="bg-white dark:bg-slate-800 rounded-xl p-5 border border-gray-200 dark:border-slate-700 shadow-sm">
  <div class="flex items-center gap-3 mb-4">
    <span class="text-gray-500 dark:text-gray-400 text-sm">方程次数</span>
    {#each degreeOptions as d}
      <button
        class="px-4 py-2 rounded-lg border transition-colors
          {degree === d
            ? 'bg-blue-500 border-blue-500 text-white'
            : 'border-gray-300 dark:border-slate-600 hover:border-blue-400'}"
        on:click={() => dispatch('degreeChange', d)}
      >
        {d}次
      </button>
    {/each}
  </div>

  <div class="flex gap-3 mb-4">
    <button
      class="flex-1 py-2.5 px-4 bg-emerald-500 hover:bg-emerald-600 text-white font-medium rounded-lg transition-colors"
      on:click={handleUnitRoots}
    >
      单位根 (x{superscript(degree)} = 1)
    </button>
    <button
      class="flex-1 py-2.5 px-4 bg-blue-500 hover:bg-blue-600 text-white font-medium rounded-lg transition-colors"
      on:click={handleRandom}
    >
      随机方程
    </button>
  </div>

  {#if equationStr}
    <div class="bg-gray-50 dark:bg-slate-700/50 p-4 rounded-lg">
      <div class="text-lg font-serif mb-2">{equationStr}</div>
      <div class="text-xs text-gray-500 dark:text-gray-400">
        根：{roots.map((r, i) => `r${i + 1} = ${format(r, 2)}`).join(', ')}
      </div>
    </div>
  {/if}
</div>
