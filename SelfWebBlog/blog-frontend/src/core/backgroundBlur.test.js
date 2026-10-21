import test from 'node:test'
import assert from 'node:assert/strict'
import {
  DEFAULT_BACKGROUND_BLUR,
  backgroundBlurRadius,
  normalizeBackgroundBlur
} from './backgroundBlur.js'

test('background blur normalizes to a whole percentage within 0–100', () => {
  assert.equal(normalizeBackgroundBlur(38), 38)
  assert.equal(normalizeBackgroundBlur('72'), 72)
  assert.equal(normalizeBackgroundBlur(-4), 0)
  assert.equal(normalizeBackgroundBlur(140), 100)
  assert.equal(normalizeBackgroundBlur('nope'), DEFAULT_BACKGROUND_BLUR)
  assert.equal(normalizeBackgroundBlur(null), DEFAULT_BACKGROUND_BLUR)
  assert.equal(normalizeBackgroundBlur(''), DEFAULT_BACKGROUND_BLUR)
})

test('background blur percentage maps to a bounded canvas radius', () => {
  assert.equal(backgroundBlurRadius(0), 0)
  assert.equal(backgroundBlurRadius(38), 8)
  assert.equal(backgroundBlurRadius(100), 20)
})
