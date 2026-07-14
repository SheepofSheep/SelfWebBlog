<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  Archive,
  BookOpen,
  LogIn,
  LogOut,
  Menu,
  MessageSquareText,
  Settings,
  User,
  X
} from 'lucide-vue-next'
import AppearanceControl from './AppearanceControl.vue'
import IconButton from './ui/IconButton.vue'
import { toAbsoluteUrl } from '../utils/url'

const props = defineProps({
  path: { type: String, required: true },
  user: { type: Object, default: null },
  siteInfo: { type: Object, default: null }
})
const emit = defineEmits(['logout'])
const router = useRouter()
const mobileOpen = ref(false)

const brandName = computed(() => props.siteInfo?.nickname || 'Gabriel')
const brandAvatar = computed(() =>
  toAbsoluteUrl(props.siteInfo?.avatarUrl || props.user?.avatarUrl || '')
)
const navItems = computed(() => {
  const items = [
    { path: '/', label: '首页', icon: BookOpen },
    { path: '/archive', label: '归档', icon: Archive },
    { path: '/guestbook', label: '留言', icon: MessageSquareText },
    { path: '/about', label: '关于', icon: User }
  ]
  if (props.user?.role === 'ADMIN') {
    items.push({ path: '/admin', label: '工作台', icon: Settings })
  } else if (props.user) {
    items.push({ path: '/me', label: '我的', icon: User })
  } else {
    items.push({ path: '/login', label: '登录', icon: LogIn })
  }
  return items
})

function go(path) {
  mobileOpen.value = false
  router.push(path)
}
</script>

<template>
  <header class="site-header">
    <div class="header-inner page-width">
      <button class="site-brand" type="button" @click="go('/')">
        <span class="brand-avatar">
          <img v-if="brandAvatar" :src="brandAvatar" :alt="brandName" />
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
        <AppearanceControl />
        <IconButton v-if="user" label="退出登录" @click="emit('logout')">
          <LogOut :size="17" />
        </IconButton>
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
  </header>
</template>

<style scoped>
.site-header {
  position: fixed;
  inset: 0 0 auto;
  z-index: 220;
  border-bottom: 1px solid var(--border-subtle);
  background: var(--surface-glass);
  backdrop-filter: blur(18px) saturate(1.05);
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
</style>
