<script lang="ts">
  import Router from 'svelte-spa-router'
  import { link } from 'svelte-spa-router'
  import HomePage from './components/HomePage.svelte'
  import FormulasPage from './components/FormulasPage.svelte'

  let darkMode = false

  const routes = {
    '/': HomePage,
    '/formulas': FormulasPage
  }

  $: if (typeof document !== 'undefined') {
    document.documentElement.classList.toggle('dark', darkMode)
  }
</script>

<div class="min-h-screen bg-gray-100 dark:bg-slate-900 text-gray-800 dark:text-gray-100 transition-colors">
  <nav class="flex items-center justify-between px-6 py-4 border-b border-gray-200 dark:border-slate-700">
    <a href="/" use:link class="text-xl font-bold hover:text-blue-600 dark:hover:text-blue-400">
      伽罗瓦理论可视化
    </a>
    <div class="flex items-center gap-4">
      <a href="/formulas" use:link class="text-sm hover:text-blue-600 dark:hover:text-blue-400">
        📐 求根公式
      </a>
      <button
        class="w-10 h-10 rounded-lg border border-gray-300 dark:border-slate-600 bg-white dark:bg-slate-800 hover:border-blue-500 transition-colors"
        on:click={() => darkMode = !darkMode}
      >
        {darkMode ? '☀️' : '🌙'}
      </button>
    </div>
  </nav>

  <main class="p-6">
    <Router {routes} />
  </main>
</div>

<style>
  :global(body) {
    margin: 0;
  }
</style>
