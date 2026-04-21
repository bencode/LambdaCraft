// Clojure kernel: Scittle (SCI + ClojureScript) loaded from CDN.
// Single window.scittle instance shared across all cells on the page —
// `def`/`defn` persist between cells.

import type {
  CellResult,
  InitListener,
  Kernel,
  KernelInitState,
  PackageLoadListener,
} from '../common/types'
import { emptyResult } from '../common/types'

const SCITTLE_VERSION = '0.6.22'
const SCITTLE_URL = `https://cdn.jsdelivr.net/npm/scittle@${SCITTLE_VERSION}/dist/scittle.js`

type ScittleCore = {
  eval_string(code: string): unknown
}

type Scittle = {
  core: ScittleCore
}

function loadScript(src: string): Promise<void> {
  return new Promise((resolve, reject) => {
    const existing = document.querySelector(`script[data-coderunner-src="${src}"]`)
    if (existing) {
      if ((existing as HTMLScriptElement).dataset.loaded === 'true') {
        resolve()
        return
      }
      existing.addEventListener('load', () => resolve())
      existing.addEventListener('error', () => reject(new Error(`load ${src}`)))
      return
    }
    const s = document.createElement('script')
    s.src = src
    s.async = false
    s.dataset.coderunnerSrc = src
    s.onload = () => {
      s.dataset.loaded = 'true'
      resolve()
    }
    s.onerror = () => reject(new Error(`load ${src}`))
    document.head.appendChild(s)
  })
}

function getScittle(): Scittle | null {
  const s = (globalThis as { scittle?: Scittle }).scittle
  return s ?? null
}

class ClojureKernel implements Kernel {
  readonly lang = 'clojure' as const
  readonly label = '🧙 Clojure'
  readonly supportsReset = false

  private loadPromise: Promise<void> | null = null
  private initState: KernelInitState = 'idle'
  private initMessage: string | undefined
  private initListeners = new Set<InitListener>()
  private packageLoadListeners = new Set<PackageLoadListener>()

  subscribe(fn: InitListener): () => void {
    this.initListeners.add(fn)
    fn(this.initState, this.initMessage)
    return () => {
      this.initListeners.delete(fn)
    }
  }

  subscribePackageLoad(fn: PackageLoadListener): () => void {
    this.packageLoadListeners.add(fn)
    fn(null)
    return () => {
      this.packageLoadListeners.delete(fn)
    }
  }

  private setInitState(state: KernelInitState, message?: string): void {
    this.initState = state
    this.initMessage = message
    for (const fn of this.initListeners) fn(state, message)
  }

  private async ensureReady(): Promise<Scittle> {
    const cached = getScittle()
    if (cached) return cached
    if (!this.loadPromise) {
      this.loadPromise = (async () => {
        this.setInitState('initializing', '正在加载 Scittle...')
        try {
          await loadScript(SCITTLE_URL)
          this.setInitState('ready')
        } catch (err) {
          this.setInitState('error', String(err))
          throw err
        }
      })()
    }
    await this.loadPromise
    const s = getScittle()
    if (!s) throw new Error('Scittle loaded but window.scittle is missing')
    return s
  }

  async run(code: string): Promise<CellResult> {
    const result = emptyResult()
    let scittle: Scittle
    try {
      scittle = await this.ensureReady()
    } catch (err) {
      result.error = `Scittle 加载失败: ${String(err)}`
      return result
    }

    const stdoutBuf: string[] = []
    const stderrBuf: string[] = []
    const origLog = console.log
    const origErr = console.error

    console.log = (...args: unknown[]) => {
      stdoutBuf.push(args.map((a) => (typeof a === 'string' ? a : String(a))).join(' '))
    }
    console.error = (...args: unknown[]) => {
      stderrBuf.push(args.map((a) => (typeof a === 'string' ? a : String(a))).join(' '))
    }

    try {
      // Evaluate user code wrapped in (do ...), then pr-str the result.
      // `println` calls inside the do execute first (captured via console),
      // then the final expression's value is returned as a printable string.
      const wrapped = `(pr-str (do\n${code}\n))`
      const repr = scittle.core.eval_string(wrapped)
      if (typeof repr === 'string' && repr !== 'nil') {
        result.value_repr = repr
      }
    } catch (err) {
      result.error = err instanceof Error ? `${err.name}: ${err.message}` : String(err)
    } finally {
      console.log = origLog
      console.error = origErr
      if (stdoutBuf.length) result.stdout = `${stdoutBuf.join('\n')}\n`
      if (stderrBuf.length) result.stderr = `${stderrBuf.join('\n')}\n`
    }
    return result
  }

  async reset(): Promise<void> {
    // Scittle doesn't expose a clean way to reset the user ns without reloading.
    // supportsReset = false — the bar's reset button is disabled.
  }
}

let _kernel: ClojureKernel | null = null

export function getClojureKernel(): ClojureKernel {
  if (!_kernel) _kernel = new ClojureKernel()
  return _kernel
}
