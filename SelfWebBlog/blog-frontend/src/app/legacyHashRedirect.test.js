import assert from 'node:assert/strict'
import test from 'node:test'
import { legacyHashTarget } from './legacyHashRedirect.js'

test('converts old post hash to history route', () => {
  assert.equal(legacyHashTarget('/#/post?id=42'), '/post/42')
})

test('keeps query filters while converting archive', () => {
  assert.equal(legacyHashTarget('/#/archive?tag=Vue'), '/archive?tag=Vue')
})

test('returns null for a history url', () => {
  assert.equal(legacyHashTarget('/archive?tag=Vue'), null)
})

test('ignores an invalid post id without dropping the route', () => {
  assert.equal(legacyHashTarget('/#/post?id=not-a-number'), '/post?id=not-a-number')
})
