import { api, unwrap } from './http'

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
