export function siteBackgroundUrl(value = '') {
  const url = String(value).trim()
  if (!/^(?:https:\/\/[^\s<>"']+|\/(?!\/)[^\s<>"']*)$/i.test(url)) return ''
  return url
}

export function siteBackgroundImage(value = '') {
  const url = siteBackgroundUrl(value)
  if (!url) return ''
  const escaped = url.replace(/\\/g, '\\\\').replace(/"/g, '\\"')
  return `url("${escaped}")`
}
