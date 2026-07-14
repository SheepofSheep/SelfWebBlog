import { api, unwrap } from './http'

export async function getProfile(opts) {
  const params = { _t: Date.now() }
  if (opts?.page != null) params.page = opts.page
  if (opts?.size != null) params.size = opts.size
  const res = await api.get('/profile', { params, signal: opts?.signal })
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
