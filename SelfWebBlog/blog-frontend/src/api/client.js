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
  thread: (type, id) => request(`/api/interactions/${type}/${id}`),
  comment: (payload) =>
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
  adminInteractions: (status = '', page = 1, size = 100) =>
    request(
      `/api/admin/interactions?status=${encodeURIComponent(status)}&page=${page}&size=${size}`
    ),
  moderate: (id, status) =>
    request(`/api/admin/interactions/${id}/status`, {
      method: 'PUT',
      body: JSON.stringify({ status })
    }),
  pin: (id, pinned) =>
    request(`/api/admin/interactions/${id}/pinned`, {
      method: 'PUT',
      body: JSON.stringify({ pinned })
    }),
  deleteInteraction: (id) => request(`/api/admin/interactions/${id}`, { method: 'DELETE' }),
  users: () => request('/api/admin/users'),
  updateUserTitle: (id, payload) =>
    request(`/api/admin/users/${id}/title`, { method: 'PUT', body: JSON.stringify(payload) }),
  deleteUser: (id) => request(`/api/admin/users/${id}`, { method: 'DELETE' }),
  assets: () => request('/api/upload/assets'),
  security: () => request('/api/admin/dashboard/security-events'),
  siteAdmin: () => request('/api/admin/site'),
  saveSite: (payload) =>
    request('/api/admin/site', { method: 'PUT', body: JSON.stringify(payload) })
}
