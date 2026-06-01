import { api, unwrap } from './http'

export async function updateUserProfile(payload) {
  const res = await api.put('/user/profile', payload)
  return unwrap(res.data)
}
