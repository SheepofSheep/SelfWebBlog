import test from 'node:test'
import assert from 'node:assert/strict'
import { filterArticles } from './articleFilter.js'

const posts = [
  { title: '标题', summary: '', content: '正文内容', tags: '日常' },
  { title: '另一篇', summary: '摘要', content: '', tags: '影像' }
]

test('article discovery filters shared title, body, summary and tag data', () => {
  assert.equal(filterArticles(posts, '正文').length, 1)
  assert.equal(filterArticles(posts, '影像').length, 1)
  assert.equal(filterArticles(posts, '').length, 2)
})
