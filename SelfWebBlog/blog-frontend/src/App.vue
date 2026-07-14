<script setup>
import { computed, nextTick, onMounted, onUnmounted, provide, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { AUTH_EXPIRED_EVENT, exchangeOAuthTicket, getProfile } from './api'
import { provideToast, showToast } from './composables/toast'
import { useAuthStore } from './stores/authStore'
import loadingStore from './stores/loadingStore'
import SiteHeader from './components/SiteHeader.vue'
import ToastHost from './components/ToastHost.vue'
import PointerTrail from './components/ui/PointerTrail.vue'

NProgress.configure({ showSpinner: false, trickleSpeed: 120, ease: 'ease', speed: 220 })
provideToast()

const route = useRoute()
const router = useRouter()
const { user, saveUser, clearUserState, loadUserInfo, logoutUser } = useAuthStore()
const siteInfo = ref(null)
const refreshKey = ref(0)
const isLoginPage = computed(() => route.name === 'login')

provide('user', user)
provide('refreshUser', loadUserInfo)
provide('loadingStore', loadingStore)
provide('refreshHome', async () => {
  refreshKey.value += 1
  await loadSiteInfo()
})

router.beforeEach(() => {
  NProgress.start()
  loadingStore.setRouterLoading(true)
})

router.afterEach(async () => {
  loadingStore.setRouterLoading(false)
  NProgress.done()
  await nextTick()
  const heading = document.querySelector('#main-content h1')
  if (heading) {
    heading.setAttribute('tabindex', '-1')
    heading.focus({ preventScroll: true })
  }
})

router.onError(() => {
  loadingStore.setRouterLoading(false)
  NProgress.done()
})

async function handleOAuthRedirect() {
  const error = route.query.error
  if (error) {
    showToast('GitHub 登录失败，请重试。', 'error')
    await router.replace({ name: 'login' })
    return
  }

  const ticket = route.query.ticket
  if (!ticket) return

  try {
    const data = await exchangeOAuthTicket(String(ticket))
    saveUser(data.user)
    showToast('GitHub 登录成功', 'success')
    await router.replace({ name: 'home' })
  } catch (error_) {
    showToast(error_?.message || '登录票据已过期，请重新登录。', 'error')
    await router.replace({ name: 'login' })
  }
}

async function loadSiteInfo() {
  try {
    const data = await getProfile({ page: 1, size: 1 })
    siteInfo.value = data?.blogInfo || null
  } catch {
    siteInfo.value = null
  }
}

async function handleLogout() {
  await logoutUser()
  showToast('已安全退出登录')
  await router.push({ name: 'home' })
}

function handleAuthExpired() {
  if (!user.value) return
  clearUserState()
  showToast('登录状态已失效，请重新登录。', 'warning')
  if (!route.meta.public) router.push({ name: 'login', query: { redirect: route.fullPath } })
}

onMounted(async () => {
  window.addEventListener(AUTH_EXPIRED_EVENT, handleAuthExpired)
  await Promise.all([handleOAuthRedirect(), loadSiteInfo()])
})

onUnmounted(() => window.removeEventListener(AUTH_EXPIRED_EVENT, handleAuthExpired))
</script>

<template>
  <div :class="['app-shell', { 'login-shell': isLoginPage }]">
    <a class="skip-link" href="#main-content">跳到主要内容</a>
    <SiteHeader
      v-if="!isLoginPage"
      :path="route.path"
      :user="user"
      :site-info="siteInfo"
      @logout="handleLogout"
    />

    <main id="main-content" :class="['page-shell', { 'login-page-shell': isLoginPage }]">
      <RouterView v-slot="{ Component, route: viewRoute }">
        <Transition name="page" mode="out-in">
          <KeepAlive :include="['HomePage']">
            <component :is="Component" :key="`${viewRoute.fullPath}-${refreshKey}`" />
          </KeepAlive>
        </Transition>
      </RouterView>
    </main>

    <ToastHost />
    <PointerTrail />
  </div>
</template>

<style scoped>
.app-shell {
  min-height: 100vh;
  padding-top: 70px;
}
.login-shell {
  padding-top: 0;
}
.page-shell {
  width: 100%;
  min-height: calc(100vh - 70px);
}
.login-page-shell {
  min-height: 100vh;
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
    padding-top: 64px;
  }
  .login-shell {
    padding-top: 0;
  }
  .page-shell {
    min-height: calc(100vh - 64px);
  }
}
</style>
