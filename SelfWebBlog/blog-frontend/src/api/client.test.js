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

test('site background uploads use the managed image endpoint', async (context) => {
  const originalFetch = globalThis.fetch
  const calls = []
  globalThis.fetch = async (path, options = {}) => {
    calls.push({ path, options })
    const data =
      path === '/api/auth/csrf' ? { token: 'fresh-token' } : { originalUrl: '/uploads/bg.webp' }
    return {
      ok: true,
      headers: new Headers({ 'content-type': 'application/json' }),
      json: async () => ({ data })
    }
  }
  context.after(() => {
    globalThis.fetch = originalFetch
  })

  const file = new Blob(['image'], { type: 'image/png' })
  const result = await api.uploadSiteBackground(file)
  const requestCall = calls.find((call) => call.path === '/api/upload/image')
  const uploadedFile = requestCall.options.body.get('file')

  assert.equal(result.originalUrl, '/uploads/bg.webp')
  assert.equal(requestCall.options.method, 'POST')
  assert.equal(requestCall.options.body instanceof FormData, true)
  assert.equal(uploadedFile.size, file.size)
  assert.equal(uploadedFile.type, file.type)
  assert.equal(await uploadedFile.text(), 'image')
  assert.equal(new Headers(requestCall.options.headers).has('content-type'), false)
})

test('public interaction threads request an explicit root page and size', async (context) => {
  const originalFetch = globalThis.fetch
  const calls = []
  globalThis.fetch = async (path) => {
    calls.push(path)
    return {
      ok: true,
      headers: new Headers({ 'content-type': 'application/json' }),
      json: async () => ({ data: { items: [], total: 0, page: 2, size: 20 } })
    }
  }
  context.after(() => {
    globalThis.fetch = originalFetch
  })

  await api.thread('POST', '2081309851788009474', 2, 20)

  assert.deepEqual(calls, ['/api/interactions/POST/2081309851788009474?page=2&size=20'])
})

test('operations methods use the dedicated admin endpoints', async (context) => {
  const originalFetch = globalThis.fetch
  const calls = []
  globalThis.fetch = async (path, options = {}) => {
    calls.push({ path, options })
    return {
      ok: true,
      headers: new Headers({ 'content-type': 'application/json' }),
      json: async () => ({ data: path === '/api/auth/csrf' ? { token: 'token' } : {} })
    }
  }
  context.after(() => {
    globalThis.fetch = originalFetch
  })

  await api.adminPosts({ status: 'ARCHIVED', keyword: '夏天', page: 2, size: 20 })
  await api.changePostStatus('7', 'PUBLISHED')
  await api.batchPostStatus(['7', '8'], 'ARCHIVED')
  await api.trends(30)
  await api.scanAssets()
  await api.assetReferences('9')
  await api.deleteAsset('9')
  await api.deleteAssets(['9', '10'])
  await api.movePin('5', 'UP')

  const sent = calls.filter((call) => call.path !== '/api/auth/csrf')
  assert.deepEqual(
    sent.map((call) => call.path),
    [
      '/api/posts/admin?status=ARCHIVED&keyword=%E5%A4%8F%E5%A4%A9&page=2&size=20',
      '/api/posts/7/status',
      '/api/posts/batch/status',
      '/api/admin/dashboard/trend?days=30',
      '/api/admin/assets/scan',
      '/api/admin/assets/9/references',
      '/api/admin/assets/9',
      '/api/admin/assets/batch',
      '/api/admin/interactions/5/pin-order'
    ]
  )
  assert.deepEqual(JSON.parse(sent[1].options.body), { status: 'PUBLISHED' })
  assert.deepEqual(JSON.parse(sent[2].options.body), { ids: ['7', '8'], status: 'ARCHIVED' })
  assert.deepEqual(JSON.parse(sent[7].options.body), { ids: ['9', '10'] })
  assert.deepEqual(JSON.parse(sent[8].options.body), { direction: 'UP' })
})

test('admin users sends paging and server-side identity filters', async (context) => {
  const originalFetch = globalThis.fetch
  const calls = []
  globalThis.fetch = async (path) => {
    calls.push(path)
    return {
      ok: true,
      headers: new Headers({ 'content-type': 'application/json' }),
      json: async () => ({ data: { records: [], current: 2, pages: 3, total: 21 } })
    }
  }
  context.after(() => {
    globalThis.fetch = originalFetch
  })

  await api.users({ keyword: ' 夏 天 ', role: 'USER', title: ' 贡献者 ', page: 2, size: 10 })

  assert.deepEqual(calls, [
    '/api/admin/users?page=2&size=10&keyword=%E5%A4%8F+%E5%A4%A9&role=USER&title=%E8%B4%A1%E7%8C%AE%E8%80%85'
  ])
})

test('interaction moderation sends content filters and exact string ids for batch delete', async (context) => {
  const originalFetch = globalThis.fetch
  const calls = []
  globalThis.fetch = async (path, options = {}) => {
    calls.push({ path, options })
    return {
      ok: true,
      headers: new Headers({ 'content-type': 'application/json' }),
      json: async () => ({ data: path === '/api/auth/csrf' ? { token: 'token' } : {} })
    }
  }
  context.after(() => {
    globalThis.fetch = originalFetch
  })

  await api.adminInteractions({ contentType: 'STICKER', page: 2, size: 10 })
  await api.batchDeleteInteractions(['9007199254740993', '9007199254740995'])

  const sent = calls.filter((call) => call.path !== '/api/auth/csrf')
  assert.equal(sent[0].path, '/api/admin/interactions?page=2&size=10&contentType=STICKER')
  assert.equal(sent[1].path, '/api/admin/interactions/batch-delete')
  assert.equal(sent[1].options.method, 'POST')
  assert.deepEqual(JSON.parse(sent[1].options.body), {
    ids: ['9007199254740993', '9007199254740995']
  })
})

test('admin replies reuse the unified interaction creation contract without captcha fields', async (context) => {
  const originalFetch = globalThis.fetch
  const calls = []
  globalThis.fetch = async (path, options = {}) => {
    calls.push({ path, options })
    return {
      ok: true,
      headers: new Headers({ 'content-type': 'application/json' }),
      json: async () => ({ data: path === '/api/auth/csrf' ? { token: 'token' } : {} })
    }
  }
  context.after(() => {
    globalThis.fetch = originalFetch
  })

  await api.createInteraction({
    targetType: 'POST',
    targetId: '9007199254740993',
    replyToId: '9007199254740995',
    content: '管理员回复'
  })

  const sent = calls.find((call) => call.path === '/api/interactions')
  assert.equal(sent.options.method, 'POST')
  assert.deepEqual(JSON.parse(sent.options.body), {
    targetType: 'POST',
    targetId: '9007199254740993',
    replyToId: '9007199254740995',
    content: '管理员回复'
  })
})
