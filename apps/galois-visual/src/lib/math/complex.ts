export type Complex = {
  re: number
  im: number
}

export const complex = (re: number, im: number = 0): Complex => ({ re, im })

export const add = (a: Complex, b: Complex): Complex => ({
  re: a.re + b.re,
  im: a.im + b.im
})

export const sub = (a: Complex, b: Complex): Complex => ({
  re: a.re - b.re,
  im: a.im - b.im
})

export const mul = (a: Complex, b: Complex): Complex => ({
  re: a.re * b.re - a.im * b.im,
  im: a.re * b.im + a.im * b.re
})

export const div = (a: Complex, b: Complex): Complex => {
  const denom = b.re * b.re + b.im * b.im
  return {
    re: (a.re * b.re + a.im * b.im) / denom,
    im: (a.im * b.re - a.re * b.im) / denom
  }
}

export const abs = (z: Complex): number => Math.sqrt(z.re * z.re + z.im * z.im)

export const arg = (z: Complex): number => Math.atan2(z.im, z.re)

export const conj = (z: Complex): Complex => ({ re: z.re, im: -z.im })

export const scale = (z: Complex, k: number): Complex => ({
  re: z.re * k,
  im: z.im * k
})

export const sqrt = (z: Complex): Complex => {
  const r = abs(z)
  const theta = arg(z)
  return {
    re: Math.sqrt(r) * Math.cos(theta / 2),
    im: Math.sqrt(r) * Math.sin(theta / 2)
  }
}

export const cbrt = (z: Complex): Complex => {
  const r = abs(z)
  const theta = arg(z)
  return {
    re: Math.cbrt(r) * Math.cos(theta / 3),
    im: Math.cbrt(r) * Math.sin(theta / 3)
  }
}

export const nthRoot = (z: Complex, n: number, k: number = 0): Complex => {
  const r = abs(z)
  const theta = arg(z)
  const rootR = Math.pow(r, 1 / n)
  const rootTheta = (theta + 2 * Math.PI * k) / n
  return {
    re: rootR * Math.cos(rootTheta),
    im: rootR * Math.sin(rootTheta)
  }
}

export const format = (z: Complex, precision: number = 3): string => {
  const re = z.re.toFixed(precision)
  const im = Math.abs(z.im).toFixed(precision)
  if (Math.abs(z.im) < 1e-10) return re
  if (Math.abs(z.re) < 1e-10) return z.im >= 0 ? `${im}i` : `-${im}i`
  return z.im >= 0 ? `${re} + ${im}i` : `${re} - ${im}i`
}
