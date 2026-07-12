export function formatTime(timeStr) {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  return `${date.getFullYear()}年${String(date.getMonth() + 1).padStart(2, '0')}月${String(
    date.getDate()
  ).padStart(2, '0')}日 ${String(date.getHours()).padStart(2, '0')}:00`
}

export function toPlainText(markdown) {
  const content = markdown || ''
  return content
    .replace(/!\[.*?\]\(.*?\)/g, '[图片]')
    .replace(/[#*_`[\]]/g, '')
    .replace(/\n/g, ' ')
    .trim()
}

export function getFirstImageUrl(content) {
  const contentStr = content || ''
  const imgMatch = contentStr.match(/!\[.*?\]\((.*?)\)/)
  if (imgMatch && imgMatch[1]) {
    return imgMatch[1]
  }
  const htmlImgMatch = contentStr.match(/<img[^>]+src="([^"]+)"/)
  if (htmlImgMatch && htmlImgMatch[1]) {
    return htmlImgMatch[1]
  }
  return null
}
