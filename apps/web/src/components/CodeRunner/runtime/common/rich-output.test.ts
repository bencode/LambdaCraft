import { describe, expect, it } from 'vitest'
import { detectRichOutput } from './rich-output'

describe('detectRichOutput', () => {
  it('returns empty for undefined (cell with no return value)', () => {
    expect(detectRichOutput(undefined)).toEqual({})
  })

  it('routes plain values to value_repr via formatValue', () => {
    expect(detectRichOutput(42)).toEqual({ value_repr: '42' })
    expect(detectRichOutput(null)).toEqual({ value_repr: 'null' })
    expect(detectRichOutput([1, 2])).toEqual({ value_repr: '[\n  1,\n  2\n]' })
  })

  it('emits value_html for DOM Element via outerHTML', () => {
    const el = document.createElement('div')
    el.textContent = 'hi'
    expect(detectRichOutput(el)).toEqual({ value_html: '<div>hi</div>' })
  })

  it('emits value_html when object exposes a toHtml() returning a string', () => {
    const obj = { toHtml: () => '<svg></svg>' }
    expect(detectRichOutput(obj)).toEqual({ value_html: '<svg></svg>' })
  })

  it('falls through to value_repr when toHtml() returns a non-string', () => {
    // JSON.stringify drops function-valued fields, so an object whose only
    // field is `toHtml: () => …` formats as `{}`. Documented limitation —
    // cells should return real data fields if they want a useful repr.
    const obj = { toHtml: () => 42 }
    expect(detectRichOutput(obj)).toEqual({ value_repr: '{}' })
  })

  it('ignores objects whose toHtml is not a function', () => {
    const obj = { toHtml: 'not callable' }
    const result = detectRichOutput(obj)
    expect(result.value_html).toBeUndefined()
    expect(result.value_repr).toContain('toHtml')
  })
})
