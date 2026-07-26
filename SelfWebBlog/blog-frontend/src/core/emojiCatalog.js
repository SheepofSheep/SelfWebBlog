const FILE_NAMES = [
  '啊？',
  '欸嘿',
  '馋',
  '嘲笑',
  '吃瓜',
  '搭讪',
  '尴尬',
  '鬼迷日眼',
  '害怕',
  '呵呵',
  '嘿嘿',
  '狐狸农',
  '看热闹',
  '酷笑',
  '卖萌',
  '喵',
  '摸头',
  '哦',
  '求收留',
  '生气',
  '哇',
  '王德法',
  '我去',
  '喜欢',
  '嫌弃',
  '小狗农',
  '小生气',
  '星星眼',
  '耶',
  '赞',
  'love',
  'love眼',
  'wink'
]

export const EMOJI_CATALOG = Object.freeze(
  FILE_NAMES.map((name) =>
    Object.freeze({
      name,
      src: `/images/emojis/${name}.jpg`,
      token: `[sticker:${name}]`
    })
  )
)

const BY_NAME = new Map(EMOJI_CATALOG.map((item) => [item.name, item]))
const BY_SRC = new Map(EMOJI_CATALOG.map((item) => [item.src, item]))

export function stickerToken(name) {
  return BY_NAME.get(String(name))?.token || ''
}

function appendText(segments, text) {
  if (!text) return
  const previous = segments.at(-1)
  if (previous?.kind === 'text') previous.text += text
  else segments.push({ kind: 'text', text })
}

export function parseComment(value) {
  const content = String(value || '')
  const segments = []
  const pattern =
    /\[sticker:([^\]\r\n]{1,40})]|!\[[^\]\r\n]{0,80}]\((\/images\/emojis\/[^)\r\n]{1,100}\.jpg)\)/g
  let cursor = 0
  let match
  while ((match = pattern.exec(content))) {
    appendText(segments, content.slice(cursor, match.index))
    const sticker = match[1] ? BY_NAME.get(match[1]) : BY_SRC.get(match[2])
    if (sticker) segments.push({ kind: 'sticker', name: sticker.name, src: sticker.src })
    else appendText(segments, match[0])
    cursor = match.index + match[0].length
  }
  appendText(segments, content.slice(cursor))
  return segments
}

export function insertStickerToken(text, from, to, name) {
  const content = String(text || '')
  const start = Math.max(0, Math.min(content.length, Number(from) || 0))
  const end = Math.max(start, Math.min(content.length, Number(to) || start))
  const token = stickerToken(name)
  const next = content.slice(0, start) + token + content.slice(end)
  return { text: next, caret: start + token.length }
}
