import QRCode from 'qrcode'
import { toAbsoluteUrl } from '../../../utils/url'
import { createShareCardLayout } from '../model/shareCardLayout'

function loadImage(source) {
  return new Promise((resolve, reject) => {
    const image = new Image()
    image.crossOrigin = 'anonymous'
    image.onload = () => resolve(image)
    image.onerror = reject
    image.src = source
  })
}

function drawCover(context, image, rect) {
  const scale = Math.max(rect.width / image.width, rect.height / image.height)
  const width = image.width * scale
  const height = image.height * scale
  context.drawImage(
    image,
    rect.x + (rect.width - width) / 2,
    rect.y + (rect.height - height) / 2,
    width,
    height
  )
}

function wrapText(context, text, maxWidth, maxLines) {
  const lines = []
  let current = ''
  for (const character of String(text || '')) {
    if (current && context.measureText(current + character).width > maxWidth) {
      lines.push(current)
      current = character
      if (lines.length === maxLines) break
    } else {
      current += character
    }
  }
  if (lines.length < maxLines && current) lines.push(current)
  if (lines.join('').length < String(text || '').length) {
    lines[lines.length - 1] = `${lines.at(-1).slice(0, -1)}…`
  }
  return lines
}

export async function renderShareCard({ post, url, orientation = 'horizontal' }) {
  const layout = createShareCardLayout({ orientation, title: post.title })
  const canvas = document.createElement('canvas')
  canvas.width = layout.width
  canvas.height = layout.height
  const context = canvas.getContext('2d')

  context.fillStyle = '#f6edcf'
  context.fillRect(0, 0, layout.width, layout.height)
  context.fillStyle = '#ead89e'
  context.fillRect(layout.cover.x, layout.cover.y, layout.cover.width, layout.cover.height)

  if (post.coverUrl) {
    try {
      drawCover(context, await loadImage(toAbsoluteUrl(post.coverUrl)), layout.cover)
    } catch {
      context.fillStyle = '#d99a1d'
      context.fillRect(layout.cover.x, layout.cover.y, 14, layout.cover.height)
    }
  }

  const left = layout.content.x
  let top = layout.content.y
  context.fillStyle = '#a96d00'
  context.font = '700 23px sans-serif'
  context.fillText("GABRIEL'S BLOG", left, top + 24)
  top += 72

  context.fillStyle = '#211d16'
  context.font = orientation === 'horizontal' ? '600 52px serif' : '600 58px serif'
  const titleLines = wrapText(context, post.title, layout.content.width, layout.maxTitleLines)
  for (const line of titleLines) {
    context.fillText(line, left, top + 54)
    top += 68
  }

  context.fillStyle = '#6d6559'
  context.font = '28px sans-serif'
  const summaryLines = wrapText(
    context,
    post.summary || post.category || '',
    layout.content.width,
    2
  )
  top += 14
  for (const line of summaryLines) {
    context.fillText(line, left, top + 34)
    top += 42
  }

  context.fillStyle = '#6d6559'
  context.font = '20px sans-serif'
  context.fillText('Gabriel', left, layout.height - 84)
  context.fillText(new URL(url).host, left, layout.height - 50)

  const qrDataUrl = await QRCode.toDataURL(url, {
    margin: 2,
    width: layout.qr.width,
    color: { dark: '#211d16', light: '#fffdf7' }
  })
  const qrImage = await loadImage(qrDataUrl)
  context.fillStyle = '#fffdf7'
  context.fillRect(layout.qr.x - 10, layout.qr.y - 10, layout.qr.width + 20, layout.qr.height + 20)
  context.drawImage(qrImage, layout.qr.x, layout.qr.y, layout.qr.width, layout.qr.height)

  const blob = await new Promise((resolve, reject) => {
    canvas.toBlob(
      (value) => (value ? resolve(value) : reject(new Error('分享卡片生成失败'))),
      'image/png'
    )
  })
  return {
    blob,
    dataUrl: canvas.toDataURL('image/png'),
    width: layout.width,
    height: layout.height
  }
}
