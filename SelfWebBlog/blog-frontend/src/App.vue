<script setup>
import { ref, computed, onMounted, onUnmounted, provide, watch, defineAsyncComponent } from 'vue'
import { useRoute, navigate, getRouteMeta } from './router'
import { getProfile, AUTH_EXPIRED_EVENT } from './api'
import { useAuthStore } from './stores/authStore'
import { provideToast, showToast } from './composables/toast'
import ToastHost from './components/ToastHost.vue'
import MeshBackground from './components/MeshBackground.vue'
import {
  Home,
  PenLine,
  Settings,
  UserPlus,
  LogOut,
  Moon,
  Sun,
  Menu,
  User,
  Search,
  X,
  ChevronLeft,
  ChevronRight
} from 'lucide-vue-next'

const HomePage = defineAsyncComponent(() => import('./pages/HomePage.vue'))
const PostPage = defineAsyncComponent(() => import('./pages/PostPage.vue'))
const ProfilePage = defineAsyncComponent(() => import('./pages/ProfilePage.vue'))
const LoginPage = defineAsyncComponent(() => import('./pages/LoginPage.vue'))
const WritePage = defineAsyncComponent(() => import('./pages/WritePage.vue'))
const VisitorProfilePage = defineAsyncComponent(() => import('./pages/VisitorProfilePage.vue'))
const ArchivePage = defineAsyncComponent(() => import('./pages/ArchivePage.vue'))
import loadingStore from './stores/loadingStore'

const routeReady = ref(false)
const isRouteChecking = computed(() => !routeReady.value && !getRouteMeta(path.value).public)

const currentPage = computed(() => {
  if (isRouteChecking.value) return null
  if (path.value === '/login') return LoginPage
  if (path.value === '/') return HomePage
  if (path.value === '/write') return WritePage
  if (path.value.startsWith('/post')) return PostPage
  if (path.value === '/profile') return ProfilePage
  if (path.value === '/me') return VisitorProfilePage
  if (path.value === '/archive') return ArchivePage
  return null
})

const pageKey = computed(() => {
  if (path.value.startsWith('/post')) return 'post-' + (query.value.get('id') || '')
  if (path.value === '/archive') return 'archive-' + query.value.toString()
  return path.value
})
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'

NProgress.configure({ showSpinner: false, trickleSpeed: 100, ease: 'ease', speed: 200 })

import { toAbsoluteUrl } from './utils/url'

provideToast()

const { path, query } = useRoute()
const { user, restoreUser, saveUser, clearUserState, loadUserInfo, logoutUser } = useAuthStore()
const mobileOpen = ref(false)
const sidebarCollapsed = ref(localStorage.getItem('sidebarCollapsed') === 'true')
const siteInfo = ref(null)
const brandAvatar = computed(() =>
  toAbsoluteUrl(siteInfo.value?.avatarUrl || user.value?.avatarUrl || '')
)
const brandName = computed(() => siteInfo.value?.nickname || 'Gabriel')

function toggleSidebar() {
  sidebarCollapsed.value = !sidebarCollapsed.value
  localStorage.setItem('sidebarCollapsed', String(sidebarCollapsed.value))
}

const theme = ref(localStorage.getItem('theme') || 'dark')
function toggleTheme() {
  theme.value = theme.value === 'light' ? 'dark' : 'light'
  localStorage.setItem('theme', theme.value)
  document.documentElement.setAttribute('data-theme', theme.value)
}
const refreshKey = ref(0)
provide('user', user)
provide('refreshUser', loadUserInfo)
provide('loadingStore', loadingStore)
provide('refreshHome', async () => {
  refreshKey.value++
  await loadBackground()
})

function handleOAuthRedirect() {
  const token = query.value.get('token')
  const userStr = query.value.get('user')
  const error = query.value.get('error')
  if (error) {
    showToast('GitHub 登录失败: ' + decodeURIComponent(error), 'error')
    window.history.replaceState({}, '', '#/login')
    return true
  }
  if (token && userStr) {
    try {
      const u = JSON.parse(decodeURIComponent(userStr))
      localStorage.setItem('token', token)
      saveUser(u)
      showToast('GitHub 登录成功', 'success')
      return true
    } catch {
      return false
    }
  }
  return false
}

async function ensureAuthForRoute() {
  routeReady.value = false
  const currentPath = path.value
  const meta = getRouteMeta(currentPath)
  const ok = handleOAuthRedirect()
  if (ok) {
    navigate('/')
    return false
  }

  if (!user.value) {
    restoreUser()
    try {
      await loadUserInfo()
    } catch {
      user.value = null
    }
  }

  if (meta.public) {
    routeReady.value = true
    return true
  }

  if (meta.requiresAuth && !user.value) {
    showToast('登录后就可以继续访问这里。', 'warning')
    const redirect = window.location.hash.replace(/^#/, '') || '/'
    navigate(`/login?redirect=${encodeURIComponent(redirect)}`)
    return false
  }

  if (meta.requiresRole && user.value?.role !== meta.requiresRole) {
    showToast('这里是博主工作台，当前账号不能进入。', 'warning')
    navigate('/')
    return false
  }

  routeReady.value = true
  return true
}

async function handleLogout() {
  await logoutUser()
  showToast('已退出登录')
  navigate('/')
}

function handleAuthExpired() {
  if (!user.value && !localStorage.getItem('token')) return
  clearUserState()
  if (!getRouteMeta(path.value).public) {
    showToast('登录状态过期了，重新登录一下就好。', 'warning')
    navigate('/login')
  }
}

function navTo(route) {
  navigate(route)
  mobileOpen.value = false
}

async function loadBackground() {
  try {
    const data = await getProfile()
    siteInfo.value = data?.blogInfo || null
    if (data?.blogInfo?.bgUrl) {
      document.body.style.backgroundImage = ''
      document.documentElement.style.setProperty('--user-bg-image', `url("${data.blogInfo.bgUrl}")`)
    } else {
      document.body.style.backgroundImage = ''
      document.documentElement.style.removeProperty('--user-bg-image')
    }
  } catch {}
}

watch(
  () => path.value,
  async () => {
    NProgress.start()
    loadingStore.setRouterLoading(true)
    try {
      await ensureAuthForRoute()
    } finally {
      loadingStore.setRouterLoading(false)
      NProgress.done()
    }
    mobileOpen.value = false
  }
)

onMounted(async () => {
  window.addEventListener(AUTH_EXPIRED_EVENT, handleAuthExpired)
  document.documentElement.setAttribute('data-theme', theme.value)
  loadingStore.setRouterLoading(true)
  try {
    await Promise.all([ensureAuthForRoute(), loadBackground()])
  } finally {
    loadingStore.setRouterLoading(false)
    NProgress.done()
  }
})

onUnmounted(() => {
  window.removeEventListener(AUTH_EXPIRED_EVENT, handleAuthExpired)
})
</script>

<template>
  <div
    class="app"
    :class="{
      'is-login': path === '/login',
      'mobile-open': mobileOpen,
      'sidebar-collapsed': sidebarCollapsed
    }"
  >
    <MeshBackground />

    <aside
      v-if="path !== '/login'"
      class="sidebar glass-panel"
      :class="{ collapsed: sidebarCollapsed }"
      aria-label="站点导航"
    >
      <button class="sb-brand" @click="navTo('/')">
        <span class="brand-mark">
          <img v-if="brandAvatar" :src="brandAvatar" :alt="brandName" class="brand-avatar" />
          <span v-else>G</span>
        </span>
        <span class="brand-copy">
          <strong>GABRIEL.ARCHIVE</strong>
          <small>{{ brandName }} / PUBLIC LOG</small>
        </span>
      </button>

      <nav class="sb-nav">
        <a class="sb-link" :class="{ active: path === '/' }" title="首页" @click="navTo('/')">
          <Home :size="18" />
          <span>首页</span>
        </a>
        <a
          class="sb-link"
          :class="{ active: path === '/archive' }"
          title="归档"
          @click="navTo('/archive')"
        >
          <Search :size="18" />
          <span>归档</span>
        </a>
        <a
          v-if="user?.role === 'ADMIN'"
          class="sb-link"
          :class="{ active: path === '/write' }"
          title="写作"
          @click="navTo('/write')"
        >
          <PenLine :size="18" />
          <span>写作</span>
        </a>
        <a
          v-if="user?.role === 'ADMIN'"
          class="sb-link"
          :class="{ active: path === '/profile' }"
          title="管理"
          @click="navTo('/profile')"
        >
          <Settings :size="18" />
          <span>管理</span>
        </a>
        <a
          v-if="user && user.role !== 'ADMIN'"
          class="sb-link"
          :class="{ active: path === '/me' }"
          title="我的"
          @click="navTo('/me')"
        >
          <User :size="18" />
          <span>我的</span>
        </a>
        <a
          v-if="!user"
          class="sb-link"
          :class="{ active: path === '/login' }"
          title="登录"
          @click="navTo('/login')"
        >
          <UserPlus :size="18" />
          <span>登录</span>
        </a>
      </nav>

      <div class="sb-footer">
        <button
          class="sb-link"
          :title="theme === 'light' ? '深色模式' : '浅色模式'"
          @click="toggleTheme"
          :aria-label="theme === 'light' ? '切换深色模式' : '切换浅色模式'"
        >
          <Moon v-if="theme === 'light'" :size="18" />
          <Sun v-else :size="18" />
          <span>{{ theme === 'light' ? 'DARK' : 'LIGHT' }}</span>
        </button>

        <div
          v-if="user"
          class="sb-user"
          :title="user?.nickname || user?.username"
          @click="navTo(user.role === 'ADMIN' ? '/profile' : '/me')"
        >
          <img
            :src="
              toAbsoluteUrl(user?.avatarUrl) ||
              'https://api.dicebear.com/7.x/avataaars/svg?seed=Felix'
            "
            :alt="user?.nickname || user?.username || '用户头像'"
            class="sb-avatar avatar-img"
            :class="{ loaded: user?.avatarUrl }"
            @load="(e) => e.target.classList.add('loaded')"
          />
          <div class="sb-user-info">
            <span class="sb-username">{{ user?.nickname || user?.username }}</span>
            <span v-if="user?.role === 'ADMIN'" class="sb-badge">博主</span>
          </div>
        </div>

        <a v-if="user" class="sb-link sb-logout" title="退出登录" @click="handleLogout">
          <LogOut :size="18" />
          <span>退出</span>
        </a>
      </div>

      <button
        class="sb-collapse-btn"
        @click="toggleSidebar"
        :aria-label="sidebarCollapsed ? '展开侧边栏' : '收起侧边栏'"
        :title="sidebarCollapsed ? '展开侧边栏' : '收起侧边栏'"
      >
        <ChevronLeft v-if="!sidebarCollapsed" :size="16" />
        <ChevronRight v-else :size="16" />
      </button>
    </aside>

    <button
      v-if="path !== '/login'"
      class="mobile-menu-btn"
      @click="mobileOpen = !mobileOpen"
      :aria-label="mobileOpen ? '关闭菜单' : '打开菜单'"
    >
      <X v-if="mobileOpen" :size="20" />
      <Menu v-else :size="20" />
    </button>

    <div v-if="mobileOpen" class="mobile-overlay" @click="mobileOpen = false"></div>

    <main :class="['main-content', { 'login-content': path === '/login' }]">
      <Transition name="page" mode="out-in">
        <KeepAlive v-if="currentPage" :include="['HomePage', 'ProfilePage']">
          <component :is="currentPage" :key="pageKey + '-' + refreshKey" />
        </KeepAlive>
        <div v-else-if="isRouteChecking" class="not-found">正在确认登录状态...</div>
        <div v-else class="not-found">页面未找到</div>
      </Transition>
    </main>

    <ToastHost />
  </div>
</template>

<style scoped>
.app {
  min-height: 100vh;
  background: transparent;
}

.app:not(.is-login) {
  padding-top: 86px;
  transition: padding-top var(--duration-normal) var(--ease-bounce);
}

.app.sidebar-collapsed:not(.is-login) {
  padding-top: 86px;
}

/* ============================================================
   Warm Archive 顶部导航
   ============================================================ */
.sidebar {
  position: fixed;
  left: 50%;
  top: 12px;
  bottom: auto;
  width: min(var(--magazine-max, 1180px), calc(100vw - 28px));
  min-height: 58px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 8px 10px;
  border-radius: var(--radius-xl);
  z-index: 200;
  overflow: visible;
  border-color: var(--border-medium);
  background:
    linear-gradient(180deg, rgba(255, 249, 232, 0.92), rgba(246, 237, 207, 0.88)),
    var(--surface-strong);
  transform: translateX(-50%);
  transition:
    transform var(--duration-normal) var(--ease-bounce),
    border-color var(--duration-normal) var(--ease-bounce),
    background var(--duration-normal) var(--ease-bounce);
}

.sidebar.collapsed {
  width: min(var(--magazine-max, 1180px), calc(100vw - 28px));
  padding: 8px 10px;
}

.sidebar:hover {
  transform: translateX(-50%);
}

.sb-brand {
  display: inline-grid;
  grid-template-columns: 38px minmax(0, 1fr);
  align-items: center;
  gap: 10px;
  min-width: 230px;
  border: 0;
  background: transparent;
  color: var(--text-main);
  font: inherit;
  text-align: left;
  cursor: pointer;
}

.brand-mark {
  display: grid;
  width: 40px;
  height: 40px;
  place-items: center;
  overflow: hidden;
  border: 1px solid var(--border);
  border-radius: 9px;
  background: linear-gradient(135deg, var(--primary), var(--primary-hover)), var(--surface-paper);
  color: var(--on-primary);
  font-family: var(--font-serif);
  font-size: 1.2rem;
  font-weight: 900;
}

.brand-avatar {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.brand-copy {
  display: grid;
  min-width: 0;
}

.brand-copy strong {
  overflow: hidden;
  color: var(--text-main);
  font-family: var(--font-mono);
  font-size: 0.95rem;
  font-weight: 900;
  line-height: 1.1;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.brand-copy small {
  overflow: hidden;
  color: var(--text-muted);
  font-size: 0.62rem;
  font-weight: 900;
  letter-spacing: 0.08em;
  text-overflow: ellipsis;
  text-transform: uppercase;
  white-space: nowrap;
}

.sb-nav {
  flex: 0 1 auto;
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: center;
  gap: 2px;
  min-width: 0;
  padding: 0;
}

.sb-link {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 12px;
  color: var(--text-secondary);
  font-size: 0.78rem;
  font-weight: 800;
  text-decoration: none;
  cursor: pointer;
  border-radius: var(--radius-md);
  border: none;
  background: none;
  width: auto;
  text-align: left;
  transition:
    color var(--duration-normal) var(--ease-bounce),
    background var(--duration-normal) var(--ease-bounce),
    transform var(--duration-normal) var(--ease-bounce);
}

.sb-link:hover {
  color: var(--primary-hover);
  background: color-mix(in srgb, var(--primary) 10%, transparent);
  transform: translateY(-1px);
}

.sb-link.active {
  color: var(--primary-hover);
  background: color-mix(in srgb, var(--primary) 14%, transparent);
  font-weight: 900;
  box-shadow: inset 0 -2px 0 var(--primary);
}

.sb-footer {
  margin-top: auto;
  padding-top: 0;
  border-top: 0;
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 6px;
}

.sb-user {
  display: flex;
  align-items: center;
  gap: 10px;
  max-width: 180px;
  padding: 6px 10px;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: background var(--duration-normal) var(--ease-bounce);
}

.sb-user:hover {
  background: color-mix(in srgb, var(--primary) 10%, transparent);
}

.sb-avatar {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}

.sb-user-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.sb-username {
  font-size: var(--font-size-xs);
  font-weight: 500;
  color: var(--text-main);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sb-badge {
  font-size: 0.6rem;
  font-weight: 600;
  color: var(--primary-hover);
}

.sb-logout:hover {
  color: var(--danger);
  background: rgba(212, 114, 122, 0.1);
}

.sb-collapse-btn {
  display: none;
}

[data-theme='dark'] .sidebar {
  background:
    linear-gradient(180deg, rgba(33, 27, 18, 0.94), rgba(20, 17, 11, 0.92)), var(--surface-strong);
  border-color: var(--border);
}

[data-theme='dark'] .sb-link:hover,
[data-theme='dark'] .sb-user:hover {
  background: color-mix(in srgb, var(--primary) 13%, transparent);
}

[data-theme='dark'] .sb-link.active {
  color: var(--primary-hover);
  background: color-mix(in srgb, var(--primary) 16%, transparent);
  box-shadow: inset 0 -2px 0 var(--primary);
}

/* ============================================================
   主内容区
   ============================================================ */
.main-content {
  max-width: none;
  margin: 0 auto;
  padding: 0 var(--space-md) var(--space-xl);
  width: 100%;
}

.login-content {
  max-width: none;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
}

.is-login {
  padding-top: 0 !important;
}

.not-found {
  padding: 80px 40px;
  text-align: center;
  color: var(--text-muted);
}

.page-enter-active {
  transition:
    opacity 0.6s var(--ease-bounce),
    transform 0.6s var(--ease-bounce);
}
.page-leave-active {
  transition:
    opacity 0.35s var(--ease-bounce),
    transform 0.35s var(--ease-bounce);
}
.page-enter-from {
  opacity: 0;
  transform: translateY(12px);
}
.page-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

.mobile-menu-btn {
  display: none;
  position: fixed;
  top: 16px;
  left: 16px;
  z-index: 210;
  width: 44px;
  height: 44px;
  border: 1px solid var(--border-warm);
  border-radius: var(--radius-md);
  background: var(--surface-strong);
  backdrop-filter: none;
  color: var(--text-main);
  cursor: pointer;
  align-items: center;
  justify-content: center;
  box-shadow: var(--shadow-soft);
}

[data-theme='dark'] .mobile-menu-btn {
  background: var(--surface-strong);
  border-color: var(--border-warm);
  color: var(--text-main);
}

.mobile-overlay {
  position: fixed;
  inset: 0;
  background: rgba(63, 52, 41, 0.16);
  backdrop-filter: blur(4px);
  z-index: 190;
}

[data-theme='dark'] .mobile-overlay {
  background: rgba(17, 16, 13, 0.58);
}

@media (max-width: 900px) {
  .app:not(.is-login),
  .app.sidebar-collapsed:not(.is-login) {
    padding-top: 0;
  }

  .sidebar,
  .sidebar.collapsed {
    left: 12px;
    top: 12px;
    bottom: 12px;
    width: min(280px, calc(100vw - 24px));
    min-height: auto;
    align-items: stretch;
    justify-content: flex-start;
    flex-direction: column;
    gap: 12px;
    padding: var(--space-md) var(--space-sm);
    transform: translateX(calc(-100% - 24px));
    transition: transform 0.4s var(--ease-bounce);
    box-shadow: var(--shadow-hover);
    overflow-x: hidden;
    overflow-y: auto;
  }

  .mobile-open .sidebar,
  .mobile-open .sidebar.collapsed {
    transform: translateX(0);
  }

  .sb-collapse-btn {
    display: none;
  }

  .sb-brand {
    width: 100%;
  }

  .sb-nav,
  .sb-footer {
    flex-direction: column;
    align-items: stretch;
  }

  .sb-link {
    width: 100%;
  }

  .sb-user {
    max-width: none;
  }

  .mobile-menu-btn {
    display: flex;
  }

  .main-content {
    padding: 72px var(--space-md) var(--space-lg);
  }
}
</style>
