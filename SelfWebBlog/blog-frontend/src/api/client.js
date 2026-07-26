async function ensureCsrf() {
  const response = await fetch('/api/auth/csrf', { credentials: 'include' })
  const body = await response.json()
  if (!response.ok || !body.data?.token) throw new Error(body.msg || '请求校验初始化失败')
  return body.data.token
}

export async function request(path, options = {}) {
  const method = (options.method || 'GET').toUpperCase()
  const headers = new Headers(options.headers || {})
  if (!['GET', 'HEAD', 'OPTIONS'].includes(method)) headers.set('X-XSRF-TOKEN', await ensureCsrf())
  if (options.body && !(options.body instanceof FormData))
    headers.set('Content-Type', 'application/json')
  const response = await fetch(path, { ...options, method, headers, credentials: 'include' })
  const contentType = response.headers.get('content-type') || ''
  const payload = contentType.includes('application/json')
    ? await response.json()
    : await response.text()
  if (!response.ok || (payload && typeof payload === 'object' && payload.code >= 400)) {
    const error = new Error(payload?.msg || `请求失败 (${response.status})`)
    error.status = response.status
    error.payload = payload
    throw error
  }
  if (payload && typeof payload === 'object' && Object.hasOwn(payload, 'data')) return payload.data
  return payload
}

export const api = {
  site: () => request('/api/site/about'),
  summary: () => request('/api/site/summary'),
  posts: (page = 1, size = 20) => request(`/api/posts?page=${page}&size=${size}`),
  post: (id) => request(`/api/posts/${id}`),
  search: (query) => request(`/api/posts/search?keyword=${encodeURIComponent(query)}`),
  drafts: () => request('/api/posts/drafts'),
  adminPosts: ({ status = '', keyword = '', page = 1, size = 20 } = {}) =>
    request(
      `/api/posts/admin?status=${encodeURIComponent(status)}&keyword=${encodeURIComponent(keyword)}&page=${page}&size=${size}`
    ),
  changePostStatus: (id, status) =>
    request(`/api/posts/${id}/status`, {
      method: 'PUT',
      body: JSON.stringify({ status })
    }),
  batchPostStatus: (ids, status) =>
    request('/api/posts/batch/status', {
      method: 'PUT',
      body: JSON.stringify({ ids, status })
    }),
  batchDeletePosts: (ids) =>
    request('/api/posts/batch-delete', { method: 'POST', body: JSON.stringify({ ids }) }),
  savePost: (post) => request('/api/posts', { method: 'POST', body: JSON.stringify(post) }),
  deletePost: (id) => request(`/api/posts/${id}`, { method: 'DELETE' }),
  tags: () => request('/api/tags'),
  tagPosts: (slug) => request(`/api/tags/${encodeURIComponent(slug)}/posts`),
  createTag: (name) => request('/api/tags', { method: 'POST', body: JSON.stringify({ name }) }),
  renameTag: (id, name) =>
    request(`/api/tags/${id}`, { method: 'PUT', body: JSON.stringify({ name }) }),
  deleteTag: (id) => request(`/api/tags/${id}`, { method: 'DELETE' }),
  calendar: (year, month) => request(`/api/content/calendar?year=${year}&month=${month}`),
  session: () => request('/api/auth/session'),
  login: (payload) => request('/api/auth/login', { method: 'POST', body: JSON.stringify(payload) }),
  githubLogin: () => request('/api/auth/github'),
  exchangeOAuth: (ticket) =>
    request('/api/auth/exchange', { method: 'POST', body: JSON.stringify({ ticket }) }),
  register: (payload) =>
    request('/api/auth/register', { method: 'POST', body: JSON.stringify(payload) }),
  captcha: (purpose) => request(`/api/captcha?purpose=${encodeURIComponent(purpose)}`),
  logout: () => request('/api/auth/logout', { method: 'POST' }),
  upload: (file, options = {}) => {
    const data = new FormData()
    data.append('file', file)
    return request('/api/upload/image', { method: 'POST', body: data, ...options })
  },
  updateProfile: (payload) =>
    request('/api/user/profile', { method: 'PUT', body: JSON.stringify(payload) }),
  uploadAvatar: (file) => {
    const data = new FormData()
    data.append('file', file)
    return request('/api/user/avatar', { method: 'POST', body: data })
  },
  uploadSiteAvatar: (file) => {
    const data = new FormData()
    data.append('file', file)
    return request('/api/upload/avatar', { method: 'POST', body: data })
  },
  thread: (type, id, page = 1, size = 20) =>
    request(`/api/interactions/${type}/${id}?page=${page}&size=${size}`),
  comment: (payload) =>
    request('/api/interactions', { method: 'POST', body: JSON.stringify(payload) }),
  createInteraction: (payload) =>
    request('/api/interactions', { method: 'POST', body: JSON.stringify(payload) }),
  reply: (id, content) =>
    request(`/api/interactions/${id}/replies`, {
      method: 'POST',
      body: JSON.stringify({ content })
    }),
  like: (type, id, liked) =>
    request(`/api/engagement/${type}/${id}/like`, {
      method: 'PUT',
      body: JSON.stringify({ liked })
    }),
  likeState: (type, id) => request(`/api/engagement/${type}/${id}/like`),
  view: (id) => request(`/api/engagement/posts/${id}/view`, { method: 'POST' }),
  overview: () => request('/api/admin/dashboard/overview'),
  trends: (days = 14) => request(`/api/admin/dashboard/trend?days=${days}`),
  adminInteractions: (options = '', legacyPage = 1, legacySize = 100) => {
    const value =
      typeof options === 'object'
        ? {
            status: '',
            targetType: '',
            pinned: '',
            contentType: '',
            keyword: '',
            page: 1,
            size: 20,
            ...options
          }
        : {
            status: options,
            targetType: '',
            pinned: '',
            contentType: '',
            keyword: '',
            page: legacyPage,
            size: legacySize
          }
    const params = new URLSearchParams({ page: String(value.page), size: String(value.size) })
    if (value.status) params.set('status', value.status)
    if (value.targetType) params.set('targetType', value.targetType)
    if (value.pinned !== '') params.set('pinned', String(value.pinned))
    if (value.contentType) params.set('contentType', value.contentType)
    if (value.keyword) params.set('keyword', value.keyword)
    return request(`/api/admin/interactions?${params}`)
  },
  moderate: (id, status) =>
    request(`/api/admin/interactions/${id}/status`, {
      method: 'PUT',
      body: JSON.stringify({ status })
    }),
  moderateBatch: (ids, status) =>
    request('/api/admin/interactions/batch/status', {
      method: 'PUT',
      body: JSON.stringify({ ids, status })
    }),
  pin: (id, pinned) =>
    request(`/api/admin/interactions/${id}/pinned`, {
      method: 'PUT',
      body: JSON.stringify({ pinned })
    }),
  movePin: (id, direction) =>
    request(`/api/admin/interactions/${id}/pin-order`, {
      method: 'PUT',
      body: JSON.stringify({ direction })
    }),
  deleteInteraction: (id) => request(`/api/admin/interactions/${id}`, { method: 'DELETE' }),
  batchDeleteInteractions: (ids) =>
    request('/api/admin/interactions/batch-delete', {
      method: 'POST',
      body: JSON.stringify({ ids })
    }),
  users: (options = {}) => {
    const value = { keyword: '', role: '', title: '', page: 1, size: 20, ...options }
    const params = new URLSearchParams({
      page: String(value.page),
      size: String(value.size)
    })
    const keyword = String(value.keyword || '').trim()
    const role = String(value.role || '').trim()
    const title = String(value.title || '').trim()
    if (keyword) params.set('keyword', keyword)
    if (role) params.set('role', role)
    if (title) params.set('title', title)
    return request(`/api/admin/users?${params}`)
  },
  updateUserTitle: (id, payload) =>
    request(`/api/admin/users/${id}/title`, { method: 'PUT', body: JSON.stringify(payload) }),
  deleteUser: (id) => request(`/api/admin/users/${id}`, { method: 'DELETE' }),
  assets: ({ page = 1, size = 24, status = '', purpose = '', keyword = '' } = {}) =>
    request(
      `/api/admin/assets?page=${page}&size=${size}&status=${encodeURIComponent(status)}&purpose=${encodeURIComponent(purpose)}&keyword=${encodeURIComponent(keyword)}`
    ),
  scanAssets: () => request('/api/admin/assets/scan', { method: 'POST' }),
  assetReferences: (id) => request(`/api/admin/assets/${id}/references`),
  deleteAsset: (id) => request(`/api/admin/assets/${id}`, { method: 'DELETE' }),
  deleteAssets: (ids) =>
    request('/api/admin/assets/batch', {
      method: 'DELETE',
      body: JSON.stringify({ ids })
    }),
  security: () => request('/api/admin/dashboard/security-events'),
  siteAdmin: () => request('/api/admin/site'),
  saveSite: (payload) =>
    request('/api/admin/site', { method: 'PUT', body: JSON.stringify(payload) }),
  siteLinks: () => request('/api/admin/site/links'),
  createSiteLink: (payload) =>
    request('/api/admin/site/links', { method: 'POST', body: JSON.stringify(payload) }),
  updateSiteLink: (id, payload) =>
    request(`/api/admin/site/links/${id}`, { method: 'PUT', body: JSON.stringify(payload) }),
  reorderSiteLinks: (ids) =>
    request('/api/admin/site/links/order', { method: 'PUT', body: JSON.stringify({ ids }) }),
  deleteSiteLink: (id) => request(`/api/admin/site/links/${id}`, { method: 'DELETE' })
}
