// Sidecar review file (`.review.md`) format: serialize one note into a
// Markdown section, append to (or create) the target file.
//
// Format — see plans/python-scheme-clojure-typescript-structured-widget.md.

import { promises as fs } from 'node:fs'

export type ReviewPosition = {
  headingPath: string[]
  blockText: string
  blockIndex: number
}

export type ReviewNote = {
  id: string
  createdAt: string
  position: ReviewPosition
  content: string
}

const HEADER_COMMENT = `<!-- Auto-generated & edited by dev review UI. Commit to preserve history.
     AI: 处理完一条后，Edit 掉对应 ## 段落（保留其他）。如果全部处理完，删除本文件。 -->
`

export function newNoteId(): string {
  const ts = Math.floor(Date.now() / 1000)
  const rand = Math.random().toString(36).slice(2, 7)
  return `n-${ts}-${rand}`
}

function escapeBlockText(text: string): string {
  // Keep the one-line preview readable: squash whitespace, cap length.
  const squashed = text.replace(/\s+/g, ' ').trim()
  return squashed.length > 200 ? `${squashed.slice(0, 200)}…` : squashed
}

function localStamp(iso: string): string {
  // Format createdAt as "YYYY-MM-DD HH:MM" in local time. We keep ISO in the
  // note object for full precision; the sidecar heading is human-readable.
  const d = new Date(iso)
  const pad = (n: number): string => String(n).padStart(2, '0')
  const date = `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
  const time = `${pad(d.getHours())}:${pad(d.getMinutes())}`
  return `${date} ${time}`
}

export function serializeNote(note: ReviewNote): string {
  const stamp = localStamp(note.createdAt)
  const sectionLine =
    note.position.headingPath.length > 0 ? note.position.headingPath.join(' › ') : '(top)'
  return [
    `## [${note.id}] ${stamp}`,
    '',
    `**Section:** ${sectionLine}`,
    `**Block #${note.position.blockIndex}:** ${escapeBlockText(note.position.blockText)}`,
    '',
    note.content.trim(),
    '',
    '---',
    '',
    '',
  ].join('\n')
}

export function initialSidecarHeader(slug: string): string {
  return `# Review · ${slug}\n\n${HEADER_COMMENT}\n`
}

export async function appendNoteToSidecar(
  sidecarPath: string,
  slug: string,
  note: ReviewNote,
): Promise<void> {
  let existing = ''
  try {
    existing = await fs.readFile(sidecarPath, 'utf8')
  } catch (err) {
    if ((err as NodeJS.ErrnoException).code !== 'ENOENT') throw err
  }

  const body = existing.length > 0 ? existing : initialSidecarHeader(slug)
  const next = body.endsWith('\n') ? body : `${body}\n`
  await fs.writeFile(sidecarPath, `${next}${serializeNote(note)}`, 'utf8')
}

export function sidecarPathFor(mdxAbsPath: string): string {
  return mdxAbsPath.replace(/\.(mdx|md)$/, '.review.md')
}

export function slugFromPath(mdxAbsPath: string): string {
  const base = mdxAbsPath.split('/').pop() ?? 'article'
  return base.replace(/\.(mdx|md)$/, '')
}

// Reverse parser — extracts notes from an existing .review.md file.
// Resilient to minor whitespace variations; skips malformed entries.

// Accept both legacy (date only) and current (date + HH:MM) heading formats.
const ENTRY_HEADER = /^## \[(n-[^\]]+)\]\s+(\d{4}-\d{2}-\d{2})(?:\s+(\d{2}:\d{2}))?\s*$/
const SECTION_LINE = /^\*\*Section:\*\*\s*(.*)\s*$/
const BLOCK_LINE = /^\*\*Block #(\d+):\*\*\s*(.*)\s*$/

export function parseNotesFromSidecar(text: string): ReviewNote[] {
  const notes: ReviewNote[] = []
  const lines = text.split('\n')
  let i = 0

  while (i < lines.length) {
    const headerMatch = lines[i].match(ENTRY_HEADER)
    if (!headerMatch) {
      i += 1
      continue
    }
    const id = headerMatch[1]
    const date = headerMatch[2]
    const time = headerMatch[3] ?? '00:00'
    i += 1

    // Skip blank lines between header and fields
    while (i < lines.length && lines[i].trim() === '') i += 1

    const sectionMatch = lines[i]?.match(SECTION_LINE)
    if (!sectionMatch) continue
    const sectionRaw = sectionMatch[1].trim()
    i += 1

    const blockMatch = lines[i]?.match(BLOCK_LINE)
    if (!blockMatch) continue
    const blockIndex = Number.parseInt(blockMatch[1], 10)
    const blockText = blockMatch[2].trim().replace(/…$/, '')
    i += 1

    // Skip blank lines between fields and content
    while (i < lines.length && lines[i].trim() === '') i += 1

    // Collect content until `---` separator or next `## [` header
    const contentLines: string[] = []
    while (i < lines.length && lines[i].trim() !== '---' && !ENTRY_HEADER.test(lines[i])) {
      contentLines.push(lines[i])
      i += 1
    }

    const content = contentLines.join('\n').trim()
    if (!content) continue

    const headingPath =
      sectionRaw === '(top)' || sectionRaw === ''
        ? []
        : sectionRaw.split(' › ').map((s) => s.trim()).filter(Boolean)

    // Heading stamp is local-time; encode back to ISO preserving the local
    // offset so the client renders the same clock time it was written at.
    const local = new Date(`${date}T${time}:00`)
    notes.push({
      id,
      createdAt: Number.isNaN(local.getTime()) ? `${date}T${time}:00` : local.toISOString(),
      position: { headingPath, blockText, blockIndex },
      content,
    })

    // Consume `---` separator if present
    if (i < lines.length && lines[i].trim() === '---') i += 1
  }

  return notes
}

export async function readSidecar(sidecarPath: string): Promise<ReviewNote[]> {
  try {
    const text = await fs.readFile(sidecarPath, 'utf8')
    return parseNotesFromSidecar(text)
  } catch (err) {
    if ((err as NodeJS.ErrnoException).code === 'ENOENT') return []
    throw err
  }
}
