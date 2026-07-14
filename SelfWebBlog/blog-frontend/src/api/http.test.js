import assert from 'node:assert/strict'
import test from 'node:test'
import { isAuthFailure } from './http.js'

test('only treats authentication failures as an expired session', () => {
  assert.equal(isAuthFailure({ code: 401, msg: '未登录' }, 401), true)
  assert.equal(isAuthFailure({ code: 403, msg: '当前账号没有管理员权限' }, 403), false)
  assert.equal(isAuthFailure({ code: 403, msg: '请求校验已过期' }, 403), false)
})

test('recognizes legacy expired credential messages without a status', () => {
  assert.equal(isAuthFailure({ msg: '登录凭证已失效' }), true)
  assert.equal(isAuthFailure({ msg: 'token 已过期' }), true)
})
