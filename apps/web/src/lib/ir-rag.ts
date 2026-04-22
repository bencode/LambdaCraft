// ir-rag collection helpers.
// URL 约定：源文件路径即 URL 片段，`<dir>/index.mdx` 映射到 `/<dir>`。
//   'index'                            → /ir-rag             (top hub)
//   'bm25/index'                       → /ir-rag/bm25        (series hub)
//   'bm25/from-vsm-to-probability'     → /ir-rag/bm25/from-vsm-to-probability

import type { CollectionEntry } from 'astro:content'

export type IrRagEntry = CollectionEntry<'ir-rag'>

const MAX_ORDER = Number.MAX_SAFE_INTEGER

export const isTopHub = (entryId: string): boolean => entryId === 'index'

// Series hub = one-level-deep `<name>/index`.
export const isSeriesHub = (entryId: string): boolean => {
  const parts = entryId.split('/')
  return parts.length === 2 && parts[1] === 'index'
}

export const hrefOf = (entry: IrRagEntry): string => {
  if (isTopHub(entry.id)) return '/ir-rag'
  return `/ir-rag/${entry.id.replace(/\/index$/, '')}`
}

// First path segment — groups articles into series.
//   'bm25/index'                       → 'bm25'
//   'bm25/from-vsm-to-probability'     → 'bm25'
//   'index'                            → 'index'  (top-level; caller should skip)
export const seriesIdOf = (entry: IrRagEntry): string => entry.id.split('/')[0]

export const orderOf = (entry: IrRagEntry): number => entry.data.order ?? MAX_ORDER

export const compareByOrder = (a: IrRagEntry, b: IrRagEntry): number => {
  const left = orderOf(a)
  const right = orderOf(b)
  if (left !== right) return left - right
  const leftTitle = a.data.title ?? a.id
  const rightTitle = b.data.title ?? b.id
  return leftTitle.localeCompare(rightTitle, 'zh-CN')
}

export const formatOrderLabel = (order?: number): string | undefined => {
  if (order === undefined) return undefined
  return String(order).padStart(2, '0')
}
