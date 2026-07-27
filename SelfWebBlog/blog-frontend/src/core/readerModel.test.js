import test from 'node:test'
import assert from 'node:assert/strict'
import { headingId } from './readerModel.js'

test('reader heading ids are stable and safe', () => {
  assert.equal(headingId(' 第一节 ', 0), 'section-0-第一节')
  assert.equal(headingId('A / B', 2), 'section-2-a-b')
})
