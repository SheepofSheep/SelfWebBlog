const permanentMedia =
  /(?:!\[[^\]]*]\((?:https?:\/\/|\/uploads\/|\/api\/media\/)[^)]+\)|<(?:img|video|audio)\b[^>]*\b(?:src|poster)=["'](?:https?:\/\/|\/uploads\/|\/api\/media\/)[^"']+["'][^>]*>)/i
const markdownMedia = /!\[[^\]]*]\([^)]+\)/gi
const htmlMedia = /<(?:img|video|audio)\b[^>]*>/gi
const nonRenderedHtml =
  /<(?:script|style|template|head)\b[^>]*>[\s\S]*?<\/(?:script|style|template|head)\s*>/gi

export function hasPermanentMedia(content = '') {
  return permanentMedia.test(stripNonRendered(content))
}

export function canPublish(content = '') {
  const rendered = stripNonRendered(content)
  if (permanentMedia.test(rendered)) return true
  const visible = rendered
    .replace(markdownMedia, '')
    .replace(htmlMedia, '')
    .replace(/<[^>]*>/g, '')
    .replace(/&(?:nbsp|#160|#x0*a0);/gi, '')
    .replace(/[\u200B-\u200D\uFEFF]/g, '')
    .replace(/[\s#*_~>`[\]()!|+-]/g, '')
  return visible.length > 0
}

function stripNonRendered(content = '') {
  return String(content)
    .replace(nonRenderedHtml, '')
    .replace(/<!--[\s\S]*?-->/g, '')
}
