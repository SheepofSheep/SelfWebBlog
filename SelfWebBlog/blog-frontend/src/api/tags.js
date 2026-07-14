import { api, unwrap } from './http'

export async function listTags() {
  const res = await api.get('/tags')
  return unwrap(res.data)
}

export async function createTag(name) {
  const res = await api.post('/tags', { name })
  return unwrap(res.data)
}

export async function listPostsByTag(slug) {
  const res = await api.get(`/tags/${encodeURIComponent(slug)}/posts`)
  return unwrap(res.data)
}

export async function deleteTagFromAdmin(id) {
  const res = await api.delete(`/tags/${id}`)
  return unwrap(res.data)
}
