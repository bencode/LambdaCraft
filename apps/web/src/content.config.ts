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

export const collections = { posts, reading }
