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

export function serializeNote(note: ReviewNote): string {
  const date = note.createdAt.slice(0, 10)
  const sectionLine =
    note.position.headingPath.length > 0 ? note.position.headingPath.join(' › ') : '(top)'
  return [
    `## [${note.id}] ${date}`,
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
