// Client-side review controller (dev-only). Double-click any block inside
// <article>, get a floating card to write a review note, save it to the
// sidecar `.review.md` via POST /__review.
//
// Loaded only by <ReviewLayer>, which itself is `import.meta.env.DEV`-gated
// in BaseLayout.

type BlockMeta = {
  el: HTMLElement
  index: number
  headingPath: string[]
  text: string
}

type ReviewPayload = {
  file: string
  position: {
    headingPath: string[]
    blockText: string
    blockIndex: number
  }
  content: string
}

const EXCLUDE_SELECTOR = '.coderunner-cell, .coderunner-kernel-bar, pre, figure, .toc-toggle'

function normalize(text: string): string {
  return text.replace(/\s+/g, ' ').trim()
}

function collectBlocks(article: HTMLElement): BlockMeta[] {
  const children = Array.from(article.children) as HTMLElement[]
  const stack: string[] = []
  const blocks: BlockMeta[] = []

  children.forEach((el, index) => {
    const tag = el.tagName.toLowerCase()
    if (/^h[1-6]$/.test(tag)) {
      const level = Number.parseInt(tag.slice(1), 10)
      stack.splice(level - 1)
      stack[level - 1] = normalize(el.textContent ?? '')
    }
    blocks.push({
      el,
      index,
      headingPath: stack.filter(Boolean),
      text: normalize(el.textContent ?? ''),
    })
  })

  return blocks
}

function findBlockForTarget(blocks: BlockMeta[], target: Element): BlockMeta | null {
  for (const b of blocks) {
    if (b.el === target || b.el.contains(target)) return b
  }
  return null
}

function sourceFileOf(article: HTMLElement): string | null {
  return article.dataset.sourceFile ?? null
}

// ─── floating card ──────────────────────────────────────────────────────

type CardCloseReason = 'save' | 'cancel'

function openCard(opts: {
  x: number
  y: number
  sectionLabel: string
  blockPreview: string
  onClose: (reason: CardCloseReason, content: string) => void
}): void {
  const overlay = document.createElement('div')
  overlay.className = 'review-card-overlay'

  const card = document.createElement('div')
  card.className = 'review-card'
  card.style.left = `${Math.min(opts.x, window.innerWidth - 360)}px`
  card.style.top = `${Math.min(opts.y, window.innerHeight - 260)}px`

  const header = document.createElement('div')
  header.className = 'review-card-header'
  header.innerHTML = `
    <div class="review-card-section">${escapeHtml(opts.sectionLabel)}</div>
    <div class="review-card-block">${escapeHtml(opts.blockPreview)}</div>
  `

  const textarea = document.createElement('textarea')
  textarea.className = 'review-card-textarea'
  textarea.placeholder = '写批注（⌘/Ctrl+Enter 保存，Esc 取消）'
  textarea.rows = 5

  const footer = document.createElement('div')
  footer.className = 'review-card-footer'

  const cancelBtn = document.createElement('button')
  cancelBtn.type = 'button'
  cancelBtn.className = 'review-card-btn review-card-btn-cancel'
  cancelBtn.textContent = '取消'

  const saveBtn = document.createElement('button')
  saveBtn.type = 'button'
  saveBtn.className = 'review-card-btn review-card-btn-save'
  saveBtn.textContent = '保存'

  footer.appendChild(cancelBtn)
  footer.appendChild(saveBtn)
  card.appendChild(header)
  card.appendChild(textarea)
  card.appendChild(footer)
  overlay.appendChild(card)
  document.body.appendChild(overlay)

  const close = (reason: CardCloseReason): void => {
    overlay.remove()
    document.removeEventListener('keydown', onKey)
    opts.onClose(reason, textarea.value.trim())
  }

  const onKey = (e: KeyboardEvent): void => {
    if (e.key === 'Escape') {
      e.preventDefault()
      close('cancel')
      return
    }
    if ((e.metaKey || e.ctrlKey) && e.key === 'Enter') {
      e.preventDefault()
      close('save')
    }
  }
  document.addEventListener('keydown', onKey)

  cancelBtn.addEventListener('click', () => close('cancel'))
  saveBtn.addEventListener('click', () => close('save'))
  overlay.addEventListener('click', (e) => {
    if (e.target === overlay) close('cancel')
  })

  setTimeout(() => textarea.focus(), 0)
}

function escapeHtml(text: string): string {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

// ─── toast ──────────────────────────────────────────────────────────────

function showToast(message: string, kind: 'ok' | 'error' = 'ok'): void {
  const t = document.createElement('div')
  t.className = `review-toast review-toast-${kind}`
  t.textContent = message
  document.body.appendChild(t)
  setTimeout(() => {
    t.classList.add('review-toast-hide')
    setTimeout(() => t.remove(), 300)
  }, 1800)
}

// ─── save ───────────────────────────────────────────────────────────────

async function postReview(payload: ReviewPayload): Promise<string> {
  const res = await fetch('/__review', {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify(payload),
  })
  const data = (await res.json()) as { sidecar?: string; error?: string }
  if (!res.ok || !data.sidecar) throw new Error(data.error ?? `HTTP ${res.status}`)
  return data.sidecar
}

// ─── bootstrap ──────────────────────────────────────────────────────────

export function initReview(): void {
  document.addEventListener('dblclick', (event) => {
    const target = event.target as Element | null
    if (!target) return
    // Ignore clicks inside code runners, pre, figures, toc-toggle.
    if (target.closest(EXCLUDE_SELECTOR)) return

    const article = target.closest('article') as HTMLElement | null
    if (!article) return

    // Ignore dblclick that falls on the article itself (not a child block).
    const file = sourceFileOf(article)
    if (!file) return

    const blocks = collectBlocks(article)
    const block = findBlockForTarget(blocks, target)
    if (!block) return

    const sectionLabel =
      block.headingPath.length > 0 ? block.headingPath.join(' › ') : '(top)'
    const blockPreview =
      block.text.length > 120 ? `#${block.index} ${block.text.slice(0, 120)}…` : `#${block.index} ${block.text}`

    openCard({
      x: event.clientX + 8,
      y: event.clientY + 8,
      sectionLabel,
      blockPreview,
      onClose: async (reason, content) => {
        if (reason !== 'save' || !content) return
        try {
          const sidecar = await postReview({
            file,
            position: {
              headingPath: block.headingPath,
              blockText: block.text,
              blockIndex: block.index,
            },
            content,
          })
          const base = sidecar.split('/').pop() ?? 'review.md'
          showToast(`已保存到 ${base}`)
        } catch (err) {
          console.error('[review] save failed', err)
          showToast(`保存失败: ${String(err)}`, 'error')
        }
      },
    })
  })
}
