import { expect, test } from '@playwright/test'

const ok = (data) => ({ code: 200, msg: 'ok', data })
const pixel = 'data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///ywAAAAAAQABAAACAUwAOw=='
const backgroundPath = '/uploads/site-background.webp'
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
  },
  {
    id: '2081309851788009475',
    title: '八月日历文章',
    content: '八月正文',
    summary: '用于验证日历切月',
    category: '记录',
    tags: '日历',
    status: 'PUBLISHED',
    likeCount: 0,
    createTime: '2026-08-03T09:30:00'
  },
  {
    id: '2081309851788009476',
    title: '跨年度归档文章标题用于验证长内容不会挤压年份栏',
    content: '归档正文',
    summary: '用于验证归档年份布局',
    category: '随笔',
    tags: '归档',
    status: 'PUBLISHED',
    likeCount: 1,
    createTime: '2025-12-31T23:00:00'
  }
]

async function mockApi(page, user = null) {
  const state = {
    posts: posts.map((post) => ({ ...post })),
    users: [
      {
        id: '1',
        username: 'root',
        nickname: '管理者',
        email: 'root@example.test',
        role: 'ADMIN',
        titleName: '',
        titleStyle: 'default',
        createTime: '2026-01-01T00:00:00'
      },
      {
        id: '2',
        username: 'reader',
        nickname: '验收用户',
        email: 'reader@example.test',
        role: 'USER',
        titleName: '旧称号',
        titleStyle: 'blue',
        createTime: '2026-06-01T00:00:00'
      }
    ],
    interactions: [
      {
        id: 'comment-1',
        targetType: 'POST',
        targetId: posts[0].id,
        sourceTitle: posts[0].title,
        rootId: null,
        replyToId: null,
        replyToNickname: null,
        content: '验收评论',
        nickname: '留言者',
        avatarUrl: '',
        titleName: '贡献者',
        titleStyle: 'gold',
        ipRegion: '上海',
        status: 'PUBLISHED',
        likeCount: 4,
        liked: false,
        pinned: true,
        pinOrder: 0,
        createTime: '2026-07-26T14:00:00',
        replyCount: 1,
        replies: [
          {
            id: 'comment-reply-1',
            targetType: 'POST',
            targetId: posts[0].id,
            sourceTitle: posts[0].title,
            rootId: 'comment-1',
            replyToId: 'comment-1',
            replyToNickname: '留言者',
            content: '折叠的验收回复',
            nickname: '回复者',
            avatarUrl: '',
            titleName: '',
            titleStyle: 'default',
            ipRegion: '杭州',
            status: 'PUBLISHED',
            likeCount: 1,
            pinned: false,
            createTime: '2026-07-26T14:10:00',
            replyCount: 0,
            replies: []
          }
        ]
      }
    ],
    assets: [
      {
        id: 'asset-1',
        originalUrl: pixel,
        smallWebpUrl: pixel,
        largeWebpUrl: pixel,
        width: 640,
        height: 480,
        byteSize: 1024,
        purpose: 'ARTICLE',
        referenceStatus: 'ACTIVE',
        uploaderId: '1',
        referenceCount: 1,
        createTime: '2026-07-26T10:00:00'
      }
    ],
    links: [
      {
        id: 'link-1',
        label: '验收链接',
        url: 'https://example.test/profile',
        iconKey: 'link',
        enabled: true,
        sortOrder: 0
      }
    ],
    failBackgroundUpload: false,
    calls: [],
    unhandled: []
  }

  await page.route(/^https?:\/\/[^/]+\/uploads\//, async (route) => {
    await route.fulfill({
      contentType: 'image/gif',
      body: Buffer.from('R0lGODlhAQABAIAAAAAAAP///ywAAAAAAQABAAACAUwAOw==', 'base64')
    })
  })
  await page.route(/^https?:\/\/[^/]+\/api\//, async (route) => {
    const request = route.request()
    const url = new URL(request.url())
    const path = url.pathname
    const method = request.method()
    let body = null
    if (request.postData()) {
      try {
        body = request.postDataJSON()
      } catch {
        body = request.postData()
      }
    }
    state.calls.push({ method, path, query: Object.fromEntries(url.searchParams), body })

    if (path === '/api/auth/csrf') return route.fulfill({ json: ok({ token: 'csrf' }) })
    if (path === '/api/auth/session') return route.fulfill({ json: ok(user) })
    if (path === '/api/auth/logout') return route.fulfill({ json: ok(null) })
    if (path === '/api/user/profile' && method === 'PUT') {
      if (user) Object.assign(user, body)
      return route.fulfill({ json: ok(user) })
    }
    if (path === '/api/user/avatar' && method === 'POST') {
      if (user) user.avatarUrl = pixel
      return route.fulfill({ json: ok(pixel) })
    }
    if (path === '/api/site/about')
      return route.fulfill({
        json: ok({
          nickname: '博主',
          bio: '',
          avatarUrl: '',
          socialLinks: {},
          links: state.links,
          aboutMarkdown: ''
        })
      })
    if (path === '/api/site/summary')
      return route.fulfill({ json: ok({ postCount: state.posts.length, tagCount: 4 }) })

    if (path === '/api/admin/dashboard/overview')
      return route.fulfill({
        json: ok({
          publishedCount: 3,
          draftCount: 1,
          pendingCount: 1,
          userCount: state.users.length,
          assetCount: state.assets.length,
          orphanedCount: 0,
          databaseStatus: 'UP',
          latestActivity: '2026-07-26T15:00:00'
        })
      })
    if (path === '/api/admin/dashboard/trend')
      return route.fulfill({
        json: ok({
          endDate: '2026-07-26',
          totals: { views: 24, interactions: 7, posts: 3 },
          days: [
            { date: '2026-07-24', views: 4, interactions: 1, posts: 0 },
            { date: '2026-07-25', views: 8, interactions: 2, posts: 1 },
            { date: '2026-07-26', views: 12, interactions: 4, posts: 2 }
          ]
        })
      })
    if (path === '/api/admin/dashboard/security-events') return route.fulfill({ json: ok([]) })

    if (path === '/api/posts/admin') {
      const status = url.searchParams.get('status') || ''
      const records = state.posts.filter((post) => !status || post.status === status)
      return route.fulfill({
        json: ok({
          records,
          current: Number(url.searchParams.get('page') || 1),
          pages: 1,
          total: records.length
        })
      })
    }
    if (/^\/api\/posts\/[^/]+\/status$/.test(path) && method === 'PUT') {
      const id = path.split('/')[3]
      const post = state.posts.find((item) => String(item.id) === id)
      if (post) post.status = body.status
      return route.fulfill({ json: ok(post || null) })
    }
    if (path === '/api/posts/batch/status' && method === 'PUT') {
      state.posts.forEach((post) => {
        if (body.ids.map(String).includes(String(post.id))) post.status = body.status
      })
      return route.fulfill({ json: ok(body.ids.length) })
    }
    if (path === '/api/posts/batch-delete' && method === 'POST') {
      state.posts = state.posts.filter((post) => !body.ids.map(String).includes(String(post.id)))
      return route.fulfill({ json: ok(body.ids.length) })
    }
    if (path === '/api/posts/search') return route.fulfill({ json: ok({ records: state.posts }) })
    if (/^\/api\/posts\/[^/]+$/.test(path)) {
      const id = path.split('/')[3]
      if (method === 'DELETE') {
        state.posts = state.posts.filter((post) => String(post.id) !== id)
        return route.fulfill({ json: ok(null) })
      }
      return route.fulfill({
        json: ok(state.posts.find((post) => String(post.id) === id) || null)
      })
    }
    if (path === '/api/posts') {
      if (method === 'POST') return route.fulfill({ json: ok({ id: 'new-post', ...body }) })
      const publicPosts = state.posts.filter((post) => post.status === 'PUBLISHED')
      const pageNumber = Math.max(1, Number(url.searchParams.get('page') || 1))
      const pageSize = Math.max(1, Number(url.searchParams.get('size') || 20))
      const from = (pageNumber - 1) * pageSize
      return route.fulfill({
        json: ok({
          records: publicPosts.slice(from, from + pageSize),
          current: pageNumber,
          pages: Math.ceil(publicPosts.length / pageSize),
          total: publicPosts.length
        })
      })
    }

    if (/^\/api\/tags\/[^/]+\/posts$/.test(path)) {
      const slug = decodeURIComponent(path.split('/')[3])
      return route.fulfill({
        json: ok(
          state.posts.filter((post) =>
            String(post.tags || '')
              .split(/[,，]/)
              .map((tag) => tag.trim())
              .includes(slug === 'layout' ? '布局' : slug === 'image' ? '影像' : slug)
          )
        )
      })
    }
    if (path === '/api/tags')
      return route.fulfill({
        json: ok([
          { id: '1', name: '布局', slug: 'layout', postCount: 1 },
          { id: '2', name: '影像', slug: 'image', postCount: 1 }
        ])
      })
    if (path === '/api/content/calendar') {
      const month = Number(url.searchParams.get('month'))
      const day =
        month === 8
          ? { date: '2026-08-03', count: 1, posts: [state.posts[1]] }
          : {
              date: '2026-07-26',
              count: 2,
              posts: [
                state.posts[0],
                { id: 'calendar-extra', title: '同日第二篇', createTime: '2026-07-26T08:00:00' }
              ]
            }
      return route.fulfill({
        json: ok({
          month: `2026-${String(month).padStart(2, '0')}`,
          postCount: day.count,
          days: [day]
        })
      })
    }
    if (/^\/api\/engagement\/POST\/[^/]+\/like$/.test(path))
      return route.fulfill({ json: ok({ liked: false, count: 2 }) })
    if (/^\/api\/engagement\/posts\/[^/]+\/view$/.test(path))
      return route.fulfill({ json: ok({ counted: true }) })
    if (path === '/api/interactions' && method === 'POST') {
      const root = state.interactions.find(
        (item) =>
          String(item.id) === String(body.replyToId) ||
          item.replies?.some((reply) => String(reply.id) === String(body.replyToId))
      )
      const target =
        String(root?.id) === String(body.replyToId)
          ? root
          : root?.replies?.find((item) => String(item.id) === String(body.replyToId))
      const reply = {
        id: `comment-reply-${Date.now()}`,
        ...body,
        rootId: root?.id || null,
        replyToNickname: target?.nickname || '已删除用户',
        nickname: user?.nickname || user?.username || '管理员',
        avatarUrl: user?.avatarUrl || '',
        titleName: user?.titleName || '',
        titleStyle: user?.titleStyle || 'default',
        ipRegion: '上海',
        status: 'PUBLISHED',
        likeCount: 0,
        pinned: false,
        createTime: '2026-07-26T15:00:00',
        replyCount: 0,
        replies: []
      }
      if (root) {
        root.replies.push(reply)
        root.replyCount = root.replies.length
      }
      return route.fulfill({ json: ok(reply) })
    }
    if (/^\/api\/interactions\/POST\/[^/]+$/.test(path)) {
      const targetId = path.split('/')[4]
      const pageNumber = Number(url.searchParams.get('page') || 1)
      const pageSize = Number(url.searchParams.get('size') || 20)
      const roots = state.interactions.filter(
        (item) => !item.rootId && String(item.targetId) === targetId
      )
      const from = (pageNumber - 1) * pageSize
      return route.fulfill({
        json: ok({
          items: roots
            .slice(from, from + pageSize)
            .map((root) => ({ ...root, replies: root.replies || [] })),
          total: roots.length,
          page: pageNumber,
          size: pageSize
        })
      })
    }

    if (path === '/api/admin/interactions' && method === 'GET')
      return route.fulfill({
        json: ok({
          records: state.interactions,
          current: 1,
          pages: 1,
          total: state.interactions.length
        })
      })
    if (path === '/api/admin/interactions/batch/status' && method === 'PUT')
      return route.fulfill({ json: ok(body.ids.length) })
    if (path === '/api/admin/interactions/batch-delete' && method === 'POST') {
      state.interactions = state.interactions.filter(
        (item) => !body.ids.map(String).includes(String(item.id))
      )
      return route.fulfill({ json: ok(body.ids.length) })
    }
    if (/^\/api\/admin\/interactions\/[^/]+\/(status|pinned|pin-order)$/.test(path))
      return route.fulfill({ json: ok(state.interactions[0]) })
    if (/^\/api\/admin\/interactions\/[^/]+$/.test(path) && method === 'DELETE')
      return route.fulfill({ json: ok(null) })

    if (path === '/api/admin/assets' && method === 'GET')
      return route.fulfill({
        json: ok({ records: state.assets, current: 1, pages: 1, total: state.assets.length })
      })
    if (path === '/api/admin/assets/scan')
      return route.fulfill({ json: ok({ active: state.assets.length, orphaned: 0 }) })
    if (/^\/api\/admin\/assets\/[^/]+\/references$/.test(path))
      return route.fulfill({
        json: ok({
          referenceStatus: 'ACTIVE',
          referenceCount: 1,
          references: [
            { sourceType: 'POST', targetId: posts[0].id, field: 'coverUrl', label: posts[0].title }
          ]
        })
      })
    if (path === '/api/admin/assets/batch' && method === 'DELETE') {
      const ids = body.ids.map(String)
      state.assets = state.assets.filter((asset) => !ids.includes(String(asset.id)))
      return route.fulfill({ json: ok({ deleted: ids.length }) })
    }
    if (/^\/api\/admin\/assets\/[^/]+$/.test(path) && method === 'DELETE')
      return route.fulfill({ json: ok(null) })

    if (path === '/api/admin/users' && method === 'GET')
      return route.fulfill({
        json: ok({ users: state.users, total: state.users.length, duplicateIpCount: 0 })
      })
    if (/^\/api\/admin\/users\/[^/]+\/title$/.test(path) && method === 'PUT') {
      const id = path.split('/')[4]
      const target = state.users.find((item) => String(item.id) === id)
      if (target) Object.assign(target, body)
      return route.fulfill({ json: ok(target || null) })
    }
    if (/^\/api\/admin\/users\/[^/]+$/.test(path) && method === 'DELETE')
      return route.fulfill({ json: ok(null) })

    if (path === '/api/upload/image' && method === 'POST') {
      if (state.failBackgroundUpload)
        return route.fulfill({ json: { code: 400, msg: '背景上传失败', data: null } })
      return route.fulfill({
        json: ok({
          id: 'site-background',
          originalUrl: backgroundPath,
          smallWebpUrl: backgroundPath,
          largeWebpUrl: backgroundPath,
          width: 1,
          height: 1,
          byteSize: 35,
          purpose: 'ARTICLE',
          referenceStatus: 'ORPHANED',
          uploaderId: '1',
          referenceCount: 0,
          createTime: '2026-07-27T00:00:00'
        })
      })
    }

    if (path === '/api/admin/site/links/order' && method === 'PUT') {
      const order = body.ids.map(String)
      state.links = order
        .map((id) => state.links.find((link) => String(link.id) === id))
        .filter(Boolean)
        .map((link, sortOrder) => ({ ...link, sortOrder }))
      return route.fulfill({ json: ok(state.links) })
    }
    if (path === '/api/admin/site/links' && method === 'GET')
      return route.fulfill({ json: ok(state.links) })
    if (path === '/api/admin/site/links' && method === 'POST') {
      state.links.push({
        id: `link-${state.links.length + 1}`,
        sortOrder: state.links.length,
        ...body
      })
      return route.fulfill({ json: ok(state.links.at(-1)) })
    }
    if (/^\/api\/admin\/site\/links\/[^/]+$/.test(path) && method === 'PUT') {
      const id = path.split('/')[5]
      const link = state.links.find((item) => String(item.id) === id)
      if (link) Object.assign(link, body)
      return route.fulfill({ json: ok(link || null) })
    }
    if (/^\/api\/admin\/site\/links\/[^/]+$/.test(path) && method === 'DELETE') {
      const id = path.split('/')[5]
      state.links = state.links.filter((item) => String(item.id) !== id)
      return route.fulfill({ json: ok(null) })
    }
    if (path === '/api/admin/site' && method === 'GET')
      return route.fulfill({
        json: ok({
          nickname: '博主',
          bio: '',
          avatarUrl: '',
          bgUrl: '',
          siteStartDate: '2026-01-01',
          currentStatus: '',
          aboutMarkdown: '',
          socialLinks: {}
        })
      })
    if (path === '/api/admin/site' && method === 'PUT') return route.fulfill({ json: ok(body) })

    state.unhandled.push(`${method} ${path}${url.search}`)
    return route.fulfill({
      status: 501,
      json: { code: 501, msg: `未覆盖的 mock API：${method} ${path}`, data: null }
    })
  })
  return state
}

function observeRuntime(page) {
  const failures = []
  page.on('console', (message) => {
    if (message.type() === 'error') failures.push(`console: ${message.text()}`)
  })
  page.on('pageerror', (error) => failures.push(`page: ${error.message}`))
  page.on('dialog', (dialog) => {
    failures.push(`native dialog: ${dialog.type()} ${dialog.message()}`)
    dialog.dismiss().catch(() => {})
  })
  return failures
}

async function expectNoOverflow(page) {
  await expect
    .poll(() => page.evaluate(() => document.documentElement.scrollWidth <= innerWidth + 1))
    .toBe(true)
}

function expectCleanRun(failures, mocks) {
  expect(failures).toEqual([])
  expect(mocks.unhandled).toEqual([])
}

async function canvasRegionHasInk(page, { x, y, width = 180, height = 180 }) {
  return page.locator('canvas.pointer-effects-canvas').evaluate(
    (canvas, region) => {
      const scale = canvas.width / Math.max(1, canvas.clientWidth)
      const startX = Math.max(0, Math.floor(region.x * scale))
      const startY = Math.max(0, Math.floor(region.y * scale))
      const sampleWidth = Math.max(
        1,
        Math.min(canvas.width - startX, Math.floor(region.width * scale))
      )
      const sampleHeight = Math.max(
        1,
        Math.min(canvas.height - startY, Math.floor(region.height * scale))
      )
      const data = canvas
        .getContext('2d')
        .getImageData(startX, startY, sampleWidth, sampleHeight).data
      for (let index = 3; index < data.length; index += 4) {
        if (data[index] > 0) return true
      }
      return false
    },
    { x, y, width, height }
  )
}

async function canvasHorizontalInkRun(page, { x, y, width, height }) {
  return page.locator('canvas.pointer-effects-canvas').evaluate(
    (canvas, region) => {
      const scale = canvas.width / Math.max(1, canvas.clientWidth)
      const startX = Math.max(0, Math.floor(region.x * scale))
      const startY = Math.max(0, Math.floor(region.y * scale))
      const sampleWidth = Math.max(
        1,
        Math.min(canvas.width - startX, Math.floor(region.width * scale))
      )
      const sampleHeight = Math.max(
        1,
        Math.min(canvas.height - startY, Math.floor(region.height * scale))
      )
      const data = canvas
        .getContext('2d')
        .getImageData(startX, startY, sampleWidth, sampleHeight).data
      let run = 0
      let longest = 0

      for (let column = 0; column < sampleWidth; column += 1) {
        let hasInk = false
        for (let row = 0; row < sampleHeight; row += 1) {
          if (data[(row * sampleWidth + column) * 4 + 3] > 2) {
            hasInk = true
            break
          }
        }
        run = hasInk ? run + 1 : 0
        longest = Math.max(longest, run)
      }

      return longest / scale
    },
    { x, y, width, height }
  )
}

test('Spops identity and filing footer replace the old browser branding', async ({
  page
}, testInfo) => {
  test.skip(testInfo.project.name !== 'desktop', 'Site identity runs once on desktop')
  const failures = observeRuntime(page)
  const mocks = await mockApi(page)

  await page.goto('/')
  await expect(page).toHaveTitle('Spops')

  const footer = page.getByRole('contentinfo', { name: '站点备案信息' })
  const filing = footer.getByRole('link', { name: '晋ICP备2026007967-1号' })
  await expect(footer).toBeVisible()
  await expect(filing).toHaveAttribute('href', 'https://beian.miit.gov.cn/')
  await expect(filing).toHaveAttribute('target', '_blank')

  const favicon = await page.request.get('/favicon.svg')
  expect(favicon.ok()).toBe(true)
  const faviconSource = await favicon.text()
  expect(faviconSource).toContain('id="spops-mark"')
  expect(faviconSource).not.toContain('#863bff')

  await expectNoOverflow(page)
  expectCleanRun(failures, mocks)
})

test('admin sidebar stays one clipped surface over a high contrast background', async ({
  page
}, testInfo) => {
  test.skip(testInfo.project.name !== 'desktop', 'Sidebar surface runs once on desktop')
  const failures = observeRuntime(page)
  const mocks = await mockApi(page, {
    id: '1',
    username: 'root',
    nickname: '管理者',
    role: 'ADMIN'
  })

  await page.goto('/admin')
  await page.locator('.app-shell').evaluate((shell) => {
    shell.classList.add('has-site-background')
    shell.style.setProperty(
      '--site-background-image',
      'linear-gradient(#eff7ff 0 32%, #85a9d6 32% 36%, #fff2d6 36% 68%, #89b5df 68%)'
    )
  })

  const sidebar = page.locator('.admin-sidebar')
  await expect(sidebar).toHaveCSS('overflow', 'hidden')
  const sidebarBackground = await sidebar.evaluate(
    (element) => getComputedStyle(element).backgroundImage
  )
  expect(sidebarBackground).toContain('linear-gradient')
  await expect(page.getByRole('contentinfo', { name: '站点备案信息' })).toHaveCount(0)
  await expectNoOverflow(page)
  expectCleanRun(failures, mocks)
})

test('home, search and responsive layout use real data contracts', async ({ page }) => {
  const failures = observeRuntime(page)
  const mocks = await mockApi(page)
  await page.goto('/')
  await expect(page.getByRole('heading', { name: '可验证的文章' })).toBeVisible()
  const searchTrigger = page.getByRole('button', { name: '搜索' })
  await searchTrigger.click()
  const searchDialog = page.getByRole('dialog', { name: '搜索文章' })
  await expect(searchDialog).toBeVisible()
  await expect(searchDialog).toHaveClass(/app-dialog-panel/)
  const searchInput = page.getByPlaceholder('搜索标题、正文或标签')
  await expect(searchInput).toBeFocused()
  await searchInput.fill('布局')
  await expect(searchDialog.getByRole('link', { name: '可验证的文章' })).toBeVisible()
  await page.keyboard.press('Escape')
  await expect(searchDialog).toBeHidden()
  await expect(searchTrigger).toBeFocused()
  await expectNoOverflow(page)
  expectCleanRun(failures, mocks)
})

test('theme transition and legacy links remain functional', async ({ page }) => {
  const failures = observeRuntime(page)
  await page.emulateMedia({ reducedMotion: 'reduce' })
  await page.addInitScript(() => {
    window.__themeAnimationCalls = []
    const animate = Element.prototype.animate
    Element.prototype.animate = function instrumentThemeAnimation(keyframes, options) {
      if (options?.pseudoElement === '::view-transition-new(root)') {
        window.__themeAnimationCalls.push({
          pseudoElement: options.pseudoElement,
          duration: options.duration,
          clipPath: Array.isArray(keyframes?.clipPath) ? [...keyframes.clipPath] : []
        })
      }
      return animate.call(this, keyframes, options)
    }
  })
  const mocks = await mockApi(page)
  await page.goto('/')
  const appearanceTrigger = page.getByRole('button', { name: '外观' })
  await appearanceTrigger.click()
  await expect(page.getByRole('dialog', { name: '外观设置' })).toBeVisible()
  await page.getByRole('button', { name: '暗色' }).click()
  await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark')
  await expect.poll(() => page.evaluate(() => window.__themeAnimationCalls.length)).toBe(1)
  expect(await page.evaluate(() => window.__themeAnimationCalls[0])).toMatchObject({
    pseudoElement: '::view-transition-new(root)',
    duration: 520,
    clipPath: expect.arrayContaining([expect.stringMatching(/^circle\(0 at /)])
  })
  await page.getByRole('button', { name: '亮色' }).click()
  await expect(page.locator('html')).toHaveAttribute('data-theme', 'light')
  await expect.poll(() => page.evaluate(() => window.__themeAnimationCalls.length)).toBe(2)
  await page.keyboard.press('Escape')
  await expect(page.getByRole('dialog', { name: '外观设置' })).toBeHidden()
  await expect(appearanceTrigger).toBeFocused()
  await page.goto('/#/post?id=2081309851788009474')
  await expect(page).toHaveURL(/\/post\/2081309851788009474$/)
  expectCleanRun(failures, mocks)
})

test('fluid pointer canvas draws a ribbon and click ripple without blocking controls', async ({
  page
}, testInfo) => {
  test.skip(testInfo.project.name !== 'desktop', 'Pointer rendering runs once on desktop')
  const failures = observeRuntime(page)
  const mocks = await mockApi(page)
  await page.goto('/')
  await page.bringToFront()
  await expect.poll(() => page.evaluate(() => document.hasFocus())).toBe(true)

  const canvas = page.locator('canvas.pointer-effects-canvas')
  await expect(canvas).toHaveCount(1)
  await expect(page.locator('.pointer-trail i')).toHaveCount(0)
  await expect(canvas).toHaveCSS('pointer-events', 'none')

  const trailRegion = { x: 170, y: 150, width: 430, height: 330 }
  await page.mouse.move(220, 210)
  await page.mouse.move(520, 390, { steps: 12 })
  await expect
    .poll(() => canvasHorizontalInkRun(page, trailRegion), {
      timeout: 1500,
      intervals: [10, 20, 40, 80]
    })
    .toBeGreaterThan(40)
  await expect.poll(() => canvasRegionHasInk(page, trailRegion), { timeout: 1500 }).toBe(false)

  const quietRegion = { x: 0, y: 300, width: 190, height: 390 }
  await page.mouse.move(90, 600)
  await expect.poll(() => canvasRegionHasInk(page, quietRegion)).toBe(true)
  await expect.poll(() => canvasRegionHasInk(page, quietRegion), { timeout: 1500 }).toBe(false)
  await page.mouse.click(90, 600, { button: 'right' })
  await page.waitForTimeout(120)
  expect(await canvasRegionHasInk(page, quietRegion)).toBe(false)

  await page.mouse.down()
  await page.mouse.move(130, 640, { steps: 4 })
  await expect.poll(() => canvasRegionHasInk(page, quietRegion)).toBe(true)
  await expect.poll(() => canvasRegionHasInk(page, quietRegion), { timeout: 1500 }).toBe(false)
  await page.mouse.up()
  await page.waitForTimeout(120)
  expect(await canvasRegionHasInk(page, quietRegion)).toBe(false)

  await page.evaluate(() => {
    const target = document.elementFromPoint(130, 640) || document.body
    const pointer = {
      bubbles: true,
      pointerId: 43,
      pointerType: 'mouse',
      isPrimary: true,
      clientX: 130,
      clientY: 640
    }
    target.dispatchEvent(new PointerEvent('pointerdown', { ...pointer, button: 0, buttons: 1 }))
    target.dispatchEvent(new PointerEvent('pointerdown', { ...pointer, button: 2, buttons: 3 }))
    target.dispatchEvent(new PointerEvent('pointerup', { ...pointer, button: 2, buttons: 1 }))
    target.dispatchEvent(new PointerEvent('pointerup', { ...pointer, button: 0, buttons: 0 }))
  })
  await page.waitForTimeout(120)
  expect(await canvasRegionHasInk(page, quietRegion)).toBe(false)

  await page.mouse.move(90, 420)
  await expect.poll(() => canvasRegionHasInk(page, quietRegion)).toBe(true)
  await expect.poll(() => canvasRegionHasInk(page, quietRegion), { timeout: 1500 }).toBe(false)
  await page.mouse.click(90, 420)
  await expect.poll(() => canvasRegionHasInk(page, quietRegion)).toBe(true)
  await expect.poll(() => canvasRegionHasInk(page, quietRegion), { timeout: 1200 }).toBe(false)

  const appearanceTrigger = page.getByRole('button', { name: '外观' })
  await appearanceTrigger.click()
  await expect(page.getByRole('dialog', { name: '外观设置' })).toBeVisible()
  await page.keyboard.press('Escape')
  await page.getByRole('link', { name: '可验证的文章' }).first().click()
  await expect(page).toHaveURL(/\/post\/2081309851788009474$/)
  await page.waitForTimeout(60)
  expect(await canvasRegionHasInk(page, { x: 0, y: 0, width: 1440, height: 1000 })).toBe(false)
  expectCleanRun(failures, mocks)
})

test('fluid pointer canvas stays disabled when the system requests reduced motion', async ({
  page
}, testInfo) => {
  test.skip(testInfo.project.name !== 'desktop', 'Reduced pointer rendering runs once on desktop')
  const failures = observeRuntime(page)
  await page.emulateMedia({ reducedMotion: 'reduce' })
  const mocks = await mockApi(page)
  await page.goto('/')

  const canvas = page.locator('canvas.pointer-effects-canvas')
  await expect(canvas).toHaveCount(1)
  await expect(canvas).toHaveCSS('display', 'none')
  await page.mouse.move(260, 240)
  await page.mouse.move(520, 390, { steps: 8 })
  await page.mouse.click(520, 390)
  await page.waitForTimeout(120)
  expect(
    await canvas.evaluate((element) => {
      const data = element.getContext('2d').getImageData(0, 0, element.width, element.height).data
      for (let index = 3; index < data.length; index += 4) {
        if (data[index] > 0) return true
      }
      return false
    })
  ).toBe(false)
  expectCleanRun(failures, mocks)
})

test.describe('touch pointer environment', () => {
  test.use({ hasTouch: true })

  test('fluid pointer canvas stays disabled on touch devices', async ({ page }, testInfo) => {
    test.skip(testInfo.project.name !== 'mobile', 'Touch rendering is verified on mobile')
    const failures = observeRuntime(page)
    const mocks = await mockApi(page)
    await page.goto('/')

    const canvas = page.locator('canvas.pointer-effects-canvas')
    await expect(canvas).toHaveCount(1)
    await expect(canvas).toHaveCSS('display', 'none')
    expect(
      await page.evaluate(
        () => matchMedia('(pointer: coarse)').matches || matchMedia('(hover: none)').matches
      )
    ).toBe(true)
    await page.touchscreen.tap(180, 260)
    await page.waitForTimeout(120)
    expect(
      await canvas.evaluate((element) => {
        const data = element.getContext('2d').getImageData(0, 0, element.width, element.height).data
        for (let index = 3; index < data.length; index += 4) {
          if (data[index] > 0) return true
        }
        return false
      })
    ).toBe(false)
    expectCleanRun(failures, mocks)
  })
})

test('reader exposes navigation, sharing and comment surfaces', async ({ page }) => {
  const failures = observeRuntime(page)
  const mocks = await mockApi(page)
  mocks.posts[0].content += '\n\n![灯箱测试图](/images/emojis/喜欢.jpg)'
  await page.goto('/post/2081309851788009474')
  if (page.viewportSize().width > 600)
    await expect(page.getByRole('navigation', { name: '文章目录' })).toContainText('小节')
  else await expect(page.getByRole('heading', { name: '小节' })).toBeVisible()
  await expect(page.getByRole('button', { name: '复制' })).toBeVisible()
  const shareTrigger = page.getByRole('button', { name: '分享' })
  await shareTrigger.click()
  const shareDialog = page.getByRole('dialog', { name: '分享' })
  await expect(shareDialog).toHaveClass(/app-dialog-panel/)
  await expect(page.getByRole('img', { name: '当前文章二维码' })).toBeVisible()
  await page.keyboard.press('Escape')
  await expect(shareDialog).toBeHidden()
  await expect(shareTrigger).toBeFocused()

  await page.getByRole('img', { name: '灯箱测试图' }).click()
  const imageDialog = page.getByRole('dialog', { name: '图片预览' })
  await expect(imageDialog).toHaveClass(/app-dialog-panel/)
  await expect(imageDialog.getByRole('img', { name: '文章图片预览' })).toBeVisible()
  await page.keyboard.press('Escape')
  await expect(imageDialog).toBeHidden()
  await expect(page.getByRole('heading', { name: '评论' })).toBeVisible()
  expectCleanRun(failures, mocks)
})

test('searching from one article opens the newly selected article in the same SPA', async ({
  page
}, testInfo) => {
  test.skip(testInfo.project.name !== 'desktop', 'Same-route navigation runs once on desktop')
  const failures = observeRuntime(page)
  const mocks = await mockApi(page)

  await page.goto('/post/2081309851788009474')
  await expect(page.getByRole('heading', { name: '可验证的文章' })).toBeVisible()
  await page.getByRole('button', { name: '搜索' }).click()
  await page.getByPlaceholder('搜索标题、正文或标签').fill('八月')
  await page
    .getByRole('dialog', { name: '搜索文章' })
    .getByRole('link', {
      name: '八月日历文章'
    })
    .click()

  await expect(page).toHaveURL(/\/post\/2081309851788009475$/)
  await expect(page.getByRole('heading', { name: '八月日历文章' })).toBeVisible()
  await expect(page.getByRole('heading', { name: '可验证的文章' })).toHaveCount(0)
  expect(
    mocks.calls.some((call) => call.path === '/api/interactions/POST/2081309851788009475')
  ).toBe(true)
  expectCleanRun(failures, mocks)
})

test('public interaction roots remain reachable beyond the first server page', async ({
  page
}, testInfo) => {
  test.skip(testInfo.project.name !== 'desktop', 'Public thread paging runs on desktop')
  const failures = observeRuntime(page)
  const mocks = await mockApi(page)
  for (let index = 2; index <= 21; index += 1) {
    mocks.interactions.push({
      ...mocks.interactions[0],
      id: `comment-${index}`,
      content: `分页评论 ${index}`,
      pinned: false,
      pinOrder: null,
      replyCount: 0,
      replies: [],
      createTime: `2026-07-${String(27 - index).padStart(2, '0')}T14:00:00`
    })
  }
  mocks.interactions[0].replies = [
    {
      id: 'reply-visible',
      rootId: 'comment-1',
      replyToId: 'comment-1',
      replyToNickname: '留言者',
      content: '展开后保持可见的回复',
      nickname: '回复者',
      status: 'PUBLISHED',
      likeCount: 0,
      liked: false,
      createTime: '2026-07-26T14:30:00'
    }
  ]

  await page.goto('/post/2081309851788009474')
  await expect(page.locator('.thread-list > .interaction-item')).toHaveCount(20)
  await expect(page.getByText('分页评论 21', { exact: true })).toHaveCount(0)
  await page.getByRole('button', { name: '1 条回复' }).click()
  await expect(page.getByText('展开后保持可见的回复', { exact: true })).toBeVisible()

  await page.getByRole('button', { name: '加载更多评论' }).click()

  await expect(page.locator('.thread-list > .interaction-item')).toHaveCount(21)
  await expect(page.getByText('分页评论 21', { exact: true })).toBeVisible()
  await expect(page.getByText('展开后保持可见的回复', { exact: true })).toBeVisible()
  await expect(page.getByRole('button', { name: '加载更多评论' })).toHaveCount(0)
  expect(
    mocks.calls.some(
      (call) =>
        call.path === '/api/interactions/POST/2081309851788009474' &&
        call.query.page === '2' &&
        call.query.size === '20'
    )
  ).toBe(true)
  expectCleanRun(failures, mocks)
})

test('editor flushes the last local change before navigation', async ({ page }, testInfo) => {
  test.skip(testInfo.project.name !== 'desktop', 'Draft lifecycle runs once on desktop')
  const failures = observeRuntime(page)
  const mocks = await mockApi(page, { id: '1', username: 'root', role: 'ADMIN' })
  await page.goto('/write')
  await page.getByPlaceholder('标题（可选）').fill('不会丢失的最后一次修改')
  await page.getByRole('textbox', { name: '文章正文' }).fill('最后一行')
  await page.goto('/')
  const draft = await page.evaluate(() => JSON.parse(localStorage.getItem('postDraft:new')))
  expect(draft.title).toBe('不会丢失的最后一次修改')
  expect(draft.content).toBe('最后一行')
  expectCleanRun(failures, mocks)
})

test('archived articles remain editable without silently leaving the archive', async ({
  page
}, testInfo) => {
  test.skip(testInfo.project.name !== 'desktop', 'Archived editor lifecycle runs once on desktop')
  const failures = observeRuntime(page)
  const mocks = await mockApi(page, { id: '1', username: 'root', role: 'ADMIN' })
  mocks.posts[0].status = 'ARCHIVED'
  mocks.posts[0].archivedTime = '2026-07-26T16:00:00'

  await page.goto('/write/2081309851788009474')
  await expect(page.getByRole('button', { name: '保存归档' })).toBeVisible()
  await expect(page.getByRole('button', { name: '恢复发布' })).toBeVisible()
  await page.getByRole('textbox', { name: '文章正文' }).fill('归档后继续修订')
  await page.getByRole('button', { name: '保存归档' }).click()
  await expect(page.getByRole('status')).toHaveText('归档内容已保存')

  const archivedSave = mocks.calls.find(
    (call) =>
      call.path === '/api/posts' && call.method === 'POST' && call.body?.status === 'ARCHIVED'
  )
  expect(archivedSave.body.content).toBe('归档后继续修订')

  await page.getByRole('button', { name: '恢复发布' }).click()
  await expect(page.getByRole('status')).toHaveText('文章已恢复发布')
  expect(
    mocks.calls.some(
      (call) =>
        call.path === '/api/posts' && call.method === 'POST' && call.body?.status === 'PUBLISHED'
    )
  ).toBe(true)
  expectCleanRun(failures, mocks)
})

test('published article edits preserve the published lifecycle state', async ({
  page
}, testInfo) => {
  test.skip(testInfo.project.name !== 'desktop', 'Published editor lifecycle runs once on desktop')
  const failures = observeRuntime(page)
  const mocks = await mockApi(page, { id: '1', username: 'root', role: 'ADMIN' })

  await page.goto('/write/2081309851788009474')
  await expect(page.getByRole('button', { name: '保存修改' })).toBeVisible()
  await expect(page.getByRole('button', { name: '草稿' })).toHaveCount(0)
  await page.getByRole('textbox', { name: '文章正文' }).fill('发布后继续修订')
  await page.getByRole('button', { name: '保存修改' }).click()
  await expect(page.getByRole('status')).toHaveText('修改已保存')

  const saved = mocks.calls.find(
    (call) =>
      call.path === '/api/posts' &&
      call.method === 'POST' &&
      call.body?.content === '发布后继续修订'
  )
  expect(saved.body.status).toBe('PUBLISHED')
  expectCleanRun(failures, mocks)
})

test('mobile editor keeps lifecycle-preserving save actions available', async ({
  page
}, testInfo) => {
  test.skip(testInfo.project.name !== 'mobile', 'Mobile editor regression runs once on mobile')
  const failures = observeRuntime(page)
  const mocks = await mockApi(page, { id: '1', username: 'root', role: 'ADMIN' })

  await page.goto('/write/2081309851788009474')
  await expect(page.getByRole('button', { name: '保存修改' })).toBeVisible()
  await page.getByRole('textbox', { name: '文章正文' }).fill('手机端发布后修订')
  await page.getByRole('button', { name: '保存修改' }).click()
  await expect(page.getByRole('status')).toHaveText('修改已保存')
  expect(
    mocks.calls.some(
      (call) =>
        call.path === '/api/posts' &&
        call.method === 'POST' &&
        call.body?.content === '手机端发布后修订' &&
        call.body?.status === 'PUBLISHED'
    )
  ).toBe(true)

  mocks.posts[0].status = 'ARCHIVED'
  await page.goto('/write/2081309851788009474?state=archived')
  await expect(page.getByRole('button', { name: '保存归档' })).toBeVisible()
  await expect(page.getByRole('button', { name: '恢复发布' })).toBeVisible()
  await expectNoOverflow(page)
  await page.getByRole('button', { name: '保存归档' }).click()
  await expect(page.getByRole('status')).toHaveText('归档内容已保存')
  expect(
    mocks.calls.some(
      (call) =>
        call.path === '/api/posts' && call.method === 'POST' && call.body?.status === 'ARCHIVED'
    )
  ).toBe(true)
  expectCleanRun(failures, mocks)
})

test('calendar redirect opens a switchable month dialog with dated article links', async ({
  page
}, testInfo) => {
  test.skip(testInfo.project.name !== 'desktop', 'Calendar acceptance runs on desktop')
  const failures = observeRuntime(page)
  const mocks = await mockApi(page)

  await page.goto('/calendar')
  await expect(page).toHaveURL(/\/?\?calendar=open$/)
  const dialog = page.getByRole('dialog', { name: '日历' })
  await expect(dialog).toBeVisible()
  await expect(dialog.locator('.calendar-expanded-nav strong')).toHaveText('2026 · 7')

  await dialog.getByRole('button', { name: '2026-07-26，2 篇文章' }).click()
  await expect(dialog.getByRole('link', { name: '可验证的文章' })).toBeVisible()

  await dialog.getByRole('button', { name: '下个月' }).click()
  await expect(dialog.locator('.calendar-expanded-nav strong')).toHaveText('2026 · 8')
  await expect(dialog.getByRole('link', { name: '八月日历文章' })).toBeVisible()

  expect(
    mocks.calls.some((call) => call.path === '/api/content/calendar' && call.query.month === '8')
  ).toBe(true)
  expectCleanRun(failures, mocks)
})

test('compact calendar keeps empty dates inert and opens nearby article links', async ({
  page
}) => {
  const failures = observeRuntime(page)
  const mocks = await mockApi(page)

  await page.goto('/')
  const calendar = page.locator('.calendar-widget')
  await expect(calendar.locator('.calendar-widget-controls strong')).toHaveText('2026 · 7')

  const emptyDate = calendar.getByRole('button', { name: '2026-07-25', exact: true })
  await expect(emptyDate).toBeDisabled()
  await emptyDate.dispatchEvent('click')
  await expect(calendar.locator('.calendar-compact-popover')).toHaveCount(0)
  await expect(page.getByRole('dialog', { name: '日历' })).toHaveCount(0)

  await calendar.getByRole('button', { name: '2026-07-26，2 篇文章' }).click()
  const nearby = calendar.getByRole('region', { name: '2026-07-26 文章' })
  await expect(nearby).toBeVisible()
  await expect(nearby.getByRole('link')).toHaveText(['可验证的文章', '同日第二篇'])
  await expect(page.getByRole('dialog', { name: '日历' })).toHaveCount(0)

  await calendar.getByRole('button', { name: '下个月' }).click()
  await expect(calendar.locator('.calendar-widget-controls strong')).toHaveText('2026 · 8')
  await calendar.getByRole('button', { name: '回到本月' }).click()
  await expect(calendar.locator('.calendar-widget-controls strong')).toHaveText('2026 · 7')

  await calendar.getByRole('button', { name: '放大日历' }).click()
  const dialog = page.getByRole('dialog', { name: '日历' })
  await expect(dialog).toBeVisible()
  await dialog.getByRole('button', { name: '2026-07-26，2 篇文章' }).click()
  await expect(dialog.getByRole('link')).toHaveText(['可验证的文章', '同日第二篇'])
  await dialog.getByRole('link', { name: '可验证的文章' }).click()
  await expect(page).toHaveURL(/\/post\/2081309851788009474$/)
  await expectNoOverflow(page)
  expectCleanRun(failures, mocks)
})

test('image-only untitled posts keep a neutral clickable label', async ({ page }, testInfo) => {
  test.skip(testInfo.project.name !== 'desktop', 'Untitled media label runs once on desktop')
  const failures = observeRuntime(page)
  const mocks = await mockApi(page)
  mocks.posts[0].title = ''
  mocks.posts[0].content = '![只有图片](/images/emojis/喜欢.jpg)'

  await page.goto('/')
  await expect(page.getByRole('heading', { name: '无题', exact: true })).toBeVisible()
  const calendar = page.locator('.calendar-widget')
  await calendar.getByRole('button', { name: '2026-07-26，2 篇文章' }).click()
  await expect(
    calendar.getByRole('region', { name: '2026-07-26 文章' }).getByRole('link', {
      name: '无题',
      exact: true
    })
  ).toBeVisible()

  await page.getByRole('button', { name: '搜索' }).click()
  const search = page.getByRole('dialog', { name: '搜索文章' })
  await search.getByPlaceholder('搜索标题、正文或标签').fill('图片')
  await expect(search.getByRole('link', { name: '无题', exact: true })).toBeVisible()
  expectCleanRun(failures, mocks)
})

test('article directory loads and searches beyond the first hundred posts', async ({
  page
}, testInfo) => {
  test.skip(testInfo.project.name !== 'desktop', 'Large article directory runs once on desktop')
  const failures = observeRuntime(page)
  const mocks = await mockApi(page)
  mocks.posts = Array.from({ length: 101 }, (_, index) => ({
    ...posts[0],
    id: `directory-${index + 1}`,
    title: `目录文章第 ${index + 1} 篇`,
    createTime: `2026-07-${String((index % 26) + 1).padStart(2, '0')}T12:00:00`
  }))

  await page.goto('/articles')
  await expect(page.locator('.articles-grid .post-card')).toHaveCount(101)
  expect(
    mocks.calls.some(
      (call) => call.path === '/api/posts' && call.query.page === '2' && call.query.size === '100'
    )
  ).toBe(true)

  await page.getByPlaceholder('搜索正文或标签').fill('第 101 篇')
  await expect(page.locator('.articles-grid .post-card')).toHaveCount(1)
  await expect(page.getByRole('heading', { name: '目录文章第 101 篇' })).toBeVisible()
  await expectNoOverflow(page)
  expectCleanRun(failures, mocks)
})

test('tag navigation keeps the latest selection when an older request finishes last', async ({
  page
}, testInfo) => {
  test.skip(testInfo.project.name !== 'desktop', 'Tag request race runs once on desktop')
  const failures = observeRuntime(page)
  const mocks = await mockApi(page)
  let releaseLayout
  let markLayoutStarted
  const layoutStarted = new Promise((resolve) => {
    markLayoutStarted = resolve
  })

  await page.route(/^https?:\/\/[^/]+\/api\/tags\/layout\/posts$/, async (route) => {
    markLayoutStarted()
    await new Promise((resolve) => {
      releaseLayout = resolve
    })
    await route.fulfill({ json: ok([mocks.posts[0]]) })
  })
  await page.route(/^https?:\/\/[^/]+\/api\/tags\/image\/posts$/, async (route) => {
    await route.fulfill({ json: ok([mocks.posts[1]]) })
  })

  await page.goto('/tags/layout')
  await layoutStarted
  await page.getByRole('link', { name: /影像/ }).click()
  await expect(page).toHaveURL(/\/tags\/image$/)
  await expect(page.getByRole('heading', { name: '影像', exact: true })).toBeVisible()
  await expect(page.getByRole('heading', { name: '八月日历文章' })).toBeVisible()

  releaseLayout()
  await expect(page.getByRole('heading', { name: '八月日历文章' })).toBeVisible()
  await expect(page.getByRole('heading', { name: '可验证的文章' })).toHaveCount(0)
  expectCleanRun(failures, mocks)
})

test('archive years and article rows do not overlap or create horizontal scrolling', async ({
  page
}, testInfo) => {
  test.skip(testInfo.project.name !== 'desktop', 'Archive geometry runs on desktop')
  const failures = observeRuntime(page)
  const mocks = await mockApi(page)

  await page.goto('/archive')
  await expect(page.getByRole('heading', { name: '2026' })).toBeVisible()
  await expect(page.getByRole('heading', { name: '2025' })).toBeVisible()
  const sections = page.locator('.archive section')
  await expect(sections).toHaveCount(2)
  const geometry = await sections.evaluateAll((nodes) =>
    nodes.map((section) => {
      const heading = section.querySelector('h2').getBoundingClientRect()
      const list = section.querySelector('.archive-year-posts').getBoundingClientRect()
      const overlaps =
        heading.left < list.right &&
        heading.right > list.left &&
        heading.top < list.bottom &&
        heading.bottom > list.top
      return { overlaps, headingRight: heading.right, listLeft: list.left }
    })
  )
  expect(geometry.every((item) => !item.overlaps && item.headingRight <= item.listLeft + 1)).toBe(
    true
  )
  await expectNoOverflow(page)
  expectCleanRun(failures, mocks)
})

test('admin overview renders SVG trends and archives through the shared dialog', async ({
  page
}, testInfo) => {
  test.skip(testInfo.project.name !== 'desktop', 'Admin acceptance runs on desktop')
  const failures = observeRuntime(page)
  const mocks = await mockApi(page, {
    id: '1',
    username: 'root',
    nickname: '管理者',
    role: 'ADMIN'
  })

  await page.goto('/admin')
  const adminAppearance = page.getByRole('button', { name: '外观' })
  await adminAppearance.click()
  await expect(page.getByRole('dialog', { name: '外观设置' })).toBeVisible()
  await page.keyboard.press('Escape')
  await expect(page.getByRole('dialog', { name: '外观设置' })).toBeHidden()
  await expect(adminAppearance).toBeFocused()

  const trend = page.getByRole('img', { name: '最近 14 天浏览、互动和文章发布趋势' })
  await expect(trend).toBeVisible()
  await expect(trend.locator('svg path.line')).toHaveCount(3)
  expect(
    await trend
      .locator('svg path.line')
      .evaluateAll((paths) => paths.every((path) => Boolean(path.getAttribute('d'))))
  ).toBe(true)

  const navigation = page.getByRole('navigation', { name: '管理功能' })
  await navigation.getByRole('button', { name: '文章', exact: true }).click()
  await expect(page.getByRole('heading', { name: '文章', exact: true })).toBeVisible()
  const contentRow = page.locator('.content-row').first()
  await contentRow.getByRole('button', { name: /^更多操作：/ }).click()
  await contentRow.getByRole('button', { name: '收纳归档', exact: true }).click()

  const archiveDialog = page.getByRole('dialog', { name: '收纳归档' })
  await expect(archiveDialog).toBeVisible()
  await expect(archiveDialog).toContainText('确定收纳归档这篇文章')
  await archiveDialog.getByRole('button', { name: '收纳归档' }).click()
  await expect(archiveDialog).toBeHidden()
  await expect(page.getByRole('status')).toHaveText('文章已归档')

  expect(
    mocks.calls.some(
      (call) =>
        call.method === 'PUT' &&
        /\/api\/posts\/[^/]+\/status/.test(call.path) &&
        call.body?.status === 'ARCHIVED'
    )
  ).toBe(true)
  expectCleanRun(failures, mocks)
})

test('admin article and media lists keep the newest overlapping response', async ({
  page
}, testInfo) => {
  test.skip(testInfo.project.name !== 'desktop', 'List race acceptance runs on desktop')
  const failures = observeRuntime(page)
  const mocks = await mockApi(page, {
    id: '1',
    username: 'root',
    nickname: '管理者',
    role: 'ADMIN'
  })
  let releaseOldPosts
  let releaseOldAssets

  await page.route(/^https?:\/\/[^/]+\/api\/posts\/admin(?:\?|$)/, async (route) => {
    const search = new URL(route.request().url()).searchParams.get('keyword')
    if (search !== '旧文章请求' && search !== '新文章请求') return route.fallback()
    if (search === '旧文章请求') {
      await new Promise((resolve) => {
        releaseOldPosts = resolve
      })
    }
    const record = {
      ...posts[0],
      id: search === '旧文章请求' ? 'old-post' : 'new-post',
      title: search === '旧文章请求' ? '旧响应文章' : '新响应文章'
    }
    await route.fulfill({ json: ok({ records: [record], current: 1, pages: 1, total: 1 }) })
  })
  await page.route(/^https?:\/\/[^/]+\/api\/admin\/assets(?:\?|$)/, async (route) => {
    const search = new URL(route.request().url()).searchParams.get('keyword')
    if (search !== '旧媒体请求' && search !== '新媒体请求') return route.fallback()
    if (search === '旧媒体请求') {
      await new Promise((resolve) => {
        releaseOldAssets = resolve
      })
    }
    const record = {
      ...mocks.assets[0],
      id: search === '旧媒体请求' ? 'old-asset' : 'new-asset'
    }
    await route.fulfill({ json: ok({ records: [record], current: 1, pages: 1, total: 1 }) })
  })

  await page.goto('/admin?tab=content')
  const search = page.getByRole('textbox', { name: '搜索' })
  const articleTable = page.locator('.admin-table')
  await search.fill('旧文章请求')
  await search.press('Enter')
  await expect.poll(() => Boolean(releaseOldPosts)).toBe(true)
  await expect(articleTable).toHaveAttribute('aria-busy', 'true')
  await search.fill('新文章请求')
  await search.press('Enter')
  await expect(page.getByText('新响应文章', { exact: true })).toBeVisible()
  await expect(articleTable).toHaveAttribute('aria-busy', 'false')
  releaseOldPosts()
  await page.waitForTimeout(100)
  await expect(page.getByText('旧响应文章', { exact: true })).toHaveCount(0)

  await page
    .getByRole('navigation', { name: '管理功能' })
    .getByRole('button', { name: '媒体', exact: true })
    .click()
  const mediaCollection = page.locator('.media-collection')
  await search.fill('旧媒体请求')
  await search.press('Enter')
  await expect.poll(() => Boolean(releaseOldAssets)).toBe(true)
  await expect(mediaCollection).toHaveAttribute('aria-busy', 'true')
  await search.fill('新媒体请求')
  await search.press('Enter')
  await expect(page.getByRole('button', { name: '查看媒体 new-asset' })).toBeVisible()
  await expect(mediaCollection).toHaveAttribute('aria-busy', 'false')
  releaseOldAssets()
  await page.waitForTimeout(100)
  await expect(page.getByRole('button', { name: '查看媒体 old-asset' })).toHaveCount(0)

  expectCleanRun(failures, mocks)
})

test('admin interactions, title dialog, media and site links use their new contracts', async ({
  page
}, testInfo) => {
  test.skip(testInfo.project.name !== 'desktop', 'Admin resource acceptance runs on desktop')
  const failures = observeRuntime(page)
  const mocks = await mockApi(page, {
    id: '1',
    username: 'root',
    nickname: '管理者',
    role: 'ADMIN'
  })
  mocks.assets[0].referenceStatus = 'ORPHANED'
  mocks.assets[0].referenceCount = 0
  await page.goto('/admin')
  const navigation = page.getByRole('navigation', { name: '管理功能' })

  await navigation.getByRole('button', { name: '评论', exact: true }).click()
  await expect(page.getByText('验收评论', { exact: true })).toBeVisible()

  await navigation.getByRole('button', { name: '用户', exact: true }).click()
  const userRow = page.locator('.user-row').filter({ hasText: '验收用户' })
  await expect(userRow).toBeVisible()
  await userRow.getByRole('button', { name: '编辑称号' }).click()
  const titleDialog = page.getByRole('dialog', { name: '用户称号' })
  await expect(titleDialog).toBeVisible()
  await titleDialog.getByPlaceholder('留空可清除称号').fill('验收称号')
  await titleDialog.getByText('金色', { exact: true }).click()
  await expect(titleDialog.getByRole('radio', { name: '金色' })).toBeChecked()
  await titleDialog.getByRole('button', { name: '保存' }).click()
  await expect(titleDialog).toBeHidden()
  await expect(userRow.getByText('验收称号', { exact: true })).toBeVisible()

  await navigation.getByRole('button', { name: '媒体', exact: true }).click()
  await expect(page.getByRole('button', { name: '网格显示' })).toHaveAttribute(
    'aria-pressed',
    'true'
  )
  await expect(page.getByRole('button', { name: '列表显示' })).toHaveAttribute(
    'aria-pressed',
    'false'
  )
  const asset = page.getByRole('button', { name: '查看媒体 asset-1' })
  await expect(asset).toBeVisible()
  await asset.click()
  const mediaDialog = page.getByRole('dialog', { name: '媒体详情' })
  await expect(mediaDialog).toBeVisible()
  await expect(mediaDialog.getByRole('button', { name: '复制原图地址' })).toBeVisible()
  await expect(mediaDialog.getByRole('link', { name: /可验证的文章/ })).toHaveAttribute(
    'href',
    `/write/${posts[0].id}`
  )
  await expect(mediaDialog.getByText('使用中', { exact: true })).toBeVisible()
  await expect(page.locator('.media-card').filter({ has: asset })).toContainText('使用中')
  await mediaDialog.getByRole('button', { name: '关闭' }).click()

  await navigation.getByRole('button', { name: '站点', exact: true }).click()
  const linkRow = page.locator('.link-list article').filter({ hasText: '验收链接' })
  await expect(linkRow).toBeVisible()
  await expect(linkRow.getByRole('link')).toHaveAttribute('href', 'https://example.test/profile')

  for (const path of [
    '/api/admin/interactions',
    '/api/admin/users',
    '/api/admin/assets',
    '/api/admin/site/links'
  ]) {
    expect(mocks.calls.some((call) => call.method === 'GET' && call.path === path)).toBe(true)
  }
  expect(
    mocks.calls.some(
      (call) =>
        call.method === 'PUT' &&
        call.path === '/api/admin/users/2/title' &&
        call.body?.titleName === '验收称号' &&
        call.body?.titleStyle === 'gold'
    )
  ).toBe(true)
  await expectNoOverflow(page)
  expectCleanRun(failures, mocks)
})

test('site background upload fills the address and persists only when saved', async ({
  page
}, testInfo) => {
  test.skip(testInfo.project.name !== 'desktop', 'Site background upload runs once on desktop')
  const failures = observeRuntime(page)
  const mocks = await mockApi(page, {
    id: '1',
    username: 'root',
    nickname: '管理者',
    role: 'ADMIN'
  })

  await page.goto('/admin?tab=site')
  const input = page.getByLabel('选择背景图片')
  await expect(input).toHaveAttribute('tabindex', '-1')
  await input.setInputFiles({
    name: 'background.png',
    mimeType: 'image/png',
    buffer: Buffer.from('image')
  })
  await expect(page.getByLabel('背景地址')).toHaveValue(backgroundPath)
  await expect(page.getByRole('img', { name: '站点背景预览' })).toBeVisible()
  await expect(page.locator('.app-shell')).not.toHaveClass(/has-site-background/)
  expect(mocks.calls.some((call) => call.path === '/api/admin/site' && call.method === 'PUT')).toBe(
    false
  )

  await page.getByRole('button', { name: '保存', exact: true }).click()
  await expect
    .poll(() =>
      mocks.calls.some(
        (call) =>
          call.path === '/api/admin/site' &&
          call.method === 'PUT' &&
          call.body?.bgUrl === backgroundPath
      )
    )
    .toBe(true)
  await expect(page.locator('.app-shell')).toHaveClass(/has-site-background/)
  expectCleanRun(failures, mocks)
})

test('failed site background upload preserves the old address and can retry the same file', async ({
  page
}, testInfo) => {
  test.skip(testInfo.project.name !== 'desktop', 'Site background retry runs once on desktop')
  const failures = observeRuntime(page)
  const mocks = await mockApi(page, {
    id: '1',
    username: 'root',
    nickname: '管理者',
    role: 'ADMIN'
  })
  mocks.failBackgroundUpload = true

  await page.goto('/admin?tab=site')
  const address = page.getByLabel('背景地址')
  const input = page.getByLabel('选择背景图片')
  await address.fill('/uploads/existing.webp')
  const file = {
    name: 'background.png',
    mimeType: 'image/png',
    buffer: Buffer.from('image')
  }
  await input.setInputFiles(file)
  await expect(address).toHaveValue('/uploads/existing.webp')
  await expect(input).toHaveValue('')
  await expect(page.getByRole('status')).toContainText('背景上传失败')
  await expect(page.getByRole('button', { name: '上传背景' })).toBeEnabled()

  mocks.failBackgroundUpload = false
  await input.setInputFiles(file)
  await expect(address).toHaveValue(backgroundPath)
  expectCleanRun(failures, mocks)
})

test('media management filters and batch deletes only selected orphaned assets', async ({
  page
}, testInfo) => {
  test.skip(testInfo.project.name !== 'desktop', 'Media batch lifecycle runs once on desktop')
  const failures = observeRuntime(page)
  const mocks = await mockApi(page, {
    id: '1',
    username: 'root',
    nickname: '管理者',
    role: 'ADMIN'
  })
  mocks.assets.push(
    {
      ...mocks.assets[0],
      id: '9001',
      originalUrl: pixel,
      referenceStatus: 'ORPHANED',
      referenceCount: 0
    },
    {
      ...mocks.assets[0],
      id: '9002',
      originalUrl: pixel,
      referenceStatus: 'ORPHANED',
      referenceCount: 0
    }
  )

  await page.goto('/admin?tab=assets')
  await page.getByLabel('媒体用途').selectOption('ARTICLE')
  await page.getByLabel('选择本页全部未引用媒体').check()
  await page.getByRole('button', { name: '删除所选 2 个' }).click()
  const dialog = page.getByRole('dialog', { name: '批量删除未引用媒体' })
  await expect(dialog).toContainText('将删除所选 2 个文件与媒体记录')
  await dialog.getByRole('button', { name: '删除' }).click()
  await expect(page.getByRole('status')).toHaveText('已删除 2 个未引用媒体')
  await expect(page.getByRole('button', { name: '查看媒体 9001' })).toHaveCount(0)

  const batchCall = mocks.calls.find(
    (call) => call.path === '/api/admin/assets/batch' && call.method === 'DELETE'
  )
  expect(batchCall.body.ids.sort()).toEqual(['9001', '9002'])
  expectCleanRun(failures, mocks)
})

test('admin moderation expands reply threads, replies without captcha, filters and batch deletes', async ({
  page
}, testInfo) => {
  test.skip(testInfo.project.name === 'tablet', 'Moderation acceptance runs on desktop and mobile')
  const failures = observeRuntime(page)
  const mocks = await mockApi(page, {
    id: '1',
    username: 'root',
    nickname: '管理者',
    role: 'ADMIN'
  })

  await page.goto('/admin?tab=interactions')
  const root = page.locator('.moderation-item').filter({ hasText: '验收评论' })
  await expect(root).toContainText('可验证的文章')
  await expect(root.getByText('折叠的验收回复', { exact: true })).toBeHidden()
  await root.getByRole('button', { name: '展开 1 条回复' }).click()
  await expect(root.getByText('折叠的验收回复', { exact: true })).toBeVisible()
  await expect(root).toContainText('回复 留言者')

  await root.getByRole('button', { name: '回复留言者' }).click()
  const replyDialog = page.getByRole('dialog', { name: '管理员回复' })
  await replyDialog.getByRole('button', { name: '插入表情' }).click()
  await replyDialog.getByRole('button', { name: '喜欢' }).click()
  await replyDialog.getByLabel('回复内容').fill('[sticker:喜欢] 管理员回复')
  await replyDialog.getByRole('button', { name: '发送回复' }).click()
  await expect(replyDialog).toBeHidden()

  await page.getByLabel('评论内容类型').selectOption('STICKER')
  await expect
    .poll(() =>
      mocks.calls.some(
        (call) => call.path === '/api/admin/interactions' && call.query.contentType === 'STICKER'
      )
    )
    .toBe(true)

  await root.getByRole('checkbox', { name: '选择 留言者 的评论' }).check()
  await page.getByRole('button', { name: '批量删除' }).click()
  const deleteDialog = page.getByRole('dialog', { name: '批量删除评论' })
  await deleteDialog.getByRole('button', { name: '删除 1 条' }).click()
  await expect(page.getByText('没有符合条件的评论')).toBeVisible()

  const replyCall = mocks.calls.find(
    (call) => call.path === '/api/interactions' && call.method === 'POST'
  )
  expect(replyCall.body).toEqual({
    targetType: 'POST',
    targetId: posts[0].id,
    replyToId: 'comment-1',
    content: '[sticker:喜欢] 管理员回复'
  })
  expect(
    mocks.calls.some(
      (call) => call.path === '/api/admin/interactions/batch-delete' && call.method === 'POST'
    )
  ).toBe(true)
  await expectNoOverflow(page)
  expectCleanRun(failures, mocks)
})

test('site link mutations update the public about page without reloading the SPA', async ({
  page
}, testInfo) => {
  test.skip(testInfo.project.name !== 'desktop', 'Site link lifecycle runs once on desktop')
  const failures = observeRuntime(page)
  const mocks = await mockApi(page, {
    id: '1',
    username: 'root',
    nickname: '管理者',
    role: 'ADMIN'
  })

  const openSitePanel = async () => {
    if (!/\/admin/.test(new URL(page.url()).pathname))
      await page.getByRole('link', { name: '账户' }).click()
    await page
      .getByRole('navigation', { name: '管理功能' })
      .getByRole('button', { name: '站点', exact: true })
      .click()
  }
  const openAbout = async () => {
    await page.getByRole('link', { name: '返回博客首页' }).click()
    await page
      .getByRole('navigation', { name: '主导航' })
      .getByRole('link', { name: '关于', exact: true })
      .click()
  }

  await page.goto('/admin')
  await openSitePanel()
  await page.getByRole('button', { name: '添加链接' }).click()
  const createDialog = page.getByRole('dialog', { name: '添加链接' })
  await createDialog.getByLabel('名称').fill('即时链接')
  await createDialog.getByLabel('地址').fill('https://example.test/live')
  await createDialog.getByRole('button', { name: '保存' }).click()

  const createdRow = page.locator('.link-list article').filter({ hasText: '即时链接' })
  await createdRow.getByRole('button', { name: '上移' }).click()
  await openAbout()
  await expect(page.locator('.social-row a')).toHaveText(['即时链接', '验收链接'])

  await openSitePanel()
  const editableRow = page.locator('.link-list article').filter({ hasText: '即时链接' })
  await editableRow.getByRole('button', { name: '编辑链接' }).click()
  const editDialog = page.getByRole('dialog', { name: '编辑链接' })
  await editDialog.getByLabel('名称').fill('已更新链接')
  await editDialog.getByRole('button', { name: '保存' }).click()
  await openAbout()
  await expect(page.getByRole('link', { name: '已更新链接' })).toBeVisible()

  await openSitePanel()
  const toggleRow = page.locator('.link-list article').filter({ hasText: '已更新链接' })
  await toggleRow.getByRole('checkbox', { name: '已更新链接是否显示' }).uncheck()
  await openAbout()
  await expect(page.getByRole('link', { name: '已更新链接' })).toHaveCount(0)

  await openSitePanel()
  const deleteRow = page.locator('.link-list article').filter({ hasText: '已更新链接' })
  await deleteRow.getByRole('button', { name: '删除链接' }).click()
  await page.getByRole('dialog', { name: '删除链接' }).getByRole('button', { name: '删除' }).click()
  await openAbout()
  await expect(page.getByRole('link', { name: '已更新链接' })).toHaveCount(0)

  expectCleanRun(failures, mocks)
})

test('mobile admin keeps home, profile and logout actions visible without overflow', async ({
  page
}, testInfo) => {
  test.skip(testInfo.project.name !== 'mobile', 'Mobile navigation acceptance runs on mobile')
  const failures = observeRuntime(page)
  const mocks = await mockApi(page, {
    id: '1',
    username: 'root',
    nickname: '管理者',
    role: 'ADMIN'
  })

  await page.goto('/admin')
  await expect(page.getByRole('link', { name: '返回博客首页' })).toBeVisible()
  await expect(page.getByRole('link', { name: '修改昵称与头像' })).toBeVisible()
  await expect(page.getByRole('button', { name: '退出登录' })).toBeVisible()
  await expectNoOverflow(page)

  await page.getByRole('link', { name: '修改昵称与头像' }).click()
  await expect(page).toHaveURL(/\/me$/)

  expectCleanRun(failures, mocks)
})

test('editing the signed-in admin title updates the profile in the same SPA', async ({
  page
}, testInfo) => {
  test.skip(testInfo.project.name !== 'desktop', 'Current-user store acceptance runs on desktop')
  const failures = observeRuntime(page)
  const mocks = await mockApi(page, {
    id: '1',
    username: 'root',
    nickname: '管理者',
    role: 'ADMIN',
    titleName: '',
    titleStyle: 'default'
  })

  await page.goto('/admin?tab=users')
  const adminRow = page.locator('.user-row').filter({ hasText: '@root' })
  await adminRow.getByRole('button', { name: '编辑称号' }).click()
  const dialog = page.getByRole('dialog', { name: '用户称号' })
  await dialog.getByPlaceholder('留空可清除称号').fill('站长')
  await dialog.getByText('蓝色', { exact: true }).click()
  await dialog.getByRole('button', { name: '保存' }).click()
  await page.getByRole('link', { name: '修改昵称与头像' }).click()
  await expect(page).toHaveURL(/\/me$/)
  await expect(page.locator('.profile-card .title-badge')).toHaveText('站长')

  expectCleanRun(failures, mocks)
})

test('moderation clears stale selection and disables row operations while filters reload', async ({
  page
}, testInfo) => {
  test.skip(testInfo.project.name !== 'desktop', 'Moderation request-state coverage runs once')
  const failures = observeRuntime(page)
  const mocks = await mockApi(page, { id: '1', username: 'root', role: 'ADMIN' })
  mocks.interactions.push({
    ...mocks.interactions[0],
    id: 'hidden-comment',
    nickname: '隐藏访客',
    content: '审核后才允许回复',
    status: 'HIDDEN',
    pinned: false,
    replies: [],
    replyCount: 0
  })
  let holdNextRequest = false
  let releaseRequest
  let markRequestStarted
  const requestStarted = new Promise((resolve) => {
    markRequestStarted = resolve
  })
  const requestHold = new Promise((resolve) => {
    releaseRequest = resolve
  })
  await page.route('**/api/admin/interactions?**', async (route) => {
    if (!holdNextRequest) return route.fallback()
    markRequestStarted()
    await requestHold
    return route.fallback()
  })

  await page.goto('/admin?tab=interactions')
  const root = page.locator('.moderation-item').filter({ hasText: '验收评论' })
  const hiddenRoot = page.locator('.moderation-item').filter({ hasText: '审核后才允许回复' })
  await expect(hiddenRoot.getByRole('button', { name: '回复隐藏访客' })).toHaveCount(0)
  await root.getByRole('checkbox', { name: '选择 留言者 的评论' }).check()
  await expect(page.getByText('已选择 1 条')).toBeVisible()

  holdNextRequest = true
  await page.getByLabel('评论内容类型').selectOption('STICKER')
  await requestStarted

  await expect(page.getByText('已选择 1 条')).toHaveCount(0)
  await expect(page.getByRole('checkbox', { name: '选择本页' })).toBeDisabled()
  await expect(root.getByRole('checkbox', { name: '选择 留言者 的评论' })).toBeDisabled()
  await expect(root.getByRole('button', { name: '回复留言者' })).toBeDisabled()
  await expect(root.locator('summary[aria-label="更多操作：留言者"]')).toHaveAttribute(
    'aria-disabled',
    'true'
  )

  releaseRequest()
  await expect(page.locator('.moderation-list')).toHaveAttribute('aria-busy', 'false')
  expectCleanRun(failures, mocks)
})

test('rapid comment likes are serialized and settle on the latest click', async ({
  page
}, testInfo) => {
  test.skip(testInfo.project.name !== 'desktop', 'Comment like concurrency runs once')
  const failures = observeRuntime(page)
  const mocks = await mockApi(page, { id: '1', username: 'reader', role: 'USER' })
  const desiredStates = []
  let releaseFirst
  let markFirstStarted
  const firstStarted = new Promise((resolve) => {
    markFirstStarted = resolve
  })
  const firstHold = new Promise((resolve) => {
    releaseFirst = resolve
  })
  await page.route('**/api/engagement/COMMENT/comment-1/like', async (route) => {
    const desired = route.request().postDataJSON().liked
    desiredStates.push(desired)
    if (desiredStates.length === 1) {
      markFirstStarted()
      await firstHold
    }
    return route.fulfill({ json: ok({ liked: desired, count: desired ? 5 : 4 }) })
  })

  await page.goto('/post/2081309851788009474')
  const root = page.locator('.interaction-item').filter({ hasText: '验收评论' })
  const like = root.locator('.interaction-actions > button').first()
  await like.click()
  await firstStarted
  await expect(like).toHaveAttribute('aria-busy', 'true')
  await like.click()
  await expect(like).toContainText('4')

  releaseFirst()
  await expect.poll(() => desiredStates).toEqual([true, false])
  await expect(like).toHaveAttribute('aria-busy', 'false')
  await expect(like).not.toHaveClass(/active/)
  await expect(like).toContainText('4')
  expectCleanRun(failures, mocks)
})

test('reply submission is single-flight and focuses the newly created reply', async ({
  page
}, testInfo) => {
  test.skip(testInfo.project.name !== 'desktop', 'Reply focus coverage runs once')
  await page.addInitScript(() => {
    window.__replyScroll = []
    Element.prototype.scrollIntoView = function (options) {
      window.__replyScroll.push({ id: this.dataset.interactionId, behavior: options?.behavior })
    }
  })
  const failures = observeRuntime(page)
  const mocks = await mockApi(page, { id: '1', username: 'reader', nickname: '读者', role: 'USER' })
  let releaseReply
  let markReplyStarted
  const replyStarted = new Promise((resolve) => {
    markReplyStarted = resolve
  })
  const replyHold = new Promise((resolve) => {
    releaseReply = resolve
  })
  let replyCalls = 0
  await page.route('**/api/interactions/comment-1/replies', async (route) => {
    replyCalls += 1
    markReplyStarted()
    await replyHold
    return route.fulfill({
      json: ok({
        id: 'new-focused-reply',
        rootId: 'comment-1',
        replyToId: 'comment-1',
        replyToNickname: '留言者',
        content: route.request().postDataJSON().content,
        nickname: '读者',
        status: 'PUBLISHED',
        likeCount: 0,
        liked: false,
        createTime: '2026-07-26T16:00:00'
      })
    })
  })

  await page.goto('/post/2081309851788009474')
  const root = page.locator('.interaction-item').filter({ hasText: '验收评论' })
  await root.getByRole('button', { name: '回复' }).first().click()
  const composer = root.locator('.reply-composer')
  await composer.getByPlaceholder('回复内容').fill('只发送一次的回复')
  const submit = composer.getByRole('button', { name: '发送' })
  await submit.click()
  await replyStarted
  await expect(submit).toBeDisabled()
  await submit.click({ force: true })
  expect(replyCalls).toBe(1)

  releaseReply()
  const created = page.locator('[data-interaction-id="new-focused-reply"]')
  await expect(created).toContainText('只发送一次的回复')
  await expect(created).toBeFocused()
  await expect
    .poll(() => page.evaluate(() => window.__replyScroll.at(-1)))
    .toEqual({ id: 'new-focused-reply', behavior: 'smooth' })
  expectCleanRun(failures, mocks)
})

test('deleting a loaded root refetches every loaded offset page without skipping the next root', async ({
  page
}, testInfo) => {
  test.skip(testInfo.project.name !== 'desktop', 'Root deletion paging coverage runs once')
  const failures = observeRuntime(page)
  const mocks = await mockApi(page, { id: '1', username: 'root', role: 'ADMIN' })
  for (let index = 2; index <= 41; index += 1) {
    const createTime = new Date(Date.UTC(2026, 6, 27 - index)).toISOString()
    mocks.interactions.push({
      ...mocks.interactions[0],
      id: `comment-${index}`,
      content: `待续页评论 ${index}`,
      pinned: false,
      pinOrder: null,
      replyCount: 0,
      replies: [],
      createTime
    })
  }
  await page.route('**/api/admin/interactions/*', async (route) => {
    const url = new URL(route.request().url())
    if (route.request().method() !== 'DELETE' || url.pathname.split('/').length !== 5) {
      return route.fallback()
    }
    const id = url.pathname.split('/')[4]
    mocks.interactions = mocks.interactions.filter((item) => String(item.id) !== id)
    return route.fulfill({ json: ok(null) })
  })

  await page.goto('/post/2081309851788009474')
  await page.getByRole('button', { name: '加载更多评论' }).click()
  await expect(page.locator('.thread-list > .interaction-item')).toHaveCount(40)
  await expect(page.getByText('待续页评论 41', { exact: true })).toHaveCount(0)

  const firstRoot = page.locator('.interaction-item').filter({ hasText: '验收评论' })
  await firstRoot.locator('summary[aria-label="更多操作"]').click()
  await firstRoot.getByRole('button', { name: '删除' }).click()
  await page.getByRole('dialog', { name: '删除内容' }).getByRole('button', { name: '删除' }).click()

  await expect(page.locator('.thread-list > .interaction-item')).toHaveCount(40)
  await expect(page.getByText('待续页评论 41', { exact: true })).toBeVisible()
  expectCleanRun(failures, mocks)
})
