# Site Background Upload Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a managed “上传背景” control that fills the site background URL, preserves explicit save semantics, and handles failure/retry safely.

**Architecture:** Add a named API wrapper around the existing managed image endpoint, then keep upload state inside `SitePanel.vue`. The upload only changes the local form model; the existing site settings `PUT` remains the single persistence boundary and continues to synchronize the public shell.

**Tech Stack:** Vue 3 Composition API, Vite, native `node:test`, Playwright, existing Spring Boot upload API

---

### Task 1: Add the background upload API contract

**Files:**
- Modify: `SelfWebBlog/blog-frontend/src/api/client.test.js`
- Modify: `SelfWebBlog/blog-frontend/src/api/client.js`

- [ ] **Step 1: Write the failing API test**

Add a test that invokes the wished-for method and verifies the multipart request without asserting an implementation-specific multipart boundary:

```js
test('site background uploads use the managed image endpoint', async (context) => {
  const originalFetch = globalThis.fetch
  const calls = []
  globalThis.fetch = async (path, options = {}) => {
    calls.push({ path, options })
    const data = path === '/api/auth/csrf' ? { token: 'fresh-token' } : { originalUrl: '/uploads/bg.webp' }
    return {
      ok: true,
      headers: new Headers({ 'content-type': 'application/json' }),
      json: async () => ({ data })
    }
  }
  context.after(() => {
    globalThis.fetch = originalFetch
  })

  const result = await api.uploadSiteBackground(new Blob(['image'], { type: 'image/png' }))
  const requestCall = calls.find((call) => call.path === '/api/upload/image')

  assert.equal(result.originalUrl, '/uploads/bg.webp')
  assert.equal(requestCall.options.method, 'POST')
  assert.equal(requestCall.options.body instanceof FormData, true)
  assert.equal(new Headers(requestCall.options.headers).has('content-type'), false)
})
```

- [ ] **Step 2: Run the test and verify RED**

Run:

```powershell
pnpm test -- --test-name-pattern "site background uploads"
```

Expected: FAIL because `api.uploadSiteBackground` is not a function.

- [ ] **Step 3: Implement the minimal API method**

Add this method next to `uploadSiteAvatar`:

```js
uploadSiteBackground: (file) => {
  const data = new FormData()
  data.append('file', file)
  return request('/api/upload/image', { method: 'POST', body: data })
},
```

- [ ] **Step 4: Run the focused API test and verify GREEN**

Run the same command. Expected: the named test passes with no warning.

### Task 2: Add upload, preview, save, failure, and retry behavior

**Files:**
- Modify: `SelfWebBlog/blog-frontend/e2e/core-flows.spec.js`
- Modify: `SelfWebBlog/blog-frontend/src/features/admin/SitePanel.vue`

- [ ] **Step 1: Extend the mock contract and write failing browser tests**

Add `failBackgroundUpload: false` to the mock state and handle the upload before the fallback route:

```js
if (path === '/api/upload/image' && method === 'POST') {
  if (state.failBackgroundUpload)
    return route.fulfill({ json: { code: 400, msg: '背景上传失败', data: null } })
  return route.fulfill({
    json: ok({
      id: 'site-background',
      originalUrl: pixel,
      smallWebpUrl: pixel,
      largeWebpUrl: pixel,
      purpose: 'ARTICLE'
    })
  })
}
```

Add two desktop tests:

```js
test('site background upload fills the address and persists only when saved', async ({ page }, testInfo) => {
  test.skip(testInfo.project.name !== 'desktop', 'Site background upload runs once on desktop')
  const failures = observeRuntime(page)
  const mocks = await mockApi(page, { id: '1', username: 'root', nickname: '管理者', role: 'ADMIN' })

  await page.goto('/admin?tab=site')
  const input = page.getByLabel('选择背景图片')
  await input.setInputFiles({ name: 'background.png', mimeType: 'image/png', buffer: Buffer.from('image') })
  await expect(page.getByLabel('背景地址')).toHaveValue(pixel)
  await expect(page.getByRole('img', { name: '站点背景预览' })).toBeVisible()
  expect(mocks.calls.some((call) => call.path === '/api/admin/site' && call.method === 'PUT')).toBe(false)

  await page.getByRole('button', { name: '保存', exact: true }).click()
  await expect.poll(() => mocks.calls.some((call) => call.path === '/api/admin/site' && call.method === 'PUT' && call.body?.bgUrl === pixel)).toBe(true)
  expectCleanRun(failures, mocks)
})

test('failed site background upload preserves the old address and can retry the same file', async ({ page }, testInfo) => {
  test.skip(testInfo.project.name !== 'desktop', 'Site background retry runs once on desktop')
  const failures = observeRuntime(page)
  const mocks = await mockApi(page, { id: '1', username: 'root', nickname: '管理者', role: 'ADMIN' })
  mocks.failBackgroundUpload = true

  await page.goto('/admin?tab=site')
  const address = page.getByLabel('背景地址')
  const input = page.getByLabel('选择背景图片')
  await address.fill('/uploads/existing.webp')
  const file = { name: 'background.png', mimeType: 'image/png', buffer: Buffer.from('image') }
  await input.setInputFiles(file)
  await expect(address).toHaveValue('/uploads/existing.webp')
  await expect(page.getByRole('status')).toContainText('背景上传失败')
  await expect(page.getByRole('button', { name: '上传背景' })).toBeEnabled()

  mocks.failBackgroundUpload = false
  await input.setInputFiles(file)
  await expect(address).toHaveValue(pixel)
  expectCleanRun(failures, mocks)
})
```

- [ ] **Step 2: Run the two tests and verify RED**

Run:

```powershell
pnpm exec playwright test e2e/core-flows.spec.js --project=desktop --grep "site background"
```

Expected: FAIL because the upload control and handler do not exist.

- [ ] **Step 3: Add isolated upload state and handler**

In `SitePanel.vue`, add:

```js
const backgroundInput = ref(null)
const backgroundUploading = ref(false)
const backgroundUploadGate = createAdminRequestGate()
```

Include `backgroundUploading.value` in `profileLocked`, invalidate the gate in `onBeforeUnmount`, and add:

```js
async function uploadBackground(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file || profileLocked.value) return
  const ticket = backgroundUploadGate.begin()
  backgroundUploading.value = true
  try {
    const asset = await api.uploadSiteBackground(file)
    if (!backgroundUploadGate.isCurrent(ticket)) return
    if (!asset?.originalUrl) throw new Error('上传结果缺少原图地址')
    site.bgUrl = asset.originalUrl
    notify('背景已上传，保存后生效')
  } catch (error) {
    if (backgroundUploadGate.isCurrent(ticket)) notify(error?.message || '背景上传失败')
  } finally {
    backgroundUploadGate.runIfCurrent(ticket, () => {
      backgroundUploading.value = false
    })
  }
}
```

- [ ] **Step 4: Add the field control and preview**

Replace the background label with a field group:

```vue
<div class="site-form-field background-field">
  <div class="field-title">
    <label for="site-background-url">背景地址</label>
    <button
      class="secondary-button"
      type="button"
      :disabled="profileLocked"
      @click="backgroundInput.click()"
    >
      <Upload :size="15" />{{ backgroundUploading ? '上传中…' : '上传背景' }}
    </button>
  </div>
  <input id="site-background-url" v-model="site.bgUrl" maxlength="1000" />
  <div v-if="site.bgUrl" class="background-preview">
    <img :src="site.bgUrl" alt="站点背景预览" />
  </div>
  <input
    ref="backgroundInput"
    class="visually-hidden"
    type="file"
    accept="image/jpeg,image/png,image/gif,image/webp"
    aria-label="选择背景图片"
    @change="uploadBackground"
  />
</div>
```

Add scoped styles for `.site-form-field`, `.field-title`, and a 16:5 rounded `.background-preview` with `object-fit: cover`. Keep the mobile grid single-column behavior.

- [ ] **Step 5: Run the focused tests and verify GREEN**

Run the focused Playwright command. Expected: both desktop tests pass and there are no unhandled mock requests.

### Task 3: Full verification and commit

**Files:**
- Verify all modified frontend files
- Preserve: `SelfWebBlog/src/main/java/org/example/selfwebblog/controller/AuthController.java`

- [ ] **Step 1: Run formatting and the full frontend check**

```powershell
pnpm exec prettier --write src/api/client.js src/api/client.test.js src/features/admin/SitePanel.vue e2e/core-flows.spec.js
pnpm check
```

Expected: all node tests, ESLint, Prettier, and the production build pass.

- [ ] **Step 2: Run the full Playwright suite**

```powershell
pnpm exec playwright test --workers=6 --reporter=list
```

Expected: zero failures; device-specific skips remain intentional.

- [ ] **Step 3: Verify the rendered flow in the in-app browser**

Check `/admin?tab=site` at desktop and mobile widths: page identity, nonblank content, no framework overlay, console health, unique “上传背景” control, upload-to-preview interaction, save request, and no horizontal overflow.

- [ ] **Step 4: Review and commit only this change**

```powershell
git diff --check
git status --short
git add -- docs/superpowers/plans/2026-07-27-site-background-upload.md SelfWebBlog/blog-frontend/src/api/client.js SelfWebBlog/blog-frontend/src/api/client.test.js SelfWebBlog/blog-frontend/src/features/admin/SitePanel.vue SelfWebBlog/blog-frontend/e2e/core-flows.spec.js
git commit -m "feat: add managed site background upload"
```

Expected: the commit excludes the pre-existing `AuthController.java` working-tree change.
