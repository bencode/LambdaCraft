<script lang="ts">
  import ComplexPlane from './components/ComplexPlane.svelte'
  import EquationPanel from './components/EquationPanel.svelte'
  import GroupActions from './components/GroupActions.svelte'
  import { applyToRoots, unitRoots } from './lib/math'
  import type { Complex, Permutation } from './lib/math'

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

<main>
  <header>
    <h1>伽罗瓦理论可视化</h1>
    <p class="subtitle">探索多项式的根与对称群</p>
  </header>

  <div class="container">
    <div class="left-panel">
      <ComplexPlane {roots} {labels} />
    </div>

    <div class="right-panel">
      <EquationPanel {degree} {roots} on:generate={handleGenerate} on:degreeChange={e => handleDegreeChange(e.detail)} />
      <GroupActions {degree} on:apply={handleApply} />
    </div>
  </div>
</main>

<style>
  :global(body) {
    margin: 0;
    background: #0f0f23;
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  }

  main {
    min-height: 100vh;
    padding: 20px;
    box-sizing: border-box;
  }

  header {
    text-align: center;
    margin-bottom: 24px;
  }

  h1 {
    color: #eee;
    margin: 0;
    font-size: 28px;
  }

  .subtitle {
    color: #666;
    margin: 8px 0 0;
  }

  .container {
    display: flex;
    gap: 24px;
    max-width: 900px;
    margin: 0 auto;
  }

  .left-panel {
    flex-shrink: 0;
  }

  .right-panel {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 16px;
  }

  @media (max-width: 768px) {
    .container {
      flex-direction: column;
    }
  }
</style>
