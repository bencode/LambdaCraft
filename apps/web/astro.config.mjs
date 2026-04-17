import { defineConfig } from 'astro/config'
import mdx from '@astrojs/mdx'
import remarkMath from 'remark-math'
import rehypeKatex from 'rehype-katex'

const remarkStripFirstH1 = () => (tree) => {
  const first = tree.children?.[0]
  if (first && first.type === 'heading' && first.depth === 1) {
    tree.children.shift()
  }
}

const shikiConfig = {
  theme: 'github-light-default',
  wrap: false,
}

export default defineConfig({
  site: 'https://lambdacraft.dev',
  integrations: [
    mdx({
      remarkPlugins: [remarkStripFirstH1, remarkMath],
      rehypePlugins: [rehypeKatex],
    }),
  ],
  markdown: {
    remarkPlugins: [remarkStripFirstH1, remarkMath],
    rehypePlugins: [rehypeKatex],
    shikiConfig,
  },
})
