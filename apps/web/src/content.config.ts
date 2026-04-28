import { defineCollection, z } from 'astro:content'
import { glob } from 'astro/loaders'

const posts = defineCollection({
  loader: glob({ pattern: '**/*.{md,mdx}', base: './src/content/posts' }),
  schema: z.object({
    title: z.string(),
    date: z.coerce.date(),
    summary: z.string().optional(),
    tags: z.array(z.string()).default([]),
    draft: z.boolean().default(false),
  }),
})

const reading = defineCollection({
  loader: glob({
    pattern: '**/chapters/*.md',
    base: './src/content/reading',
  }),
  schema: z
    .object({
      title: z.string().optional(),
      date: z.coerce.date().optional(),
      summary: z.string().optional(),
    })
    .passthrough(),
})

// URL 稳定性约定（convention over configuration）：
// 源文件路径即 URL 片段。<dir>/index.mdx → /<dir>；其余文件直接按路径映射。
// 发布后不要 rename 源文件；列表排序用 frontmatter `order`（可选）。
const irRag = defineCollection({
  loader: glob({
    pattern: '**/*.mdx',
    base: './src/content/ir-rag',
  }),
  schema: z
    .object({
      title: z.string().optional(),
      date: z.coerce.date().optional(),
      summary: z.string().optional(),
      order: z.number().int().nonnegative().optional(),
      draft: z.boolean().default(false),
    })
    .passthrough(),
})

export const collections = { posts, reading, 'ir-rag': irRag }
