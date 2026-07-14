function escapeAttribute(value) {
  return String(value || '')
    .replaceAll('&', '&amp;')
    .replaceAll('"', '&quot;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
}

export function buildImageMarkdown(asset, alt = '图片描述') {
  if (!asset || typeof asset !== 'object') return `![${alt}](${String(asset || '')})`

  const original = escapeAttribute(asset.originalUrl)
  const small = escapeAttribute(asset.smallWebpUrl)
  const large = escapeAttribute(asset.largeWebpUrl)
  const safeAlt = escapeAttribute(alt)
  const width = Number.isFinite(Number(asset.width)) ? ` width="${Number(asset.width)}"` : ''
  const height = Number.isFinite(Number(asset.height)) ? ` height="${Number(asset.height)}"` : ''

  if (!original || (!small && !large)) return `![${alt}](${asset.originalUrl || ''})`

  const sources = [
    small && `<source media="(max-width: 640px)" srcset="${small}" type="image/webp">`,
    large && `<source srcset="${large}" type="image/webp">`
  ].filter(Boolean)

  return [
    '<picture>',
    ...sources,
    `<img src="${original}" alt="${safeAlt}"${width}${height} loading="lazy">`,
    '</picture>'
  ].join('\n')
}
