// Alias → ESM URL mapping. In a cell you can write $import('ramda')
// or a full https:// URL.

const ESM_BASE = 'https://esm.sh'

export const registry: Record<string, string> = {
  ramda: `${ESM_BASE}/ramda`,
  remeda: `${ESM_BASE}/remeda`,
  lodash: `${ESM_BASE}/lodash-es`,
  'date-fns': `${ESM_BASE}/date-fns`,
  zod: `${ESM_BASE}/zod`,
  immer: `${ESM_BASE}/immer`,
}

const isUrl = (spec: string): boolean => /^https?:\/\//.test(spec)

export async function $import(spec: string): Promise<unknown> {
  if (isUrl(spec)) return import(/* @vite-ignore */ spec)
  const url = registry[spec]
  if (!url) {
    throw new Error(
      `[$import] unknown alias: "${spec}". 写完整 URL，或在 library-registry 里加映射。`,
    )
  }
  return import(/* @vite-ignore */ url)
}
