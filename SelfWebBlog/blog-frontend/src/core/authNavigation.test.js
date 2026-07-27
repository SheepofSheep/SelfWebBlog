import test from 'node:test'
import assert from 'node:assert/strict'
import { oauthErrorMessage, safeRedirect } from './authNavigation.js'

test('keeps redirects local and maps oauth errors without exposing codes', () => {
  assert.equal(safeRedirect('/write/12?mode=edit', '/me'), '/write/12?mode=edit')
  assert.equal(safeRedirect('https://evil.test', '/me'), '/me')
  assert.equal(safeRedirect('//evil.test/path', '/me'), '/me')
  assert.equal(oauthErrorMessage('invalid_state'), '登录校验已过期，请重新尝试')
  assert.equal(oauthErrorMessage('unknown'), 'GitHub 登录没有完成，请重新尝试')
})
