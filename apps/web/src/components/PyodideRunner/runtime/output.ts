// Output rendering for cell results.

import type { CellResult } from './worker'

function escape(text: string): string {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

export function renderResult(container: HTMLElement, result: CellResult): void {
  container.innerHTML = ''
  container.classList.remove('hidden')

  const parts: string[] = []

  if (result.stdout) {
    parts.push(`<pre class="pyrunner-stdout">${escape(result.stdout)}</pre>`)
  }

  if (result.stderr) {
    parts.push(`<pre class="pyrunner-stderr">${escape(result.stderr)}</pre>`)
  }

  if (result.image_png_b64) {
    parts.push(
      `<div class="pyrunner-image"><img src="data:image/png;base64,${result.image_png_b64}" alt="output figure" /></div>`,
    )
  }

  if (result.value_html) {
    parts.push(`<div class="pyrunner-html">${result.value_html}</div>`)
  } else if (result.value_repr) {
    parts.push(`<pre class="pyrunner-value">${escape(result.value_repr)}</pre>`)
  }

  if (result.error) {
    parts.push(`<pre class="pyrunner-error">${escape(result.error)}</pre>`)
  }

  if (parts.length === 0) {
    container.classList.add('hidden')
    return
  }

  container.innerHTML = parts.join('')
}

export function renderStatus(
  container: HTMLElement,
  message: string,
  kind: 'info' | 'error' = 'info',
): void {
  container.innerHTML = `<div class="pyrunner-status pyrunner-status--${kind}">${escape(message)}</div>`
  container.classList.remove('hidden')
}

export function clearOutput(container: HTMLElement): void {
  container.innerHTML = ''
  container.classList.add('hidden')
}
