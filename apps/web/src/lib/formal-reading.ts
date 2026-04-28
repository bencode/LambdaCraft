import {
  compareByOrder,
  formatOrderLabel,
  hrefOf,
  isSeriesHub,
  isTopHub,
  seriesIdOf,
  type IrRagEntry,
} from './ir-rag'

export type FormalArticle = {
  date?: Date
  href: string
  id: string
  label: string
  summary?: string
  title: string
}

export type FormalSeries = {
  articles: FormalArticle[]
  href: string
  id: string
  label?: string
  summary?: string
  title: string
}

export type RecentFormalArticle = {
  date?: Date
  href: string
  seriesId: string
  seriesTitle: string
  title: string
}

const collator = new Intl.Collator('zh-CN', { numeric: true, sensitivity: 'base' })

const compareIds = (a: string, b: string): number => collator.compare(a, b)

const recentSort = (a: RecentFormalArticle, b: RecentFormalArticle): number => {
  const left = a.date?.getTime() ?? 0
  const right = b.date?.getTime() ?? 0
  if (left !== right) return right - left
  if (a.seriesId !== b.seriesId) return compareIds(a.seriesId, b.seriesId)
  return compareIds(a.title, b.title)
}

export const extractFormalSeries = (entries: IrRagEntry[]): FormalSeries[] => {
  const hubs = new Map<string, IrRagEntry>()
  const grouped = new Map<string, IrRagEntry[]>()

  for (const entry of entries) {
    if (isTopHub(entry.id)) continue

    const seriesId = seriesIdOf(entry)
    if (isSeriesHub(entry.id)) {
      hubs.set(seriesId, entry)
      continue
    }

    if (entry.data.draft) continue

    const current = grouped.get(seriesId) ?? []
    current.push(entry)
    grouped.set(seriesId, current)
  }

  const seriesIds = [...new Set([...hubs.keys(), ...grouped.keys()])].sort((left, right) => {
    const leftHub = hubs.get(left)
    const rightHub = hubs.get(right)
    const leftOrder = leftHub?.data.order ?? Number.MAX_SAFE_INTEGER
    const rightOrder = rightHub?.data.order ?? Number.MAX_SAFE_INTEGER

    if (leftOrder !== rightOrder) return leftOrder - rightOrder
    return compareIds(left, right)
  })

  return seriesIds.map((seriesId) => {
    const hub = hubs.get(seriesId)
    const articles = (grouped.get(seriesId) ?? []).sort(compareByOrder).map((entry) => ({
      date: entry.data.date,
      href: hrefOf(entry),
      id: entry.id,
      label: formatOrderLabel(entry.data.order) ?? '·',
      summary: entry.data.summary,
      title: entry.data.title ?? entry.id,
    }))

    return {
      articles,
      href: hub ? hrefOf(hub) : `/ir-rag/${seriesId}`,
      id: seriesId,
      label: formatOrderLabel(hub?.data.order),
      summary: hub?.data.summary,
      title: hub?.data.title ?? seriesId,
    }
  })
}

export const extractRecentFormalArticles = (
  entries: IrRagEntry[],
  limit: number,
): RecentFormalArticle[] =>
  extractFormalSeries(entries)
    .flatMap((series) =>
      series.articles.map((article) => ({
        date: article.date,
        href: article.href,
        seriesId: series.id,
        seriesTitle: series.title,
        title: article.title,
      })),
    )
    .sort(recentSort)
    .slice(0, limit)
