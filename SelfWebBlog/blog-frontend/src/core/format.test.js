import test from 'node:test'
import assert from 'node:assert/strict'
import { avatarInitial, firstPostMediaUrl, postPublicationTime, postTitle } from './format.js'

test('untitled posts use author text or a neutral media-only fallback', () => {
  assert.equal(postTitle({ title: '', content: '## 第一段\n\n后续内容' }), '第一段')
  assert.equal(
    postTitle({ title: '', content: '![图片](/uploads/a.webp)', createTime: '2026-07-26' }),
    '无题'
  )
})

test('avatar fallback uses the first visible character without inventing a name', () => {
  assert.equal(avatarInitial('  小明  '), '小')
  assert.equal(avatarInitial(''), '·')
})

test('public article dates prefer the actual first publication time', () => {
  assert.equal(
    postPublicationTime({
      createTime: '2026-07-01T08:00:00',
      publishedTime: '2026-07-26T09:00:00'
    }),
    '2026-07-26T09:00:00'
  )
  assert.equal(postPublicationTime({ createTime: '2026-07-01T08:00:00' }), '2026-07-01T08:00:00')
})

test('card media extraction ignores an optional markdown image title', () => {
  assert.equal(
    firstPostMediaUrl('![图](https://img.example/a.jpg "说明")'),
    'https://img.example/a.jpg'
  )
  assert.equal(firstPostMediaUrl('<img src="/uploads/a.webp" alt="图">'), '/uploads/a.webp')
})
