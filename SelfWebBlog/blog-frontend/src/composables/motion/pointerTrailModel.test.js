import assert from 'node:assert/strict'
import test from 'node:test'
import { shouldEnablePointerTrail } from './pointerTrailModel.js'

test('disables pointer trail for reduced motion and coarse pointers', () => {
  assert.equal(shouldEnablePointerTrail({ motion: 'reduced', finePointer: true }), false)
  assert.equal(shouldEnablePointerTrail({ motion: 'full', finePointer: false }), false)
  assert.equal(shouldEnablePointerTrail({ motion: 'full', finePointer: true }), true)
})

test('disables pointer trail when motion is off or the page is hidden', () => {
  assert.equal(shouldEnablePointerTrail({ motion: 'off', finePointer: true }), false)
  assert.equal(
    shouldEnablePointerTrail({ motion: 'full', finePointer: true, documentVisible: false }),
    false
  )
})
