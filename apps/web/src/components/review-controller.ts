// Dev-only review controller: Word-style right gutter for review notes.
//
// On load: fetch existing notes from sidecar → render markers (in-article)
// + cards (in gutter), align each card to its block's vertical position.
// On double-click: create a draft note + empty card; save on blur (if
// content) or cancel on Esc / blank blur.
//
// Visibility gated by viewport width (>= 1200px) via CSS; JS keeps running
// but the gutter/cards are hidden, so double-click still becomes a no-op
// visually (no card shows).

type BlockMeta = {
  el: HTMLElement
  index: number
  headingPath: string[]
  text: string
}

type ReviewPosition = {
  headingPath: string[]
  blockText: string
  blockIndex: number
}

type ReviewNote = {
  id: string
  createdAt: string
  position: ReviewPosition
  content: string
}

type NotesResponse = { notes: ReviewNote[] }
// Server may return { ok, note, sidecar } (current) or { ok, id, sidecar }
// (legacy). Tolerate both so a stale dev-server middleware doesn't trap the
// client into an infinite retry loop.
type SaveResponse = { ok: boolean; note?: ReviewNote; id?: string; sidecar: string; error?: string }

const EXCLUDE_SELECTOR = '.coderunner-cell, .coderunner-kernel-bar, pre, figure, .toc-toggle, .review-gutter, .review-marker'
const MIN_CARD_GAP = 12 // px between stacked cards

// ─── helpers ────────────────────────────────────────────────────────────

function normalize(text: string): string {
  return text.replace(/\s+/g, ' ').trim()
}

function pad2(n: number): string {
  return String(n).padStart(2, '0')
}

// Render a note's createdAt relative to "now":
//   today     → "14:23"
//   yesterday → "昨天 14:23"
//   same year → "04-22 14:23"
//   else      → "2025-12-31 14:23"
function formatStamp(iso: string): string {
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return iso
  const now = new Date()
  const hm = `${pad2(d.getHours())}:${pad2(d.getMinutes())}`

  const sameDay =
    d.getFullYear() === now.getFullYear() &&
    d.getMonth() === now.getMonth() &&
    d.getDate() === now.getDate()
  if (sameDay) return hm

  const yest = new Date(now)
  yest.setDate(yest.getDate() - 1)
  const isYest =
    d.getFullYear() === yest.getFullYear() &&
    d.getMonth() === yest.getMonth() &&
    d.getDate() === yest.getDate()
  if (isYest) return `昨天 ${hm}`

  const mmdd = `${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}`
  if (d.getFullYear() === now.getFullYear()) return `${mmdd} ${hm}`
  return `${d.getFullYear()}-${mmdd} ${hm}`
}

// Get the block's text content, stripping any review UI that we may have
// injected into it (markers) — otherwise headingPath / blockText get
// polluted by marker glyphs on subsequent collectBlocks calls.
function blockTextOf(el: HTMLElement): string {
  const clone = el.cloneNode(true) as HTMLElement
  clone.querySelectorAll('.review-marker').forEach((n) => n.remove())
  return normalize(clone.textContent ?? '')
}

// Content blocks we care about: paragraphs, headings, lists, blockquotes,
// tables, figures. Skip inline scripts, injected style tags, and our own
// gutter container.
const BLOCK_TAGS = new Set(['p', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6', 'ul', 'ol', 'blockquote', 'pre', 'table', 'figure', 'div', 'hr'])

function collectBlocks(article: HTMLElement): BlockMeta[] {
  const children = Array.from(article.children) as HTMLElement[]
  const stack: string[] = []
  const blocks: BlockMeta[] = []
  children.forEach((el) => {
    const tag = el.tagName.toLowerCase()
    if (!BLOCK_TAGS.has(tag)) return
    if (el.classList.contains('review-gutter')) return
    if (/^h[1-6]$/.test(tag)) {
      const level = Number.parseInt(tag.slice(1), 10)
      stack.splice(level - 1)
      stack[level - 1] = blockTextOf(el)
    }
    blocks.push({
      el,
      index: blocks.length,
      headingPath: stack.filter(Boolean),
      text: blockTextOf(el),
    })
  })
  return blocks
}

// Find the best-matching block for a stored position. Multi-level fallback
// mirrors increa-reader's findBestMarkdownBlock.
function findBlock(blocks: BlockMeta[], pos: ReviewPosition): BlockMeta | null {
  const target = pos.blockText.trim()
  const scoped = pos.headingPath.length > 0
    ? blocks.filter((b) => arraysEqual(b.headingPath, pos.headingPath))
    : blocks

  if (target) {
    const exact = scoped.find((b) => b.text === target)
    if (exact) return exact
    const partial = scoped.find((b) => b.text.includes(target) || target.includes(b.text))
    if (partial) return partial
    const global = blocks.find((b) => b.text === target || b.text.includes(target) || target.includes(b.text))
    if (global) return global
  }

  if (blocks[pos.blockIndex]) return blocks[pos.blockIndex]
  return null
}

function arraysEqual(a: string[], b: string[]): boolean {
  if (a.length !== b.length) return false
  return a.every((v, i) => v === b[i])
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

// ─── DOM creation ───────────────────────────────────────────────────────

type NoteRecord = {
  note: ReviewNote | null // null = unsaved draft
  markerEl: HTMLElement
  cardEl: HTMLElement
  anchorEl: HTMLElement
  isDraft: boolean
}

function createMarker(noteId: string): HTMLElement {
  const m = document.createElement('button')
  m.type = 'button'
  m.className = 'review-marker'
  m.dataset.noteId = noteId
  m.setAttribute('aria-label', '跳到批注')
  m.textContent = '•'
  return m
}

function createCard(note: ReviewNote | null, opts: { draft: boolean }): HTMLElement {
  const card = document.createElement('div')
  card.className = 'review-card'
  if (opts.draft) card.classList.add('review-card-draft')
  card.dataset.noteId = note?.id ?? 'draft'

  if (opts.draft) {
    const ta = document.createElement('textarea')
    ta.className = 'review-card-textarea'
    ta.placeholder = '写批注（⌘/Ctrl+Enter 保存，Esc 取消）'
    ta.rows = 4
    card.appendChild(ta)
  } else if (note) {
    const body = document.createElement('div')
    body.className = 'review-card-body'
    body.textContent = note.content
    const meta = document.createElement('div')
    meta.className = 'review-card-meta'
    meta.textContent = formatStamp(note.createdAt)
    card.appendChild(body)
    card.appendChild(meta)
  }

  return card
}

// ─── main controller ────────────────────────────────────────────────────

async function fetchNotes(file: string): Promise<ReviewNote[]> {
  const res = await fetch(`/__review?file=${encodeURIComponent(file)}`)
  if (!res.ok) return []
  const data = (await res.json()) as NotesResponse
  return data.notes ?? []
}

async function saveNote(
  file: string,
  position: ReviewPosition,
  content: string,
): Promise<ReviewNote | null> {
  const res = await fetch('/__review', {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify({ file, position, content }),
  })
  const data = (await res.json()) as SaveResponse
  if (!res.ok || !data.ok) {
    console.error('[review] save failed', data)
    return null
  }
  if (data.note) return data.note
  // Legacy middleware shape — synthesize a note from what we sent plus the
  // server-assigned id.
  if (data.id) {
    return { id: data.id, createdAt: new Date().toISOString(), position, content }
  }
  console.error('[review] save returned no note / id', data)
  return null
}

function initForArticle(article: HTMLElement): void {
  const file = sourceFileOf(article)
  if (!file) return

  const gutter = document.createElement('div')
  gutter.className = 'review-gutter'
  document.body.appendChild(gutter)

  const records: NoteRecord[] = []

  const layoutCards = (): void => {
    // Cards use fixed positioning via the gutter (.review-gutter is fixed),
    // so top is in viewport coordinates. Sort by anchor top and stack.
    const sorted = [...records].sort((a, b) => {
      const ay = a.anchorEl.getBoundingClientRect().top
      const by = b.anchorEl.getBoundingClientRect().top
      return ay - by
    })
    let prevBottom = -Infinity
    for (const rec of sorted) {
      const r = rec.anchorEl.getBoundingClientRect()
      const top = Math.max(r.top, prevBottom + MIN_CARD_GAP)
      rec.cardEl.style.top = `${top}px`
      prevBottom = top + rec.cardEl.offsetHeight
    }
  }

  const attachLayoutWatchers = (): void => {
    const ro = new ResizeObserver(() => layoutCards())
    ro.observe(article)
    window.addEventListener('resize', layoutCards, { passive: true })
    // Cards are fixed to viewport; re-align on scroll.
    window.addEventListener('scroll', layoutCards, { passive: true })
  }

  const setActive = (noteId: string | null): void => {
    for (const rec of records) {
      const active = rec.markerEl.dataset.noteId === noteId
      rec.markerEl.classList.toggle('review-marker-active', active)
      rec.cardEl.classList.toggle('review-card-active', active)
    }
  }

  const removeRecord = (rec: NoteRecord): void => {
    rec.markerEl.remove()
    rec.cardEl.remove()
    const idx = records.indexOf(rec)
    if (idx !== -1) records.splice(idx, 1)
    layoutCards()
  }

  const addRecord = (
    note: ReviewNote | null,
    anchorEl: HTMLElement,
    opts: { draft: boolean },
  ): NoteRecord => {
    const id = note?.id ?? `draft-${Date.now()}`
    const marker = createMarker(id)
    const card = createCard(note, opts)

    // Insert marker at end of anchor block (inline-block tiny bullet)
    anchorEl.appendChild(marker)
    gutter.appendChild(card)

    const rec: NoteRecord = { note, markerEl: marker, cardEl: card, anchorEl, isDraft: opts.draft }
    records.push(rec)

    marker.addEventListener('click', (e) => {
      e.stopPropagation()
      setActive(id)
      card.scrollIntoView({ block: 'center', behavior: 'smooth' })
    })
    card.addEventListener('mouseenter', () => setActive(id))
    card.addEventListener('mouseleave', () => setActive(null))

    return rec
  }

  const createDraftAt = (block: BlockMeta): void => {
    // One draft at a time (save / cancel previous first)
    const existingDraft = records.find((r) => r.isDraft)
    if (existingDraft) removeRecord(existingDraft)

    const rec = addRecord(null, block.el, { draft: true })
    const ta = rec.cardEl.querySelector<HTMLTextAreaElement>('.review-card-textarea')
    if (!ta) return

    layoutCards()
    setTimeout(() => ta.focus(), 0)

    // `committing` gates concurrent commit attempts. Ctrl+Enter and blur can
    // both fire (Ctrl+Enter disables the textarea, which itself triggers
    // blur); without this gate the second path would read an orphaned ta
    // after the first path empties the card, and we'd end up saving twice
    // then removing the record.
    let committing = false

    const commit = async (): Promise<void> => {
      if (committing) return
      const content = ta.value.trim()
      if (!content) {
        removeRecord(rec)
        return
      }
      committing = true
      ta.disabled = true
      const note = await saveNote(file, {
        headingPath: block.headingPath,
        blockText: block.text,
        blockIndex: block.index,
      }, content)
      if (!note) {
        committing = false
        ta.disabled = false
        ta.focus()
        return
      }
      rec.note = note
      rec.isDraft = false
      rec.markerEl.dataset.noteId = note.id
      rec.cardEl.dataset.noteId = note.id
      rec.cardEl.classList.remove('review-card-draft')
      rec.cardEl.innerHTML = ''
      const body = document.createElement('div')
      body.className = 'review-card-body'
      body.textContent = note.content
      const meta = document.createElement('div')
      meta.className = 'review-card-meta'
      meta.textContent = formatStamp(note.createdAt)
      rec.cardEl.appendChild(body)
      rec.cardEl.appendChild(meta)
      layoutCards()
    }

    const cancel = (): void => {
      if (committing) return
      removeRecord(rec)
    }

    ta.addEventListener('keydown', (e) => {
      if (e.key === 'Escape') {
        e.preventDefault()
        cancel()
        return
      }
      if ((e.metaKey || e.ctrlKey) && e.key === 'Enter') {
        e.preventDefault()
        void commit()
      }
    })
    ta.addEventListener('blur', () => {
      // Wait a tick in case focus is moving between card controls.
      setTimeout(() => {
        if (committing) return
        if (!document.body.contains(rec.cardEl)) return
        if (rec.isDraft) void commit()
      }, 100)
    })
  }

  // Load existing notes
  void fetchNotes(file).then((notes) => {
    if (notes.length === 0) return
    const blocks = collectBlocks(article)
    for (const note of notes) {
      const block = findBlock(blocks, note.position)
      if (!block) {
        console.warn('[review] unresolved note', note)
        continue
      }
      addRecord(note, block.el, { draft: false })
    }
    layoutCards()
  })

  attachLayoutWatchers()

  // Double-click to create draft
  article.addEventListener('dblclick', (event) => {
    const target = event.target as Element | null
    if (!target) return
    if (target.closest(EXCLUDE_SELECTOR)) return

    const blocks = collectBlocks(article)
    const block = findBlockForTarget(blocks, target)
    if (!block) return
    createDraftAt(block)
  })
}

export function initReview(): void {
  const articles = document.querySelectorAll<HTMLElement>('article[data-source-file]')
  articles.forEach((article) => initForArticle(article))
}
