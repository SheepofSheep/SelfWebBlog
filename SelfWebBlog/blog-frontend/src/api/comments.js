import { api, unwrap } from './http'

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
