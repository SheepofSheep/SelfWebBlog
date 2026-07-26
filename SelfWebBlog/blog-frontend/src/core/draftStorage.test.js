import test from 'node:test'
import assert from 'node:assert/strict'
import { draftKey, hasUsefulDraft, readDraft, stripLocalMedia, writeDraft } from './draftStorage.js'

function storage() {
  const values = new Map()
  return {
    getItem: (key) => values.get(key) ?? null,
    setItem: (key, value) => values.set(key, value),
    removeItem: (key) => values.delete(key)
  }
}

test('drafts are scoped and never persist unusable blob preview urls', () => {
  const target = storage()
  assert.equal(draftKey(), 'postDraft:new')
  assert.equal(draftKey('42'), 'postDraft:42')
  assert.equal(stripLocalMedia('text\n![x](blob:http://local/1)'), 'text\n')
  assert.equal(hasUsefulDraft({ content: '  ' }), false)
  writeDraft(target, draftKey(), { title: '保留', content: '正文' })
  assert.equal(readDraft(target, draftKey()).title, '保留')
})
