export const API_ORIGIN = import.meta.env.VITE_API_ORIGIN || 'http://localhost:8080'

// 将此函数修改为直接返回，不处理背景
export function toAbsoluteUrl(url) {
  if (!url) return ''
  if (typeof url !== 'string') return ''
  if (url.startsWith('http://') || url.startsWith('https://')) return url
  if (url.startsWith('//')) return window.location.protocol + url

  // 如果是 /uploads/开头的路径，直接使用相对路径（用于文章内的图片）
  if (url.startsWith('/uploads/')) {
    return url
  }

  return url // 默认返回原路径
}

const OPTIMIZED_WIDTHS = new Set([160, 480, 1280])

export function optimizedImageUrl(url, width = 480) {
  const source = toAbsoluteUrl(url)
  if (!source || !source.startsWith('/uploads/')) return source
  const safeWidth = OPTIMIZED_WIDTHS.has(Number(width)) ? Number(width) : 480
  return `/api/media/optimized?path=${encodeURIComponent(source)}&width=${safeWidth}`
}

export function optimizedImageSrcset(url) {
  const source = toAbsoluteUrl(url)
  if (!source?.startsWith('/uploads/')) return ''
  return `${optimizedImageUrl(source, 480)} 480w, ${optimizedImageUrl(source, 1280)} 1280w`
}
