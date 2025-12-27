import type { Complex } from './complex'

// 置换表示：perm[i] = j 表示位置 i 的元素移动到位置 j
export type Permutation = number[]

export type GroupAction = {
  id: string
  label: string
  perm: Permutation
  description?: string
}

export const identity = (n: number): Permutation => Array.from({ length: n }, (_, i) => i)

export const compose = (a: Permutation, b: Permutation): Permutation =>
  a.map(i => b[i])

export const inverse = (perm: Permutation): Permutation => {
  const result = new Array(perm.length)
  perm.forEach((to, from) => result[to] = from)
  return result
}

export const apply = <T>(perm: Permutation, items: T[]): T[] =>
  perm.map(i => items[i])

export const isIdentity = (perm: Permutation): boolean =>
  perm.every((v, i) => v === i)

// S₂ 群操作
export const S2Actions: GroupAction[] = [
  { id: 'e', label: 'e', perm: [0, 1], description: '恒等' },
  { id: 'swap', label: '(12)', perm: [1, 0], description: '交换两根' }
]

// S₃ 群操作
export const S3Actions: GroupAction[] = [
  { id: 'e', label: 'e', perm: [0, 1, 2], description: '恒等' },
  { id: 'r1', label: '(123)', perm: [1, 2, 0], description: '轮换' },
  { id: 'r2', label: '(132)', perm: [2, 0, 1], description: '反向轮换' },
  { id: 's1', label: '(12)', perm: [1, 0, 2], description: '对换 1-2' },
  { id: 's2', label: '(13)', perm: [2, 1, 0], description: '对换 1-3' },
  { id: 's3', label: '(23)', perm: [0, 2, 1], description: '对换 2-3' }
]

// S₄ 群操作 (简化版，只列常用的)
export const S4Actions: GroupAction[] = [
  { id: 'e', label: 'e', perm: [0, 1, 2, 3], description: '恒等' },
  // 4-轮换
  { id: 'r90', label: '(1234)', perm: [1, 2, 3, 0], description: '旋转 90°' },
  { id: 'r180', label: '(13)(24)', perm: [2, 3, 0, 1], description: '旋转 180°' },
  { id: 'r270', label: '(1432)', perm: [3, 0, 1, 2], description: '旋转 270°' },
  // 对换
  { id: 's12', label: '(12)', perm: [1, 0, 2, 3], description: '对换 1-2' },
  { id: 's34', label: '(34)', perm: [0, 1, 3, 2], description: '对换 3-4' },
  { id: 's13', label: '(13)', perm: [2, 1, 0, 3], description: '对换 1-3' },
  { id: 's24', label: '(24)', perm: [0, 3, 2, 1], description: '对换 2-4' }
]

export const getActions = (degree: 2 | 3 | 4): GroupAction[] => {
  if (degree === 2) return S2Actions
  if (degree === 3) return S3Actions
  return S4Actions
}

// 应用置换到根的位置
export const applyToRoots = (perm: Permutation, roots: Complex[]): Complex[] =>
  apply(perm, roots)
