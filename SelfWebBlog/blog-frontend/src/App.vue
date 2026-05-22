<script setup>
import { ref, computed, onMounted, onUnmounted, provide, watch, defineAsyncComponent } from 'vue'
import { useRoute, navigate } from './router'
import { getCurrentUser, getProfile, logout } from './utils/api'
import { provideToast, showToast } from './composables/toast'
import ToastHost from './components/ToastHost.vue'
import MeshBackground from './components/MeshBackground.vue'
import {
  Home, PenLine, Settings, UserPlus, LogOut, Moon, Sun,
  ChevronLeft, ChevronRight, Menu, User
} from 'lucide-vue-next'

const HomePage = defineAsyncComponent(() => import('./pages/HomePage.vue'))
const PostPage = defineAsyncComponent(() => import('./pages/PostPage.vue'))
const ProfilePage = defineAsyncComponent(() => import('./pages/ProfilePage.vue'))
const LoginPage = defineAsyncComponent(() => import('./pages/LoginPage.vue'))
const WritePage = defineAsyncComponent(() => import('./pages/WritePage.vue'))
import loadingStore from './stores/loadingStore'

const currentPage = computed(() => {
  if (path.value === '/login') return LoginPage
  if (path.value === '/') return HomePage
  if (path.value === '/write') return WritePage
  if (path.value.startsWith('/post')) return PostPage
  if (path.value === '/profile') return ProfilePage
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
const user = ref(null)
const sidebarCollapsed = ref(false)
const mobileOpen = ref(false)

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

function restoreUser() {
  const stored = localStorage.getItem('user')
  if (stored) { try { user.value = JSON.parse(stored) } catch { user.value = null } }
}

function saveUser(u) { user.value = u; localStorage.setItem('user', JSON.stringify(u)) }

async function loadUserInfo() {
  const token = localStorage.getItem('token')
  if (!token) { user.value = null; return }
  try {
    const userInfo = await getCurrentUser()
    if (userInfo?.role === 'ADMIN') {
      try {
        const profile = await getProfile()
        saveUser({
          ...userInfo,
          nickname: profile?.blogInfo?.nickname || userInfo.username,
          avatarUrl: profile?.blogInfo?.avatarUrl || userInfo.avatarUrl
        })
      } catch { saveUser(userInfo) }
    } else { saveUser(userInfo) }
  } catch {
    user.value = null
    localStorage.removeItem('token'); localStorage.removeItem('user')
  }
}

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
  const currentPath = path.value
  const ok = handleOAuthRedirect()
  if (ok) { navigate('/'); return }

  if (!user.value) {
    restoreUser()
    try { await loadUserInfo() } catch { user.value = null }
  }

  if (currentPath === '/login' || currentPath === '/' || currentPath.startsWith('/post')) return

  if (!user.value) { showToast('请先登录', 'warning'); navigate('/login') }
  else if ((currentPath === '/profile' || currentPath === '/write') && user.value?.role !== 'ADMIN') { showToast('无权访问', 'warning'); navigate('/') }
}

async function handleLogout() {
  try { await logout() } catch {}
  localStorage.removeItem('token'); localStorage.removeItem('user')
  user.value = null; showToast('已退出登录'); navigate('/login')
}

function toggleSidebar() { sidebarCollapsed.value = !sidebarCollapsed.value }

watch(() => path.value, async () => {
  NProgress.start(); loadingStore.setRouterLoading(true)
  try { await ensureAuthForRoute() } finally { loadingStore.setRouterLoading(false); NProgress.done() }
  mobileOpen.value = false
})

onMounted(async () => {
  document.documentElement.setAttribute('data-theme', theme.value)
  loadingStore.setRouterLoading(true)
  try { await ensureAuthForRoute() } finally { loadingStore.setRouterLoading(false); NProgress.done() }
})

onUnmounted(() => {})
</script>

<template>
  <div class="app" :class="{ 'sidebar-collapsed': sidebarCollapsed, 'is-login': path === '/login', 'mobile-open': mobileOpen }">
    <MeshBackground />

    <!-- 侧边栏（登录页隐藏） -->
    <aside v-if="path !== '/login'" class="sidebar" :class="{ collapsed: sidebarCollapsed }">
      <!-- 品牌区 -->
      <div class="sb-brand" @click="navigate('/')">
        <h1 v-show="!sidebarCollapsed" class="sb-title">Anon's Blog</h1>
        <span v-show="sidebarCollapsed" class="sb-mark">A</span>
      </div>

      <!-- 导航项 -->
      <nav class="sb-nav">
        <a class="sb-item" :class="{ active: path === '/' }" @click="navigate('/')">
          <Home :size="18" />
          <span class="sb-label">Home</span>
        </a>
        <a v-if="user?.role === 'ADMIN'" class="sb-item" :class="{ active: path === '/write' }" @click="navigate('/write')">
          <PenLine :size="18" />
          <span class="sb-label">Write</span>
        </a>
        <a v-if="user?.role === 'ADMIN'" class="sb-item" :class="{ active: path === '/profile' }" @click="navigate('/profile')">
          <Settings :size="18" />
          <span class="sb-label">Manage</span>
        </a>
        <a v-if="!user" class="sb-item" :class="{ active: path === '/login' }" @click="navigate('/login')">
          <UserPlus :size="18" />
          <span class="sb-label">Sign In</span>
        </a>
      </nav>

      <!-- 底部区域 -->
      <div class="sb-footer">
        <button class="sb-item sb-theme" @click="toggleTheme" aria-label="切换主题">
          <Moon v-if="theme === 'light'" :size="18" />
          <Sun v-else :size="18" />
          <span class="sb-label">Theme</span>
        </button>

        <div v-if="user" class="sb-user" @click="navigate('/profile')">
          <img
            :src="toAbsoluteUrl(user?.avatarUrl) || 'https://api.dicebear.com/7.x/avataaars/svg?seed=Felix'"
            :alt="user?.nickname || user?.username || '用户头像'"
            class="sb-avatar avatar-img"
            :class="{ loaded: user?.avatarUrl }"
            @load="(e) => e.target.classList.add('loaded')"
          />
          <div class="sb-user-info">
            <span class="sb-username">{{ user?.nickname || user?.username }}</span>
            <span v-if="user?.role === 'ADMIN'" class="sb-badge">Owner</span>
          </div>
        </div>

        <a v-if="user" class="sb-item sb-logout" @click="handleLogout">
          <LogOut :size="18" />
          <span class="sb-label">Logout</span>
        </a>
      </div>

      <!-- 折叠按钮 -->
      <button class="sb-collapse-btn" @click="toggleSidebar" :title="sidebarCollapsed ? '展开侧边栏' : '收起侧边栏'" :aria-label="sidebarCollapsed ? '展开侧边栏' : '收起侧边栏'">
        <ChevronLeft v-if="!sidebarCollapsed" :size="16" />
        <ChevronRight v-else :size="16" />
      </button>
    </aside>

    <!-- 移动端遮罩 -->
    <div v-if="mobileOpen" class="mobile-overlay" @click="mobileOpen = false"></div>

    <!-- 移动端菜单按钮 -->
    <button v-if="path !== '/login'" class="mobile-menu-btn" @click="mobileOpen = !mobileOpen" aria-label="菜单">
      <Menu :size="20" />
    </button>

    <!-- 主内容 -->
    <main :class="['main-content', { 'login-content': path === '/login' }]">
      <Transition name="page" mode="out-in">
        <KeepAlive v-if="currentPage" :include="['HomePage', 'ProfilePage']">
          <component :is="currentPage" :key="pageKey + '-' + refreshKey" />
        </KeepAlive>
        <div v-else class="not-found">Page not found</div>
      </Transition>
    </main>

    <ToastHost />
  </div>
</template>

<style scoped>
.app {
  min-height: 100vh;
  background: var(--bg);
  padding-left: 200px;
  transition: padding-left 0.25s var(--ease-out);
}
.sidebar-collapsed .app {
  padding-left: 56px;
}

/* ============================================================
   Sidebar
   ============================================================ */
.sidebar {
  position: fixed;
  top: 0; left: 0; bottom: 0;
  width: 200px;
  display: flex;
  flex-direction: column;
  background: var(--surface);
  border-right: 1px solid var(--border);
  z-index: 200;
  padding: 16px 12px 12px;
  transition: width 0.25s var(--ease-out);
  overflow: hidden;
}

.sidebar.collapsed {
  width: 56px;
}

/* Brand */
.sb-brand {
  padding: 8px 10px 20px;
  cursor: pointer;
  white-space: nowrap;
}
.sb-title {
  font-family: var(--font-sans);
  font-size: 0.95rem;
  font-weight: var(--fw-bold);
  color: var(--text);
  margin: 0;
  letter-spacing: -0.01em;
}
.sb-mark {
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: var(--font-sans);
  font-size: 1.1rem;
  font-weight: var(--fw-bold);
  color: var(--primary);
  width: 36px; height: 36px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  margin: 0 auto;
}

/* Nav items */
.sb-nav {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.sb-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 9px 10px;
  color: var(--text-muted);
  cursor: pointer;
  font-size: var(--fs-small);
  font-weight: var(--fw-medium);
  text-decoration: none;
  white-space: nowrap;
  border-radius: var(--radius-sm);
  transition: color var(--duration-fast), background var(--duration-fast);
}
.sb-item:hover {
  color: var(--text);
  background: var(--surface-muted);
}
.sb-item.active {
  color: var(--primary);
  background: var(--primary-soft);
  font-weight: var(--fw-bold);
}

.sb-label {
  transition: opacity 0.2s var(--ease-out);
}
.sidebar.collapsed .sb-label {
  display: none;
}

/* Footer */
.sb-footer {
  border-top: 1px solid var(--border);
  padding-top: 10px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.sb-user {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  cursor: pointer;
  transition: background var(--duration-fast);
}
.sb-user:hover {
  background: var(--surface-muted);
}
.sidebar.collapsed .sb-user {
  justify-content: center;
  padding: 8px 0;
}

.sb-avatar {
  width: 30px; height: 30px;
  object-fit: cover;
  flex-shrink: 0;
}
.sb-user-info {
  display: flex;
  flex-direction: column;
  gap: 1px;
  min-width: 0;
}
.sidebar.collapsed .sb-user-info {
  display: none;
}

.sb-username {
  font-size: var(--fs-small);
  font-weight: var(--fw-medium);
  color: var(--text);
  overflow: hidden;
  text-overflow: ellipsis;
}
.sb-badge {
  font-size: 0.6rem;
  font-weight: var(--fw-bold);
  color: var(--primary);
  text-transform: uppercase;
}

.sb-theme,
.sb-logout {
  border: none;
  background: none;
  width: 100%;
  text-align: left;
}

/* Collapse toggle */
.sb-collapse-btn {
  position: absolute;
  bottom: 20px;
  right: -12px;
  width: 24px; height: 24px;
  border: 1px solid var(--border);
  border-radius: 50%;
  background: var(--surface);
  color: var(--text-muted);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  transition: color var(--duration-fast), border-color var(--duration-fast);
  z-index: 10;
}
.sb-collapse-btn:hover {
  color: var(--primary);
  border-color: var(--primary);
}

/* ============================================================
   Main content
   ============================================================ */
.main-content {
  max-width: 1060px;
  margin: 0 auto;
  padding: 36px 40px;
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
  font-size: var(--fs-body);
  text-align: center;
  color: var(--ink-muted);
}

/* Page transitions */
.page-enter-active { transition: opacity 0.3s var(--ease-out), transform 0.3s var(--ease-out); }
.page-leave-active { transition: opacity 0.15s var(--ease-out), transform 0.15s var(--ease-out); }
.page-enter-from { opacity: 0; transform: translateY(8px); }
.page-leave-to { opacity: 0; transform: translateY(-4px); }

/* ============================================================
   Mobile menu button
   ============================================================ */
.mobile-menu-btn {
  display: none;
  position: fixed;
  top: 12px;
  left: 12px;
  z-index: 210;
  width: 40px;
  height: 40px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: var(--surface);
  color: var(--text);
  cursor: pointer;
  align-items: center;
  justify-content: center;
  box-shadow: var(--shadow-soft);
}
.mobile-menu-btn:active {
  background: var(--surface-muted);
}

/* ============================================================
   Mobile overlay
   ============================================================ */
.mobile-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.2);
  z-index: 190;
}

/* ============================================================
   Responsive
   ============================================================ */
@media (max-width: 768px) {
  .app {
    padding-left: 200px;
  }
  .sidebar-collapsed .app {
    padding-left: 56px;
  }
  .main-content {
    padding: 20px 16px;
    max-width: 100%;
  }
}

@media (max-width: 640px) {
  .app,
  .sidebar-collapsed .app {
    padding-left: 0;
  }
  .sidebar {
    width: 200px;
    transform: translateX(-100%);
    border-right: none;
    box-shadow: var(--shadow-raised);
  }
  .sidebar.collapsed {
    width: 200px;
    transform: translateX(-100%);
  }
  .mobile-open .sidebar,
  .mobile-open .sidebar.collapsed {
    transform: translateX(0);
  }
  .mobile-menu-btn {
    display: flex;
    left: 12px;
    top: 12px;
  }
  .main-content {
    padding: 16px 16px;
    max-width: 100%;
  }
  .sb-collapse-btn {
    display: none;
  }
}
</style>
