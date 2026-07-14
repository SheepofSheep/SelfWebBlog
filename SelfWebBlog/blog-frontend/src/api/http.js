import axios from 'axios'

export const AUTH_EXPIRED_EVENT = 'auth:expired'

export const api = axios.create({
  baseURL: '/api',
  timeout: 15000,
  withCredentials: true,
  xsrfCookieName: 'XSRF-TOKEN',
  xsrfHeaderName: 'X-XSRF-TOKEN'
})

let csrfRequest = null

export function clearAuthState({ emit = true } = {}) {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  if (emit && typeof window !== 'undefined') {
    window.dispatchEvent(new CustomEvent(AUTH_EXPIRED_EVENT))
  }
}

export function isAuthFailure(payload, status) {
  if (status === 401) return true
  if (!payload || typeof payload !== 'object') return false
  if (payload.code === 401) return true
  const msg = String(payload.msg || '')
  return /未登录|请先登录|登录已过期|登录凭证|token.*(?:过期|失效|无效)/i.test(msg)
}

function hasCsrfCookie() {
  return Boolean(readCsrfCookie())
}

function readCsrfCookie() {
  if (typeof document === 'undefined') return ''
  const item = document.cookie.split('; ').find((entry) => entry.startsWith('XSRF-TOKEN='))
  if (!item) return ''
  return decodeURIComponent(item.slice('XSRF-TOKEN='.length))
}

async function ensureCsrfCookie() {
  if (hasCsrfCookie()) return
  if (!csrfRequest) {
    csrfRequest = axios.get('/api/auth/csrf', { withCredentials: true }).finally(() => {
      csrfRequest = null
    })
  }
  await csrfRequest
}

api.interceptors.request.use(async (config) => {
  const method = String(config.method || 'get').toLowerCase()
  if (!['get', 'head', 'options'].includes(method)) {
    await ensureCsrfCookie()
    config.headers.set('X-XSRF-TOKEN', readCsrfCookie())
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
    apiError.traceId = payload.traceId || error.response?.headers?.['x-request-id']
    apiError.retryAfterSeconds = Number(
      payload.retryAfterSeconds || error.response?.headers?.['retry-after'] || 0
    )
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
