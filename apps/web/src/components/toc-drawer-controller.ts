// TOC drawer controller.
// Default closed; toggled via .toc-toggle button.
// Closes on backdrop click, Escape, or in-drawer link click.

type DrawerEls = {
  drawer: HTMLElement
  toggle: HTMLButtonElement
  backdrop: HTMLElement
}

function queryDrawerEls(): DrawerEls | null {
  const drawer = document.querySelector<HTMLElement>('.toc-drawer')
  const toggle = document.querySelector<HTMLButtonElement>('[data-action="toggle-toc"]')
  const backdrop = drawer?.querySelector<HTMLElement>('.toc-drawer-backdrop') ?? null
  if (!drawer || !toggle || !backdrop) return null
  return { drawer, toggle, backdrop }
}

export function initTocDrawer(): void {
  const els = queryDrawerEls()
  if (!els) return

  const open = (): void => {
    els.drawer.dataset.state = 'open'
    els.drawer.setAttribute('aria-hidden', 'false')
    els.toggle.setAttribute('aria-expanded', 'true')
  }

  const close = (): void => {
    els.drawer.dataset.state = 'closed'
    els.drawer.setAttribute('aria-hidden', 'true')
    els.toggle.setAttribute('aria-expanded', 'false')
  }

  const toggleState = (): void => {
    if (els.drawer.dataset.state === 'open') close()
    else open()
  }

  els.toggle.addEventListener('click', toggleState)
  els.backdrop.addEventListener('click', close)

  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape' && els.drawer.dataset.state === 'open') close()
  })

  els.drawer.querySelectorAll<HTMLAnchorElement>('a[href^="#"]').forEach((a) => {
    a.addEventListener('click', close)
  })
}
