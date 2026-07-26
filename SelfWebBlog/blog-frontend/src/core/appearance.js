export function resolveAppearance(mode, prefersDark) {
  return mode === 'system' ? (prefersDark ? 'dark' : 'light') : mode
}

export function createLatestAppearanceCommitter(commit) {
  let current = 0
  return {
    issue(value) {
      const token = ++current
      return {
        token,
        isCurrent: () => token === current,
        commit() {
          if (token !== current) return false
          commit(value)
          return true
        }
      }
    }
  }
}

export function shouldAnimateAppearance({
  savedMotion,
  systemReduced,
  hasViewTransition,
  hasOrigin
}) {
  return (
    savedMotion !== 'reduced' && !systemReduced && Boolean(hasViewTransition) && Boolean(hasOrigin)
  )
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
  let activeTransition = null
  let activeAnimation = null
  let fallbackTimer = 0
  const normalizeMode = (mode) => (['light', 'dark', 'system'].includes(mode) ? mode : 'system')
  const commit = (requestedMode) => {
    const mode = normalizeMode(requestedMode)
    localStorage.setItem('appearance', mode)
    document.documentElement.dataset.theme = resolveAppearance(mode, media.matches)
    document.documentElement.dataset.appearance = mode
  }
  const coordinator = createLatestAppearanceCommitter(commit)
  const apply = (mode = localStorage.getItem('appearance') || 'system', origin) => {
    const ticket = coordinator.issue(normalizeMode(mode))
    const savedMotion = localStorage.getItem('motion') || 'full'
    const systemReduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches
    const canExpand = shouldAnimateAppearance({
      savedMotion,
      systemReduced,
      hasViewTransition: typeof document.startViewTransition === 'function',
      hasOrigin: Boolean(origin)
    })
    activeTransition?.skipTransition?.()
    activeAnimation?.cancel?.()
    window.clearTimeout(fallbackTimer)
    if (!canExpand) {
      const shouldFallback = savedMotion !== 'reduced' && !systemReduced && Boolean(origin)
      if (shouldFallback) document.documentElement.classList.add('theme-changing')
      ticket.commit()
      if (shouldFallback) {
        fallbackTimer = window.setTimeout(
          () => document.documentElement.classList.remove('theme-changing'),
          260
        )
      } else {
        document.documentElement.classList.remove('theme-changing')
      }
      return
    }
    const x = origin.clientX ?? innerWidth / 2
    const y = origin.clientY ?? innerHeight / 2
    const radius = Math.hypot(Math.max(x, innerWidth - x), Math.max(y, innerHeight - y))
    const transition = document.startViewTransition(() => ticket.commit())
    activeTransition = transition
    transition.ready
      .then(() => {
        if (!ticket.isCurrent()) return
        activeAnimation = document.documentElement.animate(
          { clipPath: [`circle(0 at ${x}px ${y}px)`, `circle(${radius}px at ${x}px ${y}px)`] },
          {
            duration: 520,
            easing: 'cubic-bezier(.2,.75,.2,1)',
            pseudoElement: '::view-transition-new(root)'
          }
        )
      })
      .catch(() => {})
      .finally(() => {
        if (activeTransition === transition) activeTransition = null
      })
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
