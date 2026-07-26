export function siteBackgroundImage(value = '') {
  const url = String(value).trim()
  if (!/^(?:https:\/\/[^\s<>"']+|\/(?!\/)[^\s<>"']*)$/i.test(url)) return ''
  const escaped = url.replace(/\\/g, '\\\\').replace(/"/g, '\\"')
  return `url("${escaped}")`
}
