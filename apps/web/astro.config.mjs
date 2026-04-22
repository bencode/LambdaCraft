import { defineConfig } from 'astro/config'
import mdx from '@astrojs/mdx'
import remarkMath from 'remark-math'
import rehypeKatex from 'rehype-katex'
import rehypeExternalLinks from 'rehype-external-links'
import { reviewSidecar } from './src/integrations/review-sidecar'

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

// 外链（http/https 且非本站）自动开新标签页
const externalLinksConfig = [
  rehypeExternalLinks,
  { target: '_blank', rel: ['noopener', 'noreferrer'] },
]

export default defineConfig({
  site: 'https://lambdacraft.dev',
  integrations: [
    mdx({
      remarkPlugins: [remarkStripFirstH1, remarkMath],
      rehypePlugins: [rehypeKatex, externalLinksConfig],
    }),
    reviewSidecar(),
  ],
  markdown: {
    remarkPlugins: [remarkStripFirstH1, remarkMath],
    rehypePlugins: [rehypeKatex, externalLinksConfig],
    shikiConfig,
  },
})
