const OAUTH_ERRORS = {
  not_configured: '站点暂未配置 GitHub 登录',
  invalid_state: '登录校验已过期，请重新尝试',
  no_access_token: 'GitHub 没有返回登录凭据，请重新尝试',
  oauth_error: 'GitHub 登录没有完成，请重新尝试'
}

export function safeRedirect(value, fallback = '/') {
  if (typeof value !== 'string' || !value.startsWith('/') || value.startsWith('//')) return fallback
  return value
}

export function oauthErrorMessage(code) {
  return OAUTH_ERRORS[code] || 'GitHub 登录没有完成，请重新尝试'
}
