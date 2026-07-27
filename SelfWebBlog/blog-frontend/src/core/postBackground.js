const GRADIENTS = new Set(['tide', 'mist', 'dusk', 'leaf', 'ember'])
const CUSTOM_IMAGE = /^(?:https?:\/\/|\/uploads\/|\/api\/media\/)[^\s<>]+$/i
const SOLID = /^#[0-9a-f]{6}$/i

export function normalizePostBackgroundMode(value) {
  return value === 'SITE_DEFAULT' ? 'NONE' : value || 'COVER'
}

export function backgroundValueForMode(mode, currentValue = '') {
  const normalizedMode = normalizePostBackgroundMode(mode)
  const value = String(currentValue || '').trim()
  if (normalizedMode === 'GRADIENT') return GRADIENTS.has(value) ? value : 'tide'
  if (normalizedMode === 'SOLID') return SOLID.test(value) ? value : '#dcecff'
  if (normalizedMode === 'CUSTOM_IMAGE') return CUSTOM_IMAGE.test(value) ? value : ''
  return ''
}

export function normalizePostBackground(source = {}) {
  const backgroundMode = normalizePostBackgroundMode(source.backgroundMode)
  return {
    backgroundMode,
    backgroundValue: backgroundValueForMode(backgroundMode, source.backgroundValue)
  }
}
