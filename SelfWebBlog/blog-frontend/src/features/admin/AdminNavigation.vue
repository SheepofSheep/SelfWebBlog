<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { Home, PenLine, Pencil, LogOut, X, ChevronRight } from 'lucide-vue-next'
import AppAvatar from '@/components/AppAvatar.vue'
const props = defineProps({
  tabs: { type: Array, required: true },
  activeKey: { type: String, default: 'overview' },
  open: Boolean,
  site: { type: Object, default: null },
  user: { type: Object, default: null }
})
const emit = defineEmits(['select', 'close', 'logout'])
const menu = ref(null)
let returnFocus
const groups = computed(() => [
  {
    name: '内容工作区',
    items: props.tabs.filter((tab) => ['overview', 'content', 'interactions'].includes(tab.key))
  },
  {
    name: '资源与社区',
    items: props.tabs.filter((tab) => ['tags', 'assets', 'users'].includes(tab.key))
  },
  { name: '偏好与安全', items: props.tabs.filter((tab) => ['site', 'security'].includes(tab.key)) }
])
function close() {
  emit('close')
}
watch(
  () => props.open,
  async (open) => {
    if (open) {
      returnFocus = document.activeElement
      await nextTick()
      menu.value?.querySelector('button')?.focus()
    } else if (returnFocus?.isConnected) {
      returnFocus.focus()
      returnFocus = null
    }
  }
)
function onKeydown(event) {
  if (!props.open || !menu.value) return
  if (event.key === 'Escape') {
    event.preventDefault()
    close()
    return
  }
  if (event.key !== 'Tab') return
  const controls = [...menu.value.querySelectorAll('a[href],button:not([disabled])')].filter(
    (el) => el.getClientRects().length
  )
  const first = controls[0],
    last = controls.at(-1)
  if (event.shiftKey && document.activeElement === first) {
    event.preventDefault()
    last?.focus()
  } else if (!event.shiftKey && document.activeElement === last) {
    event.preventDefault()
    first?.focus()
  }
}
onMounted(() => document.addEventListener('keydown', onKeydown))
onBeforeUnmount(() => document.removeEventListener('keydown', onKeydown))
</script>
<template>
  <button
    v-if="open"
    class="admin-nav-scrim"
    type="button"
    aria-label="关闭管理导航"
    tabindex="-1"
    @click="close"
  />
  <aside
    id="admin-navigation"
    ref="menu"
    class="admin-sidebar glass"
    :class="{ open }"
    :role="open ? 'dialog' : undefined"
    :aria-modal="open ? 'true' : undefined"
    :aria-label="open ? '管理导航' : undefined"
  >
    <div class="admin-brand-row">
      <RouterLink class="admin-brand" to="/" aria-label="博客首页"
        ><AppAvatar
          :src="site?.avatarUrl"
          :name="site?.nickname || '博客'"
          :size="38"
          rounded
        /><span>{{ site?.nickname || '我的博客' }}<small>创作管理台</small></span></RouterLink
      ><button
        class="icon-button admin-nav-close"
        type="button"
        aria-label="收起管理导航"
        @click="close"
      >
        <X :size="18" />
      </button>
    </div>
    <RouterLink class="studio-write" to="/write"
      ><PenLine :size="17" /><span>开始一篇新文章</span><ChevronRight :size="14"
    /></RouterLink>
    <nav aria-label="管理功能">
      <section v-for="group in groups" :key="group.name">
        <p>{{ group.name }}</p>
        <button
          v-for="tab in group.items"
          :key="tab.key"
          type="button"
          :class="{ active: activeKey === tab.key }"
          :aria-current="activeKey === tab.key ? 'page' : undefined"
          @click="emit('select', tab.key)"
        >
          <component :is="tab.icon" :size="18" :stroke-width="1.7" /><span>{{ tab.label }}</span
          ><ChevronRight v-if="activeKey === tab.key" :size="14" />
        </button>
      </section>
    </nav>
    <div class="admin-sidebar-bottom">
      <RouterLink class="admin-home-link" to="/"><Home :size="16" />回到博客</RouterLink>
      <div class="admin-user">
        <AppAvatar :src="user?.avatarUrl" :name="user?.nickname || user?.username" :size="36" />
        <div>
          <strong>{{ user?.nickname || user?.username || '管理员' }}</strong
          ><small>管理员</small>
        </div>
      </div>
      <div class="admin-mobile-actions" aria-label="账户操作">
        <RouterLink class="icon-button" to="/" aria-label="返回博客首页" title="返回博客"
          ><Home :size="16" /></RouterLink
        ><RouterLink class="icon-button" to="/me" aria-label="修改昵称与头像" title="个人资料"
          ><Pencil :size="16" /></RouterLink
        ><button
          class="icon-button"
          type="button"
          aria-label="退出登录"
          title="退出登录"
          @click="emit('logout')"
        >
          <LogOut :size="16" />
        </button>
      </div>
    </div>
  </aside>
</template>
