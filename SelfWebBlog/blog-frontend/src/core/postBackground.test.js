import test from 'node:test'
import assert from 'node:assert/strict'
import {
  backgroundValueForMode,
  normalizePostBackground,
  normalizePostBackgroundMode
} from './postBackground.js'

test('loading an existing card background preserves every compatible custom value', () => {
  assert.deepEqual(
    normalizePostBackground({ backgroundMode: 'GRADIENT', backgroundValue: 'mist' }),
    {
      backgroundMode: 'GRADIENT',
      backgroundValue: 'mist'
    }
  )
  assert.equal(
    backgroundValueForMode('CUSTOM_IMAGE', '/uploads/cards/custom.webp'),
    '/uploads/cards/custom.webp'
  )
  assert.equal(backgroundValueForMode('SOLID', '#1a2B3c'), '#1a2B3c')
})

test('switching to a mode with an incompatible value receives a safe default', () => {
  assert.equal(backgroundValueForMode('GRADIENT', '#ffffff'), 'tide')
  assert.equal(backgroundValueForMode('SOLID', 'mist'), '#dcecff')
  assert.equal(backgroundValueForMode('CUSTOM_IMAGE', 'javascript:alert(1)'), '')
  assert.equal(normalizePostBackgroundMode('SITE_DEFAULT'), 'NONE')
})
