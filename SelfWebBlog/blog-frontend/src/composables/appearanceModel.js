export const THEMES = ['light', 'dark', 'system']
export const MOTION_MODES = ['full', 'reduced', 'system']

export function resolveTheme(theme, prefersDark) {
  return theme === 'system' ? (prefersDark ? 'dark' : 'light') : theme
}

export function resolveReducedMotion(mode, prefersReduced) {
  return mode === 'system' ? prefersReduced : mode === 'reduced'
}

export function normalizePreference(value, options, fallback) {
  return options.includes(value) ? value : fallback
}
