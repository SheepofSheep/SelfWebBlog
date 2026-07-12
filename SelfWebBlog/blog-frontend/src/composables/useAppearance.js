import { computed, readonly, ref } from 'vue'
import {
  MOTION_MODES,
  normalizePreference,
  resolveReducedMotion,
  resolveTheme,
  THEMES
} from './appearanceModel'

const theme = ref(
  normalizePreference(
    localStorage.getItem('appearance.theme') || localStorage.getItem('theme'),
    THEMES,
    'system'
  )
)
const motionMode = ref(
  normalizePreference(localStorage.getItem('appearance.motion'), MOTION_MODES, 'system')
)
const prefersDark = ref(window.matchMedia('(prefers-color-scheme: dark)').matches)
const prefersReduced = ref(window.matchMedia('(prefers-reduced-motion: reduce)').matches)
const effectiveTheme = computed(() => resolveTheme(theme.value, prefersDark.value))
const reducedMotion = computed(() => resolveReducedMotion(motionMode.value, prefersReduced.value))
let initialized = false

function applyAppearance() {
  document.documentElement.dataset.theme = effectiveTheme.value
  document.documentElement.dataset.motion = reducedMotion.value ? 'reduced' : 'full'
  document.documentElement.style.colorScheme = effectiveTheme.value
}

function initializeAppearance() {
  if (initialized) return
  initialized = true
  const darkQuery = window.matchMedia('(prefers-color-scheme: dark)')
  const motionQuery = window.matchMedia('(prefers-reduced-motion: reduce)')
  darkQuery.addEventListener('change', (event) => {
    prefersDark.value = event.matches
    applyAppearance()
  })
  motionQuery.addEventListener('change', (event) => {
    prefersReduced.value = event.matches
    applyAppearance()
  })
  applyAppearance()
}

function setTheme(nextTheme, origin) {
  theme.value = normalizePreference(nextTheme, THEMES, 'system')
  localStorage.setItem('appearance.theme', theme.value)
  if (origin) {
    document.documentElement.style.setProperty('--theme-x', `${origin.x}px`)
    document.documentElement.style.setProperty('--theme-y', `${origin.y}px`)
  }
  const update = () => applyAppearance()
  if (!reducedMotion.value && document.startViewTransition) {
    document.startViewTransition(update)
  } else {
    update()
  }
}

function setMotionMode(nextMode) {
  motionMode.value = normalizePreference(nextMode, MOTION_MODES, 'system')
  localStorage.setItem('appearance.motion', motionMode.value)
  applyAppearance()
}

export function useAppearance() {
  return {
    theme: readonly(theme),
    motionMode: readonly(motionMode),
    effectiveTheme,
    reducedMotion,
    initializeAppearance,
    setTheme,
    setMotionMode
  }
}
