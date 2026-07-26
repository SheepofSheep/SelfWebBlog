import test from 'node:test'
import assert from 'node:assert/strict'
import {
  createLatestAppearanceCommitter,
  resolveAppearance,
  shouldAnimateAppearance
} from './appearance.js'

test('system mode follows media while explicit modes stay stable', () => {
  assert.equal(resolveAppearance('system', true), 'dark')
  assert.equal(resolveAppearance('system', false), 'light')
  assert.equal(resolveAppearance('light', true), 'light')
  assert.equal(resolveAppearance('dark', false), 'dark')
})

test('latest appearance ticket prevents an older transition from committing late', () => {
  const values = []
  const coordinator = createLatestAppearanceCommitter((value) => values.push(value))
  const first = coordinator.issue('dark')
  const second = coordinator.issue('light')

  assert.equal(first.commit(), false)
  assert.equal(second.commit(), true)
  assert.deepEqual(values, ['light'])
})

test('saved reduced motion disables theme expansion independently of system motion', () => {
  assert.equal(
    shouldAnimateAppearance({
      savedMotion: 'reduced',
      systemReduced: false,
      hasViewTransition: true,
      hasOrigin: true
    }),
    false
  )
  assert.equal(
    shouldAnimateAppearance({
      savedMotion: 'full',
      systemReduced: false,
      hasViewTransition: true,
      hasOrigin: true
    }),
    true
  )
})
