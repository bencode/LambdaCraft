// Shared types across all language kernels.

export type Lang = 'python' | 'scheme' | 'clojure' | 'typescript'

export type KernelInitState = 'idle' | 'initializing' | 'ready' | 'error'

export type CellResult = {
  stdout: string
  stderr: string
  value_repr: string | null
  value_html: string | null
  image_png_b64: string | null
  error: string | null
}

export type InitListener = (state: KernelInitState, message?: string) => void
export type PackageLoadListener = (message: string | null) => void

export type Kernel = {
  readonly lang: Lang
  readonly label: string
  readonly supportsReset: boolean
  subscribe(fn: InitListener): () => void
  subscribePackageLoad(fn: PackageLoadListener): () => void
  run(code: string): Promise<CellResult>
  reset(): Promise<void>
}

export const emptyResult = (): CellResult => ({
  stdout: '',
  stderr: '',
  value_repr: null,
  value_html: null,
  image_png_b64: null,
  error: null,
})
