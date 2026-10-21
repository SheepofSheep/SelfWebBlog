<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import {
  Archive,
  ExternalLink,
  FileUp,
  MoreHorizontal,
  Pencil,
  RotateCcw,
  Trash2
} from 'lucide-vue-next'
import { api } from '@/api/client.js'
import { createAdminRequestGate } from '@/core/adminModel.js'
import { formatDate, postTitle } from '@/core/format.js'
import { notify } from '@/stores/app.js'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import AdminPager from './AdminPager.vue'
import AdminToolbar from './AdminToolbar.vue'

const route = useRoute()
const status = ref(
  ['PUBLISHED', 'DRAFT', 'ARCHIVED'].includes(route.query.status) ? route.query.status : 'PUBLISHED'
)
const keyword = ref('')
const page = ref(1)
const result = ref({ records: [], current: 1, pages: 1, total: 0 })
const selected = ref(new Set())
const loading = ref(false)
const busy = ref(false)
const pending = ref(null)
const requestGate = createAdminRequestGate()

const records = computed(() => result.value.records || [])
const allSelected = computed(
  () =>
    records.value.length > 0 && records.value.every((post) => selected.value.has(String(post.id)))
)
const statusMeta = {
  PUBLISHED: { label: '已发布', action: '收纳归档', target: 'ARCHIVED', icon: Archive },
  DRAFT: { label: '草稿', action: '发布', target: 'PUBLISHED', icon: FileUp },
  ARCHIVED: { label: '已归档', action: '恢复发布', target: 'PUBLISHED', icon: RotateCcw }
}

async function load(nextPage = page.value) {
  const ticket = requestGate.begin()
  loading.value = true
  selected.value = new Set()
  try {
    const value = await api.adminPosts({
      status: status.value,
      keyword: keyword.value.trim(),
      page: nextPage,
      size: 20
    })
    if (!requestGate.isCurrent(ticket)) return false
    result.value = value
    page.value = Number(value.current || nextPage)
    selected.value = new Set()
    return true
  } catch (error) {
    if (requestGate.isCurrent(ticket)) notify(error.message)
    return false
  } finally {
    requestGate.runIfCurrent(ticket, () => {
      loading.value = false
    })
  }
}

async function reloadAfterMutation() {
  const applied = await load(page.value)
  if (!applied) return
  if (!records.value.length && page.value > 1) {
    await load(Math.max(1, Math.min(page.value - 1, Number(result.value.pages || 1))))
  }
}

function chooseStatus(next) {
  status.value = next
  page.value = 1
  load(1)
}

function toggle(id) {
  if (loading.value || busy.value) return
  const next = new Set(selected.value)
  const key = String(id)
  next.has(key) ? next.delete(key) : next.add(key)
  selected.value = next
}

function toggleAll() {
  if (loading.value || busy.value) return
  selected.value = allSelected.value
    ? new Set()
    : new Set(records.value.map((post) => String(post.id)))
}

function closeRowMenu(event) {
  event.currentTarget.closest('details')?.removeAttribute('open')
}

function requestRowStatus(event, postId) {
  closeRowMenu(event)
  if (loading.value || busy.value) return
  requestStatus([String(postId)], statusMeta[status.value].target)
}

function requestRowDelete(event, postId) {
  closeRowMenu(event)
  if (loading.value || busy.value) return
  requestDelete([String(postId)])
}

function requestStatus(ids, target) {
  if (loading.value || busy.value || !ids.length) return
  const count = ids.length
  const label = target === 'ARCHIVED' ? '收纳归档' : status.value === 'DRAFT' ? '发布' : '恢复发布'
  pending.value = {
    kind: 'status',
    ids,
    target,
    source: status.value,
    title: label,
    message: count > 1 ? `将对所选 ${count} 篇文章执行“${label}”。` : `确定${label}这篇文章？`,
    confirmLabel: label,
    danger: false
  }
}

function requestDelete(ids) {
  if (loading.value || busy.value || !ids.length) return
  pending.value = {
    kind: 'delete',
    ids,
    title: '删除文章',
    message:
      ids.length > 1
        ? `将永久删除所选 ${ids.length} 篇文章及其关联数据。`
        : '将永久删除这篇文章及其关联数据。',
    confirmLabel: '删除',
    danger: true
  }
}

async function execute() {
  if (!pending.value || busy.value) return
  busy.value = true
  try {
    const action = pending.value
    if (action.kind === 'delete') {
      if (action.ids.length === 1) await api.deletePost(action.ids[0])
      else await api.batchDeletePosts(action.ids)
      notify('文章已删除')
    } else if (action.ids.length === 1) {
      await api.changePostStatus(action.ids[0], action.target)
      notify(
        action.target === 'ARCHIVED'
          ? '文章已归档'
          : action.source === 'DRAFT'
            ? '文章已发布'
            : '文章已恢复发布'
      )
    } else {
      await api.batchPostStatus(action.ids, action.target)
      notify(
        action.target === 'ARCHIVED'
          ? '所选文章已归档'
          : action.source === 'DRAFT'
            ? '所选文章已发布'
            : '所选文章已恢复发布'
      )
    }
    pending.value = null
    await reloadAfterMutation()
  } catch (error) {
    notify(error.message)
  } finally {
    busy.value = false
  }
}

onMounted(load)
</script>

<template>
  <section class="content-panel">
    <AdminToolbar
      v-model="keyword"
      placeholder="搜索标题、正文、分类或标签"
      :busy="loading"
      @submit="load(1)"
      @refresh="load()"
    >
      <div class="status-tabs" role="tablist" aria-label="文章状态">
        <button
          v-for="(meta, key) in statusMeta"
          :key="key"
          type="button"
          role="tab"
          :aria-selected="status === key"
          :class="{ active: status === key }"
          @click="chooseStatus(key)"
        >
          {{ meta.label }}
        </button>
      </div>
    </AdminToolbar>

    <div v-if="selected.size" class="batch-bar glass">
      <strong>已选择 {{ selected.size }} 篇</strong>
      <div>
        <button
          class="secondary-button"
          type="button"
          :disabled="loading || busy"
          @click="requestStatus([...selected], statusMeta[status].target)"
        >
          <component :is="statusMeta[status].icon" :size="16" />{{ statusMeta[status].action }}
        </button>
        <button
          class="danger-text-button"
          type="button"
          :disabled="loading || busy"
          @click="requestDelete([...selected])"
        >
          <Trash2 :size="16" />删除
        </button>
      </div>
    </div>

    <div class="admin-table glass" :aria-busy="loading">
      <header class="content-head">
        <label
          ><input
            type="checkbox"
            :checked="allSelected"
            :disabled="loading || busy"
            aria-label="选择本页全部文章"
            @change="toggleAll"
          />文章</label
        >
        <span>更新时间</span><span>操作</span>
      </header>
      <article v-for="post in records" :key="post.id" class="content-row">
        <label class="post-cell">
          <input
            type="checkbox"
            :checked="selected.has(String(post.id))"
            :disabled="loading || busy"
            :aria-label="`选择 ${postTitle(post)}`"
            @change="toggle(post.id)"
          />
          <span
            class="post-cover"
            :style="post.coverUrl ? { backgroundImage: `url(${post.coverUrl})` } : undefined"
          ></span>
          <span
            ><strong>{{ postTitle(post) }}</strong
            ><small
              >{{ post.category || '未分类'
              }}<template v-if="post.tags"> · {{ post.tags }}</template></small
            ></span
          >
        </label>
        <time :datetime="post.updateTime || post.createTime">{{
          formatDate(post.updateTime || post.createTime)
        }}</time>
        <div class="row-actions">
          <RouterLink class="edit-action" :to="`/write/${post.id}`" aria-label="编辑文章">
            <Pencil :size="15" /><span>编辑</span>
          </RouterLink>
          <details :class="['content-more', { disabled: loading || busy }]">
            <summary class="icon-button" role="button" :aria-label="`更多操作：${postTitle(post)}`">
              <MoreHorizontal :size="17" />
            </summary>
            <div class="content-action-menu glass">
              <RouterLink
                v-if="status === 'PUBLISHED'"
                class="menu-action"
                :to="`/post/${post.id}`"
              >
                <ExternalLink :size="15" />查看文章
              </RouterLink>
              <button
                class="menu-action"
                type="button"
                :disabled="loading || busy"
                @click="requestRowStatus($event, post.id)"
              >
                <component :is="statusMeta[status].icon" :size="15" />
                {{ statusMeta[status].action }}
              </button>
              <button
                class="menu-action danger"
                type="button"
                :disabled="loading || busy"
                @click="requestRowDelete($event, post.id)"
              >
                <Trash2 :size="15" />删除文章
              </button>
            </div>
          </details>
        </div>
      </article>
      <p v-if="!loading && !records.length" class="admin-empty">没有符合条件的文章</p>
      <AdminPager
        :page="page"
        :pages="Number(result.pages || 1)"
        :total="Number(result.total || 0)"
        :busy="loading"
        @change="load"
      />
    </div>

    <ConfirmDialog
      :open="Boolean(pending)"
      :title="pending?.title"
      :message="pending?.message"
      :confirm-label="pending?.confirmLabel"
      :danger="pending?.danger"
      :busy="busy"
      @close="!busy && (pending = null)"
      @confirm="execute"
    />
  </section>
</template>

<style scoped>
.content-panel {
  min-width: 0;
}
.status-tabs {
  display: flex;
  gap: 3px;
  padding: 3px;
  border-radius: 11px;
  background: var(--accent-soft);
}
.status-tabs button {
  padding: 7px 10px;
  border: 0;
  border-radius: 8px;
  color: var(--muted);
  background: transparent;
  white-space: nowrap;
}
.status-tabs button.active {
  color: var(--accent);
  background: var(--glass-strong);
  box-shadow: 0 4px 14px rgb(20 62 105 / 8%);
}
.batch-bar {
  display: flex;
  gap: 14px;
  align-items: center;
  justify-content: space-between;
  padding: 11px 14px;
  margin-bottom: 10px;
  border-radius: 16px;
}
.batch-bar > div {
  display: flex;
  gap: 7px;
}
.batch-bar button {
  display: inline-flex;
  gap: 6px;
  align-items: center;
}
.danger-text-button {
  padding: 9px 12px;
  border: 0;
  border-radius: 11px;
  color: var(--danger);
  background: color-mix(in srgb, var(--danger) 8%, transparent);
}
.admin-table {
  padding: 8px 16px 16px;
  overflow: visible;
  border-radius: 24px;
}
.content-head,
.content-row {
  display: grid;
  grid-template-columns: minmax(260px, 1fr) 150px 160px;
  gap: 12px;
  align-items: center;
}
.content-head {
  padding: 11px 8px;
  color: var(--muted);
  border-bottom: 1px solid var(--line);
  font-size: 11px;
}
.content-head label {
  display: flex;
  gap: 12px;
  align-items: center;
}
.content-row {
  min-width: 0;
  padding: 13px 8px;
  border-bottom: 1px solid var(--line);
}
.post-cell {
  display: flex;
  min-width: 0;
  gap: 11px;
  align-items: center;
}
.post-cell > span:last-child {
  display: grid;
  min-width: 0;
  gap: 3px;
}
.post-cell strong,
.post-cell small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.post-cell small,
.content-row time {
  color: var(--muted);
  font-size: 11px;
}
.post-cover {
  flex: 0 0 42px;
  width: 42px;
  height: 42px;
  border-radius: 12px;
  background: var(--accent-soft) center / cover;
}
.row-actions {
  display: flex;
  gap: 6px;
  align-items: center;
  justify-content: flex-end;
}
.edit-action,
.menu-action {
  display: inline-flex;
  gap: 6px;
  align-items: center;
  border: 0;
  color: var(--ink);
  text-decoration: none;
}
.edit-action {
  min-height: 34px;
  padding: 7px 10px;
  border: 1px solid color-mix(in srgb, var(--accent) 20%, var(--line));
  border-radius: 10px;
  color: var(--accent);
  background: var(--accent-soft);
  font-size: 12px;
  font-weight: 700;
}
.content-more {
  position: relative;
}
.content-more.disabled > summary {
  pointer-events: none;
  opacity: 0.5;
}
.content-more[open] {
  z-index: 9;
}
.content-more summary {
  list-style: none;
}
.content-more summary::-webkit-details-marker {
  display: none;
}
.content-action-menu {
  position: absolute;
  z-index: 8;
  top: calc(100% + 7px);
  right: 0;
  display: grid;
  min-width: 142px;
  padding: 6px;
  border-radius: 13px;
  box-shadow: 0 16px 40px rgb(21 55 88 / 18%);
}
.menu-action {
  width: 100%;
  padding: 9px 10px;
  border-radius: 9px;
  background: transparent;
  font-size: 12px;
  text-align: left;
}
.menu-action:hover,
.menu-action:focus-visible {
  background: var(--accent-soft);
}
.menu-action.danger {
  color: var(--danger);
}
.admin-empty {
  padding: 45px 10px;
  color: var(--muted);
  text-align: center;
}
@media (max-width: 760px) {
  .content-head {
    display: none;
  }
  .content-row {
    grid-template-columns: 1fr auto;
    gap: 8px;
  }
  .content-row time {
    grid-row: 2;
    grid-column: 1;
    padding-left: 76px;
  }
  .row-actions {
    grid-row: 1 / span 2;
    grid-column: 2;
  }
  .batch-bar {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
