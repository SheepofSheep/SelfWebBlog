import test from 'node:test'
import assert from 'node:assert/strict'
import {
  archivePostDate,
  collectAllPosts,
  filterArchivePosts,
  groupArchive
} from './archiveModel.js'

test('archive groups and posts are ordered newest first', () => {
  const groups = groupArchive([
    { id: 'old', createTime: '2025-05-01T10:00:00' },
    {
      id: 'newer',
      createTime: '2024-01-01T10:00:00',
      publishedTime: '2026-07-20T10:00:00'
    },
    { id: 'newest', createTime: '2026-07-26T10:00:00' },
    { id: 'missing-date' },
    { id: 'invalid-date', createTime: 'not-a-date' }
  ])

  assert.deepEqual(
    groups.map((group) => group.year),
    [2026, 2025]
  )
  assert.deepEqual(
    groups[0].posts.map((post) => post.id),
    ['newest', 'newer']
  )
  assert.equal(archivePostDate(groups[0].posts[1]), '2026-07-20T10:00:00')
})

test('archive filter includes title summary content and tags', () => {
  const posts = [
    { id: '1', title: '', summary: '', content: '夏天的正文', tags: ['生活'] },
    { id: '2', title: '代码', summary: '', content: '', tags: 'Vue, Java' }
  ]

  assert.deepEqual(
    filterArchivePosts(posts, '夏天').map((post) => post.id),
    ['1']
  )
  assert.deepEqual(
    filterArchivePosts(posts, 'java').map((post) => post.id),
    ['2']
  )
})

test('archive collection follows pagination until every public post is loaded', async () => {
  const pages = [
    { records: [{ id: '1' }, { id: '2' }], total: 3, pages: 2 },
    { records: [{ id: '3' }], total: 3, pages: 2 }
  ]
  const requested = []

  const posts = await collectAllPosts(async (page, size) => {
    requested.push([page, size])
    return pages[page - 1]
  }, 2)

  assert.deepEqual(
    posts.map((post) => post.id),
    ['1', '2', '3']
  )
  assert.deepEqual(requested, [
    [1, 2],
    [2, 2]
  ])
})
