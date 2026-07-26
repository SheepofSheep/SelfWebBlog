<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  BarChart3,
  FileText,
  Home,
  Image,
  LogOut,
  MessageSquare,
  Pencil,
  Plus,
  Settings,
  ShieldCheck,
  SunMoon,
  Tags,
  Users
} from 'lucide-vue-next'
import { api } from '@/api/client.js'
import { appStore, notify } from '@/stores/app.js'
import AppAvatar from '@/components/AppAvatar.vue'
import AppearanceControl from '@/components/AppearanceControl.vue'
import OverviewPanel from '@/features/admin/OverviewPanel.vue'
import ContentPanel from '@/features/admin/ContentPanel.vue'
import InteractionsPanel from '@/features/admin/InteractionsPanel.vue'
import TagsPanel from '@/features/admin/TagsPanel.vue'
import MediaPanel from '@/features/admin/MediaPanel.vue'
import UsersPanel from '@/features/admin/UsersPanel.vue'
import SitePanel from '@/features/admin/SitePanel.vue'
import SecurityPanel from '@/features/admin/SecurityPanel.vue'

const route = useRoute()
const router = useRouter()
const appearanceOpen = ref(false)
const appearanceTrigger = ref(null)
const tabs = [
  { key: 'overview', label: '概览', icon: BarChart3, component: OverviewPanel },
  { key: 'content', label: '文章', icon: FileText, component: ContentPanel },
  { key: 'interactions', label: '评论', icon: MessageSquare, component: InteractionsPanel },
  { key: 'tags', label: '标签', icon: Tags, component: TagsPanel },
  { key: 'assets', label: '媒体', icon: Image, component: MediaPanel },
  { key: 'users', label: '用户', icon: Users, component: UsersPanel },
  { key: 'site', label: '站点', icon: Settings, component: SitePanel },
  { key: 'security', label: '安全', icon: ShieldCheck, component: SecurityPanel }
]
const activeTab = computed(() => tabs.find((tab) => tab.key === route.query.tab) || tabs[0])

function select(key) {
  router.replace({ query: { ...route.query, tab: key } })
}

async function logout() {
  try {
    await api.logout()
    appStore.user = null
    router.push('/')
  } catch (error) {
    notify(error.message)
  }
}

watch(
  () => appStore.ready,
  (ready) => {
    if (!ready) return
    if (appStore.user?.role !== 'ADMIN') {
      router.replace({ path: '/login', query: { redirect: route.fullPath } })
      return
    }
    if (!tabs.some((tab) => tab.key === route.query.tab)) select('overview')
  },
  { immediate: true }
)
</script>

<template>
  <div class="admin-page">
    <aside class="admin-sidebar glass">
      <RouterLink class="admin-brand" to="/" aria-label="返回博客首页">
        <AppAvatar
          :src="appStore.site?.avatarUrl"
          :name="appStore.site?.nickname || '博客'"
          :size="38"
          rounded
        />
        <span>{{ appStore.site?.nickname || '博客' }}</span>
      </RouterLink>

      <nav aria-label="管理功能">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          type="button"
          :class="{ active: activeTab.key === tab.key }"
          :aria-current="activeTab.key === tab.key ? 'page' : undefined"
          :title="tab.label"
          @click="select(tab.key)"
        >
          <component :is="tab.icon" :size="18" />
          <span>{{ tab.label }}</span>
        </button>
      </nav>

      <div class="admin-mobile-actions" aria-label="账户操作">
        <RouterLink class="icon-button" to="/" aria-label="返回博客首页">
          <Home :size="17" />
        </RouterLink>
        <RouterLink class="icon-button" to="/me" aria-label="修改昵称与头像">
          <Pencil :size="16" />
        </RouterLink>
        <button class="icon-button" type="button" aria-label="退出登录" @click="logout">
          <LogOut :size="17" />
        </button>
      </div>

      <div class="admin-user">
        <AppAvatar
          :src="appStore.user?.avatarUrl"
          :name="appStore.user?.nickname || appStore.user?.username"
          :size="38"
          rounded
        />
        <div>
          <strong>{{ appStore.user?.nickname || appStore.user?.username }}</strong>
          <small>管理员</small>
        </div>
        <RouterLink class="icon-button" to="/me" aria-label="修改昵称与头像">
          <Pencil :size="16" />
        </RouterLink>
        <button class="icon-button" type="button" aria-label="退出登录" @click="logout">
          <LogOut :size="17" />
        </button>
      </div>
    </aside>

    <main class="admin-main">
      <header class="admin-top">
        <div>
          <span class="eyebrow">ADMIN</span>
          <h1>{{ activeTab.label }}</h1>
        </div>
        <div class="admin-top-actions">
          <div class="admin-appearance">
            <button
              ref="appearanceTrigger"
              class="icon-button glass"
              type="button"
              aria-label="外观"
              :aria-expanded="appearanceOpen"
              aria-haspopup="dialog"
              @click="appearanceOpen = !appearanceOpen"
            >
              <SunMoon :size="18" />
            </button>
            <AppearanceControl
              v-if="appearanceOpen"
              :trigger="appearanceTrigger"
              @close="appearanceOpen = false"
            />
          </div>
          <RouterLink v-if="activeTab.key === 'content'" class="primary-button" to="/write">
            <Plus :size="17" />新文章
          </RouterLink>
        </div>
      </header>
      <component :is="activeTab.component" :key="activeTab.key" />
    </main>
  </div>
</template>

<style scoped>
.admin-brand {
  display: flex;
  gap: 10px;
  align-items: center;
  min-width: 0;
  padding: 7px;
  font-weight: 800;
}
.admin-brand > span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.admin-user > :deep(.app-avatar) {
  flex: 0 0 auto;
}
.admin-mobile-actions {
  display: none;
}
.admin-top-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}
.admin-appearance {
  position: relative;
}
.admin-appearance :deep(.appearance-popover) {
  z-index: 80;
  top: 46px;
  right: 0;
}
.admin-top-actions .primary-button {
  display: inline-flex;
  gap: 7px;
  align-items: center;
}
@media (max-width: 980px) {
  .admin-brand > span,
  .admin-sidebar nav button span,
  .admin-user > div,
  .admin-user > :deep(.app-avatar) {
    display: none;
  }
  .admin-brand {
    justify-content: center;
  }
  .admin-user {
    flex-direction: column;
    padding: 6px;
  }
}
@media (max-width: 720px) {
  .admin-brand,
  .admin-user {
    display: none;
  }
  .admin-top {
    align-items: flex-start;
  }
  .admin-sidebar nav {
    min-width: 0;
  }
  .admin-mobile-actions {
    display: flex;
    flex: 0 0 auto;
    gap: 2px;
    margin-left: 4px;
    padding-left: 5px;
    border-left: 1px solid var(--line);
  }
  .admin-mobile-actions .icon-button {
    width: 38px;
    height: 38px;
  }
}
</style>
