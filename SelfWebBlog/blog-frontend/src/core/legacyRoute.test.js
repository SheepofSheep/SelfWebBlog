import test from 'node:test'
import assert from 'node:assert/strict'
import { legacyRouteTarget } from './legacyRoute.js'

test('converts legacy hash routes and post query links', () => {
  assert.equal(legacyRouteTarget('https://blog.test/#/archive?page=2'), '/archive?page=2')
  assert.equal(
    legacyRouteTarget('https://blog.test/#/post?id=2081309851788009474'),
    '/post/2081309851788009474'
  )
  assert.equal(legacyRouteTarget('https://blog.test/post/7'), null)
  assert.equal(legacyRouteTarget('https://blog.test/#section'), null)
})
