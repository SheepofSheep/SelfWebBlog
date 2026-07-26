<script setup>
import { ref } from 'vue'
import { RouterLink } from 'vue-router'
import { Search, SunMoon, UserRound, PenLine, Menu, X } from 'lucide-vue-next'
import SearchDialog from './SearchDialog.vue'
import AppearanceControl from './AppearanceControl.vue'
import { appStore } from '@/stores/app.js'

defineProps({ siteName: { type: String, default: '博客' } })
const searchOpen = ref(false)
const appearanceOpen = ref(false)
const mobileOpen = ref(false)
const nav = [
  ['首页', '/'],
  ['文章', '/articles'],
  ['归档', '/archive'],
  ['留言', '/guestbook'],
  ['关于', '/about']
]
</script>

<template>
  <header class="site-header glass">
    <RouterLink class="brand" to="/" aria-label="返回首页"
      ><span class="brand-mark">◌</span><span>{{ siteName }}</span></RouterLink
    >
    <nav :class="['main-nav', { open: mobileOpen }]" aria-label="主导航">
      <RouterLink v-for="item in nav" :key="item[1]" :to="item[1]" @click="mobileOpen = false">{{
        item[0]
      }}</RouterLink>
    </nav>
    <div class="header-actions">
      <button class="icon-button" aria-label="搜索" @click="searchOpen = true">
        <Search :size="19" />
      </button>
      <button class="icon-button" aria-label="外观" @click="appearanceOpen = !appearanceOpen">
        <SunMoon :size="19" />
      </button>
      <RouterLink
        v-if="appStore.user?.role === 'ADMIN'"
        class="icon-button"
        to="/write"
        aria-label="写文章"
        ><PenLine :size="19"
      /></RouterLink>
      <RouterLink
        class="icon-button"
        :to="appStore.user ? (appStore.user.role === 'ADMIN' ? '/admin' : '/me') : '/login'"
        aria-label="账户"
        ><UserRound :size="19"
      /></RouterLink>
      <button
        class="icon-button mobile-menu"
        :aria-expanded="mobileOpen"
        aria-label="菜单"
        @click="mobileOpen = !mobileOpen"
      >
        <X v-if="mobileOpen" :size="20" /><Menu v-else :size="20" />
      </button>
    </div>
    <AppearanceControl v-if="appearanceOpen" @close="appearanceOpen = false" />
    <SearchDialog v-if="searchOpen" @close="searchOpen = false" />
  </header>
</template>
