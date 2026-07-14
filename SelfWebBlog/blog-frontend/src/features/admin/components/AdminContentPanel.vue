<script setup>
import { computed, inject, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Edit3, Search, Trash2 } from 'lucide-vue-next'
import { deletePost, deletePosts, listDrafts, listPosts } from '../../../api/posts'
import { formatTime, toPlainText } from '../../../utils/format'
import AdminSectionHeader from './AdminSectionHeader.vue'

const router = useRouter()
const confirmAction = inject('adminConfirm')
const posts = ref([])
const drafts = ref([])
const query = ref('')
const status = ref('all')
const loading = ref(true)
const error = ref('')
const selectedIds = ref([])

const rows = computed(() => {
  const source =
    status.value === 'published'
      ? posts.value
      : status.value === 'draft'
        ? drafts.value
        : [...drafts.value, ...posts.value]
  const keyword = query.value.trim().toLowerCase()
  return source
    .filter(
      (post) =>
        !keyword || `${post.title} ${post.summary} ${post.category}`.toLowerCase().includes(keyword)
    )
    .sort((a, b) =>
      String(b.updateTime || b.createTime).localeCompare(String(a.updateTime || a.createTime))
    )
})
const allSelected = computed(
  () => rows.value.length > 0 && rows.value.every((post) => selectedIds.value.includes(post.id))
)

async function load() {
  loading.value = true
  try {
    const [published, draftPage] = await Promise.all([
      listPosts({ page: 1, size: 100 }),
      listDrafts(1, 100)
    ])
    posts.value = published?.records || []
    drafts.value = draftPage?.records || []
  } catch (err) {
    error.value = err.message
  } finally {
    loading.value = false
  }
}

function edit(post) {
  sessionStorage.setItem('editingPost', JSON.stringify(post))
  router.push(`/write/${post.id}`)
}

async function remove(post) {
  const accepted = await confirmAction({
    title: '删除内容',
    message: `确定删除「${post.title || '未命名草稿'}」吗？此操作无法恢复。`,
    confirmText: '删除'
  })
  if (!accepted) return
  await deletePost(post.id)
  await load()
}

function toggleAll() {
  selectedIds.value = allSelected.value ? [] : rows.value.map((post) => post.id)
}

async function removeSelected() {
  const count = selectedIds.value.length
  if (!count) return
  const accepted = await confirmAction({
    title: '批量删除内容',
    message: `将永久删除选中的 ${count} 篇内容。请输入数量确认。`,
    confirmText: `删除 ${count} 篇`,
    requiredText: String(count)
  })
  if (!accepted) return
  await deletePosts([...selectedIds.value])
  selectedIds.value = []
  await load()
}

watch(rows, (visibleRows) => {
  const visible = new Set(visibleRows.map((post) => post.id))
  selectedIds.value = selectedIds.value.filter((id) => visible.has(id))
})

onMounted(load)
</script>

<template>
  <div>
    <AdminSectionHeader title="内容管理" description="文章和草稿使用同一套检索与操作。">
      <button class="admin-button primary" type="button" @click="router.push('/write')">
        <Edit3 :size="16" />新建
      </button>
    </AdminSectionHeader>
    <div class="admin-toolbar">
      <label class="admin-search"
        ><Search :size="16" /><input v-model="query" placeholder="搜索标题、摘要或分类"
      /></label>
      <div class="admin-segments" aria-label="文章状态">
        <button
          v-for="item in [
            ['all', '全部'],
            ['published', '已发布'],
            ['draft', '草稿']
          ]"
          :key="item[0]"
          :class="{ active: status === item[0] }"
          @click="status = item[0]"
        >
          {{ item[1] }}
        </button>
      </div>
      <label class="admin-select-all"
        ><input type="checkbox" :checked="allSelected" @change="toggleAll" />当前列表</label
      >
      <button
        v-if="selectedIds.length"
        class="admin-button danger"
        type="button"
        @click="removeSelected"
      >
        <Trash2 :size="15" />删除 {{ selectedIds.length }} 项
      </button>
    </div>
    <div v-if="loading" class="admin-loading">正在读取内容</div>
    <div v-else-if="error" class="admin-error">{{ error }}</div>
    <div v-else class="admin-table-list">
      <article v-for="post in rows" :key="post.id" class="admin-content-row">
        <label class="admin-row-check"
          ><span class="sr-only">选择 {{ post.title || '未命名草稿' }}</span
          ><input v-model="selectedIds" type="checkbox" :value="post.id"
        /></label>
        <div class="admin-row-main">
          <div class="admin-row-title">
            <strong>{{ post.title || '未命名草稿' }}</strong
            ><span :class="['admin-status', (post.status || 'PUBLISHED').toLowerCase()]">{{
              post.status === 'DRAFT' ? '草稿' : '已发布'
            }}</span>
          </div>
          <p>{{ post.summary || toPlainText(post.content).slice(0, 100) || '还没有摘要' }}</p>
          <small
            >{{ post.category || '未分类' }} ·
            {{ formatTime(post.updateTime || post.createTime) }}</small
          >
        </div>
        <div class="admin-row-actions">
          <button type="button" aria-label="编辑" title="编辑" @click="edit(post)">
            <Edit3 :size="16" />
          </button>
          <button type="button" class="danger" aria-label="删除" title="删除" @click="remove(post)">
            <Trash2 :size="16" />
          </button>
        </div>
      </article>
      <p v-if="!rows.length" class="admin-empty">没有符合当前条件的内容。</p>
    </div>
  </div>
</template>
