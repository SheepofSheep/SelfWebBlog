<script setup>
import { ref, computed, onMounted, onUnmounted, provide, watch, defineAsyncComponent } from 'vue'
import { useRoute, navigate, getRouteMeta } from './router'
import { getProfile, AUTH_EXPIRED_EVENT } from './api'
import { useAuthStore } from './stores/authStore'
import { provideToast, showToast } from './composables/toast'
import ToastHost from './components/ToastHost.vue'
import MeshBackground from './components/MeshBackground.vue'
import {
  Home, PenLine, Settings, UserPlus, LogOut, Moon, Sun,
  Menu, User, Search, X, ChevronLeft, ChevronRight
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

function toggleSidebar() {
  sidebarCollapsed.value = !sidebarCollapsed.value
  localStorage.setItem('sidebarCollapsed', String(sidebarCollapsed.value))
}

const theme = ref(localStorage.getItem('theme') || 'light')
function toggleTheme() {
  theme.value = theme.value === 'light' ? 'dark' : 'light'
  localStorage.setItem('theme', theme.value)
  document.documentElement.setAttribute('data-theme', theme.value)
}
const refreshKey = ref(0)
provide('user', user)
provide('refreshUser', loadUserInfo)
provide('loadingStore', loadingStore)
provide('refreshHome', () => { refreshKey.value++ })

function handleOAuthRedirect() {
  const token = query.value.get('token')
  const userStr = query.value.get('user')
  const error = query.value.get('error')
  if (error) { showToast('GitHub 登录失败: ' + decodeURIComponent(error), 'error'); window.history.replaceState({}, '', '#/login'); return true }
  if (token && userStr) {
    try {
      const u = JSON.parse(decodeURIComponent(userStr))
      localStorage.setItem('token', token); saveUser(u)
      showToast('GitHub 登录成功', 'success')
      return true
    } catch { return false }
  }
  return false
}

async function ensureAuthForRoute() {
  routeReady.value = false
  const currentPath = path.value
  const meta = getRouteMeta(currentPath)
  const ok = handleOAuthRedirect()
  if (ok) { navigate('/'); return false }

  if (!user.value) {
    restoreUser()
    try { await loadUserInfo() } catch { user.value = null }
  }

  if (meta.public) {
    routeReady.value = true
    return true
  }

  if (meta.requiresAuth && !user.value) {
    showToast('请先登录', 'warning')
    navigate('/login')
    return false
  }

  if (meta.requiresRole && user.value?.role !== meta.requiresRole) {
    showToast('无权访问', 'warning')
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
    showToast('登录状态已失效，请重新登录', 'warning')
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
    if (data?.blogInfo?.bgUrl) {
      document.body.style.backgroundImage = `url(${data.blogInfo.bgUrl})`
    }
  } catch {}
}

watch(() => path.value, async () => {
  NProgress.start(); loadingStore.setRouterLoading(true)
  try { await ensureAuthForRoute() } finally { loadingStore.setRouterLoading(false); NProgress.done() }
  mobileOpen.value = false
})

onMounted(async () => {
  window.addEventListener(AUTH_EXPIRED_EVENT, handleAuthExpired)
  document.documentElement.setAttribute('data-theme', theme.value)
  loadingStore.setRouterLoading(true)
  try {
    await Promise.all([ensureAuthForRoute(), loadBackground()])
  } finally {
    loadingStore.setRouterLoading(false); NProgress.done()
  }
})

onUnmounted(() => {
  window.removeEventListener(AUTH_EXPIRED_EVENT, handleAuthExpired)
})
</script>

<template>
  <div class="app" :class="{ 'is-login': path === '/login', 'mobile-open': mobileOpen, 'sidebar-collapsed': sidebarCollapsed }">
    <MeshBackground />

    <aside v-if="path !== '/login'" class="sidebar glass-panel" :class="{ collapsed: sidebarCollapsed }" aria-label="站点导航">
      <nav class="sb-nav">
        <a class="sb-link" :class="{ active: path === '/' }" title="首页" @click="navTo('/')">
          <Home :size="18" />
          <span v-show="!sidebarCollapsed">首页</span>
        </a>
        <a class="sb-link" :class="{ active: path === '/archive' }" title="归档" @click="navTo('/archive')">
          <Search :size="18" />
          <span v-show="!sidebarCollapsed">归档</span>
        </a>
        <a v-if="user?.role === 'ADMIN'" class="sb-link" :class="{ active: path === '/write' }" title="写作" @click="navTo('/write')">
          <PenLine :size="18" />
          <span v-show="!sidebarCollapsed">写作</span>
        </a>
        <a v-if="user?.role === 'ADMIN'" class="sb-link" :class="{ active: path === '/profile' }" title="管理" @click="navTo('/profile')">
          <Settings :size="18" />
          <span v-show="!sidebarCollapsed">管理</span>
        </a>
        <a v-if="user && user.role !== 'ADMIN'" class="sb-link" :class="{ active: path === '/me' }" title="我的" @click="navTo('/me')">
          <User :size="18" />
          <span v-show="!sidebarCollapsed">我的</span>
        </a>
        <a v-if="!user" class="sb-link" :class="{ active: path === '/login' }" title="登录" @click="navTo('/login')">
          <UserPlus :size="18" />
          <span v-show="!sidebarCollapsed">登录</span>
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
          <span v-show="!sidebarCollapsed">{{ theme === 'light' ? '深色模式' : '浅色模式' }}</span>
        </button>

        <div v-if="user" class="sb-user" :title="user?.nickname || user?.username" @click="navTo(user.role === 'ADMIN' ? '/profile' : '/me')">
          <img
            :src="toAbsoluteUrl(user?.avatarUrl) || 'https://api.dicebear.com/7.x/avataaars/svg?seed=Felix'"
            :alt="user?.nickname || user?.username || '用户头像'"
            class="sb-avatar avatar-img"
            :class="{ loaded: user?.avatarUrl }"
            @load="(e) => e.target.classList.add('loaded')"
          />
          <div v-show="!sidebarCollapsed" class="sb-user-info">
            <span class="sb-username">{{ user?.nickname || user?.username }}</span>
            <span v-if="user?.role === 'ADMIN'" class="sb-badge">博主</span>
          </div>
        </div>

        <a v-if="user" class="sb-link sb-logout" title="退出登录" @click="handleLogout">
          <LogOut :size="18" />
          <span v-show="!sidebarCollapsed">退出登录</span>
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

    <button v-if="path !== '/login'" class="mobile-menu-btn" @click="mobileOpen = !mobileOpen" :aria-label="mobileOpen ? '关闭菜单' : '打开菜单'">
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
  padding-left: calc(var(--sidebar-width) + var(--space-xl));
  transition: padding-left var(--duration-normal) var(--ease-bounce);
}

.app.sidebar-collapsed:not(.is-login) {
  padding-left: calc(76px + var(--space-xl));
}

/* ============================================================
   悬浮胶囊侧边栏
   ============================================================ */
.sidebar {
  position: fixed;
  left: var(--space-md);
  top: var(--space-md);
  bottom: var(--space-md);
  width: var(--sidebar-width);
  display: flex;
  flex-direction: column;
  padding: var(--space-md) var(--space-sm);
  border-radius: var(--radius-xl);
  z-index: 200;
  overflow: hidden;
  transition: width var(--duration-normal) var(--ease-bounce),
              padding var(--duration-normal) var(--ease-bounce);
}

.sidebar.collapsed {
  width: 76px;
  padding: var(--space-md) 10px;
}

.sidebar:hover {
  transform: none;
}

.sb-nav {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 0 4px;
}

.sb-link {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  color: var(--text-secondary);
  font-size: var(--font-size-sm);
  font-weight: 500;
  text-decoration: none;
  cursor: pointer;
  border-radius: var(--radius-pill);
  border: none;
  background: none;
  width: 100%;
  text-align: left;
  transition: color var(--duration-normal) var(--ease-bounce),
              background var(--duration-normal) var(--ease-bounce),
              transform var(--duration-normal) var(--ease-bounce);
}

.sb-link:hover {
  color: var(--theme-pink-hover);
  background: rgba(244, 164, 184, 0.1);
  transform: translateX(2px);
}

.sidebar.collapsed .sb-link {
  justify-content: center;
  padding: 10px;
  gap: 0;
}

.sidebar.collapsed .sb-link:hover {
  transform: translateY(-2px);
}

.sidebar.collapsed .sb-user {
  justify-content: center;
  padding: 10px 8px;
}

.sb-link.active {
  color: var(--theme-pink-hover);
  background: rgba(244, 164, 184, 0.18);
  font-weight: 600;
  box-shadow: inset 0 0 0 1px rgba(244, 164, 184, 0.25);
}

.sb-footer {
  margin-top: auto;
  padding-top: var(--space-sm);
  border-top: 1px solid rgba(244, 164, 184, 0.15);
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.sb-user {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  border-radius: var(--radius-pill);
  cursor: pointer;
  transition: background var(--duration-normal) var(--ease-bounce);
}

.sb-user:hover {
  background: rgba(244, 164, 184, 0.1);
}

.sb-avatar {
  width: 32px;
  height: 32px;
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
  color: var(--theme-pink-hover);
}

.sb-logout:hover {
  color: var(--danger);
  background: rgba(212, 114, 122, 0.1);
}

.sb-collapse-btn {
  position: absolute;
  right: -12px;
  top: 50%;
  transform: translateY(-50%);
  width: 26px;
  height: 26px;
  border: 1px solid rgba(255, 255, 255, 0.55);
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(255,255,255,0.75), rgba(255,255,255,0.45));
  backdrop-filter: blur(8px);
  color: var(--text-muted);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  box-shadow: var(--shadow-soft);
  transition: color var(--duration-normal) var(--ease-bounce),
              border-color var(--duration-normal) var(--ease-bounce),
              transform var(--duration-normal) var(--ease-bounce);
  z-index: 10;
}

.sb-collapse-btn:hover {
  color: var(--theme-pink-hover);
  border-color: rgba(244, 164, 184, 0.45);
  transform: translateY(-50%) scale(1.08);
}

[data-theme='dark'] .sb-collapse-btn {
  background: linear-gradient(135deg, rgba(50,44,50,0.85), rgba(38,34,38,0.65));
  border-color: rgba(255, 255, 255, 0.12);
}

/* ============================================================
   主内容区
   ============================================================ */
.main-content {
  max-width: 920px;
  margin: 0 auto;
  padding: var(--space-md) var(--space-xl) var(--space-xl);
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
  padding-left: 0 !important;
}

.not-found {
  padding: 80px 40px;
  text-align: center;
  color: var(--text-muted);
}

.page-enter-active {
  transition: opacity 0.6s var(--ease-bounce), transform 0.6s var(--ease-bounce);
}
.page-leave-active {
  transition: opacity 0.35s var(--ease-bounce), transform 0.35s var(--ease-bounce);
}
.page-enter-from { opacity: 0; transform: translateY(12px); }
.page-leave-to { opacity: 0; transform: translateY(-6px); }

.mobile-menu-btn {
  display: none;
  position: fixed;
  top: 16px;
  left: 16px;
  z-index: 210;
  width: 44px;
  height: 44px;
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: var(--radius-pill);
  background: linear-gradient(135deg, rgba(255,255,255,0.65), rgba(255,255,255,0.35));
  backdrop-filter: blur(12px);
  color: var(--text-main);
  cursor: pointer;
  align-items: center;
  justify-content: center;
  box-shadow: var(--shadow-soft);
}

.mobile-overlay {
  position: fixed;
  inset: 0;
  background: rgba(92, 77, 77, 0.12);
  backdrop-filter: blur(4px);
  z-index: 190;
}

@media (max-width: 900px) {
  .app:not(.is-login),
  .app.sidebar-collapsed:not(.is-login) {
    padding-left: 0;
  }

  .sidebar,
  .sidebar.collapsed {
    left: 12px;
    top: 12px;
    bottom: 12px;
    width: min(280px, calc(100vw - 24px));
    padding: var(--space-md) var(--space-sm);
    transform: translateX(calc(-100% - 24px));
    transition: transform 0.4s var(--ease-bounce);
    box-shadow: var(--shadow-hover);
  }

  .mobile-open .sidebar,
  .mobile-open .sidebar.collapsed {
    transform: translateX(0);
  }

  .sb-collapse-btn {
    display: none;
  }

  .mobile-menu-btn {
    display: flex;
  }

  .main-content {
    padding: 72px var(--space-md) var(--space-lg);
  }
}
</style>
