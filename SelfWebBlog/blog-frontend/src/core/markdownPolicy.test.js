import test from 'node:test'
import assert from 'node:assert/strict'
import { editorMediaUrlPattern } from './markdownPolicy.js'

test('editor preview permits local blobs without permitting script URLs', () => {
  assert.equal(editorMediaUrlPattern.test('blob:http://localhost/image-id'), true)
  assert.equal(editorMediaUrlPattern.test('/uploads/article/image.webp'), true)
  assert.equal(editorMediaUrlPattern.test('javascript:alert(1)'), false)
})
