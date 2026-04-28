// String formatting for JS runtime values, shared across main-thread JS kernels.
// Previously inlined in typescript/kernel.ts — factored out so future
// JS-flavored kernels can reuse without copy-paste.

export function formatValue(v: unknown): string {
  if (v === null) return 'null'
  if (v === undefined) return 'undefined'
  if (typeof v === 'bigint') return `${v}n`
  if (typeof v === 'function') return v.toString()
  if (typeof v === 'symbol') return v.toString()
  if (typeof v === 'string') return JSON.stringify(v)
  if (typeof v === 'number' || typeof v === 'boolean') return String(v)
  try {
    return JSON.stringify(
      v,
      (_key, val) => {
        if (typeof val === 'bigint') return `${val}n`
        if (val instanceof Map) return Object.fromEntries(val)
        if (val instanceof Set) return Array.from(val)
        return val
      },
      2,
    )
  } catch {
    return String(v)
  }
}

export function fmtArg(a: unknown): string {
  if (typeof a === 'string') return a
  return formatValue(a)
}
