import test from 'node:test'
import assert from 'node:assert/strict'
import { canPublish, hasPermanentMedia } from './publishPolicy.js'

test('publishes text-only or permanent-media-only posts', () => {
  assert.equal(canPublish('正文'), true)
  assert.equal(canPublish('`inlineCode()`'), true)
  assert.equal(canPublish('![](/uploads/photo.webp)'), true)
  assert.equal(canPublish('<video src="https://cdn.example/video.mp4"></video>'), true)
})

test('rejects empty formatting, blob previews, and failed upload markers', () => {
  assert.equal(canPublish('  # **  '), false)
  assert.equal(canPublish('![](blob:http://localhost/temp)'), false)
  assert.equal(canPublish('<!-- upload-failed -->'), false)
  assert.equal(hasPermanentMedia('![](/api/media/optimized/4)'), true)
})
