import test from 'node:test'
import assert from 'node:assert/strict'
import * as threadModel from '../core/threadModel.js'
import { componentHarness } from '../test/componentHarness.js'

test('anonymous visitors can send two messages using the displayed single-use captchas', async () => {
  const challenges = new Set()
  let generated = 0
  let accepted = 0
  const messages = []
  const thread = componentHarness(
    new URL('./InteractionThread.vue', import.meta.url),
    {
      ...threadModel,
      defineProps: () => ({ targetType: 'GUESTBOOK', targetId: '1', guestbook: true }),
      appStore: { user: null },
      notify: (message) => messages.push(message),
      api: {
        thread: async () => ({ items: [], total: 0, page: 1, size: 20 }),
        captcha: async () => {
          const challengeId = `challenge-${++generated}`
          challenges.add(challengeId)
          return { challengeId, imageDataUrl: '' }
        },
        comment: async (payload) => {
          if (!challenges.delete(payload.captchaId)) throw new Error('验证码错误或已过期')
          return { id: String(++accepted), status: 'PENDING', pinned: false }
        }
      }
    },
    ['send', 'content', 'guestName', 'captchaAnswer', 'captcha']
  )
  try {
    await thread.mount()
    const firstChallenge = thread.state.captcha.value.challengeId
    thread.state.guestName.value = '访客'
    thread.state.content.value = '第一条留言'
    thread.state.captchaAnswer.value = 'ABCDE'
    await thread.state.send()
    assert.notEqual(thread.state.captcha.value.challengeId, firstChallenge)
    thread.state.content.value = '第二条留言'
    thread.state.captchaAnswer.value = 'ABCDE'
    await thread.state.send()
    assert.equal(accepted, 2)
    assert.equal(messages.includes('验证码错误或已过期'), false)
  } finally {
    thread.unmount()
  }
})

test('sending a comment preserves edits made while the request is in flight and exposes load retry failures', async () => {
  let finish
  let loadFails = false
  const thread = componentHarness(
    new URL('./InteractionThread.vue', import.meta.url),
    {
      ...threadModel,
      defineProps: () => ({ targetType: 'POST', targetId: '7', guestbook: false }),
      appStore: { user: { id: '1' } },
      notify() {},
      api: {
        thread: async () => {
          if (loadFails) throw new Error('评论加载失败')
          return { items: [], total: 0, page: 1, size: 20 }
        },
        comment: () =>
          new Promise((resolve) => {
            finish = resolve
          })
      }
    },
    ['send', 'content', 'loadError', 'refreshLoadedPages']
  )
  try {
    await thread.mount()
    thread.state.content.value = '提交的评论'
    const pending = thread.state.send()
    thread.state.content.value = '下一条还没发送的评论'
    finish({ id: 'comment-1', replies: [] })
    await pending
    assert.equal(thread.state.content.value, '下一条还没发送的评论')
    loadFails = true
    await thread.state.refreshLoadedPages()
    assert.equal(thread.state.loadError.value, '评论加载失败')
    loadFails = false
    await thread.state.refreshLoadedPages()
    assert.equal(thread.state.loadError.value, '')
    assert.equal(thread.state.content.value, '下一条还没发送的评论')
  } finally {
    thread.unmount()
  }
})
