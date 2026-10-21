import test from 'node:test'
import assert from 'node:assert/strict'
import { componentHarness } from '../test/componentHarness.js'
import { oauthErrorMessage, safeRedirect } from '../core/authNavigation.js'

function loginHarness(api) {
  const navigations = []
  const editor = componentHarness(
    new URL('./LoginPage.vue', import.meta.url),
    {
      api,
      oauthErrorMessage,
      safeRedirect,
      useRoute: () => ({ query: { redirect: '/post/7' } }),
      useRouter: () => ({ replace: (path) => navigations.push(path) }),
      appStore: { ready: true, user: null },
      notify() {}
    },
    [
      'form',
      'submit',
      'mode',
      'switchMode',
      'loadCaptcha',
      'captcha',
      'captchaError',
      'captchaLoading',
      'formError',
      'busy'
    ]
  )
  return { ...editor, navigations }
}

test('register captcha failures are recoverable and switching to login ignores a late captcha response', async () => {
  let fail = true
  let finish
  const login = loginHarness({
    captcha: () =>
      fail
        ? Promise.reject(new Error('验证码暂不可用'))
        : new Promise((resolve) => {
            finish = resolve
          })
  })
  try {
    login.state.switchMode('register')
    await new Promise((resolve) => setImmediate(resolve))
    assert.equal(login.state.captchaError.value, '验证码暂不可用')
    assert.equal(login.state.captchaLoading.value, false)
    fail = false
    login.state.form.value.captchaAnswer = '旧答案'
    const pending = login.state.loadCaptcha()
    assert.equal(login.state.form.value.captchaAnswer, '')
    login.state.switchMode('login')
    finish({ challengeId: 'late' })
    await pending
    assert.equal(login.state.captcha.value, null)
    assert.equal(login.state.captchaLoading.value, false)
  } finally {
    login.unmount()
  }
})

test('login ignores duplicate submits and mode switches while preserving its return destination', async () => {
  let finish
  let calls = 0
  const login = loginHarness({
    login: () => {
      calls++
      return new Promise((resolve) => {
        finish = resolve
      })
    }
  })
  try {
    login.state.form.value = { username: ' reader ', password: 'password' }
    const pending = login.state.submit()
    await login.state.submit()
    login.state.switchMode('register')
    assert.equal(login.state.mode.value, 'login')
    assert.equal(calls, 1)
    finish({ user: { id: '7', role: 'USER' } })
    await pending
    assert.deepEqual(login.navigations, ['/post/7'])
  } finally {
    login.unmount()
  }
})
