import { describe, expect, it } from 'vitest'
import { splitLastExpression } from './kernel'

describe('splitLastExpression', () => {
  it('treats a bare last line as the trailing expression', () => {
    const code = 'const xs = [1, 2, 3]\nxs.map(x => x + 1)'
    expect(splitLastExpression(code)).toEqual({
      body: 'const xs = [1, 2, 3]',
      trailing: 'xs.map(x => x + 1)',
    })
  })

  it('walks back through leading-dot chain to the receiver', () => {
    const code = [
      'const xs = [0, 1, 2, 3, 4, 5, 6, 7]',
      '',
      'xs',
      '  .map(x => x + 1)',
      '  .filter(x => x % 2 === 1)',
      '  .slice(0, 3)',
    ].join('\n')
    const { body, trailing } = splitLastExpression(code)
    expect(body).toBe('const xs = [0, 1, 2, 3, 4, 5, 6, 7]\n')
    expect(trailing).toBe(
      'xs\n  .map(x => x + 1)\n  .filter(x => x % 2 === 1)\n  .slice(0, 3)',
    )
  })

  it('returns null trailing when the last line is statement-like', () => {
    const code = 'const xs = [1, 2, 3]\nconst ys = xs.map(x => x + 1)'
    expect(splitLastExpression(code)).toEqual({ body: code, trailing: null })
  })

  it('bails when chain walks back into a statement-like line', () => {
    const code = 'const ys = xs\n  .map(x => x + 1)'
    expect(splitLastExpression(code)).toEqual({ body: code, trailing: null })
  })

  it('handles a chain that starts at the very first line', () => {
    const code = '[1, 2, 3]\n  .map(x => x + 1)\n  .filter(x => x > 1)'
    expect(splitLastExpression(code)).toEqual({
      body: '',
      trailing: '[1, 2, 3]\n  .map(x => x + 1)\n  .filter(x => x > 1)',
    })
  })
})
