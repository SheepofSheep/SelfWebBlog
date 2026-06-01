import axios from 'axios'

export const AUTH_EXPIRED_EVENT = 'auth:expired'

export const api = axios.create({
  baseURL: '/',
  timeout: 15000,
  withCredentials: true
})

export function clearAuthState({ emit = true } = {}) {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  if (emit && typeof window !== 'undefined') {
    window.dispatchEvent(new CustomEvent(AUTH_EXPIRED_EVENT))
  }
}

export function isAuthFailure(payload, status) {
  if (status === 401 || status === 403) return true
  if (!payload || typeof payload !== 'object') return false
  if (payload.code === 401 || payload.code === 403) return true
  const msg = String(payload.msg || '')
  return /未登录|请先登录|无权限|登录已过期|token/i.test(msg)
}

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers = config.headers || {}
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  (response) => {
    if (isAuthFailure(response.data, response.status)) {
      clearAuthState()
    }
    return response
  },
  (error) => {
    if (isAuthFailure(error.response?.data, error.response?.status)) {
      clearAuthState()
    }
    return Promise.reject(toApiError(error))
  }
)

function toApiError(error) {
  const payload = error.response?.data
  if (payload && typeof payload === 'object') {
    const apiError = new Error(payload.msg || error.message || '请求失败')
    apiError.code = payload.code
    apiError.status = error.response?.status
    apiError.authRequired = isAuthFailure(payload, error.response?.status)
    return apiError
  }
  return error
}

export function unwrap(result) {
  if (!result || typeof result !== 'object') {
    throw new Error('接口返回异常')
  }
  if (result.code !== 200) {
    const error = new Error(result.msg || '请求失败')
    error.code = result.code
    error.authRequired = isAuthFailure(result)
    throw error
  }
  return result.data
}

export async function request(config) {
  const res = await api.request(config)
  return unwrap(res.data)
}
