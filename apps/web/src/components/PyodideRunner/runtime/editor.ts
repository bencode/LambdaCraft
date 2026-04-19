// Minimal CodeMirror 6 setup for Python editing.

import { EditorState } from '@codemirror/state'
import {
  EditorView,
  keymap,
  lineNumbers,
  highlightActiveLine,
  drawSelection,
} from '@codemirror/view'
import { python } from '@codemirror/lang-python'
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
} from '@codemirror/language'

export interface EditorHandle {
  view: EditorView
  getValue(): string
  setValue(code: string): void
  focus(): void
}

export function createEditor(
  parent: HTMLElement,
  initialCode: string,
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
        python(),
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
