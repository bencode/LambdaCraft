// Pyodide Web Worker
// Loads Pyodide from CDN, exposes runCode / reset via Comlink.

import * as Comlink from 'comlink'
import type { CellResult } from '../common/types'

const PYODIDE_VERSION = '0.27.4'
const PYODIDE_INDEX_URL = `https://cdn.jsdelivr.net/pyodide/v${PYODIDE_VERSION}/full/`

// biome-ignore lint/suspicious/noExplicitAny: pyodide global
let pyodide: any = null
let initPromise: Promise<void> | null = null

async function ensureReady(progress?: (msg: string) => void): Promise<void> {
  if (pyodide) return
  if (initPromise) return initPromise

  initPromise = (async () => {
    progress?.('正在下载 Python 环境...')
    const pyodideModule = await import(/* @vite-ignore */ `${PYODIDE_INDEX_URL}pyodide.mjs`)
    progress?.('正在初始化 Pyodide...')
    pyodide = await pyodideModule.loadPyodide({
      indexURL: PYODIDE_INDEX_URL,
    })
    progress?.('正在准备运行环境...')
    // Force matplotlib to use Agg (non-interactive PNG) backend.
    // Default html5_canvas backend imports `document` from JS — only exists
    // in main thread, not in workers. Set BEFORE any user matplotlib import.
    pyodide.runPython("import os; os.environ['MPLBACKEND'] = 'AGG'")
    pyodide.runPython(EXECUTE_WRAPPER)
    progress?.('就绪')
  })()

  return initPromise
}

const EXECUTE_WRAPPER = `
import sys
import io
import base64
import traceback
from pyodide.code import eval_code_async

_pyrunner_globals = {'__name__': '__main__'}

async def _pyrunner_execute(code):
    stdout_buf = io.StringIO()
    stderr_buf = io.StringIO()
    old_stdout, old_stderr = sys.stdout, sys.stderr
    sys.stdout, sys.stderr = stdout_buf, stderr_buf

    result = {
        'stdout': '',
        'stderr': '',
        'value_repr': None,
        'value_html': None,
        'image_png_b64': None,
        'error': None,
    }

    try:
        last_value = await eval_code_async(
            code,
            globals=_pyrunner_globals,
            filename='<cell>',
        )

        if last_value is not None:
            if hasattr(last_value, 'to_html') and hasattr(last_value, 'columns'):
                try:
                    result['value_html'] = last_value.to_html(max_rows=20, max_cols=12, classes='coderunner-df')
                except Exception:
                    result['value_repr'] = repr(last_value)
            else:
                result['value_repr'] = repr(last_value)

        try:
            import matplotlib.pyplot as plt
            figs = [plt.figure(num) for num in plt.get_fignums()]
            if figs:
                fig = figs[-1]
                buf = io.BytesIO()
                fig.savefig(buf, format='png', bbox_inches='tight', dpi=100)
                result['image_png_b64'] = base64.b64encode(buf.getvalue()).decode()
                plt.close('all')
        except ImportError:
            pass

    except Exception:
        result['error'] = traceback.format_exc()
    finally:
        sys.stdout, sys.stderr = old_stdout, old_stderr
        result['stdout'] = stdout_buf.getvalue()
        result['stderr'] = stderr_buf.getvalue()

    return result
`

const api = {
  async init(progressCb?: (msg: string) => void): Promise<void> {
    await ensureReady(progressCb)
  },

  async runCode(
    code: string,
    progressCb?: (msg: string) => void,
  ): Promise<CellResult> {
    await ensureReady()
    try {
      await pyodide.loadPackagesFromImports(code, {
        messageCallback: (msg: string) => progressCb?.(msg),
      })
    } catch (err) {
      console.warn('loadPackagesFromImports warning:', err)
    }
    const pyResult = await pyodide.globals.get('_pyrunner_execute')(code)
    const result = pyResult.toJs({ dict_converter: Object.fromEntries }) as CellResult
    pyResult.destroy()
    return result
  },

  async reset(): Promise<void> {
    if (!pyodide) return
    pyodide.runPython("_pyrunner_globals.clear(); _pyrunner_globals['__name__'] = '__main__'")
  },

  async installPackage(name: string): Promise<void> {
    await ensureReady()
    await pyodide.loadPackage('micropip')
    const micropip = pyodide.pyimport('micropip')
    await micropip.install(name)
  },
}

export type WorkerApi = typeof api

Comlink.expose(api)
