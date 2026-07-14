import { api, unwrap } from './http'

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

export async function getAdminOverview() {
  const res = await api.get('/admin/dashboard/overview')
  return unwrap(res.data)
}

export async function listAdminInteractions({ status, page = 1, size = 30 } = {}) {
  const res = await api.get('/admin/interactions', { params: { status, page, size } })
  return unwrap(res.data)
}

export async function moderateInteraction(id, status) {
  const res = await api.put(`/admin/interactions/${id}/status`, { status })
  return unwrap(res.data)
}

export async function moderateInteractions(ids, status) {
  const res = await api.put('/admin/interactions/batch/status', { ids, status })
  return unwrap(res.data)
}

export async function revealInteractionIp(id) {
  const res = await api.post(`/admin/interactions/${id}/ip`)
  return unwrap(res.data)
}

export async function setInteractionPinned(id, pinned) {
  const res = await api.put(`/admin/interactions/${id}/pinned`, { pinned })
  return unwrap(res.data)
}

export async function deleteInteraction(id) {
  const res = await api.delete(`/admin/interactions/${id}`)
  return unwrap(res.data)
}

export async function getAdminSite() {
  const res = await api.get('/admin/site')
  return unwrap(res.data)
}

export async function updateAdminSite(payload) {
  const res = await api.put('/admin/site', payload)
  return unwrap(res.data)
}

export async function listAdminAssets(limit = 200) {
  const res = await api.get('/admin/assets', { params: { limit } })
  return unwrap(res.data)
}

export async function scanAdminAssets() {
  const res = await api.post('/admin/assets/scan')
  return unwrap(res.data)
}

export async function deleteAdminAsset(id) {
  const res = await api.delete(`/admin/assets/${id}`)
  return unwrap(res.data)
}

export async function listSecurityEvents(limit = 50) {
  const res = await api.get('/admin/dashboard/security-events', { params: { limit } })
  return unwrap(res.data)
}
