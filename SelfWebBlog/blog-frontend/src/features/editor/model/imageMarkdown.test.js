import assert from 'node:assert/strict'
import test from 'node:test'
import { buildImageMarkdown } from './imageMarkdown.js'

test('builds responsive picture markup from an uploaded asset', () => {
  const markdown = buildImageMarkdown(
    {
      originalUrl: '/uploads/original.png',
      smallWebpUrl: '/uploads/small.webp',
      largeWebpUrl: '/uploads/large.webp',
      width: 1280,
      height: 720
    },
    '文章配图'
  )

  assert.match(markdown, /<picture>/)
  assert.match(markdown, /media="\(max-width: 640px\)"/)
  assert.match(markdown, /width="1280" height="720"/)
  assert.match(markdown, /alt="文章配图"/)
})

test('falls back to ordinary markdown for legacy upload responses', () => {
  assert.equal(buildImageMarkdown('/uploads/legacy.png', '旧图'), '![旧图](/uploads/legacy.png)')
})

test('escapes image attributes', () => {
  const markdown = buildImageMarkdown(
    {
      originalUrl: '/uploads/a.png',
      smallWebpUrl: '/uploads/a-small.webp',
      largeWebpUrl: '/uploads/a-large.webp'
    },
    'a" onerror="alert(1)'
  )

  assert.doesNotMatch(markdown, /"\s+onerror=/)
  assert.match(markdown, /&quot;/)
})
