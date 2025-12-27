import type { Complex } from './complex'
import { complex, add, sub, mul, sqrt, cbrt, nthRoot, scale } from './complex'

export type Equation = {
  degree: 2 | 3 | 4
  coefficients: number[]  // [a, b, c] for ax² + bx + c = 0
}

// 二次方程 ax² + bx + c = 0
export const solveQuadratic = (a: number, b: number, c: number): Complex[] => {
  const discriminant = b * b - 4 * a * c
  if (discriminant >= 0) {
    const sqrtD = Math.sqrt(discriminant)
    return [
      complex((-b + sqrtD) / (2 * a)),
      complex((-b - sqrtD) / (2 * a))
    ]
  } else {
    const realPart = -b / (2 * a)
    const imagPart = Math.sqrt(-discriminant) / (2 * a)
    return [
      complex(realPart, imagPart),
      complex(realPart, -imagPart)
    ]
  }
}

// 三次方程 x³ + px + q = 0 (已化简形式，卡尔达诺公式)
const solveCubicDepressed = (p: number, q: number): Complex[] => {
  const discriminant = (q * q / 4) + (p * p * p / 27)

  if (Math.abs(p) < 1e-10 && Math.abs(q) < 1e-10) {
    return [complex(0), complex(0), complex(0)]
  }

  const omega = complex(-0.5, Math.sqrt(3) / 2)  // 单位根 ω = e^(2πi/3)
  const omega2 = complex(-0.5, -Math.sqrt(3) / 2)

  const A = complex(-q / 2 + (discriminant >= 0 ? Math.sqrt(discriminant) : 0),
                    discriminant < 0 ? Math.sqrt(-discriminant) : 0)
  const B = complex(-q / 2 - (discriminant >= 0 ? Math.sqrt(discriminant) : 0),
                    discriminant < 0 ? -Math.sqrt(-discriminant) : 0)

  const u = cbrt(A)
  const v = cbrt(B)

  const x1 = add(u, v)
  const x2 = add(mul(omega, u), mul(omega2, v))
  const x3 = add(mul(omega2, u), mul(omega, v))

  return [x1, x2, x3]
}

// 三次方程 ax³ + bx² + cx + d = 0
export const solveCubic = (a: number, b: number, c: number, d: number): Complex[] => {
  // 化简为 t³ + pt + q = 0, 其中 x = t - b/(3a)
  const shift = b / (3 * a)
  const p = (3 * a * c - b * b) / (3 * a * a)
  const q = (2 * b * b * b - 9 * a * b * c + 27 * a * a * d) / (27 * a * a * a)

  const roots = solveCubicDepressed(p, q)
  return roots.map(r => sub(r, complex(shift)))
}

// 四次方程 ax⁴ + bx³ + cx² + dx + e = 0 (费拉里方法)
export const solveQuartic = (a: number, b: number, c: number, d: number, e: number): Complex[] => {
  // 化简为 y⁴ + py² + qy + r = 0, 其中 x = y - b/(4a)
  const shift = b / (4 * a)
  const p = (8 * a * c - 3 * b * b) / (8 * a * a)
  const q = (b * b * b - 4 * a * b * c + 8 * a * a * d) / (8 * a * a * a)
  const r = (-3 * b * b * b * b + 256 * a * a * a * e - 64 * a * a * b * d + 16 * a * b * b * c) / (256 * a * a * a * a)

  if (Math.abs(q) < 1e-10) {
    // 双二次方程 y⁴ + py² + r = 0
    const quadRoots = solveQuadratic(1, p, r)
    const roots: Complex[] = []
    for (const z of quadRoots) {
      roots.push(sqrt(z))
      roots.push(scale(sqrt(z), -1))
    }
    return roots.map(root => sub(root, complex(shift)))
  }

  // 解辅助三次方程找 m
  const cubicRoots = solveCubic(1, 2 * p, p * p - 4 * r, -q * q)
  const m = cubicRoots[0]  // 取第一个根

  const sqrtM = sqrt(m)
  const term = sub(complex(-p), m)
  const qOverSqrtM = scale(complex(q), -1 / (sqrtM.re || 1e-10))

  const roots1 = solveQuadratic(1, -sqrtM.re, (term.re + qOverSqrtM.re) / 2)
  const roots2 = solveQuadratic(1, sqrtM.re, (term.re - qOverSqrtM.re) / 2)

  return [...roots1, ...roots2].map(root => sub(root, complex(shift)))
}

// 生成单位根 (xⁿ = 1 的解)
export const unitRoots = (n: number): Complex[] => {
  return Array.from({ length: n }, (_, k) => nthRoot(complex(1), n, k))
}

// 生成随机方程的根（保证共轭对称，使系数为实数）
export const generateRandomRoots = (degree: 2 | 3 | 4): Complex[] => {
  const roots: Complex[] = []
  let remaining = degree

  while (remaining > 0) {
    if (remaining === 1 || Math.random() < 0.5) {
      // 添加实根
      roots.push(complex((Math.random() - 0.5) * 4))
      remaining -= 1
    } else {
      // 添加共轭复根对
      const re = (Math.random() - 0.5) * 3
      const im = (Math.random() * 0.5 + 0.5) * 2
      roots.push(complex(re, im))
      roots.push(complex(re, -im))
      remaining -= 2
    }
  }

  return roots.slice(0, degree)
}

// 从根计算多项式系数
export const rootsToCoefficients = (roots: Complex[]): number[] => {
  // 对于实系数多项式，展开 (x - r1)(x - r2)...
  let coeffs = [complex(1)]

  for (const root of roots) {
    const newCoeffs: Complex[] = []
    for (let i = 0; i <= coeffs.length; i++) {
      const a = i > 0 ? coeffs[i - 1] : complex(0)
      const b = i < coeffs.length ? mul(coeffs[i], scale(root, -1)) : complex(0)
      newCoeffs.push(add(a, b))
    }
    coeffs = newCoeffs
  }

  return coeffs.map(c => c.re)
}

export const solve = (eq: Equation): Complex[] => {
  const [a, b, c, d, e] = eq.coefficients
  if (eq.degree === 2) return solveQuadratic(a, b, c)
  if (eq.degree === 3) return solveCubic(a, b, c, d)
  return solveQuartic(a, b, c, d, e)
}
