import test from 'node:test'
import assert from 'node:assert/strict'
import {
  normalizePreference,
  resolveReducedMotion,
  resolveTheme,
  THEMES
} from './appearanceModel.js'

test('system theme follows the operating system', () => {
  assert.equal(resolveTheme('system', true), 'dark')
  assert.equal(resolveTheme('system', false), 'light')
})

test('explicit motion preference overrides the operating system', () => {
  assert.equal(resolveReducedMotion('full', true), false)
  assert.equal(resolveReducedMotion('reduced', false), true)
})

test('unknown stored preferences fall back safely', () => {
  assert.equal(normalizePreference('neon', THEMES, 'system'), 'system')
})
