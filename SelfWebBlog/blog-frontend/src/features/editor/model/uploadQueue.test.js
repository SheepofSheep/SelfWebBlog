import assert from 'node:assert/strict'
import test from 'node:test'
import { createUploadQueue } from './uploadQueue.js'

test('enforces upload task transitions and retry', () => {
  const queue = createUploadQueue()
  const task = queue.add({ name: 'a.png' }, 'a')
  queue.transition(task.id, 'uploading')
  queue.transition(task.id, 'failed', { error: 'network' })
  queue.retry(task.id)
  assert.equal(queue.get(task.id).status, 'queued')
  assert.throws(() => queue.transition(task.id, 'completed'))
})

test('out-of-order completion keeps task identity', () => {
  const queue = createUploadQueue()
  const first = queue.add({ name: 'a.png' }, 'a')
  const second = queue.add({ name: 'b.png' }, 'b')
  queue.transition(first.id, 'uploading')
  queue.transition(second.id, 'uploading')
  queue.transition(second.id, 'completed', { markdown: '![b](b.webp)' })
  queue.transition(first.id, 'completed', { markdown: '![a](a.webp)' })
  assert.equal(queue.get(first.id).markdown, '![a](a.webp)')
  assert.equal(queue.get(second.id).markdown, '![b](b.webp)')
})
