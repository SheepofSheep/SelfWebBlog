import test from 'node:test'
import assert from 'node:assert/strict'
import { resolveAppearance } from './appearance.js'

test('system mode follows media while explicit modes stay stable', () => {
  assert.equal(resolveAppearance('system', true), 'dark')
  assert.equal(resolveAppearance('system', false), 'light')
  assert.equal(resolveAppearance('light', true), 'light')
  assert.equal(resolveAppearance('dark', false), 'dark')
})
