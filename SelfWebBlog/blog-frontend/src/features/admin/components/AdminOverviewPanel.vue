<script setup>
import { onMounted, ref } from 'vue'
import { Database, FileClock, FileText, Images, MessageSquareText, Users } from 'lucide-vue-next'
import { getAdminOverview } from '../../../api/admin'
import { formatTime } from '../../../utils/format'
import AdminSectionHeader from './AdminSectionHeader.vue'

const emit = defineEmits(['navigate'])
const data = ref(null)
const loading = ref(true)
const error = ref('')

const stats = () => [
  { key: 'content', label: '已发布', value: data.value?.publishedCount ?? 0, icon: FileText },
  { key: 'content', label: '草稿', value: data.value?.draftCount ?? 0, icon: FileClock },
  {
    key: 'interactions',
    label: '待审核',
    value: data.value?.pendingCount ?? 0,
    icon: MessageSquareText
  },
  { key: 'users', label: '用户', value: data.value?.userCount ?? 0, icon: Users },
  { key: 'library', label: '图片资产', value: data.value?.assetCount ?? 0, icon: Images },
  { key: 'security', label: '数据库', value: data.value?.databaseStatus ?? '-', icon: Database }
]

async function load() {
  loading.value = true
  error.value = ''
  try {
    data.value = await getAdminOverview()
  } catch (err) {
    error.value = err.message
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div>
    <AdminSectionHeader title="工作台概览" description="今天需要处理的内容与站点状态。">
      <button class="admin-button secondary" type="button" @click="load">刷新</button>
    </AdminSectionHeader>
    <div v-if="loading" class="admin-loading">正在读取站点状态</div>
    <div v-else-if="error" class="admin-error">{{ error }}</div>
    <template v-else>
      <div class="admin-stat-grid">
        <button
          v-for="item in stats()"
          :key="item.label"
          type="button"
          @click="emit('navigate', item.key)"
        >
          <component :is="item.icon" :size="19" />
          <strong>{{ item.value }}</strong>
          <span>{{ item.label }}</span>
        </button>
      </div>
      <section class="admin-band">
        <div>
          <span>最近活动</span
          ><strong>{{ formatTime(data?.latestActivity) || '还没有活动' }}</strong>
        </div>
        <div>
          <span>待清理图片</span><strong>{{ data?.orphanedCount ?? 0 }}</strong>
        </div>
        <button type="button" @click="emit('navigate', 'site')">编辑当前近况</button>
      </section>
    </template>
  </div>
</template>
