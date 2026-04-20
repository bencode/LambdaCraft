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
    base: '../../../../brain2/curriculum',
  }),
  schema: z
    .object({
      title: z.string().optional(),
      date: z.coerce.date().optional(),
      summary: z.string().optional(),
    })
    .passthrough(),
})

const irRagCourse = defineCollection({
  // 源在 brain2，这里只做 render + publish
  // .mdx = 对外发布（hub / chapter），.md = brain2 内部（README / sources / outline / reviews）不进发布
  loader: glob({
    pattern: '**/*.mdx',
    base: '../../../../brain2/learning-paths/ir-rag-series',
  }),
  schema: z
    .object({
      title: z.string().optional(),
      date: z.coerce.date().optional(),
      summary: z.string().optional(),
    })
    .passthrough(),
})

export const collections = { posts, reading, 'ir-rag-course': irRagCourse }
