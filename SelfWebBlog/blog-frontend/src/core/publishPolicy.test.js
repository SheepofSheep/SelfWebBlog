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
  assert.equal(canPublish('<div></div><br>&nbsp;\u200B'), false)
  assert.equal(canPublish('<style>body { color: red; }</style>'), false)
  assert.equal(canPublish('<!-- ![](/uploads/hidden.webp) -->'), false)
  assert.equal(canPublish('<script><img src="https://cdn.example/hidden.webp"></script>'), false)
  assert.equal(canPublish('正文中可以讨论 upload-failed 和 blob: 字样'), true)
  assert.equal(canPublish('正文 ![](blob:http://localhost/temp)'), true)
  assert.equal(hasPermanentMedia('![](/api/media/optimized/4)'), true)
})
