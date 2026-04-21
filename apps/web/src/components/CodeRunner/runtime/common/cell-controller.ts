// Per-cell controller: glues editor, kernel, and output rendering.

import { getKernel, isLang } from '../registry'
import { createEditor, type EditorHandle } from './editor'
import { renderResult, renderStatus, clearOutput } from './output'

type CellState = 'idle' | 'running' | 'error'

type CellEls = {
  editorContainer: HTMLElement
  outputContainer: HTMLElement
  runBtn: HTMLButtonElement
  statusEl: HTMLElement
}

function queryCellEls(root: HTMLElement): CellEls | null {
  const editorContainer = root.querySelector<HTMLElement>('.editor-container')
  const outputContainer = root.querySelector<HTMLElement>('.output-container')
  const runBtn = root.querySelector<HTMLButtonElement>('[data-action="run"]')
  const statusEl = root.querySelector<HTMLElement>('.cell-status')
  if (!editorContainer || !outputContainer || !runBtn || !statusEl) return null
  return { editorContainer, outputContainer, runBtn, statusEl }
}

function setCellState(
  statusEl: HTMLElement,
  state: CellState,
  message?: string,
): void {
  statusEl.dataset.state = state
  if (state === 'running') {
    statusEl.textContent = message ?? '运行中...'
  } else if (state === 'error') {
    statusEl.textContent = message ?? '出错'
  } else {
    statusEl.textContent = ''
  }
}

export function initCell(root: HTMLElement): void {
  const els = queryCellEls(root)
  if (!els) {
    console.error('CodeRunner: missing required elements', root)
    return
  }

  const langRaw = root.dataset.lang ?? 'python'
  if (!isLang(langRaw)) {
    console.error('CodeRunner: unknown lang', langRaw)
    return
  }

  const initialCode = root.dataset.initialCode ?? ''
  const kernel = getKernel(langRaw)

  const editor: EditorHandle = createEditor(
    els.editorContainer,
    initialCode,
    langRaw,
    () => {
      void runCell()
    },
  )

  async function runCell(): Promise<void> {
    const code = editor.getValue()
    if (!code.trim()) return

    els!.runBtn.disabled = true
    const previousLabel = els!.runBtn.textContent
    els!.runBtn.textContent = '运行中...'
    setCellState(els!.statusEl, 'running')
    clearOutput(els!.outputContainer)

    try {
      const result = await kernel.run(code)
      renderResult(els!.outputContainer, result)
      setCellState(els!.statusEl, result.error ? 'error' : 'idle')
    } catch (err) {
      renderStatus(els!.outputContainer, `运行失败: ${String(err)}`, 'error')
      setCellState(els!.statusEl, 'error')
    } finally {
      els!.runBtn.disabled = false
      els!.runBtn.textContent = previousLabel
    }
  }

  els.runBtn.addEventListener('click', () => void runCell())
}

export function initAllCells(): void {
  document.querySelectorAll<HTMLElement>('.coderunner-cell').forEach((el) => {
    if (el.dataset.hydrated === 'true') return
    el.dataset.hydrated = 'true'
    initCell(el)
  })
}
