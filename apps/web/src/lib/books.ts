export type Book = {
  id: string
  title: string
  subtitle?: string
  dirPrefix: string
}

export const books: readonly Book[] = [
  {
    id: 'ir',
    title: 'Introduction to Information Retrieval',
    subtitle: '《信息检索导论》读书笔记',
    dirPrefix: 'introduction-to-information-retrieval/chapters',
  },
]

export const getBook = (id: string): Book | undefined => books.find((b) => b.id === id)

export const bookOfEntry = (entryId: string): Book | undefined =>
  books.find((b) => entryId.startsWith(`${b.dirPrefix}/`))

export const slugInBook = (entryId: string, book: Book): string =>
  entryId.slice(book.dirPrefix.length + 1)

export const titleFromBody = (body: string | undefined, fallback: string): string => {
  if (!body) return fallback
  const match = body.match(/^#\s+(.+)$/m)
  return match ? match[1].trim() : fallback
}

export type ChapterParts = {
  num: number | null
  sub: number | null
  rest: string
}

export const parseChapterSlug = (slug: string): ChapterParts => {
  const m = slug.match(/^chapter-(\d+)(?:-(\d+))?-(.+)$/)
  if (!m) return { num: null, sub: null, rest: slug }
  return {
    num: Number.parseInt(m[1], 10),
    sub: m[2] ? Number.parseInt(m[2], 10) : null,
    rest: m[3],
  }
}

export const chapterLabel = (slug: string): string => {
  const p = parseChapterSlug(slug)
  if (p.num === null) return slug
  const n = String(p.num).padStart(2, '0')
  return p.sub !== null ? `${n}.${p.sub}` : n
}

export const readingTime = (body: string | undefined): number => {
  if (!body) return 1
  return Math.max(1, Math.round(body.length / 350))
}
