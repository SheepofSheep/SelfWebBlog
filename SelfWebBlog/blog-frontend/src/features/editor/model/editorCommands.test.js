import assert from 'node:assert/strict'
import test from 'node:test'
import {
  applyEditorCommand,
  replaceUploadPlaceholder,
  uploadPlaceholder
} from './editorCommands.js'

test('wraps selected text and keeps the selection inside markers', () => {
  const result = applyEditorCommand('bold', { text: 'hello world', from: 6, to: 11 })
  assert.equal(result.text, 'hello **world**')
  assert.deepEqual(result.selection, { from: 8, to: 13 })
})

test('inserts structural markdown commands', () => {
  assert.equal(applyEditorCommand('heading', { text: 'Title', from: 0, to: 5 }).text, '## Title')
  assert.match(
    applyEditorCommand('link', { text: 'docs', from: 0, to: 4 }).text,
    /\[docs\]\(https:\/\/\)/
  )
  assert.match(
    applyEditorCommand('codeBlock', { text: 'const a = 1', from: 0, to: 11 }).text,
    /```/
  )
  assert.match(applyEditorCommand('table', { text: '', from: 0, to: 0 }).text, /\| 列 1 \|/)
  assert.equal(applyEditorCommand('task', { text: '', from: 0, to: 0 }).text, '- [ ] 待办事项')
})

test('replaces only the matching upload placeholder', () => {
  const first = uploadPlaceholder('a')
  const second = uploadPlaceholder('b')
  const text = `${first}\n${second}`
  assert.equal(replaceUploadPlaceholder(text, 'b', '![图](b.webp)'), `${first}\n![图](b.webp)`)
})
