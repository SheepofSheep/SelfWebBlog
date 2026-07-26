import test from 'node:test'
import assert from 'node:assert/strict'
import {
  appendThreadReply,
  createLatestLikeQueue,
  mergeThreadRoots,
  patchThreadItem,
  replyTargetName,
  scrollBehaviorForMotion,
  sortPinned,
  toggleReplyGroup,
  visibleReplies
} from './threadModel.js'

function deferred() {
  let resolve
  let reject
  const promise = new Promise((onResolve, onReject) => {
    resolve = onResolve
    reject = onReject
  })
  return { promise, resolve, reject }
}

async function flushPromises() {
  await Promise.resolve()
  await Promise.resolve()
}

test('root pages merge without duplicates and keep the public pin ordering', () => {
  const firstPage = [
    { id: 'pinned', pinned: true, pinOrder: 2, createTime: '2026-07-20T12:00:00' },
    { id: 'newer', pinned: false, createTime: '2026-07-26T12:00:00' }
  ]
  const secondPage = [
    {
      id: 'pinned',
      pinned: true,
      pinOrder: 1,
      likeCount: 4,
      createTime: '2026-07-20T12:00:00'
    },
    { id: 'older', pinned: false, createTime: '2026-07-25T12:00:00' }
  ]

  const merged = mergeThreadRoots(firstPage, secondPage)

  assert.deepEqual(
    merged.map((item) => item.id),
    ['pinned', 'newer', 'older']
  )
  assert.equal(merged[0].likeCount, 4)
  assert.equal(merged[0].pinOrder, 1)
})

test('a posted reply patches its loaded root without dropping other root pages', () => {
  const loaded = [
    { id: 'root-1', replies: [{ id: 'reply-1' }] },
    { id: 'root-21', replies: [] }
  ]

  const patched = appendThreadReply(loaded, 'root-21', { id: 'reply-22', rootId: 'root-21' })
  const duplicate = appendThreadReply(patched, 'root-21', {
    id: 'reply-22',
    rootId: 'root-21',
    likeCount: 1
  })

  assert.deepEqual(
    duplicate.map((item) => item.id),
    ['root-1', 'root-21']
  )
  assert.deepEqual(
    duplicate[1].replies.map((item) => item.id),
    ['reply-22']
  )
  assert.equal(duplicate[1].replies[0].likeCount, 1)
})

test('replies stay collapsed until their root is expanded', () => {
  const replies = [{ id: '2' }, { id: '3' }]
  assert.deepEqual(visibleReplies(replies, false), [])
  assert.deepEqual(visibleReplies(replies, true), replies)
  assert.deepEqual(toggleReplyGroup(new Set(), '1'), new Set(['1']))
})

test('pinned roots use explicit pin order before creation time', () => {
  const sorted = sortPinned([
    { id: 'plain', pinned: false, createTime: '2026-07-26T12:00:00' },
    { id: 'second', pinned: true, pinOrder: 2, createTime: '2026-07-26T11:00:00' },
    { id: 'first', pinned: true, pinOrder: 1, createTime: '2026-07-25T11:00:00' }
  ])

  assert.deepEqual(
    sorted.map((item) => item.id),
    ['first', 'second', 'plain']
  )
})

test('reply target names resolve roots and sibling replies', () => {
  const root = { id: 'root', nickname: '甲' }
  const replies = [{ id: 'reply', nickname: '乙', replyToId: 'root' }]

  assert.equal(replyTargetName(root, replies[0], replies), '甲')
  assert.equal(replyTargetName(root, { replyToId: 'reply' }, replies), '乙')
  assert.equal(replyTargetName(root, { replyToNickname: '快照昵称' }, []), '快照昵称')
})

test('thread patches roots and replies without changing unrelated entries', () => {
  const thread = [{ id: 'root', likeCount: 0, replies: [{ id: 'reply', likeCount: 1 }] }]
  const next = patchThreadItem(thread, 'reply', (item) => ({ ...item, likeCount: 2 }))

  assert.equal(next[0].likeCount, 0)
  assert.equal(next[0].replies[0].likeCount, 2)
})

test('rapid like toggles serialize requests and keep the newest user intent visible', async () => {
  const requests = []
  const rendered = []
  const queue = createLatestLikeQueue(
    { liked: false, count: 4 },
    {
      request(liked) {
        const task = deferred()
        requests.push({ liked, ...task })
        return task.promise
      },
      onChange: (state) => rendered.push(state)
    }
  )

  queue.toggle()
  queue.toggle()

  assert.equal(requests.length, 1)
  assert.equal(requests[0].liked, true)
  assert.deepEqual(rendered.at(-1), { liked: false, count: 4, pending: true })

  requests[0].resolve({ liked: true, count: 5 })
  await flushPromises()

  assert.equal(requests.length, 2)
  assert.equal(requests[1].liked, false)
  assert.deepEqual(rendered.at(-1), { liked: false, count: 4, pending: true })

  requests[1].resolve({ liked: false, count: 4 })
  await queue.waitForIdle()

  assert.deepEqual(queue.snapshot(), { liked: false, count: 4, pending: false })
  assert.deepEqual(rendered.at(-1), { liked: false, count: 4, pending: false })
})

test('a failed newest like intent rolls back to the last confirmed server state', async () => {
  const task = deferred()
  const errors = []
  const queue = createLatestLikeQueue(
    { liked: false, count: 7 },
    {
      request: () => task.promise,
      onError: (error) => errors.push(error.message)
    }
  )

  queue.toggle()
  task.reject(new Error('点赞失败'))
  await queue.waitForIdle()

  assert.deepEqual(queue.snapshot(), { liked: false, count: 7, pending: false })
  assert.deepEqual(errors, ['点赞失败'])
})

test('new replies scroll smoothly unless reduced motion is requested', () => {
  assert.equal(scrollBehaviorForMotion(false), 'smooth')
  assert.equal(scrollBehaviorForMotion(true), 'auto')
})
