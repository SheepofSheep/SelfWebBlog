import { api, unwrap } from './http'

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

export async function exchangeOAuthTicket(ticket) {
  const res = await api.post('/auth/exchange', { ticket })
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
