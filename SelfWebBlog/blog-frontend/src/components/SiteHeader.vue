<script setup>
import { computed, ref } from 'vue'
import { Archive, Home, LogIn, LogOut, Menu, PenLine, Settings, User, X } from 'lucide-vue-next'
import AppearanceControl from './AppearanceControl.vue'
import { toAbsoluteUrl } from '../utils/url'

const props = defineProps({
  path: { type: String, required: true },
  user: { type: Object, default: null },
  siteInfo: { type: Object, default: null }
})
const emit = defineEmits(['navigate', 'logout'])
const mobileOpen = ref(false)

const brandName = computed(() => props.siteInfo?.nickname || 'Gabriel')
const brandAvatar = computed(() =>
  toAbsoluteUrl(props.siteInfo?.avatarUrl || props.user?.avatarUrl || '')
)
const navItems = computed(() => {
  const items = [
    { path: '/', label: '首页', icon: Home },
    { path: '/archive', label: '归档', icon: Archive }
  ]
  if (props.user?.role === 'ADMIN') {
    items.push({ path: '/write', label: '写作', icon: PenLine })
    items.push({ path: '/profile', label: '管理', icon: Settings })
  } else if (props.user) {
    items.push({ path: '/me', label: '我的', icon: User })
  } else {
    items.push({ path: '/login', label: '登录', icon: LogIn })
  }
  return items
})

function go(path) {
  mobileOpen.value = false
  emit('navigate', path)
}
</script>

<template>
  <header class="site-header">
    <div class="header-inner">
      <button class="site-brand" type="button" @click="go('/')">
        <span class="brand-avatar">
          <img v-if="brandAvatar" :src="brandAvatar" :alt="brandName" />
          <span v-else>G</span>
        </span>
        <span class="brand-text">
          <strong>Gabriel</strong>
          <small>notes on code &amp; life</small>
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
          <component :is="item.icon" :size="16" />
          {{ item.label }}
        </button>
      </nav>

      <div class="header-actions">
        <AppearanceControl />
        <button
          v-if="user"
          class="header-icon"
          type="button"
          aria-label="退出登录"
          title="退出登录"
          @click="emit('logout')"
        >
          <LogOut :size="17" />
        </button>
        <button
          class="menu-toggle"
          type="button"
          :aria-expanded="mobileOpen"
          :aria-label="mobileOpen ? '关闭菜单' : '打开菜单'"
          @click="mobileOpen = !mobileOpen"
        >
          <X v-if="mobileOpen" :size="20" />
          <Menu v-else :size="20" />
        </button>
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
  padding: 12px 18px 0;
  pointer-events: none;
}
.header-inner {
  width: min(1180px, 100%);
  min-height: 58px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: minmax(190px, 1fr) auto minmax(190px, 1fr);
  align-items: center;
  gap: 20px;
  padding: 8px 10px 8px 14px;
  border: 1px solid var(--border-light);
  border-radius: 10px;
  background: color-mix(in srgb, var(--surface-base) 88%, transparent);
  box-shadow: 0 8px 28px rgba(28, 20, 10, 0.08);
  backdrop-filter: blur(18px) saturate(1.1);
  pointer-events: auto;
}
.site-brand {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 10px;
  border: 0;
  background: transparent;
  color: var(--text-main);
  text-align: left;
  cursor: pointer;
}
.brand-avatar {
  display: grid;
  width: 38px;
  height: 38px;
  flex: 0 0 auto;
  place-items: center;
  overflow: hidden;
  border-radius: 8px;
  background: var(--primary);
  color: var(--on-primary);
  font-family: var(--font-serif);
  font-weight: 900;
}
.brand-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.brand-text {
  display: grid;
  min-width: 0;
}
.brand-text strong {
  font-family: var(--font-serif);
  font-size: 1rem;
  line-height: 1.1;
}
.brand-text small {
  overflow: hidden;
  color: var(--text-muted);
  font-size: 0.66rem;
  text-overflow: ellipsis;
  white-space: nowrap;
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
  font-size: 0.8rem;
  font-weight: 700;
  cursor: pointer;
}
.desktop-nav button {
  min-height: 40px;
  padding: 0 12px;
  border-radius: 7px;
}
.desktop-nav button:hover,
.desktop-nav button.active {
  background: var(--surface-muted);
  color: var(--text-main);
}
.desktop-nav button.active {
  box-shadow: inset 0 -2px var(--primary);
}
.header-actions {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 2px;
}
.header-icon,
.menu-toggle {
  display: grid;
  width: 40px;
  height: 40px;
  place-items: center;
  border: 0;
  border-radius: 50%;
  background: transparent;
  color: var(--text-main);
  cursor: pointer;
}
.header-icon:hover,
.menu-toggle:hover {
  background: var(--surface-muted);
}
.menu-toggle {
  display: none;
}
.mobile-nav {
  width: min(360px, calc(100% - 36px));
  margin: 8px auto 0;
  display: grid;
  gap: 4px;
  padding: 8px;
  border: 1px solid var(--border-medium);
  border-radius: 10px;
  background: var(--surface-strong);
  box-shadow: var(--shadow-lift);
  pointer-events: auto;
}
.mobile-nav button {
  min-height: 46px;
  padding: 0 14px;
  border-radius: 7px;
}
.mobile-nav button.active {
  background: var(--surface-muted);
  color: var(--primary-hover);
}
.drawer-enter-active,
.drawer-leave-active {
  transition:
    opacity var(--motion-fast),
    transform var(--motion-normal);
}
.drawer-enter-from,
.drawer-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
@media (max-width: 820px) {
  .site-header {
    padding: 10px 12px 0;
  }
  .header-inner {
    min-height: 54px;
    grid-template-columns: 1fr auto;
    padding: 6px 7px 6px 10px;
  }
  .desktop-nav {
    display: none;
  }
  .menu-toggle {
    display: grid;
  }
  .brand-text small {
    display: none;
  }
}
</style>
