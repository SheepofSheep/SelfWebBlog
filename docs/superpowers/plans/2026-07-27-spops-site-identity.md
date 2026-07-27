# Spops Site Identity Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the old browser-tab identity with Spops, add a refined ICP filing footer to public pages, and make the admin sidebar render as one continuous glass surface over custom backgrounds.

**Architecture:** Keep identity assets static in `index.html` and `public/favicon.svg`, isolate the legal footer in a small Vue component, and let `App.vue` decide whether public chrome is visible. Fix the sidebar at its owning global selector instead of adding nested containers, and protect both behaviors with Playwright checks against real computed styles and served assets.

**Tech Stack:** Vue 3, CSS custom properties and `color-mix()`, Vite, Playwright, pnpm.

---

### Task 1: Add failing browser-identity and sidebar regression checks

**Files:**
- Modify: `SelfWebBlog/blog-frontend/e2e/core-flows.spec.js`

- [ ] **Step 1: Add the identity regression test**

Add this desktop-only test after the shared helper definitions:

```js
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
```

- [ ] **Step 2: Add the custom-background sidebar regression test**

Add this second desktop-only test:

```js
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
```

- [ ] **Step 3: Run the two checks and verify RED**

Run:

```powershell
pnpm exec playwright test e2e/core-flows.spec.js --project=desktop --grep "Spops identity|admin sidebar"
```

Expected: both checks fail. The current title is `博客`, the footer and `spops-mark` do not exist, and `.admin-sidebar` computes to `overflow: visible` with no gradient background image.

- [ ] **Step 4: Commit the regression checks**

```powershell
git add SelfWebBlog/blog-frontend/e2e/core-flows.spec.js
git commit -m "test: cover Spops identity and sidebar surface"
```

### Task 2: Replace the browser-tab identity

**Files:**
- Modify: `SelfWebBlog/blog-frontend/index.html`
- Modify: `SelfWebBlog/blog-frontend/public/favicon.svg`

- [ ] **Step 1: Update the document identity**

Keep the SVG favicon link and replace the old title area with:

```html
<meta name="application-name" content="Spops" />
<meta name="apple-mobile-web-app-title" content="Spops" />
<title>Spops</title>
```

- [ ] **Step 2: Replace the old lightning SVG completely**

Replace `public/favicon.svg` with this compact liquid-S mark:

```svg
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64">
  <defs>
    <linearGradient id="spops-glass" x1="11" y1="8" x2="54" y2="57" gradientUnits="userSpaceOnUse">
      <stop stop-color="#79c8ff"/>
      <stop offset=".48" stop-color="#3d86ed"/>
      <stop offset="1" stop-color="#2255b8"/>
    </linearGradient>
    <linearGradient id="spops-shine" x1="17" y1="10" x2="45" y2="48" gradientUnits="userSpaceOnUse">
      <stop stop-color="#fff" stop-opacity=".92"/>
      <stop offset="1" stop-color="#d9efff" stop-opacity=".18"/>
    </linearGradient>
  </defs>
  <rect x="3" y="3" width="58" height="58" rx="19" fill="url(#spops-glass)"/>
  <path d="M12 25C16 13 28 7 42 10c6 1 10 4 13 8" fill="none" stroke="url(#spops-shine)" stroke-width="5" stroke-linecap="round" opacity=".72"/>
  <path id="spops-mark" d="M44 20c-3-3-7-4.5-12-4.5-7 0-12 3.4-12 8.3 0 5.1 4.7 7 12 8.5 7 1.5 11 3.4 11 8.5 0 5.4-5 8.7-12 8.7-5 0-9.5-1.8-13-5.2" fill="none" stroke="#fff" stroke-width="6" stroke-linecap="round" stroke-linejoin="round"/>
</svg>
```

- [ ] **Step 3: Run Prettier on the HTML and inspect the SVG**

Run:

```powershell
pnpm exec prettier --write index.html
git diff --check
```

Expected: no whitespace errors, and `favicon.svg` contains no old purple lightning paths.

### Task 3: Add the public filing footer

**Files:**
- Create: `SelfWebBlog/blog-frontend/src/components/SiteFooter.vue`
- Modify: `SelfWebBlog/blog-frontend/src/App.vue`
- Modify: `SelfWebBlog/blog-frontend/src/styles.css`

- [ ] **Step 1: Create the focused footer component**

Create `SiteFooter.vue` with this complete implementation:

```vue
<template>
  <footer class="site-footer" aria-label="站点备案信息">
    <a
      class="filing-link"
      href="https://beian.miit.gov.cn/"
      target="_blank"
      rel="noopener noreferrer"
    >
      <span class="filing-seal" aria-hidden="true">晋</span>
      <span>晋ICP备2026007967-1号</span>
    </a>
  </footer>
</template>

<style scoped>
.site-footer {
  display: flex;
  justify-content: center;
  width: min(1180px, calc(100% - 32px));
  margin: 54px auto 0;
  padding: 0 0 24px;
}

.filing-link {
  display: inline-flex;
  min-height: 42px;
  align-items: center;
  gap: 9px;
  padding: 6px 14px 6px 7px;
  border: 1px solid color-mix(in srgb, var(--line) 78%, white);
  border-radius: 999px;
  color: var(--muted);
  background: color-mix(in srgb, var(--glass-strong) 72%, transparent);
  box-shadow: 0 12px 32px color-mix(in srgb, var(--accent) 10%, transparent), inset 0 1px rgba(255, 255, 255, 0.62);
  backdrop-filter: blur(18px) saturate(125%);
  -webkit-backdrop-filter: blur(18px) saturate(125%);
  font-size: 12px;
  letter-spacing: 0.02em;
  transition: transform 180ms ease, color 180ms ease, border-color 180ms ease, box-shadow 180ms ease;
}

.filing-link:hover {
  color: var(--accent-deep);
  border-color: color-mix(in srgb, var(--accent) 34%, var(--line));
  box-shadow: 0 15px 38px color-mix(in srgb, var(--accent) 15%, transparent), inset 0 1px rgba(255, 255, 255, 0.72);
  transform: translateY(-2px);
}

.filing-seal {
  display: grid;
  width: 28px;
  height: 28px;
  place-items: center;
  border-radius: 10px;
  color: #fff;
  background: linear-gradient(145deg, var(--accent), var(--accent-deep));
  box-shadow: 0 7px 16px color-mix(in srgb, var(--accent) 24%, transparent);
  font-size: 12px;
  font-weight: 800;
}

@media (max-width: 720px) {
  .site-footer {
    width: calc(100% - 24px);
    margin-top: 38px;
    padding-bottom: 18px;
  }
}
</style>
```

- [ ] **Step 2: Render the footer only in public chrome**

Import `SiteFooter` in `App.vue` and add it after the root `<main>`:

```vue
<SiteFooter v-if="!immersive" />
```

- [ ] **Step 3: Make short public pages keep the footer at the bottom**

Update the global shell rules:

```css
.app-shell {
  display: flex;
  min-height: 100vh;
  flex-direction: column;
}

.app-shell > main {
  flex: 1 0 auto;
  width: 100%;
}
```

- [ ] **Step 4: Format the Vue and CSS files**

```powershell
pnpm exec prettier --write src/components/SiteFooter.vue src/App.vue src/styles.css
```

### Task 4: Normalize the admin sidebar surface

**Files:**
- Modify: `SelfWebBlog/blog-frontend/src/styles.css`

- [ ] **Step 1: Apply the minimal owning-selector fix**

Extend the existing `.admin-sidebar` rule with:

```css
overflow: hidden;
border: 1px solid color-mix(in srgb, var(--line) 82%, white);
background-image: linear-gradient(
  180deg,
  color-mix(in srgb, var(--bg) 72%, var(--glass-strong)),
  color-mix(in srgb, var(--bg-deep) 68%, var(--glass-strong))
);
box-shadow: var(--shadow);
backdrop-filter: blur(34px) saturate(112%);
-webkit-backdrop-filter: blur(34px) saturate(112%);
```

Do not add backgrounds to `nav`, the brand row, or `.admin-user`; the active navigation rule remains the only internal highlighted surface.

- [ ] **Step 2: Run the focused tests and verify GREEN**

```powershell
pnpm exec playwright test e2e/core-flows.spec.js --project=desktop --grep "Spops identity|admin sidebar"
```

Expected: 2 passed.

- [ ] **Step 3: Commit the implementation**

```powershell
git add SelfWebBlog/blog-frontend/index.html SelfWebBlog/blog-frontend/public/favicon.svg SelfWebBlog/blog-frontend/src/App.vue SelfWebBlog/blog-frontend/src/components/SiteFooter.vue SelfWebBlog/blog-frontend/src/styles.css
git commit -m "feat: add Spops identity and unified site chrome"
```

### Task 5: Validate, publish, and deploy

**Files:**
- Verify only; no unrelated source changes.

- [ ] **Step 1: Run the complete frontend gate**

```powershell
pnpm check
pnpm test:e2e
```

Expected: unit tests, ESLint, Prettier, production build, and all Playwright projects pass.

- [ ] **Step 2: Confirm the branch scope**

```powershell
git status --short
git diff origin/master...HEAD --stat
git log --oneline origin/master..HEAD
```

Expected: only the two design commits, regression test, identity/footer/sidebar implementation, and this plan are present.

- [ ] **Step 3: Push and open a ready PR**

```powershell
git push -u origin codex/spops-site-identity
```

Open a PR into `master` describing the Spops identity, ICP footer, sidebar root cause, and validation results. Wait for CI and merge only when all checks pass.

- [ ] **Step 4: Build the exact merge commit and deploy the frontend release**

Build `SelfWebBlog/blog-frontend/dist` from the clean merged `origin/master`, upload it to a new `/var/www/releases/selfwebblog-<sha>` directory, preserve previous hashed assets, update `/var/www/selfwebblog-current` through the existing release pointer, run `nginx -t`, and reload Nginx. Do not replace the backend JAR because this scope is frontend-only.

- [ ] **Step 5: Verify production**

Check:

```text
https://hayaanon317.top/                 -> 200 HTML, title Spops
https://hayaanon317.top/favicon.svg      -> 200 image/svg+xml, contains spops-mark
https://hayaanon317.top/admin            -> 200 HTML, no public footer
https://hayaanon317.top/actuator/health  -> 200 {"status":"UP"}
```

Also confirm the homepage footer contains the exact ICP number and MIIT link, successful assets use immutable caching and gzip, a missing asset returns 404 without immutable caching, and the backend/Nginx services remain active.
