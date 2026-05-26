export const API_ORIGIN = import.meta.env.VITE_API_ORIGIN || ''

export function toAbsoluteUrl(url) {
  if (!url) return ''
  if (typeof url !== 'string') return ''
  if (url.startsWith('http://') || url.startsWith('https://')) return url
  if (url.startsWith('//')) return window.location.protocol + url

  // 相对路径：若配置了 API_ORIGIN（跨域部署），补全为绝对路径
  if (url.startsWith('/')) {
    return API_ORIGIN ? API_ORIGIN + url : url
  }

  return url
}

