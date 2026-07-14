import assert from 'node:assert/strict'
import test from 'node:test'
import { applyOptimisticLike, insertReply, normalizeThread } from './threadModel.js'

test('normalizes flat and nested interaction responses', () => {
  const flat = normalizeThread([
    { id: '1', content: 'root' },
    { id: '2', rootId: '1', content: 'reply' }
  ])
  const nested = normalizeThread([{ id: '1', content: 'root', replies: [{ id: '2' }] }])

  assert.equal(flat[0].replies[0].id, '2')
  assert.equal(nested[0].replies[0].id, '2')
})

test('optimistic like includes a rollback snapshot', () => {
  const item = { id: '1', liked: false, likeCount: 2 }
  const result = applyOptimisticLike(item, true)

  assert.deepEqual(result.next, { id: '1', liked: true, likeCount: 3 })
  assert.deepEqual(result.rollback, item)
})

test('inserting a reply preserves root order', () => {
  const roots = [
    { id: '1', replies: [] },
    { id: '3', replies: [] }
  ]
  const next = insertReply(roots, '1', { id: '2' })

  assert.deepEqual(
    next.map((item) => item.id),
    ['1', '3']
  )
  assert.equal(next[0].replies[0].id, '2')
})
