import { describe, expect, it } from 'vitest'
import { fmtArg, formatValue } from './js-format'

describe('formatValue', () => {
  it('renders primitives', () => {
    expect(formatValue(null)).toBe('null')
    expect(formatValue(undefined)).toBe('undefined')
    expect(formatValue(42)).toBe('42')
    expect(formatValue(true)).toBe('true')
    expect(formatValue('hi')).toBe('"hi"')
  })

  it('marks bigints with trailing n', () => {
    expect(formatValue(123n)).toBe('123n')
  })

  it('serializes functions and symbols via their own toString', () => {
    expect(formatValue(() => 1)).toMatch(/=>/)
    expect(formatValue(Symbol('x'))).toBe('Symbol(x)')
  })

  it('pretty-prints plain objects and arrays', () => {
    expect(formatValue({ a: 1 })).toBe('{\n  "a": 1\n}')
    expect(formatValue([1, 2])).toBe('[\n  1,\n  2\n]')
  })

  it('reaches into Map and Set inside objects', () => {
    expect(formatValue(new Map([['k', 1]]))).toBe('{\n  "k": 1\n}')
    expect(formatValue(new Set([1, 2]))).toBe('[\n  1,\n  2\n]')
  })

  it('serializes nested bigints as quoted strings ending in n', () => {
    expect(formatValue({ n: 9n })).toBe('{\n  "n": "9n"\n}')
  })

  it('falls back to String() on circular references', () => {
    const a: Record<string, unknown> = {}
    a.self = a
    expect(formatValue(a)).toBe('[object Object]')
  })
})

describe('fmtArg', () => {
  it('returns strings unchanged (no JSON quoting)', () => {
    expect(fmtArg('hello')).toBe('hello')
  })

  it('delegates non-strings to formatValue', () => {
    expect(fmtArg(42)).toBe('42')
    expect(fmtArg({ a: 1 })).toBe('{\n  "a": 1\n}')
  })
})
