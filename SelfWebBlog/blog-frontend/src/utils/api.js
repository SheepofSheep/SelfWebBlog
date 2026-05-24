import axios from 'axios'

const api = axios.create({
  baseURL: '/',
  timeout: 15000,
  withCredentials: true
})

// 请求拦截器：自动附加 JWT Token
api.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 响应拦截器：401 自动清除 token
api.interceptors.response.use(
  response => response,
  error => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
    }
    return Promise.reject(error)
  }
)

function unwrap(result) {
  if (!result || typeof result !== 'object') {
    throw new Error('接口返回异常')
  }
  if (result.code !== 200) {
    throw new Error(result.msg || '请求失败')
  }
  return result.data
}

// ==================== 认证相关 ====================

export async function register(payload) {
  const res = await api.post('/auth/register', payload)
  return unwrap(res.data)
}

export async function login(username, password) {
  const res = await api.post('/auth/login', { username, password })
  return unwrap(res.data)
}

export async function adminLogin(username, password) {
  const res = await api.post('/auth/admin', { username, password })
  return unwrap(res.data)
}

export async function getGithubAuthUrl() {
  const res = await api.get('/auth/github')
  return unwrap(res.data)
}

export async function githubCallback(code, state) {
  const res = await api.get('/auth/github/callback', { params: { code, state } })
  return unwrap(res.data)
}

export async function getCurrentUser() {
  const res = await api.get('/auth/me')
  return unwrap(res.data)
}

export async function logout() {
  const res = await api.post('/auth/logout')
  return unwrap(res.data)
}

// ==================== 个人资料相关 ====================

export async function getProfile(opts) {
  const params = { _t: Date.now() }
  if (opts?.page != null) params.page = opts.page
  if (opts?.size != null) params.size = opts.size
  const res = await api.get('/profile', { params })
  return unwrap(res.data)
}

export async function updateProfile(payload) {
  const res = await api.post('/profile/update', payload)
  return unwrap(res.data)
}

export async function updateBackground(bgUrl) {
  const res = await api.post('/profile/background', { bgUrl })
  return unwrap(res.data)
}

// ==================== 文章相关 ====================

export async function listPosts(opts) {
  const params = { page: opts?.page ?? 1, size: opts?.size ?? 10 }
  const res = await api.get('/posts', { params })
  return unwrap(res.data)
}

export async function getPost(id) {
  const res = await api.get(`/posts/${id}`)
  return unwrap(res.data)
}

export async function savePost(payload) {
  const res = await api.post('/posts', payload)
  return unwrap(res.data)
}

export async function deletePost(id) {
  const res = await api.delete(`/posts/${id}`)
  return unwrap(res.data)
}

export async function listDrafts(pageNum = 1, pageSize = 20) {
  const res = await api.get('/posts/drafts', { params: { page: pageNum, size: pageSize } })
  return unwrap(res.data)
}

export async function searchPosts({ keyword, category, tag, sort, page = 1, size = 20 } = {}) {
  const res = await api.get('/posts/search', { params: { keyword, category, tag, sort, page, size } })
  return unwrap(res.data)
}

export async function listCategories() {
  const res = await api.get('/posts/categories')
  return unwrap(res.data)
}

// ==================== 标签管理 ====================

export async function listTags() {
  const res = await api.get('/tags')
  return unwrap(res.data)
}

export async function createTag(name) {
  const res = await api.post('/tags', { name })
  return unwrap(res.data)
}

export async function deleteTagFromAdmin(id) {
  const res = await api.delete(`/tags/${id}`)
  return unwrap(res.data)
}

// ==================== 评论相关 ====================

export async function getComments(postId) {
  const res = await api.get(`/comments/post/${postId}`)
  return unwrap(res.data)
}

export async function addComment(payload) {
  const res = await api.post('/comments', payload)
  return unwrap(res.data)
}

export async function deleteComment(commentId) {
  const res = await api.delete(`/comments/${commentId}`)
  return unwrap(res.data)
}

export async function togglePinComment(commentId) {
  const res = await api.put(`/comments/${commentId}/pin`)
  return unwrap(res.data)
}

export async function listAllComments(pageNum = 1, pageSize = 20) {
  const res = await api.get('/comments/all', { params: { pageNum, pageSize } })
  return unwrap(res.data)
}

// ==================== 海报轮播 ====================

export async function listPosters() {
  const res = await api.get('/posters')
  return unwrap(res.data)
}

export async function savePoster(payload) {
  const res = await api.post('/posters', payload)
  return unwrap(res.data)
}

export async function updatePoster(id, payload) {
  const res = await api.put(`/posters/${id}`, payload)
  return unwrap(res.data)
}

export async function deletePoster(id) {
  const res = await api.delete(`/posters/${id}`)
  return unwrap(res.data)
}

export async function updatePosterSort(payload) {
  const res = await api.put('/posters/sort', payload)
  return unwrap(res.data)
}

// ==================== 文件上传 ====================

export async function uploadImage(file) {
  const form = new FormData()
  form.append('file', file)
  const res = await api.post('/upload/image', form, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
  return unwrap(res.data)
}

export async function uploadAvatar(file) {
  const form = new FormData()
  form.append('file', file)
  const res = await api.post('/upload/avatar', form, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
  return unwrap(res.data)
}

// ==================== 用户个人资料 ====================

export async function updateUserProfile(payload) {
  const res = await api.put('/user/profile', payload)
  return unwrap(res.data)
}

export async function uploadUserAvatar(file) {
  const form = new FormData()
  form.append('file', file)
  const res = await api.post('/user/avatar', form, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
  return unwrap(res.data)
}

// ==================== 管理员：用户管理 ====================

export async function listUsers() {
  const res = await api.get('/admin/users')
  return unwrap(res.data)
}

export async function grantTitle(userId, titleName, titleStyle) {
  const res = await api.put(`/admin/users/${userId}/title`, { titleName, titleStyle })
  return unwrap(res.data)
}

export async function deleteUser(userId) {
  const res = await api.delete(`/admin/users/${userId}`)
  return unwrap(res.data)
}
