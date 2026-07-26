export function resolveAppearance(mode, prefersDark) {
  return mode === 'system' ? (prefersDark ? 'dark' : 'light') : mode
}

let appearanceController

export function installAppearance() {
  if (appearanceController) return appearanceController
  const media = window.matchMedia('(prefers-color-scheme: dark)')
  const setMotion = (mode) => {
    const normalized = mode === 'reduced' ? 'reduced' : 'full'
    localStorage.setItem('motion', normalized)
    document.documentElement.dataset.motion = normalized
  }
  const commit = (mode) => {
    localStorage.setItem('appearance', mode)
    document.documentElement.dataset.theme = resolveAppearance(mode, media.matches)
    document.documentElement.dataset.appearance = mode
  }
  const apply = (mode = localStorage.getItem('appearance') || 'system', origin) => {
    const reduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches
    if (!document.startViewTransition || reduced || !origin) return commit(mode)
    const x = origin.clientX ?? innerWidth / 2
    const y = origin.clientY ?? innerHeight / 2
    const radius = Math.hypot(Math.max(x, innerWidth - x), Math.max(y, innerHeight - y))
    const transition = document.startViewTransition(() => commit(mode))
    transition.ready
      .then(() =>
        document.documentElement.animate(
          { clipPath: [`circle(0 at ${x}px ${y}px)`, `circle(${radius}px at ${x}px ${y}px)`] },
          {
            duration: 520,
            easing: 'cubic-bezier(.2,.75,.2,1)',
            pseudoElement: '::view-transition-new(root)'
          }
        )
      )
      .catch(() => {})
  }
  commit(localStorage.getItem('appearance') || 'system')
  setMotion(localStorage.getItem('motion') || 'full')
  media.addEventListener('change', () => {
    if ((localStorage.getItem('appearance') || 'system') === 'system') apply('system')
  })
  appearanceController = {
    apply,
    current: () => localStorage.getItem('appearance') || 'system',
    setMotion,
    motion: () => localStorage.getItem('motion') || 'full'
  }
  return appearanceController
}
