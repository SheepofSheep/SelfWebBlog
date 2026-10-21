# Gabriel Blog Frontend

Vue 3 + Vite frontend for SelfWebBlog. The application is an article-first personal blog with authoring, interaction, discovery, appearance, and administration workflows.

## Requirements

- Node.js 20+
- pnpm 10+
- SelfWebBlog backend on `http://localhost:8080`

## Local Development

```powershell
pnpm install --frozen-lockfile
pnpm dev --host 127.0.0.1 --port 5174
```

Vite proxies `/api`, `/uploads`, and `/actuator` to the local backend.

## Quality Checks

```powershell
pnpm test
pnpm lint
pnpm build
pnpm format:check
pnpm test:e2e
```

`pnpm check` runs unit tests, ESLint, and the production build. Playwright tests use desktop, tablet, and mobile viewports and reuse an existing server on port 5174.

## Routes

- `/`: article-first home
- `/archive`: URL-backed search and filters
- `/tags`, `/tags/:slug`: tag discovery
- `/calendar`: publication calendar
- `/post/:id`: reading, engagement, and sharing
- `/guestbook`: public guestbook
- `/about`: author profile
- `/login`: login and registration
- `/me`: visitor profile
- `/write`, `/write/:id`: authoring
- `/admin`: administration workbench

## Source Boundaries

```text
src/
  api/            HTTP client and resource APIs
  components/     shared UI, Markdown editor and renderer, uploads, interactions
  core/           formatting, rendering policies and independently tested models
  features/admin/ administration workbench panels
  pages/          route composition
  stores/         site, authenticated user and notification state
  router.js       routes and scroll behavior
  main.js         application bootstrapping
  styles/         Aurora visual tokens, base, public, workspace, editor and Markdown styles
```

Pages compose feature components. API access belongs in `src/api` or a feature API module. Do not create parallel theme controls, interaction cards, post feeds, or filter state.

## 写作与 Markdown

写作页使用项目已有的 CodeMirror 6，支持选区格式化、Markdown 语法着色、撤销重做、编辑器内搜索，以及粘贴、拖入和上传图片。顶部可以切换预览、文章设置和专注写作，标题、摘要及封面区域支持折叠。草稿恢复和服务器保存仍沿用原来的接口。

- `Ctrl / ⌘ + B`：粗体；`Ctrl / ⌘ + I`：斜体；`Ctrl / ⌘ + K`：插入链接。
- `Ctrl / ⌘ + Z`：撤销；`Ctrl / ⌘ + Shift + Z`：重做；`Ctrl / ⌘ + S`：保存到服务器。
- 工具栏还提供标题、字号、左中右对齐、删除线、引用、列表、任务、表格和代码块。

粗体、斜体等保存为标准 Markdown。字号和段落对齐是本站的受限扩展，支持组合使用；预览和文章阅读页通过同一个渲染器解释：

```markdown
::: align-center
::: size-large
**居中的大号文字**
:::
:::
```

扩展仅允许 `align-left/center/right` 和 `size-small/normal/large/xlarge`，不接受任意 CSS。导出给其他 Markdown 工具时，需要转换这些扩展。渲染流程保留 DOMPurify 清洗，移除脚本、事件和任意样式，外部链接增加安全属性，代码块支持复制，长代码和表格可独立滚动。

相关实现：`src/components/MarkdownEditor.vue`、`src/core/markdownFormatting.js`、`src/core/markdownRenderer.js`、`src/components/MarkdownBody.vue`。标题、摘要及封面位于发布设置栏，可直接上传、拖入、替换或移除封面。

## 液态玻璃界面与自定义背景

前台、管理台和写作页采用参考 Apple Liquid Glass 的网页材质：亮色模式为中性浅色、深色文字和通透边缘高光，暗色模式单独配置。悬浮导航与工具栏使用较通透的控制层，内容区使用更平静的半透明表面。这是 CSS 对设计语言的适配，并非 Apple 原生材质 API。视觉令牌和各区域样式由 `src/styles/index.css` 统一导入。

所有页面的背景均读取站点设置中的 `bgUrl`，沿用已有上传及保存接口；没有配置图片时使用纯色底。站点设置通过上传头像、背景预览管理图片，隐藏原始图片地址；“一句话介绍”用于简短资料，“简介”用于关于页 Markdown 正文。正文与写作预览共享主题材质和文字颜色。

管理台按内容、资源和设置分组；概览中的草稿、待处理评论及孤立媒体可直接跳到对应筛选结果。评论继续支持按文章和留言板归类。前台桌面导航可展开文字标签，窄屏使用底部导航；管理模块在窄屏通过抽屉访问。系统减少动态效果设置仍受支持。

## Authentication

The frontend uses an HttpOnly session cookie and a readable `XSRF-TOKEN` cookie. The shared Axios interceptor sends `X-XSRF-TOKEN` for writes. JWTs and OAuth secrets must never be stored in frontend code or localStorage.

## Production

```powershell
pnpm build
pnpm preview --host 127.0.0.1 --port 4174
```

Production hosting must provide Vue Router history fallback and proxy `/api`, `/uploads`, and `/actuator/health` to the backend. Deployment and environment requirements are maintained in the local project documentation directory.
