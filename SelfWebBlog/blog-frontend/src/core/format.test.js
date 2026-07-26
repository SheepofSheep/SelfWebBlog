import test from 'node:test'
import assert from 'node:assert/strict'
import { postTitle } from './format.js'

test('untitled posts use the author text without inventing an image-only title', () => {
  assert.equal(postTitle({ title: '', content: '## 第一段\n\n后续内容' }), '第一段')
  assert.equal(
    postTitle({ title: '', content: '![图片](/uploads/a.webp)', createTime: '2026-07-26' }),
    ''
  )
})
