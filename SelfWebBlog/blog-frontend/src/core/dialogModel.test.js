import test from 'node:test'
import assert from 'node:assert/strict'
import { nextDialogFocusIndex } from './dialogModel.js'

test('dialog focus wraps in both tab directions', () => {
  assert.equal(nextDialogFocusIndex(2, 3, 1), 0)
  assert.equal(nextDialogFocusIndex(0, 3, -1), 2)
  assert.equal(nextDialogFocusIndex(1, 3, 1), 2)
  assert.equal(nextDialogFocusIndex(-1, 0, 1), -1)
})
