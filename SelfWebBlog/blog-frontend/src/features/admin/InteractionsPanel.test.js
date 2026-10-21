import test from 'node:test'
import assert from 'node:assert/strict'
import * as adminModel from '../../core/adminModel.js'
import { componentHarness } from '../../test/componentHarness.js'

const postId = '9007199254740993'
const otherId = '9007199254740995'
function moderationHarness(api) {
  return componentHarness(
    new URL('./InteractionsPanel.vue', import.meta.url),
    {
      ...adminModel,
      api,
      useRoute: () => ({ query: {} }),
      appStore: { user: { role: 'ADMIN' } },
      notify() {}
    },
    [
      'sources',
      'visibleSources',
      'currentSource',
      'sourceCategories',
      'sourceQuery',
      'sourceHeading',
      'chooseSource',
      'chooseCategory',
      'load',
      'loadSources',
      'moderate',
      'result',
      'selected',
      'expandedRoots',
      'targetType',
      'targetId',
      'loadError',
      'sourcesError',
      'keyword',
      'status',
      'clearFilters'
    ]
  )
}

test('moderation source switches keep exact article ids, reset selections and retain scope after mutation', async () => {
  let pendingCount = 1
  let sourceRequests = 0
  const calls = []
  const panel = moderationHarness({
    interactionSources: async () => {
      sourceRequests++
      return [
        {
          targetType: 'POST',
          targetId: postId,
          sourceTitle: '同名文章',
          rootCount: 24,
          replyCount: 1,
          pendingCount
        },
        {
          targetType: 'POST',
          targetId: otherId,
          sourceTitle: '同名文章',
          rootCount: 1,
          replyCount: 0,
          pendingCount: 0
        },
        {
          targetType: 'GUESTBOOK',
          targetId: '1',
          sourceTitle: '留言板',
          rootCount: 2,
          replyCount: 1,
          pendingCount: 0
        }
      ]
    },
    adminInteractions: async (options) => {
      calls.push(options)
      return {
        records: [
          {
            id: 'root',
            targetType: options.targetType,
            targetId: options.targetId,
            status: 'PENDING'
          }
        ],
        current: options.page,
        pages: 2,
        total: 24
      }
    },
    moderate: async () => {
      pendingCount = 0
    }
  })
  try {
    await panel.mount()
    assert.equal(panel.state.sourceCategories.value[0].count, 27)
    panel.state.selected.value = new Set(['old-selection'])
    panel.state.expandedRoots.value = new Set(['old-root'])
    panel.state.chooseSource(panel.state.sources.value[0])
    await new Promise((resolve) => setImmediate(resolve))
    assert.equal(calls.at(-1).targetId, postId)
    assert.equal(panel.state.selected.value.size, 0)
    assert.equal(panel.state.expandedRoots.value.size, 0)
    await panel.state.load(2)
    assert.equal(calls.at(-1).targetId, postId)
    assert.equal(calls.at(-1).page, 2)
    await panel.state.moderate(panel.state.result.value.records[0], 'PUBLISHED')
    assert.equal(sourceRequests, 2)
    assert.equal(panel.state.currentSource.value.pendingCount, 0)
    assert.equal(calls.at(-1).targetId, postId)
    panel.state.sourceQuery.value = otherId
    assert.equal(panel.state.visibleSources.value.length, 1)
    assert.equal(panel.state.visibleSources.value[0].targetId, otherId)
    panel.state.chooseCategory('GUESTBOOK')
    await new Promise((resolve) => setImmediate(resolve))
    assert.equal(calls.at(-1).targetType, 'GUESTBOOK')
    assert.equal(calls.at(-1).targetId, '')
    assert.equal(panel.state.visibleSources.value.length, 1)
  } finally {
    panel.unmount()
  }
})

test('a failed source switch clears old actionable rows and source-directory failures are retryable', async () => {
  let fail = false
  const panel = moderationHarness({
    interactionSources: async () => {
      if (fail) throw new Error('来源统计不可用')
      return []
    },
    adminInteractions: async () => {
      if (fail) throw new Error('评论加载失败')
      return { records: [{ id: 'old-row' }], total: 1, current: 1, pages: 1 }
    }
  })
  try {
    await panel.mount()
    fail = true
    panel.state.chooseSource({ targetType: 'POST', targetId: postId })
    await new Promise((resolve) => setImmediate(resolve))
    assert.equal(panel.state.result.value.records.length, 0)
    assert.equal(panel.state.loadError.value, '评论加载失败')
    await panel.state.loadSources()
    assert.equal(panel.state.sourcesError.value, '来源统计不可用')
    fail = false
    await panel.state.loadSources()
    assert.equal(panel.state.sourcesError.value, '')
  } finally {
    panel.unmount()
  }
})
