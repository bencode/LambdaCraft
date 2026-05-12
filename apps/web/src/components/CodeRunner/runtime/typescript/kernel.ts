// TypeScript kernel: sucrase transpile + main-thread eval.
// Each run creates a fresh async IIFE — so `const`/`let`/`type` do NOT persist
// across cells. To share state between cells, attach to `globalThis`.

import { transform } from 'sucrase'
import { fmtArg } from '../common/js-format'
import { detectRichOutput } from '../common/rich-output'
import type {
  CellResult,
  InitListener,
  Kernel,
  PackageLoadListener,
} from '../common/types'
import { emptyResult } from '../common/types'
import { $import } from './library-registry'

const STMT_KW_RE =
  /^(if|for|while|switch|case|break|continue|return|let|const|var|function|class|try|catch|finally|throw|import|export|do|else|interface|type|enum)\b/

function isStatementLike(line: string): boolean {
  const t = line.trim()
  if (!t) return true
  if (t.endsWith(';') || t.endsWith('{') || t.endsWith('}')) return true
  if (t.startsWith('//') || t.startsWith('/*')) return true
  // lines that start with closing brackets are the tail of a multi-line
  // expression — e.g. the `})` closing `({ ... })`. they are not standalone.
  if (t.startsWith(')') || t.startsWith('}') || t.startsWith(']')) return true
  if (STMT_KW_RE.test(t)) return true
  return false
}

// A leading-dot line (`.foo()` or `?.foo()`) is a chain continuation, not a
// standalone expression. Walk past these to find where the chain's receiver is.
const CHAIN_CONT_RE = /^\??\./

export function splitLastExpression(code: string): { body: string; trailing: string | null } {
  const lines = code.split('\n')
  let lastIdx = lines.length - 1
  while (lastIdx >= 0 && lines[lastIdx].trim() === '') lastIdx--
  if (lastIdx < 0) return { body: code, trailing: null }

  if (isStatementLike(lines[lastIdx])) return { body: code, trailing: null }

  let startIdx = lastIdx
  while (startIdx > 0 && CHAIN_CONT_RE.test(lines[startIdx].trim())) {
    startIdx--
  }

  if (isStatementLike(lines[startIdx])) return { body: code, trailing: null }

  const body = lines.slice(0, startIdx).join('\n')
  const trailing = lines.slice(startIdx, lastIdx + 1).join('\n')
  return { body, trailing }
}

class TypeScriptKernel implements Kernel {
  readonly lang = 'typescript' as const
  readonly label = 'TS'
  readonly supportsReset = false

  private initListeners = new Set<InitListener>()
  private packageLoadListeners = new Set<PackageLoadListener>()

  subscribe(fn: InitListener): () => void {
    this.initListeners.add(fn)
    // sucrase is a pure JS module — we consider the kernel "ready" the moment
    // someone subscribes (it'll be imported at module-eval time anyway).
    fn('ready')
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

  async run(code: string): Promise<CellResult> {
    const result = emptyResult()
    const stdoutBuf: string[] = []
    const stderrBuf: string[] = []

    const pushStdout = (...args: unknown[]): void => {
      stdoutBuf.push(args.map(fmtArg).join(' '))
    }
    const pushStderr = (...args: unknown[]): void => {
      stderrBuf.push(args.map(fmtArg).join(' '))
    }

    const fakeConsole = {
      log: pushStdout,
      info: pushStdout,
      debug: pushStdout,
      warn: pushStderr,
      error: pushStderr,
    }

    try {
      const { body, trailing } = splitLastExpression(code)
      // Strip leading whitespace + semicolons from the trailing expression —
      // users often write `;(expr)` to avoid ASI hazards, but that breaks
      // `return (;...)`. The leading `;` is a no-op statement anyway.
      const trailingExpr = trailing ? trailing.replace(/^[\s;]+/, '') : ''
      const innerTs = trailingExpr
        ? (body.trim() ? `${body}\n;return (${trailingExpr});` : `return (${trailingExpr});`)
        : `${body}`

      const wrappedTs = `(async () => {\n${innerTs}\n})()`

      const transpiled = transform(wrappedTs, {
        transforms: ['typescript'],
        disableESTransforms: true,
      }).code

      const fn = new Function('console', '$import', `return ${transpiled};`)
      const value = await fn(fakeConsole, $import)

      Object.assign(result, detectRichOutput(value))
    } catch (err) {
      result.error = err instanceof Error ? `${err.name}: ${err.message}` : String(err)
    }

    result.stdout = stdoutBuf.length ? `${stdoutBuf.join('\n')}\n` : ''
    result.stderr = stderrBuf.length ? `${stderrBuf.join('\n')}\n` : ''
    return result
  }

  async reset(): Promise<void> {
    // No-op: each run() is a fresh closure. State intentionally lives on
    // `globalThis` (author's explicit opt-in).
  }
}

let _kernel: TypeScriptKernel | null = null

export function getTypeScriptKernel(): TypeScriptKernel {
  if (!_kernel) _kernel = new TypeScriptKernel()
  return _kernel
}
