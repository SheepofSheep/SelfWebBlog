<script setup>
import { ref } from 'vue'
import {
  Home,
  BookOpen,
  Archive,
  MessagesSquare,
  UserRound,
  Tags,
  Search,
  SunMoon,
  PenLine,
  PanelLeftOpen,
  PanelLeftClose
} from 'lucide-vue-next'
import SearchDialog from './SearchDialog.vue'
import AppearanceControl from './AppearanceControl.vue'
import AppAvatar from './AppAvatar.vue'
import { appStore } from '@/stores/app.js'
defineProps({ siteName: { type: String, default: '我的博客' } })
const searchOpen = ref(false)
const appearanceOpen = ref(false)
const appearanceTrigger = ref(null)
const nav = [
  { label: '首页', path: '/', icon: Home },
  { label: '文章', path: '/articles', icon: BookOpen },
  { label: '归档', path: '/archive', icon: Archive },
  { label: '标签', path: '/tags', icon: Tags },
  { label: '留言', path: '/guestbook', icon: MessagesSquare },
  { label: '关于', path: '/about', icon: UserRound }
]
function openSearch() {
  appearanceOpen.value = false
  searchOpen.value = true
}
function toggleAppearance() {
  searchOpen.value = false
  appearanceOpen.value = !appearanceOpen.value
}
</script>
<template>
  <aside class="public-navigation glass" :class="{ expanded: appStore.navigationExpanded }">
    <RouterLink class="navigation-mark" to="/" aria-label="返回首页" :title="siteName"
      ><AppAvatar :src="appStore.site?.avatarUrl" :name="siteName" :size="40" rounded
    /></RouterLink>
    <nav id="public-main-nav" class="main-nav" aria-label="主导航">
      <RouterLink
        v-for="item in nav"
        :key="item.path"
        :to="item.path"
        :title="item.label"
        :aria-label="item.label"
        :data-label="item.label"
        ><component :is="item.icon" :size="22" :stroke-width="1.6" /><span>{{
          item.label
        }}</span></RouterLink
      >
    </nav>
    <button
      class="navigation-expand icon-button"
      type="button"
      :aria-label="appStore.navigationExpanded ? '收起导航' : '展开导航'"
      :aria-expanded="appStore.navigationExpanded"
      aria-controls="public-main-nav"
      @click="appStore.navigationExpanded = !appStore.navigationExpanded"
    >
      <PanelLeftClose v-if="appStore.navigationExpanded" :size="20" />
      <PanelLeftOpen v-else :size="20" />
      <span aria-hidden="true">收起导航</span>
    </button>
    <RouterLink
      class="navigation-account"
      :to="appStore.user ? (appStore.user.role === 'ADMIN' ? '/admin' : '/me') : '/login'"
      :aria-label="appStore.user?.role === 'ADMIN' ? '管理博客' : '登录博客'"
      :title="appStore.user?.role === 'ADMIN' ? '管理博客' : '我的账户'"
      ><UserRound :size="22"
    /></RouterLink>
  </aside>
  <header class="site-header">
    <RouterLink class="site-greeting" to="/"
      ><span>{{ appStore.user ? '欢迎回来' : '欢迎来到' }}</span
      ><strong>{{ siteName }}</strong></RouterLink
    >
    <div class="header-actions">
      <RouterLink
        v-if="appStore.user?.role === 'ADMIN'"
        class="round-tool"
        to="/write"
        aria-label="写文章"
        title="写文章"
        ><PenLine :size="20"
      /></RouterLink>
      <button
        class="round-tool"
        type="button"
        aria-label="搜索"
        title="搜索文章"
        @click="openSearch"
      >
        <Search :size="20" />
      </button>
      <div class="appearance-anchor">
        <button
          ref="appearanceTrigger"
          class="round-tool appearance-trigger"
          type="button"
          aria-label="外观"
          title="外观"
          :aria-expanded="appearanceOpen"
          aria-haspopup="dialog"
          @click="toggleAppearance"
        >
          <SunMoon :size="20" />
        </button>
        <AppearanceControl
          v-if="appearanceOpen"
          :trigger="appearanceTrigger"
          @close="appearanceOpen = false"
        />
      </div>
      <RouterLink
        class="round-tool account-tool"
        :to="appStore.user ? (appStore.user.role === 'ADMIN' ? '/admin' : '/me') : '/login'"
        aria-label="账户"
        ><AppAvatar
          :src="appStore.user?.avatarUrl || appStore.site?.avatarUrl"
          :name="appStore.user?.nickname || siteName"
          :size="44"
      /></RouterLink>
    </div>
  </header>
  <SearchDialog v-if="searchOpen" @close="searchOpen = false" />
</template>
