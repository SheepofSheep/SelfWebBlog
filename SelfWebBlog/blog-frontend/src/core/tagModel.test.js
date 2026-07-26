import test from 'node:test'
import assert from 'node:assert/strict'
import { commitTag, normalizeTags } from './tagModel.js'

test('one input creates reusable, case-insensitive tags', () => {
  assert.deepEqual(normalizeTags([' Vue ', 'vue', '生活　随笔']), ['Vue', '生活 随笔'])
  assert.deepEqual(commitTag(['Vue'], ' vue '), { tags: ['Vue'], input: '' })
  assert.deepEqual(commitTag(['Vue'], '随笔'), { tags: ['Vue', '随笔'], input: '' })
})
