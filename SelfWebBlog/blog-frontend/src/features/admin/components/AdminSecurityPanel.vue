<script setup>
import { onMounted, ref } from 'vue'
import { RefreshCw, ShieldCheck } from 'lucide-vue-next'
import { listSecurityEvents } from '../../../api/admin'
import { formatTime } from '../../../utils/format'
import AdminSectionHeader from './AdminSectionHeader.vue'
const events = ref([])
const loading = ref(true)
async function load() {
  loading.value = true
  try {
    events.value = await listSecurityEvents()
  } finally {
    loading.value = false
  }
}
onMounted(load)
</script>
<template>
  <div>
    <AdminSectionHeader title="安全记录" description="登录、敏感查看与管理写操作的脱敏审计。"
      ><button class="admin-button secondary" @click="load">
        <RefreshCw :size="16" />刷新
      </button></AdminSectionHeader
    >
    <div class="admin-security-summary">
      <ShieldCheck :size="20" />
      <div>
        <strong>会话 Cookie 与 CSRF 已启用</strong
        ><span>完整 IP 仅按需解密；注册来源只保留不可逆哈希。</span>
      </div>
    </div>
    <div v-if="loading" class="admin-loading">正在读取审计记录</div>
    <div v-else class="admin-table-list">
      <div v-for="event in events" :key="event.id" class="admin-event-row">
        <strong>{{ event.action }}</strong
        ><span>{{ event.detail || event.targetId || '系统事件' }}</span
        ><code v-if="event.actorUserId">用户 {{ event.actorUserId }}</code
        ><time>{{ formatTime(event.createTime) }}</time>
      </div>
      <p v-if="!events.length" class="admin-empty">尚无安全事件。</p>
    </div>
  </div>
</template>
