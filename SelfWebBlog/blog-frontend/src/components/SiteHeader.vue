<script setup>
import { computed, onBeforeUnmount, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  Archive,
  BookOpen,
  ChevronDown,
  FileText,
  LogIn,
  LogOut,
  Menu,
  MessageSquareText,
  Search,
  Settings,
  Tags,
  User,
  X
} from 'lucide-vue-next'
import AppearanceControl from './AppearanceControl.vue'
import SiteSearchDialog from './SiteSearchDialog.vue'
import IconButton from './ui/IconButton.vue'
import { optimizedImageUrl, toAbsoluteUrl } from '../utils/url'

const props = defineProps({
  path: { type: String, required: true },
  user: { type: Object, default: null },
  siteInfo: { type: Object, default: null }
})
const emit = defineEmits(['logout'])
const router = useRouter()
const mobileOpen = ref(false)
const accountOpen = ref(false)
const searchOpen = ref(false)
const accountRoot = ref(null)

const brandName = computed(() => props.siteInfo?.nickname || 'Gabriel')
const brandAvatar = computed(() =>
  toAbsoluteUrl(props.siteInfo?.avatarUrl || props.user?.avatarUrl || '')
)
const navItems = computed(() => {
  const items = [
    { path: '/', label: '首页', icon: BookOpen },
    { path: '/archive', label: '归档', icon: Archive },
    { path: '/tags', label: '标签', icon: Tags },
    { path: '/guestbook', label: '留言', icon: MessageSquareText },
    { path: '/about', label: '关于', icon: User }
  ]
  return items
})

function go(path) {
  mobileOpen.value = false
  accountOpen.value = false
  router.push(path)
}

function closeOutside(event) {
  if (!accountRoot.value?.contains(event.target)) accountOpen.value = false
}

function logout() {
  accountOpen.value = false
  emit('logout')
}

document.addEventListener('pointerdown', closeOutside)
onBeforeUnmount(() => document.removeEventListener('pointerdown', closeOutside))
</script>

<template>
  <header class="site-header">
    <div class="header-inner page-width">
      <button class="site-brand" type="button" @click="go('/')">
        <span class="brand-avatar">
          <img v-if="brandAvatar" :src="optimizedImageUrl(brandAvatar, 160)" :alt="brandName" />
          <span v-else>G</span>
        </span>
        <span class="brand-text">
          <strong>{{ brandName }}</strong>
          <small>Personal Blog</small>
        </span>
      </button>

      <nav class="desktop-nav" aria-label="主导航">
        <button
          v-for="item in navItems"
          :key="item.path"
          type="button"
          :class="{ active: path === item.path }"
          @click="go(item.path)"
        >
          <component :is="item.icon" :size="15" />
          {{ item.label }}
        </button>
      </nav>

      <div class="header-actions">
        <IconButton label="搜索文章" @click="searchOpen = true"><Search :size="18" /></IconButton>
        <AppearanceControl />
        <div ref="accountRoot" class="account-control">
          <IconButton
            :label="user ? '账号菜单' : '登录'"
            :active="accountOpen"
            :aria-expanded="accountOpen"
            @click="accountOpen = !accountOpen"
          >
            <User v-if="user" :size="18" />
            <LogIn v-else :size="18" />
            <ChevronDown v-if="user" class="account-chevron" :size="11" />
          </IconButton>
          <Transition name="account-menu">
            <div v-if="accountOpen" class="account-menu" role="menu">
              <div v-if="user" class="account-summary">
                <strong>{{ user.nickname || user.username }}</strong>
                <span>{{ user.role === 'ADMIN' ? '博主管理员' : '已登录访客' }}</span>
              </div>
              <button
                v-if="user?.role === 'ADMIN'"
                type="button"
                role="menuitem"
                @click="go('/write')"
              >
                <FileText :size="16" />写文章
              </button>
              <button
                v-if="user?.role === 'ADMIN'"
                type="button"
                role="menuitem"
                @click="go('/admin')"
              >
                <Settings :size="16" />工作台
              </button>
              <button v-else-if="user" type="button" role="menuitem" @click="go('/me')">
                <User :size="16" />我的资料
              </button>
              <button
                v-if="user"
                class="account-logout"
                type="button"
                role="menuitem"
                @click="logout"
              >
                <LogOut :size="16" />退出登录
              </button>
              <button v-else type="button" role="menuitem" @click="go('/login')">
                <LogIn :size="16" />登录或注册
              </button>
            </div>
          </Transition>
        </div>
        <IconButton
          class="menu-toggle"
          :label="mobileOpen ? '关闭菜单' : '打开菜单'"
          :active="mobileOpen"
          @click="mobileOpen = !mobileOpen"
        >
          <X v-if="mobileOpen" :size="19" />
          <Menu v-else :size="19" />
        </IconButton>
      </div>
    </div>

    <Transition name="drawer">
      <nav v-if="mobileOpen" class="mobile-nav" aria-label="移动导航">
        <button
          v-for="item in navItems"
          :key="item.path"
          type="button"
          :class="{ active: path === item.path }"
          @click="go(item.path)"
        >
          <component :is="item.icon" :size="17" />
          <span>{{ item.label }}</span>
        </button>
      </nav>
    </Transition>
    <SiteSearchDialog :open="searchOpen" @close="searchOpen = false" />
  </header>
</template>

<style scoped>
.site-header {
  position: fixed;
  inset: 0 0 auto;
  z-index: 220;
  border-bottom: 1px solid var(--border-subtle);
  background: var(--surface-glass);
  backdrop-filter: blur(10px) saturate(1.02);
}
.header-inner {
  min-height: 70px;
  display: grid;
  grid-template-columns: minmax(180px, 1fr) auto minmax(120px, 1fr);
  align-items: center;
  gap: 20px;
}
.site-brand {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--text-primary);
  text-align: left;
  cursor: pointer;
}
.brand-avatar {
  width: 40px;
  height: 40px;
  display: grid;
  flex: 0 0 auto;
  overflow: hidden;
  place-items: center;
  border-radius: var(--radius-card);
  background: var(--accent);
  color: #2c1d06;
  font-family: var(--font-reading);
  font-weight: 900;
}
.brand-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.brand-text {
  min-width: 0;
  display: grid;
}
.brand-text strong {
  overflow: hidden;
  font-family: var(--font-reading);
  font-size: 1rem;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.brand-text small {
  color: var(--text-tertiary);
  font-size: 0.62rem;
  font-weight: 700;
  text-transform: uppercase;
}
.desktop-nav {
  display: flex;
  align-items: center;
  gap: 2px;
}
.desktop-nav button,
.mobile-nav button {
  display: flex;
  align-items: center;
  gap: 7px;
  border: 0;
  background: transparent;
  color: var(--text-secondary);
  font: inherit;
  font-size: 0.78rem;
  font-weight: 700;
  cursor: pointer;
}
.desktop-nav button {
  min-height: 40px;
  padding: 0 11px;
  border-radius: var(--radius-control);
}
.desktop-nav button:hover,
.desktop-nav button.active {
  background: var(--surface-soft);
  color: var(--text-primary);
}
.desktop-nav button.active {
  box-shadow: inset 0 -2px var(--accent);
}
.header-actions {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 2px;
}
.account-control {
  position: relative;
}
.account-control :deep(.icon-button) {
  position: relative;
}
.account-chevron {
  position: absolute;
  right: 3px;
  bottom: 5px;
}
.account-menu {
  position: absolute;
  top: calc(100% + 12px);
  right: 0;
  z-index: 270;
  width: 190px;
  padding: 7px;
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-card);
  background: var(--surface-solid);
  box-shadow: var(--shadow-float);
}
.account-summary {
  display: grid;
  gap: 2px;
  padding: 8px 9px 10px;
  border-bottom: 1px solid var(--border-subtle);
  margin-bottom: 4px;
}
.account-summary strong {
  overflow: hidden;
  color: var(--text-primary);
  font-size: 0.78rem;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.account-summary span {
  color: var(--text-tertiary);
  font-size: 0.65rem;
}
.account-menu button {
  width: 100%;
  min-height: 40px;
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 0 9px;
  border: 0;
  border-radius: var(--radius-control);
  background: transparent;
  color: var(--text-secondary);
  font: inherit;
  font-size: 0.76rem;
  font-weight: 700;
  cursor: pointer;
}
.account-menu button:hover {
  background: var(--surface-soft);
  color: var(--text-primary);
}
.account-menu .account-logout {
  color: var(--danger-color);
}
.account-menu-enter-active,
.account-menu-leave-active {
  transition:
    opacity var(--motion-fast-duration),
    transform var(--motion-fast-duration) var(--motion-ease);
}
.account-menu-enter-from,
.account-menu-leave-to {
  opacity: 0;
  transform: translateY(-5px);
}
.menu-toggle {
  display: none;
}
.mobile-nav {
  width: min(360px, calc(100% - 24px));
  margin: 0 auto 10px;
  display: grid;
  gap: 3px;
  padding: 7px;
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-card);
  background: var(--surface-solid);
  box-shadow: var(--shadow-float);
}
.mobile-nav button {
  min-height: 44px;
  padding: 0 12px;
  border-radius: var(--radius-control);
}
.mobile-nav button.active {
  background: var(--accent-soft);
  color: var(--accent-strong);
}
.drawer-enter-active,
.drawer-leave-active {
  transition:
    opacity var(--motion-fast-duration),
    transform var(--motion-normal-duration) var(--motion-ease);
}
.drawer-enter-from,
.drawer-leave-to {
  opacity: 0;
  transform: translateY(-7px);
}
@media (max-width: 860px) {
  .header-inner {
    min-height: 64px;
    grid-template-columns: minmax(0, 1fr) auto;
  }
  .desktop-nav {
    display: none;
  }
  .menu-toggle {
    display: inline-grid;
  }
}
@media (max-width: 480px) {
  .header-inner {
    gap: 8px;
  }
  .brand-avatar {
    width: 36px;
    height: 36px;
  }
  .brand-text small {
    display: none;
  }
  .account-menu {
    position: fixed;
    top: 58px;
    right: 10px;
    width: min(220px, calc(100vw - 20px));
  }
}
</style>
