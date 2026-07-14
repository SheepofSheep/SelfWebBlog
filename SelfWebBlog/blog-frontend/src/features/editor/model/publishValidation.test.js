import assert from 'node:assert/strict'
import test from 'node:test'
import { validatePost } from './publishValidation.js'

test('draft only needs a title or body', () => {
  assert.equal(validatePost({ title: 'Draft' }, 'DRAFT').valid, true)
  assert.equal(validatePost({}, 'DRAFT').valid, false)
})

test('published post requires complete metadata and safe uploads', () => {
  const complete = {
    title: 'Title',
    content: 'Body',
    summary: 'Summary',
    coverUrl: '/uploads/cover.webp',
    category: 'Tech',
    tags: 'Vue, Java'
  }
  assert.equal(validatePost(complete, 'PUBLISHED').valid, true)
  assert.match(validatePost({ ...complete, tags: 'Vue,vue' }, 'PUBLISHED').issues[0].message, /重复/)
  assert.match(
    validatePost({ ...complete, content: '![正在上传](uploading:a)' }, 'PUBLISHED').issues[0]
      .message,
    /上传/
  )
  assert.match(validatePost({ ...complete, content: '![](/uploads/a.png)' }, 'PUBLISHED').issues[0].message, /替代文本/)
})
