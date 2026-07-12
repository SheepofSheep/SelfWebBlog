import test from 'node:test'
import assert from 'node:assert/strict'
import {
  getDraftKey,
  hasUsefulDraft,
  migrateLegacyDraft,
  readDraft,
  writeDraft
} from './draftStorage.js'

function memoryStorage() {
  const values = new Map()
  return {
    getItem: (key) => values.get(key) ?? null,
    setItem: (key, value) => values.set(key, value),
    removeItem: (key) => values.delete(key)
  }
}

test('draft keys isolate new and existing posts', () => {
  assert.equal(getDraftKey(null), 'postDraft:new')
  assert.equal(getDraftKey('42'), 'postDraft:42')
})

test('legacy draft migrates to its scoped key', () => {
  const storage = memoryStorage()
  writeDraft(storage, 'postDraft', { editingId: '42', title: 'draft' })

  migrateLegacyDraft(storage)

  assert.equal(readDraft(storage, 'postDraft'), null)
  assert.equal(readDraft(storage, 'postDraft:42').title, 'draft')
})

test('empty metadata is not treated as useful draft content', () => {
  assert.equal(hasUsefulDraft({ postStatus: 'PUBLISHED' }), false)
  assert.equal(hasUsefulDraft({ content: 'hello' }), true)
})
