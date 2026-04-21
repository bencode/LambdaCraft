// Per-language kernel registry. Each language lazily constructs a page-level
// singleton on first request (so all cells in the same page share kernel state).

import type { Kernel, Lang } from './common/types'
import { getPythonKernel } from './python/kernel'
import { getSchemeKernel } from './scheme/kernel'
import { getClojureKernel } from './clojure/kernel'
import { getTypeScriptKernel } from './typescript/kernel'

type KernelFactory = () => Kernel

const factories: Record<Lang, KernelFactory> = {
  python: getPythonKernel,
  scheme: getSchemeKernel,
  clojure: getClojureKernel,
  typescript: getTypeScriptKernel,
}

export function getKernel(lang: Lang): Kernel {
  const factory = factories[lang]
  if (!factory) throw new Error(`unknown kernel lang: ${lang}`)
  return factory()
}

export function isLang(value: string): value is Lang {
  return value === 'python' || value === 'scheme' || value === 'clojure' || value === 'typescript'
}
