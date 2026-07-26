import test from 'node:test'
import assert from 'node:assert/strict'
import { toggleReplyGroup, visibleReplies } from './threadModel.js'

test('replies stay collapsed until their root is expanded', () => {
  const replies = [{ id: '2' }, { id: '3' }]
  assert.deepEqual(visibleReplies(replies, false), [])
  assert.deepEqual(visibleReplies(replies, true), replies)
  assert.deepEqual(toggleReplyGroup(new Set(), '1'), new Set(['1']))
})
