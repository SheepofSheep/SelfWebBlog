import assert from 'node:assert/strict'
import test from 'node:test'
import { createShareCardLayout } from './shareCardLayout.js'

test('keeps horizontal and vertical content inside canvas', () => {
  for (const orientation of ['horizontal', 'vertical']) {
    const layout = createShareCardLayout({
      orientation,
      title: '这是一篇标题很长但不能跑出分享卡片画布边界的文章标题'
    })
    for (const rect of [layout.cover, layout.qr, layout.content]) {
      assert.ok(rect.x >= 0 && rect.y >= 0)
      assert.ok(rect.x + rect.width <= layout.width)
      assert.ok(rect.y + rect.height <= layout.height)
    }
    assert.ok(layout.titleLines.length <= layout.maxTitleLines)
  }
})

test('reserves a stable qr quiet zone without a cover', () => {
  const layout = createShareCardLayout({ orientation: 'horizontal', title: 'Title' })
  assert.ok(layout.qr.width >= 180)
  assert.ok(layout.qr.x - layout.content.x > 100)
})
