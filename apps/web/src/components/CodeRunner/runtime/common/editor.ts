// CodeMirror 6 editor, per-language syntax selected dynamically.

import { EditorState, type Extension } from '@codemirror/state'
import {
  EditorView,
  keymap,
  lineNumbers,
  highlightActiveLine,
  drawSelection,
} from '@codemirror/view'
import {
  defaultKeymap,
  history,
  historyKeymap,
  indentWithTab,
} from '@codemirror/commands'
import {
  syntaxHighlighting,
  defaultHighlightStyle,
  bracketMatching,
  indentOnInput,
  StreamLanguage,
} from '@codemirror/language'
import { python } from '@codemirror/lang-python'
import { javascript } from '@codemirror/lang-javascript'
import { scheme } from '@codemirror/legacy-modes/mode/scheme'
import { clojure } from '@codemirror/legacy-modes/mode/clojure'
import type { Lang } from './types'

export type EditorHandle = {
  view: EditorView
  getValue(): string
  setValue(code: string): void
  focus(): void
}

const langExtensions: Record<Lang, () => Extension> = {
  python: () => python(),
  scheme: () => StreamLanguage.define(scheme),
  clojure: () => StreamLanguage.define(clojure),
  typescript: () => javascript({ typescript: true }),
}

export function createEditor(
  parent: HTMLElement,
  initialCode: string,
  lang: Lang,
  onSubmit: () => void,
): EditorHandle {
  const view = new EditorView({
    parent,
    state: EditorState.create({
      doc: initialCode.replace(/^\n+|\n+$/g, ''),
      extensions: [
        lineNumbers(),
        highlightActiveLine(),
        drawSelection(),
        history(),
        bracketMatching(),
        indentOnInput(),
        syntaxHighlighting(defaultHighlightStyle),
        langExtensions[lang](),
        keymap.of([
          ...defaultKeymap,
          ...historyKeymap,
          indentWithTab,
          {
            key: 'Ctrl-Enter',
            preventDefault: true,
            run: () => {
              onSubmit()
              return true
            },
          },
        ]),
        EditorView.theme({
          '&': {
            fontSize: '14px',
            fontFamily:
              'ui-monospace, SFMono-Regular, "SF Mono", Menlo, Monaco, Consolas, monospace',
          },
          '.cm-content': { padding: '8px 0' },
          '.cm-gutters': { backgroundColor: '#fafafa', border: 'none' },
          '.cm-line': { padding: '0 8px' },
          '&.cm-focused': { outline: 'none' },
        }),
      ],
    }),
  })

  return {
    view,
    getValue: () => view.state.doc.toString(),
    setValue: (code: string) => {
      view.dispatch({
        changes: { from: 0, to: view.state.doc.length, insert: code },
      })
    },
    focus: () => view.focus(),
  }
}
