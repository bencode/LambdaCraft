<script lang="ts">
  import ComplexPlane from './ComplexPlane.svelte'
  import EquationPanel from './EquationPanel.svelte'
  import GroupActions from './GroupActions.svelte'
  import { applyToRoots, unitRoots } from '../lib/math'
  import type { Complex, Permutation } from '../lib/math'

  let degree: 2 | 3 | 4 = 4
  let roots: Complex[] = unitRoots(4)
  let labels = ['r₁', 'r₂', 'r₃', 'r₄']

  const handleGenerate = (e: CustomEvent<{ roots: Complex[] }>) => {
    roots = e.detail.roots
    labels = roots.map((_, i) => `r${subscript(i + 1)}`)
  }

  const handleApply = (e: CustomEvent<{ perm: Permutation }>) => {
    roots = applyToRoots(e.detail.perm, roots)
    labels = applyToRoots(e.detail.perm, labels)
  }

  const subscript = (n: number): string => {
    const map: Record<number, string> = { 1: '₁', 2: '₂', 3: '₃', 4: '₄' }
    return map[n] || String(n)
  }

  const handleDegreeChange = (newDegree: 2 | 3 | 4) => {
    degree = newDegree
    roots = unitRoots(degree)
    labels = roots.map((_, i) => `r${subscript(i + 1)}`)
  }
</script>

<div class="max-w-5xl mx-auto">
  <p class="text-center text-gray-500 dark:text-gray-400 mb-6">
    探索多项式的根与对称群
  </p>

  <div class="flex flex-col lg:flex-row gap-6">
    <div class="flex-shrink-0">
      <ComplexPlane {roots} {labels} />
    </div>

    <div class="flex-1 flex flex-col gap-4">
      <EquationPanel {degree} {roots} on:generate={handleGenerate} on:degreeChange={e => handleDegreeChange(e.detail)} />
      <GroupActions {degree} on:apply={handleApply} />
    </div>
  </div>
</div>
