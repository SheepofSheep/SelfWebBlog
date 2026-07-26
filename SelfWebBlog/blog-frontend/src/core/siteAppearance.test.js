import test from 'node:test'
import assert from 'node:assert/strict'
import { siteBackgroundImage } from './siteAppearance.js'

test('site background accepts persisted HTTPS and local media paths', () => {
  assert.equal(
    siteBackgroundImage('https://cdn.example.test/background.webp'),
    'url("https://cdn.example.test/background.webp")'
  )
  assert.equal(
    siteBackgroundImage('/uploads/site/background.webp'),
    'url("/uploads/site/background.webp")'
  )
})

test('site background rejects executable, insecure, and protocol-relative URLs', () => {
  assert.equal(siteBackgroundImage('javascript:alert(1)'), '')
  assert.equal(siteBackgroundImage('http://example.test/background.webp'), '')
  assert.equal(siteBackgroundImage('//example.test/background.webp'), '')
})
