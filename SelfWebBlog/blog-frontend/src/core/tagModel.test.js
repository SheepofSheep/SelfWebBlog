import test from 'node:test'
import assert from 'node:assert/strict'
import { commitTag, normalizeTags, prepareTagCreation } from './tagModel.js'

test('one input creates reusable, case-insensitive tags', () => {
  assert.deepEqual(normalizeTags([' Vue ', 'vue', '生活　随笔']), ['Vue', '生活 随笔'])
  assert.deepEqual(commitTag(['Vue'], ' vue '), { tags: ['Vue'], input: '' })
  assert.deepEqual(commitTag(['Vue'], '随笔'), { tags: ['Vue', '随笔'], input: '' })
})

test('tag creation reuses an existing normalized name and creates only new names', () => {
  const existing = { id: 7, name: 'Vue' }
  assert.deepEqual(prepareTagCreation([existing], ' vue '), {
    name: 'Vue',
    existing,
    shouldCreate: false
  })
  assert.deepEqual(prepareTagCreation([existing], ' 随  笔 '), {
    name: '随 笔',
    existing: null,
    shouldCreate: true
  })
  assert.deepEqual(prepareTagCreation([existing], 'x'.repeat(31)), {
    name: '',
    existing: null,
    shouldCreate: false
  })
})
