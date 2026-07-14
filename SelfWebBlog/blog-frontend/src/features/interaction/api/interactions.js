import { api, unwrap } from '../../../api/http'

export async function getInteractionThread(targetType, targetId, page = 1, size = 20) {
  const response = await api.get(`/interactions/${targetType}/${targetId}`, {
    params: { page, size }
  })
  return unwrap(response.data)
}

export async function createInteraction(payload) {
  const response = await api.post('/interactions', payload)
  return unwrap(response.data)
}

export async function replyToInteraction(interactionId, content) {
  const response = await api.post(`/interactions/${interactionId}/replies`, { content })
  return unwrap(response.data)
}

export async function getCaptcha(purpose = 'guestbook') {
  const response = await api.get('/captcha', { params: { purpose } })
  return unwrap(response.data)
}

export async function getLikeState(targetType, targetId) {
  const response = await api.get(`/engagement/${targetType}/${targetId}/like`)
  return unwrap(response.data)
}

export async function setLikeState(targetType, targetId, liked) {
  const response = await api.put(`/engagement/${targetType}/${targetId}/like`, { liked })
  return unwrap(response.data)
}

export async function recordPostView(postId) {
  const response = await api.post(`/engagement/posts/${postId}/view`)
  return unwrap(response.data)
}
