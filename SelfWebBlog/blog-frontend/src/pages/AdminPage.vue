<script setup>
import { computed, ref, watch, onBeforeUnmount, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  BarChart3,
  FileText,
  Home,
  Image,
  MessageSquare,
  Plus,
  Settings,
  ShieldCheck,
  SunMoon,
  Menu,
  Tags,
  Users
} from 'lucide-vue-next'
import { api } from '@/api/client.js'
import { appStore, notify } from '@/stores/app.js'
import AdminNavigation from '@/features/admin/AdminNavigation.vue'
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
const navigationOpen = ref(false)
function closeNavigation(event) {
  if (event.key === 'Escape') navigationOpen.value = false
}
onMounted(() => window.addEventListener('keydown', closeNavigation))
onBeforeUnmount(() => window.removeEventListener('keydown', closeNavigation))
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
const descriptions = {
  overview: '创作、互动与站点近况，一眼掌握。',
  content: '管理已发布文章、草稿和归档。',
  interactions: '按文章或留言板集中审核与回复。',
  tags: '整理内容，让每个主题都容易找到。',
  assets: '查看上传素材、引用位置与存储占用。',
  users: '管理账户、展示头衔与互动身份。',
  site: '定制博客资料、上传背景与导航链接。',
  security: '管理账户安全与登录保护。'
}
const activeTab = computed(() => tabs.find((tab) => tab.key === route.query.tab) || tabs[0])

function select(key) {
  navigationOpen.value = false
  router.replace({ query: { tab: key } })
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
    <AdminNavigation
      :tabs="tabs"
      :active-key="activeTab.key"
      :open="navigationOpen"
      :site="appStore.site"
      :user="appStore.user"
      @select="select"
      @close="navigationOpen = false"
      @logout="logout"
    />
    <div class="admin-workspace">
      <header class="admin-top">
        <div class="admin-title-group">
          <button
            class="round-tool admin-menu-button"
            type="button"
            aria-label="管理导航"
            aria-controls="admin-navigation"
            :aria-expanded="navigationOpen"
            @click="navigationOpen = !navigationOpen"
          >
            <Menu :size="20" />
          </button>
          <div>
            <p class="workspace-context">
              {{ appStore.site?.nickname || '我的博客' }} <span>/ 创作管理台</span>
            </p>
            <h1>{{ activeTab.label }}</h1>
          </div>
        </div>
        <div class="admin-top-actions">
          <RouterLink class="secondary-button studio-back" to="/"
            ><Home :size="16" /><span>查看博客</span></RouterLink
          >
          <div class="admin-appearance">
            <button
              ref="appearanceTrigger"
              class="round-tool"
              type="button"
              aria-label="外观"
              :aria-expanded="appearanceOpen"
              aria-haspopup="dialog"
              @click="appearanceOpen = !appearanceOpen"
            >
              <SunMoon :size="19" /></button
            ><AppearanceControl
              v-if="appearanceOpen"
              :trigger="appearanceTrigger"
              @close="appearanceOpen = false"
            />
          </div>
          <RouterLink class="primary-button new-post-action" to="/write"
            ><Plus :size="17" /><span>写文章</span></RouterLink
          >
        </div>
      </header>
      <div class="workspace-description">
        <p>{{ descriptions[activeTab.key] }}</p>
        <span class="workspace-live"><i />管理空间</span>
      </div>
      <section class="admin-main" :aria-label="activeTab.label + '管理内容'">
        <component :is="activeTab.component" :key="activeTab.key" />
      </section>
    </div>
  </div>
</template>
