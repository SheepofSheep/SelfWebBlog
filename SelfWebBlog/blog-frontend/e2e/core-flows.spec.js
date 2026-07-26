import { expect, test } from '@playwright/test'

const ok = (data) => ({ code: 200, msg: 'ok', data })
const posts = [
  {
    id: '2081309851788009474',
    title: '可验证的文章',
    content: '## 小节\n\n正文内容\n\n```js\nconst ready = true\n```',
    summary: '用于端到端验收',
    category: '记录',
    tags: '布局, 影像',
    status: 'PUBLISHED',
    likeCount: 2,
    createTime: '2026-07-26T12:00:00'
  }
]

async function mockApi(page, user = null) {
  await page.route(/^https?:\/\/[^/]+\/api\//, async (route) => {
    const request = route.request()
    const url = new URL(request.url())
    const path = url.pathname
    if (path === '/api/auth/csrf') return route.fulfill({ json: ok({ token: 'csrf' }) })
    if (path === '/api/auth/session') return route.fulfill({ json: ok(user) })
    if (path === '/api/site/about')
      return route.fulfill({
        json: ok({ nickname: '博主', bio: '', socialLinks: {}, aboutMarkdown: '' })
      })
    if (path === '/api/posts/search') return route.fulfill({ json: ok({ records: posts }) })
    if (path === '/api/posts/2081309851788009474') return route.fulfill({ json: ok(posts[0]) })
    if (path === '/api/posts') return route.fulfill({ json: ok({ records: posts }) })
    if (path === '/api/tags')
      return route.fulfill({
        json: ok([
          { id: '1', name: '布局', slug: 'layout', postCount: 1 },
          { id: '2', name: '影像', slug: 'image', postCount: 1 }
        ])
      })
    if (path === '/api/content/calendar')
      return route.fulfill({
        json: ok({ days: [{ date: '2026-07-26', count: 1, posts }] })
      })
    if (path === '/api/engagement/POST/2081309851788009474/like')
      return route.fulfill({ json: ok({ liked: false, count: 2 }) })
    if (path === '/api/engagement/posts/2081309851788009474/view')
      return route.fulfill({ json: ok({ counted: true }) })
    if (path === '/api/interactions/POST/2081309851788009474')
      return route.fulfill({ json: ok({ items: [], total: 0, page: 1, size: 20 }) })
    return route.fulfill({ json: ok(null) })
  })
}

function observeRuntime(page) {
  const failures = []
  page.on('console', (message) => {
    if (message.type() === 'error') failures.push(`console: ${message.text()}`)
  })
  page.on('pageerror', (error) => failures.push(`page: ${error.message}`))
  return failures
}

async function expectNoOverflow(page) {
  await expect
    .poll(() => page.evaluate(() => document.documentElement.scrollWidth <= innerWidth + 1))
    .toBe(true)
}

test('home, search and responsive layout use real data contracts', async ({ page }) => {
  const failures = observeRuntime(page)
  await mockApi(page)
  await page.goto('/')
  await expect(page.getByRole('heading', { name: '可验证的文章' })).toBeVisible()
  await page.getByRole('button', { name: '搜索' }).click()
  await page.getByPlaceholder('搜索标题、正文或标签').fill('布局')
  await expect(page.getByRole('link', { name: '可验证的文章' })).toBeVisible()
  await expectNoOverflow(page)
  expect(failures).toEqual([])
})

test('theme transition and legacy links remain functional', async ({ page }) => {
  const failures = observeRuntime(page)
  await mockApi(page)
  await page.goto('/')
  await page.getByRole('button', { name: '外观' }).click()
  await page.getByRole('button', { name: '暗色' }).click()
  await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark')
  await page.goto('/#/post?id=2081309851788009474')
  await expect(page).toHaveURL(/\/post\/2081309851788009474$/)
  expect(failures).toEqual([])
})

test('reader exposes navigation, sharing and comment surfaces', async ({ page }) => {
  const failures = observeRuntime(page)
  await mockApi(page)
  await page.goto('/post/2081309851788009474')
  if (page.viewportSize().width > 600)
    await expect(page.getByRole('navigation', { name: '文章目录' })).toContainText('小节')
  else await expect(page.getByRole('heading', { name: '小节' })).toBeVisible()
  await expect(page.getByRole('button', { name: '复制' })).toBeVisible()
  await page.getByRole('button', { name: '分享' }).click()
  await expect(page.getByRole('img', { name: '当前文章二维码' })).toBeVisible()
  await expect(page.getByRole('heading', { name: '评论' })).toBeVisible()
  expect(failures).toEqual([])
})

test('editor flushes the last local change before navigation', async ({ page }, testInfo) => {
  test.skip(testInfo.project.name !== 'desktop', 'Draft lifecycle runs once on desktop')
  const failures = observeRuntime(page)
  await mockApi(page, { id: '1', username: 'root', role: 'ADMIN' })
  await page.goto('/write')
  await page.getByPlaceholder('标题（可选）').fill('不会丢失的最后一次修改')
  await page.getByRole('textbox', { name: '文章正文' }).fill('最后一行')
  await page.goto('/')
  const draft = await page.evaluate(() => JSON.parse(localStorage.getItem('postDraft:new')))
  expect(draft.title).toBe('不会丢失的最后一次修改')
  expect(draft.content).toBe('最后一行')
  expect(failures).toEqual([])
})
