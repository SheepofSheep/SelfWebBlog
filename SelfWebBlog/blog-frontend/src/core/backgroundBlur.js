export const DEFAULT_BACKGROUND_BLUR = 38
export const MAX_BACKGROUND_BLUR_PX = 20

export function normalizeBackgroundBlur(value) {
  if (value === null || value === undefined || value === '') return DEFAULT_BACKGROUND_BLUR
  const number = Number(value)
  if (!Number.isFinite(number)) return DEFAULT_BACKGROUND_BLUR
  return Math.min(100, Math.max(0, Math.round(number)))
}

export function backgroundBlurRadius(value) {
  return Math.round((MAX_BACKGROUND_BLUR_PX * normalizeBackgroundBlur(value)) / 100)
}
