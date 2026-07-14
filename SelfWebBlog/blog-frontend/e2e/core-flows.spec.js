import { expect, test } from '@playwright/test'
import jsQR from 'jsqr'

function observeRuntime(page) {
  const failures = []
  page.on('console', (message) => {
    if (message.type() === 'error') failures.push(`console: ${message.text()}`)
  })
  page.on('pageerror', (error) => failures.push(`page: ${error.message}`))
  page.on('response', (response) => {
    if (response.status() >= 500) failures.push(`http ${response.status()}: ${response.url()}`)
  })
  return failures
}

async function expectNoHorizontalOverflow(page) {
  await expect
    .poll(() => page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth + 1))
    .toBe(true)
}

test('home, unified search and archive filters stay usable', async ({ page }) => {
  const failures = observeRuntime(page)
  await page.goto('/')
  await expect(page.getByRole('heading', { level: 1 })).toBeVisible()
  await expectNoHorizontalOverflow(page)

  await page.getByRole('button', { name: '搜索文章' }).click()
  await page.getByRole('searchbox', { name: '搜索关键词' }).fill('更新')
  await page.getByRole('dialog').getByRole('button', { name: '查找' }).click()
  await expect(page).toHaveURL(/\/archive\?keyword=/)
  await expect(page.getByRole('heading', { name: '文章归档' })).toBeVisible()
  await expectNoHorizontalOverflow(page)
  expect(failures).toEqual([])
})

test('archive restores URL filters after browser history navigation', async ({ page }) => {
  const failures = observeRuntime(page)
  await page.goto('/archive?keyword=%E6%9B%B4%E6%96%B0')
  const search = page.getByRole('searchbox', { name: '搜索文章' })
  await expect(search).toHaveValue('更新')
  await search.fill('BUG')
  await page.getByRole('button', { name: '查找' }).click()
  await expect(page).toHaveURL(/keyword=BUG/)
  await page.goBack()
  await expect(page).toHaveURL(/keyword=%E6%9B%B4%E6%96%B0/)
  await expect(search).toHaveValue('更新')
  expect(failures).toEqual([])
})

test('anonymous guest can leave a root message but replies require login', async ({ page }) => {
  const failures = observeRuntime(page)
  const ok = (data) => ({ code: 200, msg: 'ok', data })
  await page.route('**/api/auth/session', (route) => route.fulfill({ json: ok(null) }))
  await page.route('**/api/captcha?*', (route) =>
    route.fulfill({
      json: ok({
        challengeId: 'challenge-1',
        imageDataUrl:
          'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII='
      })
    })
  )
  await page.route('**/api/interactions/GUESTBOOK/1**', (route) =>
    route.fulfill({
      json: ok({
        items: [
          {
            id: '10',
            targetType: 'GUESTBOOK',
            targetId: 1,
            content: '已有留言',
            nickname: '访客甲',
            role: 'USER',
            status: 'PUBLISHED',
            likeCount: 0,
            replies: []
          }
        ],
        total: 1,
        page: 1,
        size: 20
      })
    })
  )
  await page.route('**/api/interactions', async (route) => {
    if (route.request().method() !== 'POST') return route.continue()
    await route.fulfill({
      json: ok({
        id: '11',
        targetType: 'GUESTBOOK',
        targetId: 1,
        content: '新留言',
        nickname: '访客乙',
        role: 'USER',
        status: 'PENDING',
        likeCount: 0,
        replies: []
      })
    })
  })

  await page.goto('/guestbook')
  await page.getByRole('textbox', { name: '昵称' }).fill('访客乙')
  await page.getByRole('textbox', { name: '留言内容' }).fill('新留言')
  await page.getByRole('textbox', { name: '验证码' }).fill('ABCD')
  await page.getByRole('button', { name: '发布留言' }).click()
  await expect(page.getByText('新留言', { exact: true })).toBeVisible()
  await page.getByRole('button', { name: '回复' }).first().click()
  await expect(page).toHaveURL(/\/login\?redirect=/)
  expect(failures).toEqual([])
})

test('editor flushes the last local change before navigation', async ({ page }, testInfo) => {
  test.skip(testInfo.project.name !== 'desktop', 'Draft lifecycle runs once on desktop')
  const failures = observeRuntime(page)
  const ok = (data) => ({ code: 200, msg: 'ok', data })
  await page.route('**/api/auth/session', (route) =>
    route.fulfill({ json: ok({ id: '1', username: 'root', role: 'ADMIN' }) })
  )
  await page.route('**/api/profile?*', (route) =>
    route.fulfill({ json: ok({ blogInfo: { nickname: 'Gabriel', avatarUrl: '' }, posts: [] }) })
  )
  await page.route('**/api/tags', (route) => route.fulfill({ json: ok([]) }))
  await page.route('**/api/posts/categories', (route) => route.fulfill({ json: ok([]) }))
  await page.route('**/api/upload/assets', (route) => route.fulfill({ json: ok([]) }))

  await page.goto('/')
  await page.goto('/write')
  await page.getByRole('textbox', { name: '文章标题' }).fill('不会丢失的最后一次修改')
  page.once('dialog', (dialog) => dialog.accept())
  await page.getByRole('button', { name: '返回' }).click()
  await expect(page).toHaveURL(/\/$/)
  const draftTitle = await page.evaluate(
    () => JSON.parse(localStorage.getItem('postDraft:new')).title
  )
  expect(draftTitle).toBe('不会丢失的最后一次修改')
  expect(failures).toEqual([])
})

test('theme and motion share one accessible control', async ({ page }) => {
  const failures = observeRuntime(page)
  await page.goto('/')
  await page.getByRole('button', { name: '外观设置' }).click()
  await page.getByRole('button', { name: '深色' }).click()
  await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark')
  await page.getByRole('button', { name: '精简' }).click()
  await expect(page.locator('html')).toHaveAttribute('data-motion', 'reduced')
  await expectNoHorizontalOverflow(page)
  expect(failures).toEqual([])
})

test('login and registration switch without changing panel width', async ({ page }) => {
  const failures = observeRuntime(page)
  await page.goto('/login')
  const panel = page.locator('.auth-panel')
  const loginWidth = (await panel.boundingBox())?.width
  await page.getByRole('button', { name: '注册' }).click()
  await expect(page.getByRole('textbox', { name: '用户名' })).toBeVisible()
  const registerWidth = (await panel.boundingBox())?.width
  expect(Math.abs((loginWidth || 0) - (registerWidth || 0))).toBeLessThan(1)
  await expectNoHorizontalOverflow(page)
  expect(failures).toEqual([])
})

test('admin workbench hides raw IP fields', async ({ page }, testInfo) => {
  test.skip(testInfo.project.name !== 'desktop', 'Admin smoke runs once on desktop')
  const username = process.env.E2E_ADMIN_USERNAME
  const password = process.env.E2E_ADMIN_PASSWORD
  test.skip(!username || !password, 'Admin credentials were not supplied')

  const failures = observeRuntime(page)
  await page.goto('/login')
  await page.getByRole('textbox', { name: '用户名' }).fill(username)
  await page.getByRole('textbox', { name: '密码' }).fill(password)
  await Promise.all([
    page.waitForURL((url) => url.pathname === '/'),
    page.locator('form').getByRole('button', { name: '登录' }).click()
  ])
  await page.goto('/admin')
  await expect(page.getByRole('heading', { name: '工作台概览' })).toBeVisible()
  await page.getByRole('button', { name: '用户', exact: true }).click()
  await expect(page.getByRole('heading', { name: '用户管理' })).toBeVisible()
  const result = await page.evaluate(async () => {
    const payload = await fetch('/api/admin/users').then((response) => response.json())
    return (payload.data?.users || []).some(
      (user) => 'ipAddress' in user || 'registrationIpHash' in user
    )
  })
  expect(result).toBe(false)
  expect(failures).toEqual([])
})

test('share card contains a decodable current article URL', async ({ page }, testInfo) => {
  test.skip(testInfo.project.name !== 'desktop', 'Share card pixel verification runs once')
  const failures = observeRuntime(page)
  await page.goto('/')
  const postId = await page.evaluate(async () => {
    const payload = await fetch('/api/posts?page=1&size=1').then((response) => response.json())
    return payload.data?.records?.[0]?.id
  })
  expect(postId).toBeTruthy()
  await page.goto(`/post/${postId}`)
  await page.getByRole('button', { name: '分享', exact: true }).click()
  const preview = page.getByRole('img', { name: '文章分享卡片预览' })
  await expect(preview).toBeVisible()
  const qrPixels = await preview.evaluate(async (image) => {
    await image.decode()
    const canvas = document.createElement('canvas')
    canvas.width = 184
    canvas.height = 184
    const context = canvas.getContext('2d', { willReadFrequently: true })
    context.drawImage(image, 952, 427, 184, 184, 0, 0, 184, 184)
    return Array.from(context.getImageData(0, 0, 184, 184).data)
  })
  const decoded = jsQR(Uint8ClampedArray.from(qrPixels), 184, 184)
  expect(decoded?.data).toBe(page.url())
  expect(failures).toEqual([])
})
