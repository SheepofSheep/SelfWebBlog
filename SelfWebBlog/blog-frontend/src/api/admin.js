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
