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

<div class="group-actions">
  <div class="header">
    <span class="group-name">{groupName(degree)}</span>
    <span class="group-size">|G| = {actions.length}</span>
  </div>

  <div class="actions">
    {#each actions as action}
      <button
        class="action-btn"
        on:click={() => handleAction(action)}
        title={action.description}
      >
        <span class="label">{action.label}</span>
        {#if action.description}
          <span class="desc">{action.description}</span>
        {/if}
      </button>
    {/each}
  </div>
</div>

<style>
  .group-actions {
    background: #16213e;
    border-radius: 8px;
    padding: 16px;
    color: #eee;
  }

  .header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
    padding-bottom: 8px;
    border-bottom: 1px solid #333;
  }

  .group-name {
    font-size: 24px;
    font-weight: bold;
    font-family: 'Times New Roman', serif;
  }

  .group-size {
    color: #888;
    font-size: 14px;
  }

  .actions {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
    gap: 8px;
  }

  .action-btn {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 12px 8px;
    background: rgba(255, 255, 255, 0.05);
    border: 1px solid #333;
    border-radius: 4px;
    color: #eee;
    cursor: pointer;
    transition: all 0.2s;
  }

  .action-btn:hover {
    background: rgba(52, 152, 219, 0.2);
    border-color: #3498db;
  }

  .action-btn .label {
    font-size: 18px;
    font-family: 'Times New Roman', serif;
    font-weight: bold;
  }

  .action-btn .desc {
    font-size: 11px;
    color: #666;
    margin-top: 4px;
  }
</style>
