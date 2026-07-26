const permanentMedia =
  /(?:!\[[^\]]*]\((?:https?:\/\/|\/uploads\/|\/api\/media\/)[^)]+\)|<(?:img|video|audio)\b[^>]*\bsrc=["'](?:https?:\/\/|\/uploads\/|\/api\/media\/)[^"']+["'][^>]*>)/i

export function hasPermanentMedia(content = '') {
  return permanentMedia.test(content)
}

export function canPublish(content = '') {
  if (/upload-failed|blob:/i.test(content)) return false
  if (hasPermanentMedia(content)) return true
  const visible = content.replace(/<[^>]+>/g, '').replace(/[\s#*_~>`[\]()!|+-]/g, '')
  return visible.length > 0
}
