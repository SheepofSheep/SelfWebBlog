import test from 'node:test'
import assert from 'node:assert/strict'
import { api, request } from './client.js'

test('unsafe requests refresh csrf from the server before sending', async (context) => {
  const originalFetch = globalThis.fetch
  const calls = []
  globalThis.fetch = async (path, options = {}) => {
    calls.push({ path, options })
    const payload = path === '/api/auth/csrf' ? { data: { token: 'fresh-token' } } : { data: 'ok' }
    return {
      ok: true,
      headers: new Headers({ 'content-type': 'application/json' }),
      json: async () => payload
    }
  }
  context.after(() => {
    globalThis.fetch = originalFetch
  })

  assert.equal(await request('/api/test', { method: 'POST' }), 'ok')
  assert.deepEqual(
    calls.map((call) => call.path),
    ['/api/auth/csrf', '/api/test']
  )
  assert.equal(calls[1].options.headers.get('X-XSRF-TOKEN'), 'fresh-token')
})

test('successful null data stays null instead of becoming the response envelope', async (context) => {
  const originalFetch = globalThis.fetch
  globalThis.fetch = async () => ({
    ok: true,
    headers: new Headers({ 'content-type': 'application/json' }),
    json: async () => ({ code: 200, data: null, msg: '操作成功' })
  })
  context.after(() => {
    globalThis.fetch = originalFetch
  })

  assert.equal(await request('/api/auth/session'), null)
})

test('profile updates and avatar uploads use the authenticated user endpoints', async (context) => {
  const originalFetch = globalThis.fetch
  const calls = []
  globalThis.fetch = async (path, options = {}) => {
    calls.push({ path, options })
    const data = path === '/api/auth/csrf' ? { token: 'fresh-token' } : 'ok'
    return {
      ok: true,
      headers: new Headers({ 'content-type': 'application/json' }),
      json: async () => ({ data })
    }
  }
  context.after(() => {
    globalThis.fetch = originalFetch
  })

  await api.updateProfile({ nickname: '新昵称' })
  await api.uploadAvatar(new Blob(['image'], { type: 'image/png' }))

  assert.deepEqual(
    calls.filter((call) => call.path !== '/api/auth/csrf').map((call) => call.path),
    ['/api/user/profile', '/api/user/avatar']
  )
})
