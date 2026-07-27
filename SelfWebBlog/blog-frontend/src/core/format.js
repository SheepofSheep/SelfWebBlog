export const formatDate = (value, compact = false) => {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  return new Intl.DateTimeFormat(
    'zh-CN',
    compact
      ? { month: 'short', day: 'numeric' }
      : { year: 'numeric', month: 'long', day: 'numeric' }
  ).format(date)
}

export const avatarInitial = (value) => Array.from(String(value || '').trim())[0] || '·'

export const postTitle = (post) => {
  const title = post?.title?.trim()
  if (title) return title
  const firstLine = String(post?.content || '')
    .replace(/```[\s\S]*?```/g, ' ')
    .replace(/!\[[^\]]*]\([^)]+\)/g, ' ')
    .replace(/\[([^\]]+)]\([^)]+\)/g, '$1')
    .replace(/<[^>]+>/g, ' ')
    .split(/\r?\n/)
    .map((line) => line.replace(/^[\s#>*_~`|+-]+|[\s#*_~`|]+$/g, '').trim())
    .find(Boolean)
  return firstLine?.slice(0, 42) || '无题'
}
export const postPublicationTime = (post) => post?.publishedTime || post?.createTime || ''
export const firstPostMediaUrl = (content = '') =>
  String(content).match(
    /!\[[^\]]*]\(\s*<?((?:https?:\/\/|\/uploads\/|\/api\/media\/)[^\s)>]+)>?(?:\s+(?:"[^"]*"|'[^']*'))?\s*\)/i
  )?.[1] ||
  String(content).match(
    /<img\b[^>]*\bsrc=["']((?:https?:\/\/|\/uploads\/|\/api\/media\/)[^"']+)["']/i
  )?.[1] ||
  ''
export const postTags = (post) =>
  Array.isArray(post?.tags)
    ? post.tags
    : String(post?.tags || '')
        .split(/[,，]/)
        .map((item) => item.trim())
        .filter(Boolean)
