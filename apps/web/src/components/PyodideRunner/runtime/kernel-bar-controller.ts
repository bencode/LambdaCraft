// Page-level KernelBar controller.
// Subscribes to kernel init lifecycle + package-load events, owns the reset button.

import { getKernel } from './kernel'

type BarEls = {
  statusEl: HTMLElement
  resetBtn: HTMLButtonElement
}

function queryBarEls(root: HTMLElement): BarEls | null {
  const statusEl = root.querySelector<HTMLElement>('.kernel-bar-status')
  const resetBtn = root.querySelector<HTMLButtonElement>('[data-action="reset-kernel"]')
  if (!statusEl || !resetBtn) return null
  return { statusEl, resetBtn }
}

export function initKernelBar(root: HTMLElement): void {
  const els = queryBarEls(root)
  if (!els) {
    console.error('PyodideKernelBar: missing required elements', root)
    return
  }

  const kernel = getKernel()

  // Track current init/package state so we render the higher-priority message.
  // init issues take precedence over package-load chatter.
  let initLabel = ''
  let initState: 'idle' | 'initializing' | 'ready' | 'error' = 'idle'
  let packageMsg: string | null = null

  function render(): void {
    if (initState === 'initializing') {
      els!.statusEl.textContent = initLabel || '正在启动 Python 环境...'
      els!.statusEl.dataset.state = 'loading'
      els!.resetBtn.disabled = true
      return
    }
    if (initState === 'error') {
      els!.statusEl.textContent = initLabel || '内核启动失败'
      els!.statusEl.dataset.state = 'error'
      els!.resetBtn.disabled = true
      return
    }
    if (packageMsg) {
      els!.statusEl.textContent = packageMsg
      els!.statusEl.dataset.state = 'loading'
      els!.resetBtn.disabled = true
      return
    }
    if (initState === 'ready') {
      els!.statusEl.textContent = '就绪'
      els!.statusEl.dataset.state = 'ready'
      els!.resetBtn.disabled = false
      return
    }
    els!.statusEl.textContent = '尚未启动（点任意 cell 的 ▶ 即可启动）'
    els!.statusEl.dataset.state = 'idle'
    els!.resetBtn.disabled = true
  }

  kernel.subscribe((state, message) => {
    initState = state
    initLabel = message ?? ''
    render()
  })

  kernel.subscribePackageLoad((msg) => {
    packageMsg = msg
    render()
  })

  els.resetBtn.addEventListener('click', async () => {
    if (!els) return
    els.resetBtn.disabled = true
    const original = els.resetBtn.textContent
    els.resetBtn.textContent = '重置中...'
    try {
      await kernel.reset()
      els.resetBtn.textContent = '已重置 ✓'
      setTimeout(() => {
        els!.resetBtn.textContent = original
        els!.resetBtn.disabled = false
      }, 1200)
    } catch {
      els.resetBtn.textContent = original
      els.resetBtn.disabled = false
    }
  })
}

export function initAllKernelBars(): void {
  document.querySelectorAll<HTMLElement>('.pyrunner-kernel-bar').forEach((el) => {
    if (el.dataset.hydrated === 'true') return
    el.dataset.hydrated = 'true'
    initKernelBar(el)
  })
}
