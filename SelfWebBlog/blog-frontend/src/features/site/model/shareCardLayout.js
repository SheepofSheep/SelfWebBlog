function splitTitle(title, lineLength, maxLines) {
  const value = String(title || 'Gabriel').trim()
  const lines = []
  let current = ''
  for (const character of value) {
    const weight = character.codePointAt(0) <= 255 ? 0.55 : 1
    const currentWeight = [...current].reduce(
      (sum, item) => sum + (item.codePointAt(0) <= 255 ? 0.55 : 1),
      0
    )
    if (current && currentWeight + weight > lineLength) {
      lines.push(current)
      current = character
      if (lines.length === maxLines) break
    } else {
      current += character
    }
  }
  if (lines.length < maxLines && current) lines.push(current)
  if (lines.join('').length < value.length) {
    lines[lines.length - 1] = `${lines.at(-1).slice(0, -1)}…`
  }
  return lines
}

export function createShareCardLayout({ orientation = 'horizontal', title = '' } = {}) {
  const horizontal = orientation === 'horizontal'
  const width = horizontal ? 1200 : 900
  const height = horizontal ? 675 : 1200
  const margin = 64
  const cover = horizontal ? { x: 0, y: 0, width: 480, height } : { x: 0, y: 0, width, height: 480 }
  const content = horizontal
    ? { x: 540, y: margin, width: 596, height: height - margin * 2 }
    : { x: margin, y: 540, width: width - margin * 2, height: 596 }
  const qrSize = horizontal ? 184 : 196
  const qr = {
    x: width - margin - qrSize,
    y: height - margin - qrSize,
    width: qrSize,
    height: qrSize
  }
  const maxTitleLines = horizontal ? 3 : 3
  return {
    width,
    height,
    orientation,
    cover,
    content,
    qr,
    maxTitleLines,
    titleLines: splitTitle(title, horizontal ? 12 : 16, maxTitleLines)
  }
}
