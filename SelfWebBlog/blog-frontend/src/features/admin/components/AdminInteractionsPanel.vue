<script setup>
import { computed, inject, onMounted, ref } from 'vue'
import { Eye, Pin, PinOff, RefreshCw, ShieldAlert, Trash2 } from 'lucide-vue-next'
import {
  deleteInteraction,
  listAdminInteractions,
  moderateInteraction,
  moderateInteractions,
  revealInteractionIp,
  setInteractionPinned
} from '../../../api/admin'
import { formatTime } from '../../../utils/format'
import AdminSectionHeader from './AdminSectionHeader.vue'

const confirmAction = inject('adminConfirm')
const page = ref(null)
const filter = ref('PENDING')
const loading = ref(true)
const revealed = ref({})
const selectedIds = ref([])
const filters = [
  ['PENDING', '待审核'],
  ['PUBLISHED', '已发布'],
  ['HIDDEN', '已隐藏'],
  ['SPAM', '垃圾'],
  ['DELETED', '已删除'],
  ['', '全部']
]
const rows = computed(() => page.value?.records || [])
const allSelected = computed(
  () => rows.value.length > 0 && rows.value.every((item) => selectedIds.value.includes(item.id))
)
const batchActions = computed(
  () =>
    ({
      PENDING: [
        ['PUBLISHED', '批量通过'],
        ['SPAM', '批量标记垃圾']
      ],
      PUBLISHED: [
        ['HIDDEN', '批量隐藏'],
        ['SPAM', '批量标记垃圾']
      ],
      HIDDEN: [['PUBLISHED', '批量恢复']],
      SPAM: [['HIDDEN', '批量移出垃圾']]
    })[filter.value] || []
)

async function load() {
  loading.value = true
  try {
    page.value = await listAdminInteractions({ status: filter.value || undefined })
    selectedIds.value = []
    revealed.value = {}
  } finally {
    loading.value = false
  }
}

function selectFilter(value) {
  filter.value = value
  load()
}

async function setStatus(item, status) {
  if (status === 'SPAM' || status === 'HIDDEN') {
    const ok = await confirmAction({
      title: status === 'SPAM' ? '标记为垃圾内容' : '隐藏互动',
      message: '该内容将不再向访客公开，但仍保留在审核记录中。',
      confirmText: '确认'
    })
    if (!ok) return
  }
  await moderateInteraction(item.id, status)
  await load()
}

async function reveal(item) {
  const ok = await confirmAction({
    title: '查看完整 IP',
    message: '本次查看会写入安全审计记录。请仅在处理滥用行为时使用。',
    confirmText: '查看'
  })
  if (!ok) return
  const result = await revealInteractionIp(item.id)
  revealed.value = { ...revealed.value, [item.id]: result.ip }
}

async function togglePinned(item) {
  await setInteractionPinned(item.id, !item.pinned)
  await load()
}

async function remove(item) {
  const ok = await confirmAction({
    title: '删除互动',
    message: '删除后访客将无法看到这条内容，审核记录仍会保留。请输入“删除”确认。',
    confirmText: '删除',
    requiredText: '删除'
  })
  if (!ok) return
  await deleteInteraction(item.id)
  await load()
}

function toggleAll() {
  selectedIds.value = allSelected.value ? [] : rows.value.map((item) => item.id)
}

async function moderateSelected(status, label) {
  const count = selectedIds.value.length
  if (!count) return
  const destructive = status === 'HIDDEN' || status === 'SPAM'
  const ok = await confirmAction({
    title: label,
    message: `将处理选中的 ${count} 条互动。${destructive ? '请输入数量确认。' : ''}`,
    confirmText: label,
    requiredText: destructive ? String(count) : ''
  })
  if (!ok) return
  await moderateInteractions([...selectedIds.value], status)
  await load()
}

onMounted(load)
</script>

<template>
  <div>
    <AdminSectionHeader
      title="互动审核"
      description="评论与留言共享同一审核队列，完整 IP 默认隐藏。"
    >
      <button class="admin-button secondary" type="button" @click="load">
        <RefreshCw :size="16" />刷新
      </button>
    </AdminSectionHeader>
    <div class="admin-toolbar">
      <div class="admin-segments" aria-label="审核状态">
        <button
          v-for="item in filters"
          :key="item[0]"
          :class="{ active: filter === item[0] }"
          @click="selectFilter(item[0])"
        >
          {{ item[1] }}
        </button>
      </div>
      <label class="admin-select-all"
        ><input type="checkbox" :checked="allSelected" @change="toggleAll" />当前页</label
      >
      <button
        v-for="action in batchActions"
        v-show="selectedIds.length"
        :key="action[0]"
        :class="['admin-button', action[0] === 'SPAM' ? 'danger' : 'secondary']"
        type="button"
        @click="moderateSelected(action[0], action[1])"
      >
        {{ action[1] }}
      </button>
    </div>
    <div v-if="loading" class="admin-loading">正在读取审核队列</div>
    <div v-else class="admin-table-list">
      <article v-for="item in rows" :key="item.id" class="admin-interaction-row">
        <header>
          <label class="admin-row-check"
            ><span class="sr-only">选择这条互动</span
            ><input v-model="selectedIds" type="checkbox" :value="item.id" /></label
          ><strong>{{ item.nickname || '匿名访客' }}</strong
          ><span>{{ item.targetType === 'GUESTBOOK' ? '留言' : '文章评论' }}</span
          ><time>{{ formatTime(item.createTime) }}</time>
        </header>
        <p>{{ item.content }}</p>
        <footer>
          <span>{{ item.ipRegion || '属地未知' }}</span>
          <code v-if="revealed[item.id]">{{ revealed[item.id] }}</code>
          <button v-else type="button" @click="reveal(item)"><Eye :size="14" />查看 IP</button>
          <div class="admin-row-actions">
            <button
              v-if="item.status === 'PUBLISHED' && !item.rootId"
              type="button"
              @click="togglePinned(item)"
            >
              <PinOff v-if="item.pinned" :size="14" /><Pin v-else :size="14" />{{
                item.pinned ? '取消置顶' : '置顶'
              }}
            </button>
            <button
              v-if="item.status !== 'DELETED' && item.status !== 'PUBLISHED'"
              type="button"
              @click="setStatus(item, 'PUBLISHED')"
            >
              通过
            </button>
            <button
              v-if="item.status !== 'DELETED' && item.status !== 'HIDDEN'"
              type="button"
              @click="setStatus(item, 'HIDDEN')"
            >
              隐藏
            </button>
            <button
              v-if="item.status !== 'DELETED' && item.status !== 'SPAM'"
              type="button"
              class="danger"
              @click="setStatus(item, 'SPAM')"
            >
              <ShieldAlert :size="14" />垃圾
            </button>
            <button
              v-if="item.status !== 'DELETED'"
              type="button"
              class="danger"
              @click="remove(item)"
            >
              <Trash2 :size="14" />删除
            </button>
          </div>
        </footer>
      </article>
      <p v-if="!rows.length" class="admin-empty">当前队列已经处理完毕。</p>
    </div>
  </div>
</template>
