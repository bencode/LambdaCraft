// TOC drawer controller.
// Two-state toggle: only the ☰ button flips open ↔ closed.
// No backdrop, no Escape, no close-on-link-click — this is a side panel,
// not a dialog.

export function initTocDrawer(): void {
  const drawer = document.querySelector<HTMLElement>('.toc-drawer')
  const toggle = document.querySelector<HTMLButtonElement>('[data-action="toggle-toc"]')
  if (!drawer || !toggle) return

  toggle.addEventListener('click', () => {
    const isOpen = drawer.dataset.state === 'open'
    drawer.dataset.state = isOpen ? 'closed' : 'open'
    drawer.setAttribute('aria-hidden', isOpen ? 'true' : 'false')
    toggle.setAttribute('aria-expanded', isOpen ? 'false' : 'true')
    if (isOpen) drawer.setAttribute('inert', '')
    else drawer.removeAttribute('inert')
  })
}
