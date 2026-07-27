import test from 'node:test'
import assert from 'node:assert/strict'
import { EMOJI_CATALOG, insertStickerToken, parseComment, stickerToken } from './emojiCatalog.js'

test('known sticker tokens become trusted catalog segments', () => {
  const segments = parseComment(`前面${stickerToken('喜欢')}后面`)

  assert.deepEqual(
    segments.map((segment) => segment.kind),
    ['text', 'sticker', 'text']
  )
  assert.equal(segments[1].name, '喜欢')
  assert.equal(segments[1].src, '/images/emojis/喜欢.jpg')
  assert.equal(EMOJI_CATALOG.length, 33)
})

test('legacy emoji markdown remains compatible with the trusted sticker catalog', () => {
  const segments = parseComment('旧评论![搭讪](/images/emojis/搭讪.jpg)继续')

  assert.deepEqual(
    segments.map((segment) => segment.kind),
    ['text', 'sticker', 'text']
  )
  assert.equal(segments[1].name, '搭讪')
  assert.equal(segments[1].src, '/images/emojis/搭讪.jpg')
})

test('legacy markdown cannot load images outside the sticker whitelist', () => {
  const content = '![x](/images/emojis/不存在.jpg) ![x](https://example.com/x.jpg)'

  assert.deepEqual(parseComment(content), [{ kind: 'text', text: content }])
})

test('unknown and malformed sticker tokens remain plain text', () => {
  assert.deepEqual(parseComment('[sticker:不存在]'), [{ kind: 'text', text: '[sticker:不存在]' }])
  assert.deepEqual(parseComment('[sticker:]'), [{ kind: 'text', text: '[sticker:]' }])
})

test('sticker insertion replaces the current selection and returns the next caret', () => {
  const result = insertStickerToken('今天很好', 2, 3, '赞')

  assert.equal(result.text, `今天${stickerToken('赞')}好`)
  assert.equal(result.caret, 2 + stickerToken('赞').length)
})
