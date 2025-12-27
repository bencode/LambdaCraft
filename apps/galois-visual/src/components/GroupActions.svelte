<script lang="ts">
  import { createEventDispatcher } from 'svelte'
  import { getActions } from '../lib/math'
  import type { GroupAction, Permutation } from '../lib/math'

  export let degree: 2 | 3 | 4 = 4

  const dispatch = createEventDispatcher<{
    apply: { perm: Permutation }
  }>()

  $: actions = getActions(degree)

  const handleAction = (action: GroupAction) => {
    dispatch('apply', { perm: action.perm })
  }

  const groupName = (d: number): string => {
    return `S${['', '', '₂', '₃', '₄'][d]}`
  }
</script>

<div class="bg-white dark:bg-slate-800 rounded-xl p-5 border border-gray-200 dark:border-slate-700 shadow-sm">
  <div class="flex justify-between items-center mb-4 pb-3 border-b border-gray-200 dark:border-slate-700">
    <span class="text-2xl font-serif font-bold">{groupName(degree)}</span>
    <span class="text-sm text-gray-500 dark:text-gray-400">|G| = {actions.length}</span>
  </div>

  <div class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-2">
    {#each actions as action}
      <button
        class="flex flex-col items-center p-3 rounded-lg border border-gray-200 dark:border-slate-600
          bg-gray-50 dark:bg-slate-700/50 hover:border-blue-400 hover:bg-blue-50 dark:hover:bg-blue-900/20
          transition-colors"
        on:click={() => handleAction(action)}
        title={action.description}
      >
        <span class="text-lg font-serif font-bold">{action.label}</span>
        {#if action.description}
          <span class="text-xs text-gray-500 dark:text-gray-400 mt-1">{action.description}</span>
        {/if}
      </button>
    {/each}
  </div>
</div>
