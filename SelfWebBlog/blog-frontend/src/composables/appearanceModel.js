export const THEMES = ['light', 'dark', 'system']
export const MOTION_MODES = ['full', 'reduced', 'off']

export function resolveTheme(theme, prefersDark) {
  return theme === 'system' ? (prefersDark ? 'dark' : 'light') : theme
}

export function resolveReducedMotion(mode) {
  if (mode === 'off' || mode === 'reduced') return true
  return false
}

export function normalizePreference(value, options, fallback) {
  return options.includes(value) ? value : fallback
}
