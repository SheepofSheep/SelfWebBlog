import test from 'node:test'
import assert from 'node:assert/strict'
import { componentHarness } from '../test/componentHarness.js'

test('search distinguishes failure and empty results and ignores a stale response after clearing', async () => {
  let runSearch
  let finish
  const search = componentHarness(
    new URL('./SearchDialog.vue', import.meta.url),
    {
      defineEmits() {},
      setTimeout: (callback) => {
        runSearch = callback
      },
      clearTimeout() {},
      api: {
        search: () =>
          new Promise((resolve, reject) => {
            finish = { resolve, reject }
          })
      }
    },
    ['query', 'results', 'loading', 'error', 'search', 'clearSearch']
  )
  try {
    search.state.query.value = '数据库'
    search.state.search()
    assert.equal(search.state.loading.value, true)
    const first = runSearch()
    finish.reject(new Error('网络暂时不可用'))
    await first
    assert.equal(search.state.error.value, '网络暂时不可用')
    assert.equal(search.state.loading.value, false)

    search.state.search()
    const retry = runSearch()
    finish.resolve({ records: [] })
    await retry
    assert.equal(search.state.error.value, '')
    assert.deepEqual(search.state.results.value, [])

    search.state.search()
    const pending = runSearch()
    search.state.clearSearch()
    finish.resolve({ records: [{ id: 'old', title: '旧结果' }] })
    await pending
    assert.deepEqual(search.state.results.value, [])
    assert.equal(search.state.loading.value, false)
  } finally {
    search.unmount()
  }
})
