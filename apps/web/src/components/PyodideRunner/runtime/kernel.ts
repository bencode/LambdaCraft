// Page-level singleton kernel manager.
// All <PyodideRunner> cells on a page share the same Worker / Pyodide instance.
//
// Two distinct event streams:
// - subscribe(fn): kernel init lifecycle (idle | initializing | ready | error)
// - subscribePackageLoad(fn): page-level package loading events
//
// Per-cell run state is NOT broadcast — each cell tracks its own running flag.

import * as Comlink from 'comlink'
import type { WorkerApi, CellResult } from './worker'

export type { CellResult }

export type KernelInitState = 'idle' | 'initializing' | 'ready' | 'error'

type InitListener = (state: KernelInitState, message?: string) => void
type PackageLoadListener = (message: string | null) => void

class Kernel {
  private worker: Worker | null = null
  private api: Comlink.Remote<WorkerApi> | null = null
  private initState: KernelInitState = 'idle'
  private initMessage: string | undefined
  private initListeners = new Set<InitListener>()
  private packageLoadListeners = new Set<PackageLoadListener>()
  private currentPackageLoad: string | null = null

  subscribe(fn: InitListener): () => void {
    this.initListeners.add(fn)
    fn(this.initState, this.initMessage)
    return () => {
      this.initListeners.delete(fn)
    }
  }

  subscribePackageLoad(fn: PackageLoadListener): () => void {
    this.packageLoadListeners.add(fn)
    fn(this.currentPackageLoad)
    return () => {
      this.packageLoadListeners.delete(fn)
    }
  }

  private setInitState(state: KernelInitState, message?: string): void {
    this.initState = state
    this.initMessage = message
    for (const fn of this.initListeners) fn(state, message)
  }

  private setPackageLoad(message: string | null): void {
    this.currentPackageLoad = message
    for (const fn of this.packageLoadListeners) fn(message)
  }

  private async ensureWorker(): Promise<Comlink.Remote<WorkerApi>> {
    if (this.api) return this.api

    if (this.initState === 'initializing') {
      return new Promise((resolve, reject) => {
        const unsub = this.subscribe((s) => {
          if (s === 'ready' && this.api) {
            unsub()
            resolve(this.api)
          } else if (s === 'error') {
            unsub()
            reject(new Error('kernel init failed'))
          }
        })
      })
    }

    this.setInitState('initializing', '正在启动 Python 环境')
    try {
      this.worker = new Worker(new URL('./worker.ts', import.meta.url), {
        type: 'module',
      })
      this.api = Comlink.wrap<WorkerApi>(this.worker)
      const onProgress = Comlink.proxy((msg: string) => {
        this.setInitState('initializing', msg)
      })
      await this.api.init(onProgress)
      this.setInitState('ready')
      return this.api
    } catch (err) {
      this.setInitState('error', String(err))
      throw err
    }
  }

  async run(code: string): Promise<CellResult> {
    const api = await this.ensureWorker()
    const onPackageProgress = Comlink.proxy((msg: string) => {
      this.setPackageLoad(msg)
    })
    try {
      return await api.runCode(code, onPackageProgress)
    } finally {
      this.setPackageLoad(null)
    }
  }

  async reset(): Promise<void> {
    if (!this.api) return
    await this.api.reset()
  }

  async installPackage(name: string): Promise<void> {
    const api = await this.ensureWorker()
    await api.installPackage(name)
  }

  destroy(): void {
    this.worker?.terminate()
    this.worker = null
    this.api = null
    this.setInitState('idle')
    this.setPackageLoad(null)
  }
}

let _kernel: Kernel | null = null

export function getKernel(): Kernel {
  if (!_kernel) _kernel = new Kernel()
  return _kernel
}
