import { api, unwrap } from './http'

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

export async function deletePosts(ids) {
  const res = await api.post('/posts/batch-delete', { ids })
  return unwrap(res.data)
}

export async function listDrafts(pageNum = 1, pageSize = 20) {
  const res = await api.get('/posts/drafts', { params: { page: pageNum, size: pageSize } })
  return unwrap(res.data)
}

export async function searchPosts({
  keyword,
  category,
  tag,
  sort,
  page = 1,
  size = 20,
  signal
} = {}) {
  const res = await api.get('/posts/search', {
    params: { keyword, category, tag, sort, page, size },
    signal
  })
  return unwrap(res.data)
}

export async function listCategories() {
  const res = await api.get('/posts/categories')
  return unwrap(res.data)
}
