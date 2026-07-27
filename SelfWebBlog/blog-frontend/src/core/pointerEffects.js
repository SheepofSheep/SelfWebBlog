export const TRAIL_LIMIT = 32
export const TRAIL_MAX_AGE = 260
export const RIPPLE_LIMIT = 4
export const RIPPLE_DURATION = 520

const clamp = (value, min, max) => Math.min(max, Math.max(min, value))

export function canRenderPointerEffects(state) {
  return (
    !state.siteReduced &&
    !state.systemReduced &&
    state.finePointer &&
    state.hoverCapable &&
    state.visible &&
    state.focused
  )
}

export function trailPointLimit(speed = 0, interactive = false) {
  const speedRatio = clamp((speed - 0.05) / 1.45, 0, 1)
  return clamp(Math.round(10 + speedRatio * 22) - (interactive ? 4 : 0), 8, TRAIL_LIMIT)
}

export function isPrimaryTap({ button, distance, pointerType, cancelled = false }) {
  return !cancelled && pointerType === 'mouse' && button === 0 && distance <= 7
}

export function trimTrailPoints(points, { now, maxPoints, maxAge = TRAIL_MAX_AGE }) {
  return points.filter((point) => now - point.time < maxAge).slice(-maxPoints)
}

export function createRipple(x, y, createdAt, phase = 0) {
  return { x, y, createdAt, phase }
}

export function appendRipple(ripples, ripple, limit = RIPPLE_LIMIT) {
  return [...ripples, ripple].slice(-limit)
}

export function rippleFrame(ripple, now) {
  const elapsed = now - ripple.createdAt
  if (elapsed < 0) {
    return { active: false, progress: 0, radius: 10, alpha: 0, lineWidth: 2.2 }
  }

  const progress = clamp(elapsed / RIPPLE_DURATION, 0, 1)
  const eased = 1 - Math.pow(1 - progress, 3)
  return {
    active: progress < 1,
    progress,
    radius: 10 + eased * 78,
    alpha: Math.pow(1 - progress, 1.7),
    lineWidth: 2.2 - progress * 1.35
  }
}

export function resolveCanvasDpr({
  deviceDpr = 1,
  width,
  height,
  maxDpr = 1.75,
  maxPixels = 4_000_000
}) {
  const pixelCap = Math.sqrt(maxPixels / Math.max(1, width * height))
  return Math.max(Number.EPSILON, Math.min(deviceDpr, maxDpr, pixelCap))
}
