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

// Convert ```mermaid fenced blocks into raw <pre class="mermaid">…</pre>
// so Shiki skips them and the client-side mermaid script can render
// them in place. Runs before Shiki since it's a remark (mdast) plugin.
const remarkMermaid = () => (tree) => {
  const escape = (s) =>
    s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
  const walk = (node) => {
    if (!node.children) return
    for (let i = 0; i < node.children.length; i += 1) {
      const child = node.children[i]
      if (child.type === 'code' && child.lang === 'mermaid') {
        node.children[i] = {
          type: 'html',
          value: `<pre class="mermaid">${escape(child.value)}</pre>`,
        }
      } else {
        walk(child)
      }
    }
  }
  walk(tree)
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
      remarkPlugins: [remarkStripFirstH1, remarkMermaid, remarkMath],
      rehypePlugins: [rehypeKatex, externalLinksConfig],
    }),
    reviewSidecar(),
  ],
  markdown: {
    remarkPlugins: [remarkStripFirstH1, remarkMermaid, remarkMath],
    rehypePlugins: [rehypeKatex, externalLinksConfig],
    shikiConfig,
  },
})
