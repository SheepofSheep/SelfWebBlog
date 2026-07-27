import test from 'node:test'
import assert from 'node:assert/strict'
import { publicSiteLinks } from './siteLinkModel.js'

test('custom ordered links replace the legacy fixed social fields', () => {
  const links = publicSiteLinks({
    links: [
      { id: '2', label: '作品', url: 'https://example.com/work', enabled: true, sortOrder: 1 },
      { id: '1', label: '主页', url: 'https://example.com', enabled: true, sortOrder: 0 },
      { id: '3', label: '隐藏', url: 'https://example.com/hidden', enabled: false, sortOrder: 2 }
    ],
    socialLinks: { github: 'https://github.com/legacy' }
  })

  assert.deepEqual(
    links.map((link) => link.label),
    ['主页', '作品']
  )
})

test('legacy email remains a safe mailto fallback before migration', () => {
  assert.equal(
    publicSiteLinks({ socialLinks: { email: 'hello@example.com' } })[0].url,
    'mailto:hello@example.com'
  )
})

test('an explicit custom-link collection can intentionally hide every legacy link', () => {
  assert.deepEqual(
    publicSiteLinks({ links: [], socialLinks: { github: 'https://github.com/legacy' } }),
    []
  )
  assert.deepEqual(
    publicSiteLinks({
      links: [
        {
          id: 'hidden',
          label: '隐藏',
          url: 'https://example.com/hidden',
          enabled: false,
          sortOrder: 0
        }
      ],
      socialLinks: { github: 'https://github.com/legacy' }
    }),
    []
  )
})
