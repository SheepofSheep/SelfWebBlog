<script setup>
import { computed, defineAsyncComponent, provide, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AdminShell from '../features/admin/components/AdminShell.vue'
import ConfirmDialog from '../components/ConfirmDialog.vue'
import '../features/admin/admin.css'

const route = useRoute()
const router = useRouter()
const allowed = new Set([
  'overview',
  'content',
  'interactions',
  'library',
  'users',
  'site',
  'security'
])
const panels = {
  overview: defineAsyncComponent(
    () => import('../features/admin/components/AdminOverviewPanel.vue')
  ),
  content: defineAsyncComponent(() => import('../features/admin/components/AdminContentPanel.vue')),
  interactions: defineAsyncComponent(
    () => import('../features/admin/components/AdminInteractionsPanel.vue')
  ),
  library: defineAsyncComponent(() => import('../features/admin/components/AdminLibraryPanel.vue')),
  users: defineAsyncComponent(() => import('../features/admin/components/AdminUsersPanel.vue')),
  site: defineAsyncComponent(() => import('../features/admin/components/AdminSitePanel.vue')),
  security: defineAsyncComponent(
    () => import('../features/admin/components/AdminSecurityPanel.vue')
  )
}
const section = computed({
  get: () => (allowed.has(String(route.query.section)) ? String(route.query.section) : 'overview'),
  set: (value) =>
    router.replace({ query: { ...route.query, section: value === 'overview' ? undefined : value } })
})
const panel = computed(() => panels[section.value])
const dialog = reactive({
  open: false,
  title: '',
  message: '',
  confirmText: '',
  requiredText: '',
  resolve: null
})

function confirmAction(options) {
  if (dialog.resolve) dialog.resolve(false)
  Object.assign(dialog, {
    open: true,
    title: options.title,
    message: options.message,
    confirmText: options.confirmText || '确认',
    requiredText: options.requiredText || '',
    resolve: null
  })
  return new Promise((resolve) => {
    dialog.resolve = resolve
  })
}
function finishConfirm(value) {
  dialog.open = false
  const resolve = dialog.resolve
  dialog.resolve = null
  resolve?.(value)
}

provide('adminConfirm', confirmAction)
</script>

<template>
  <div class="admin-page">
    <AdminShell v-model:section="section" @write="router.push('/write')">
      <Suspense>
        <component :is="panel" @navigate="section = $event" />
        <template #fallback><div class="admin-loading">正在打开管理模块</div></template>
      </Suspense>
    </AdminShell>
    <ConfirmDialog
      v-model="dialog.open"
      :title="dialog.title"
      :message="dialog.message"
      :confirm-text="dialog.confirmText"
      :required-text="dialog.requiredText"
      cancel-text="取消"
      @confirm="finishConfirm(true)"
      @cancel="finishConfirm(false)"
    />
  </div>
</template>
