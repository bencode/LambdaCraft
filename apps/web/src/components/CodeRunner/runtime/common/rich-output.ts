// Dispatch a JS runtime value into the appropriate CellResult field.
//
// Check order (highest precedence first):
//   1. DOM Element / SVGElement       → value_html (outerHTML)
//   2. Object with toHtml(): string   → value_html (method return value)
//   3. Fallback                        → value_repr (stringified)
//
// Extending with a new output kind (e.g. mount/cleanup for live rendering,
// toPng for base64 images):
//   (1) Add a new field to CellResult in common/types.ts
//   (2) Add a detection branch here that returns { <new field>: value }
//   (3) Add a matching render branch in common/output.ts
// The protocol is duck-typed dispatch — additive, non-breaking.

import type { CellResult } from './types'
import { formatValue } from './js-format'

export function detectRichOutput(value: unknown): Partial<CellResult> {
  if (value === undefined) return {}

  if (typeof Element !== 'undefined' && value instanceof Element) {
    return { value_html: value.outerHTML }
  }

  if (value !== null && typeof value === 'object') {
    const obj = value as { toHtml?: () => unknown }
    if (typeof obj.toHtml === 'function') {
      const html = obj.toHtml()
      if (typeof html === 'string') return { value_html: html }
    }
  }

  return { value_repr: formatValue(value) }
}
