// Scheme kernel: BiwaScheme, main thread, lazily imported on first run.
// All <CodeRunner lang="scheme"> cells on a page share one Interpreter instance
// (so `define`d names persist across cells, mirroring the Python shared-kernel
// behavior).

import type {
  CellResult,
  InitListener,
  Kernel,
  KernelInitState,
  PackageLoadListener,
} from '../common/types'
import { emptyResult } from '../common/types'

// biome-ignore lint/suspicious/noExplicitAny: BiwaScheme has no shipped types
type BiwaSchemeModule = any

function formatSchemeValue(
  BiwaScheme: BiwaSchemeModule,
  value: unknown,
): string | null {
  if (value === undefined || value === null) return null
  if (BiwaScheme?.undef !== undefined && value === BiwaScheme.undef) return null
  if (BiwaScheme?.nil !== undefined && value === BiwaScheme.nil) return "'()"
  if (typeof BiwaScheme?.to_write === 'function') {
    try {
      return BiwaScheme.to_write(value)
    } catch {
      /* fall through */
    }
  }
  if (typeof (value as { to_write_string?: () => string }).to_write_string === 'function') {
    try {
      return (value as { to_write_string: () => string }).to_write_string()
    } catch {
      /* fall through */
    }
  }
  return String(value)
}

class SchemeKernel implements Kernel {
  readonly lang = 'scheme' as const
  readonly label = 'λ Scheme'
  readonly supportsReset = true

  private BiwaScheme: BiwaSchemeModule = null
  private interp: unknown = null
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

  private async ensureReady(): Promise<void> {
    if (this.interp) return
    if (this.loadPromise) return this.loadPromise

    this.loadPromise = (async () => {
      this.setInitState('initializing', '正在加载 BiwaScheme...')
      try {
        const mod = (await import('biwascheme')) as BiwaSchemeModule
        this.BiwaScheme = mod.default ?? mod
        this.interp = new this.BiwaScheme.Interpreter((err: unknown) => {
          // interpreter-level error sink; per-run errors flow through run()
          console.error('[scheme]', err)
        })
        this.setInitState('ready')
      } catch (err) {
        this.setInitState('error', String(err))
        throw err
      }
    })()

    return this.loadPromise
  }

  async run(code: string): Promise<CellResult> {
    const result = emptyResult()
    try {
      await this.ensureReady()
    } catch (err) {
      result.error = `BiwaScheme 加载失败: ${String(err)}`
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
      // biome-ignore lint/suspicious/noExplicitAny: dynamic interp
      const value = (this.interp as any).evaluate(code)
      const repr = formatSchemeValue(this.BiwaScheme, value)
      if (repr !== null) result.value_repr = repr
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
    if (!this.BiwaScheme) return
    this.interp = new this.BiwaScheme.Interpreter((err: unknown) => {
      console.error('[scheme]', err)
    })
  }
}

let _kernel: SchemeKernel | null = null

export function getSchemeKernel(): SchemeKernel {
  if (!_kernel) _kernel = new SchemeKernel()
  return _kernel
}
