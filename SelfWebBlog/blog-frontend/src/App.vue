<script setup>
import { computed, defineAsyncComponent, onMounted, onUnmounted, provide, ref, watch } from 'vue'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { AUTH_EXPIRED_EVENT, exchangeOAuthTicket, getProfile } from './api'
import { provideToast, showToast } from './composables/toast'
import { getRouteMeta, navigate, useRoute } from './router'
import { useAuthStore } from './stores/authStore'
import loadingStore from './stores/loadingStore'
import SiteHeader from './components/SiteHeader.vue'
import ToastHost from './components/ToastHost.vue'

const HomePage = defineAsyncComponent(() => import('./pages/HomePage.vue'))
const PostPage = defineAsyncComponent(() => import('./pages/PostPage.vue'))
const ProfilePage = defineAsyncComponent(() => import('./pages/ProfilePage.vue'))
const LoginPage = defineAsyncComponent(() => import('./pages/LoginPage.vue'))
const WritePage = defineAsyncComponent(() => import('./pages/WritePage.vue'))
const VisitorProfilePage = defineAsyncComponent(() => import('./pages/VisitorProfilePage.vue'))
const ArchivePage = defineAsyncComponent(() => import('./pages/ArchivePage.vue'))

NProgress.configure({ showSpinner: false, trickleSpeed: 120, ease: 'ease', speed: 220 })
provideToast()

const { path, query } = useRoute()
const { user, restoreUser, saveUser, clearUserState, loadUserInfo, logoutUser } = useAuthStore()
const routeReady = ref(false)
const siteInfo = ref(null)
const refreshKey = ref(0)
const isRouteChecking = computed(() => !routeReady.value && !getRouteMeta(path.value).public)

const currentPage = computed(() => {
  if (isRouteChecking.value) return null
  if (path.value === '/') return HomePage
  if (path.value === '/archive') return ArchivePage
  if (path.value === '/login') return LoginPage
  if (path.value === '/write') return WritePage
  if (path.value.startsWith('/post')) return PostPage
  if (path.value === '/profile') return ProfilePage
  if (path.value === '/me') return VisitorProfilePage
  return null
})

const pageKey = computed(() => {
  if (path.value.startsWith('/post')) return `post-${query.value.get('id') || ''}`
  if (path.value === '/archive') return `archive-${query.value.toString()}`
  return path.value
})

provide('user', user)
provide('refreshUser', loadUserInfo)
provide('loadingStore', loadingStore)
provide('refreshHome', async () => {
  refreshKey.value++
  await loadSiteInfo()
})

async function handleOAuthRedirect() {
  const error = query.value.get('error')
  if (error) {
    showToast('GitHub 登录失败，请重试。', 'error')
    return true
  }

  const ticket = query.value.get('ticket')
  if (!ticket) return false

  try {
    const data = await exchangeOAuthTicket(ticket)
    localStorage.setItem('token', data.token)
    saveUser(data.user)
    showToast('GitHub 登录成功', 'success')
  } catch (error_) {
    showToast(error_?.message || '登录票据已过期，请重新登录。', 'error')
  }
  return true
}

async function ensureAuthForRoute() {
  routeReady.value = false
  const meta = getRouteMeta(path.value)
  if (await handleOAuthRedirect()) {
    navigate(user.value ? '/' : '/login')
    return false
  }

  if (!user.value) {
    restoreUser()
    if (localStorage.getItem('token')) {
      try {
        await loadUserInfo()
      } catch {
        clearUserState()
      }
    }
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

async function loadSiteInfo() {
  try {
    const data = await getProfile({ page: 1, size: 1 })
    siteInfo.value = data?.blogInfo || null
    if (data?.blogInfo?.bgUrl) {
      document.documentElement.style.setProperty('--user-bg-image', `url("${data.blogInfo.bgUrl}")`)
    } else {
      document.documentElement.style.removeProperty('--user-bg-image')
    }
  } catch {
    siteInfo.value = null
  }
}

async function handleLogout() {
  await logoutUser()
  showToast('已安全退出登录')
  navigate('/')
}

function handleAuthExpired() {
  if (!user.value && !localStorage.getItem('token')) return
  clearUserState()
  showToast('登录状态已失效，请重新登录。', 'warning')
  if (!getRouteMeta(path.value).public) navigate('/login')
}

async function prepareRoute() {
  NProgress.start()
  loadingStore.setRouterLoading(true)
  try {
    await ensureAuthForRoute()
  } finally {
    loadingStore.setRouterLoading(false)
    NProgress.done()
  }
}

watch(() => path.value, prepareRoute)

onMounted(async () => {
  window.addEventListener(AUTH_EXPIRED_EVENT, handleAuthExpired)
  await Promise.all([prepareRoute(), loadSiteInfo()])
})

onUnmounted(() => window.removeEventListener(AUTH_EXPIRED_EVENT, handleAuthExpired))
</script>

<template>
  <div :class="['app-shell', { 'login-shell': path === '/login' }]">
    <SiteHeader
      v-if="path !== '/login'"
      :path="path"
      :user="user"
      :site-info="siteInfo"
      @navigate="navigate"
      @logout="handleLogout"
    />

    <main :class="['page-shell', { 'login-page-shell': path === '/login' }]">
      <Transition name="page" mode="out-in">
        <KeepAlive v-if="currentPage" :include="['HomePage', 'ProfilePage']">
          <component :is="currentPage" :key="`${pageKey}-${refreshKey}`" />
        </KeepAlive>
        <div v-else-if="isRouteChecking" class="route-state">正在确认登录状态...</div>
        <div v-else class="route-state">页面未找到</div>
      </Transition>
    </main>

    <ToastHost />
  </div>
</template>

<style scoped>
.app-shell {
  min-height: 100vh;
  padding-top: 88px;
}
.login-shell {
  padding-top: 0;
}
.page-shell {
  width: 100%;
  min-height: calc(100vh - 88px);
}
.login-page-shell {
  min-height: 100vh;
}
.route-state {
  display: grid;
  min-height: 60vh;
  place-items: center;
  color: var(--text-muted);
}
.page-enter-active,
.page-leave-active {
  transition:
    opacity var(--motion-normal),
    transform var(--motion-normal);
}
.page-enter-from {
  opacity: 0;
  transform: translateY(8px);
}
.page-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}
@media (max-width: 820px) {
  .app-shell {
    padding-top: 76px;
  }
  .login-shell {
    padding-top: 0;
  }
  .page-shell {
    min-height: calc(100vh - 76px);
  }
}
</style>
