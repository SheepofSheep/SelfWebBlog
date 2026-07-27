<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import {
  ChevronDown,
  ChevronUp,
  Heart,
  MessageCircle,
  MoreHorizontal,
  Pin,
  Reply,
  Trash2
} from 'lucide-vue-next'
import { api } from '@/api/client.js'
import {
  allowedInteractionTransitions,
  canMovePinnedInteraction,
  canPinInteraction,
  canReplyToInteraction,
  createAdminRequestGate,
  pageAfterMutation,
  sharedInteractionTransitions
} from '@/core/adminModel.js'
import { formatDate } from '@/core/format.js'
import { appStore, notify } from '@/stores/app.js'
import AppAvatar from '@/components/AppAvatar.vue'
import CommentBody from '@/components/CommentBody.vue'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import EmojiPicker from '@/components/EmojiPicker.vue'
import FormDialog from '@/components/FormDialog.vue'
import AdminPager from './AdminPager.vue'
import AdminToolbar from './AdminToolbar.vue'

const result = ref({ records: [], current: 1, pages: 1, total: 0 })
const page = ref(1)
const keyword = ref('')
const status = ref('')
const targetType = ref('')
const pinned = ref('')
const contentType = ref('')
const selected = ref(new Set())
const expandedRoots = ref(new Set())
const loading = ref(false)
const busy = ref(false)
const removing = ref(null)
const batchRemoving = ref(false)
const replyingTo = ref(null)
const replyContent = ref('')
const replyInput = ref(null)
const requestGate = createAdminRequestGate()
const controlsLocked = computed(() => loading.value || busy.value)

const transitionLabels = {
  PUBLISHED: '通过',
  HIDDEN: '隐藏',
  SPAM: '标记垃圾'
}

const records = computed(() => result.value.records || [])
const selectedRecords = computed(() =>
  records.value.filter((item) => selected.value.has(String(item.id)))
)
const batchTransitions = computed(() => sharedInteractionTransitions(selectedRecords.value))
const canDeleteSelected = computed(
  () =>
    selectedRecords.value.length > 0 &&
    selectedRecords.value.every((item) => item.status !== 'DELETED')
)
const allSelected = computed(
  () =>
    records.value.length > 0 && records.value.every((item) => selected.value.has(String(item.id)))
)

async function load(nextPage = page.value) {
  const ticket = requestGate.begin()
  selected.value = new Set()
  batchRemoving.value = false
  loading.value = true
  try {
    const value = await api.adminInteractions({
      status: status.value,
      targetType: targetType.value,
      pinned: pinned.value,
      contentType: contentType.value,
      keyword: keyword.value.trim(),
      page: nextPage,
      size: 20
    })
    if (!requestGate.isCurrent(ticket)) return false
    result.value = value
    page.value = Number(result.value.current || nextPage)
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

function itemReplyCount(item) {
  return Math.max(0, Number(item?.replyCount ?? item?.replies?.length) || 0)
}

function repliesExpanded(id) {
  return expandedRoots.value.has(String(id))
}

function toggleReplies(id) {
  if (controlsLocked.value) return
  const next = new Set(expandedRoots.value)
  const key = String(id)
  next.has(key) ? next.delete(key) : next.add(key)
  expandedRoots.value = next
}

function expandReplies(id) {
  const next = new Set(expandedRoots.value)
  next.add(String(id))
  expandedRoots.value = next
}

async function loadAfterMutation() {
  const previousPage = page.value
  if (!(await load(previousPage))) return
  const nextPage = pageAfterMutation(previousPage, records.value)
  if (nextPage !== previousPage) await load(nextPage)
}

function changeFilter() {
  page.value = 1
  load(1)
}

function toggle(id) {
  if (controlsLocked.value) return
  const next = new Set(selected.value)
  const key = String(id)
  next.has(key) ? next.delete(key) : next.add(key)
  selected.value = next
}

function toggleAll() {
  if (controlsLocked.value) return
  selected.value = allSelected.value
    ? new Set()
    : new Set(records.value.map((item) => String(item.id)))
}

async function moderate(item, nextStatus) {
  if (controlsLocked.value) return
  if (!allowedInteractionTransitions(item.status).includes(nextStatus)) {
    notify('当前评论状态不支持此操作')
    return
  }
  busy.value = true
  try {
    await api.moderate(item.id, nextStatus)
    notify('评论状态已更新')
    await loadAfterMutation()
  } catch (error) {
    notify(error.message)
  } finally {
    busy.value = false
  }
}

async function moderateSelected(nextStatus) {
  if (controlsLocked.value) return
  if (!selected.value.size || !batchTransitions.value.includes(nextStatus)) {
    notify('所选评论没有共同可用的审核操作')
    return
  }
  const count = selected.value.size
  busy.value = true
  try {
    await api.moderateBatch([...selected.value], nextStatus)
    notify(`已处理 ${count} 条评论`)
    await loadAfterMutation()
  } catch (error) {
    notify(error.message)
  } finally {
    busy.value = false
  }
}

async function togglePin(item) {
  if (controlsLocked.value) return
  busy.value = true
  try {
    await api.pin(item.id, !item.pinned)
    notify(item.pinned ? '已取消置顶' : '评论已置顶')
    await loadAfterMutation()
  } catch (error) {
    notify(error.message)
  } finally {
    busy.value = false
  }
}

async function movePin(item, direction) {
  if (controlsLocked.value) return
  busy.value = true
  try {
    await api.movePin(item.id, direction)
    await loadAfterMutation()
  } catch (error) {
    notify(error.message)
  } finally {
    busy.value = false
  }
}

async function remove() {
  if (!removing.value || controlsLocked.value) return
  busy.value = true
  try {
    await api.deleteInteraction(removing.value.id)
    removing.value = null
    notify('评论已删除')
    await loadAfterMutation()
  } catch (error) {
    notify(error.message)
  } finally {
    busy.value = false
  }
}

async function removeSelected() {
  if (controlsLocked.value) return
  if (!canDeleteSelected.value) {
    notify('所选评论包含不可删除的状态')
    return
  }
  const ids = [...selected.value]
  busy.value = true
  try {
    await api.batchDeleteInteractions(ids)
    batchRemoving.value = false
    notify(`已删除 ${ids.length} 条评论`)
    await loadAfterMutation()
  } catch (error) {
    notify(error.message)
  } finally {
    busy.value = false
  }
}

function openReply(item, root) {
  if (controlsLocked.value || !canReplyToInteraction(item)) return
  replyingTo.value = { item, rootId: String(root?.id || item?.rootId || item?.id) }
  replyContent.value = ''
  nextTick(() => replyInput.value?.focus())
}

function closeReply() {
  if (busy.value) return
  replyingTo.value = null
  replyContent.value = ''
}

async function insertReplySticker(sticker) {
  const input = replyInput.value
  const token = sticker?.token || ''
  if (!token) return
  const start = input?.selectionStart ?? replyContent.value.length
  const end = input?.selectionEnd ?? start
  replyContent.value = replyContent.value.slice(0, start) + token + replyContent.value.slice(end)
  await nextTick()
  input?.focus()
  input?.setSelectionRange(start + token.length, start + token.length)
}

async function submitReply() {
  const target = replyingTo.value
  const content = replyContent.value.trim()
  if (controlsLocked.value || !target || !content || content.length > 2000) return
  busy.value = true
  try {
    await api.createInteraction({
      targetType: target.item.targetType,
      targetId: String(target.item.targetId),
      replyToId: String(target.item.id),
      content
    })
    expandReplies(target.rootId)
    replyingTo.value = null
    replyContent.value = ''
    notify('回复已发布')
    await loadAfterMutation()
  } catch (error) {
    notify(error.message)
  } finally {
    busy.value = false
  }
}

function requestRemove(item) {
  if (controlsLocked.value) return
  removing.value = item
}

function openBatchRemove() {
  if (controlsLocked.value) return
  batchRemoving.value = true
}

function blockLockedMenu(event) {
  if (!controlsLocked.value) return
  event.preventDefault()
}

onMounted(load)
</script>

<template>
  <section class="interactions-panel">
    <AdminToolbar
      v-model="keyword"
      placeholder="搜索评论内容或昵称"
      :busy="loading || busy"
      @submit="changeFilter"
      @refresh="load()"
    >
      <select
        v-model="status"
        aria-label="评论状态"
        :disabled="controlsLocked"
        @change="changeFilter"
      >
        <option value="">全部状态</option>
        <option value="PENDING">待处理</option>
        <option value="PUBLISHED">已显示</option>
        <option value="HIDDEN">已隐藏</option>
        <option value="SPAM">垃圾评论</option>
        <option value="DELETED">已删除</option>
      </select>
      <select
        v-model="contentType"
        aria-label="评论内容类型"
        :disabled="controlsLocked"
        @change="changeFilter"
      >
        <option value="">全部内容</option>
        <option value="TEXT">文字</option>
        <option value="STICKER">表情</option>
      </select>
      <select
        v-model="targetType"
        aria-label="评论位置"
        :disabled="controlsLocked"
        @change="changeFilter"
      >
        <option value="">全部位置</option>
        <option value="POST">文章</option>
        <option value="GUESTBOOK">留言板</option>
      </select>
      <select
        v-model="pinned"
        aria-label="置顶状态"
        :disabled="controlsLocked"
        @change="changeFilter"
      >
        <option value="">全部评论</option>
        <option value="true">仅置顶</option>
        <option value="false">未置顶</option>
      </select>
    </AdminToolbar>

    <div v-if="selected.size" class="moderation-batch glass">
      <strong>已选择 {{ selected.size }} 条</strong>
      <div>
        <button
          v-for="target in batchTransitions"
          :key="target"
          :class="{ danger: target === 'SPAM' }"
          type="button"
          :disabled="controlsLocked"
          @click="moderateSelected(target)"
        >
          {{ transitionLabels[target] }}
        </button>
        <button
          v-if="canDeleteSelected"
          class="danger"
          type="button"
          :disabled="controlsLocked"
          @click="openBatchRemove"
        >
          <Trash2 :size="14" />批量删除
        </button>
        <span v-if="!batchTransitions.length && !canDeleteSelected">没有共同可用的操作</span>
      </div>
    </div>

    <div class="moderation-list" :aria-busy="loading">
      <div class="moderation-select-all glass">
        <label
          ><input
            type="checkbox"
            :checked="allSelected"
            :disabled="controlsLocked"
            @change="toggleAll"
          />选择本页</label
        ><span>共 {{ result.total || 0 }} 条</span>
      </div>
      <article
        v-for="item in records"
        :key="item.id"
        :class="['moderation-item', 'glass', { pinned: item.pinned }]"
      >
        <div class="moderation-root">
          <label class="item-check">
            <input
              type="checkbox"
              :checked="selected.has(String(item.id))"
              :aria-label="`选择 ${item.nickname || '访客'} 的评论`"
              :disabled="controlsLocked"
              @change="toggle(item.id)"
            />
          </label>
          <AppAvatar :src="item.avatarUrl" :name="item.nickname || '访客'" :size="42" rounded />
          <div class="moderation-content">
            <header>
              <strong>{{ item.nickname || '访客' }}</strong>
              <span
                v-if="item.titleName"
                :class="['title-badge', `title-${item.titleStyle || 'default'}`]"
                >{{ item.titleName }}</span
              >
              <span class="target-chip" :title="item.sourceTitle || ''">
                {{
                  item.sourceTitle ||
                  (item.targetType === 'POST' ? `文章 ${item.targetId}` : '留言板')
                }}
              </span>
              <time>{{ formatDate(item.createTime) }}</time>
            </header>
            <CommentBody :content="item.content" />
            <footer>
              <span :class="['status-chip', item.status?.toLowerCase()]">{{ item.status }}</span>
              <span><Heart :size="13" />{{ item.likeCount || 0 }}</span>
              <span><MessageCircle :size="13" />{{ itemReplyCount(item) }}</span>
              <span v-if="item.ipRegion">{{ item.ipRegion }}</span>
              <span v-if="item.pinned" class="pin-chip">
                <Pin :size="13" />置顶 {{ Number(item.pinOrder || 0) + 1 }}
              </span>
            </footer>
          </div>
          <div class="quick-actions">
            <button
              v-if="canReplyToInteraction(item)"
              type="button"
              :aria-label="`回复${item.nickname || '访客'}`"
              :disabled="controlsLocked"
              @click="openReply(item, item)"
            >
              <Reply :size="14" />回复
            </button>
            <button
              v-for="target in allowedInteractionTransitions(item.status)"
              :key="target"
              :class="{ danger: target === 'SPAM' }"
              type="button"
              :disabled="controlsLocked"
              @click="moderate(item, target)"
            >
              {{ transitionLabels[target] }}
            </button>
          </div>
          <details :class="['more-menu', { disabled: controlsLocked }]">
            <summary
              :aria-label="`更多操作：${item.nickname || '访客'}`"
              :aria-disabled="controlsLocked"
              @click="blockLockedMenu"
            >
              <MoreHorizontal :size="18" />
            </summary>
            <div class="glass">
              <button
                v-if="canPinInteraction(item)"
                type="button"
                :disabled="controlsLocked"
                @click="togglePin(item)"
              >
                <Pin :size="15" />{{ item.pinned ? '取消置顶' : '置顶' }}
              </button>
              <button
                v-if="canMovePinnedInteraction(item)"
                type="button"
                :disabled="controlsLocked"
                @click="movePin(item, 'UP')"
              >
                <ChevronUp :size="15" />向上
              </button>
              <button
                v-if="canMovePinnedInteraction(item)"
                type="button"
                :disabled="controlsLocked"
                @click="movePin(item, 'DOWN')"
              >
                <ChevronDown :size="15" />向下
              </button>
              <button
                v-if="item.status !== 'DELETED'"
                class="danger"
                type="button"
                :disabled="controlsLocked"
                @click="requestRemove(item)"
              >
                <Trash2 :size="15" />删除
              </button>
            </div>
          </details>
        </div>

        <div v-if="itemReplyCount(item)" class="moderation-thread">
          <button
            class="thread-toggle"
            type="button"
            :aria-expanded="repliesExpanded(item.id)"
            :aria-controls="`moderation-replies-${item.id}`"
            :aria-label="`${repliesExpanded(item.id) ? '收起' : '展开'} ${itemReplyCount(item)} 条回复`"
            :disabled="controlsLocked"
            @click="toggleReplies(item.id)"
          >
            <ChevronUp v-if="repliesExpanded(item.id)" :size="15" />
            <ChevronDown v-else :size="15" />
            {{ repliesExpanded(item.id) ? '收起回复' : `查看 ${itemReplyCount(item)} 条回复` }}
          </button>
          <div
            v-show="repliesExpanded(item.id)"
            :id="`moderation-replies-${item.id}`"
            class="moderation-replies"
            role="region"
            :aria-label="`${item.nickname || '访客'} 的回复`"
          >
            <article v-for="reply in item.replies || []" :key="reply.id" class="moderation-reply">
              <AppAvatar
                :src="reply.avatarUrl"
                :name="reply.nickname || '访客'"
                :size="34"
                rounded
              />
              <div class="moderation-content">
                <header>
                  <strong>{{ reply.nickname || '访客' }}</strong>
                  <span
                    v-if="reply.titleName"
                    :class="['title-badge', `title-${reply.titleStyle || 'default'}`]"
                    >{{ reply.titleName }}</span
                  >
                  <span v-if="reply.replyToNickname" class="reply-target">
                    回复 {{ reply.replyToNickname }}
                  </span>
                  <time>{{ formatDate(reply.createTime) }}</time>
                </header>
                <CommentBody :content="reply.content" />
                <footer>
                  <span :class="['status-chip', reply.status?.toLowerCase()]">{{
                    reply.status
                  }}</span>
                  <span><Heart :size="13" />{{ reply.likeCount || 0 }}</span>
                  <span v-if="reply.ipRegion">{{ reply.ipRegion }}</span>
                </footer>
              </div>
              <div class="quick-actions reply-actions">
                <button
                  v-if="canReplyToInteraction(reply)"
                  type="button"
                  :aria-label="`回复${reply.nickname || '访客'}`"
                  :disabled="controlsLocked"
                  @click="openReply(reply, item)"
                >
                  <Reply :size="14" />回复
                </button>
                <button
                  v-for="target in allowedInteractionTransitions(reply.status)"
                  :key="target"
                  :class="{ danger: target === 'SPAM' }"
                  type="button"
                  :disabled="controlsLocked"
                  @click="moderate(reply, target)"
                >
                  {{ transitionLabels[target] }}
                </button>
              </div>
              <details :class="['more-menu', { disabled: controlsLocked }]">
                <summary
                  :aria-label="`更多操作：${reply.nickname || '访客'}`"
                  :aria-disabled="controlsLocked"
                  @click="blockLockedMenu"
                >
                  <MoreHorizontal :size="18" />
                </summary>
                <div class="glass">
                  <button
                    v-if="reply.status !== 'DELETED'"
                    class="danger"
                    type="button"
                    :disabled="controlsLocked"
                    @click="requestRemove(reply)"
                  >
                    <Trash2 :size="15" />删除
                  </button>
                </div>
              </details>
            </article>
          </div>
        </div>
      </article>
      <p v-if="!loading && !records.length" class="moderation-empty">没有符合条件的评论</p>
    </div>
    <AdminPager
      :page="page"
      :pages="Number(result.pages || 1)"
      :total="Number(result.total || 0)"
      :busy="controlsLocked"
      @change="load"
    />
    <ConfirmDialog
      :open="Boolean(removing)"
      title="删除评论"
      :message="`${removing?.nickname || '这条评论'}将进入删除状态，不再在公开页面显示。`"
      confirm-label="删除"
      danger
      :busy="busy"
      @close="!busy && (removing = null)"
      @confirm="remove"
    />
    <ConfirmDialog
      :open="batchRemoving"
      title="批量删除评论"
      :message="`所选 ${selected.size} 条根评论将进入删除状态，相关回复不会被物理移除。`"
      :confirm-label="`删除 ${selected.size} 条`"
      danger
      :busy="busy"
      @close="!busy && (batchRemoving = false)"
      @confirm="removeSelected"
    />
    <FormDialog
      :open="Boolean(replyingTo)"
      title="管理员回复"
      :description="`回复 ${replyingTo?.item?.nickname || '访客'}，将以 ${appStore.user?.nickname || appStore.user?.username || '当前管理员'} 身份发布。`"
      submit-label="发送回复"
      :busy="busy"
      :valid="Boolean(replyContent.trim()) && replyContent.length <= 2000"
      @close="closeReply"
      @submit="submitReply"
    >
      <label class="reply-field">
        <span>回复内容</span>
        <textarea
          ref="replyInput"
          v-model="replyContent"
          aria-label="回复内容"
          :disabled="controlsLocked"
          maxlength="2000"
          rows="6"
          placeholder="写下回复"
        />
      </label>
      <div class="reply-composer-tools" :inert="controlsLocked">
        <EmojiPicker label="插入表情" @select="insertReplySticker" />
        <span>{{ replyContent.length }}/2000</span>
      </div>
    </FormDialog>
  </section>
</template>

<style scoped>
.interactions-panel select {
  padding: 8px 10px;
  border: 1px solid var(--line);
  border-radius: 10px;
  color: var(--ink);
  background: var(--glass-strong);
}
.moderation-batch,
.moderation-select-all {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
  padding: 11px 14px;
  margin-bottom: 10px;
  border-radius: 16px;
}
.moderation-batch > div {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
  align-items: center;
  justify-content: flex-end;
}
.moderation-batch > div > span {
  color: var(--muted);
  font-size: 11px;
}
.moderation-batch button {
  display: inline-flex;
  gap: 5px;
  align-items: center;
  padding: 7px 10px;
  border: 0;
  border-radius: 9px;
  color: var(--accent);
  background: var(--accent-soft);
}
.moderation-batch button.danger {
  color: var(--danger);
}
.moderation-select-all {
  color: var(--muted);
  font-size: 11px;
}
.moderation-select-all label {
  display: flex;
  gap: 8px;
  align-items: center;
}
.moderation-list {
  display: grid;
  gap: 9px;
}
.moderation-item {
  position: relative;
  border-radius: 21px;
}
.moderation-root {
  display: grid;
  grid-template-columns: auto auto minmax(0, 1fr) auto auto;
  gap: 12px;
  align-items: start;
  padding: 16px;
}
.moderation-item.pinned {
  border-color: color-mix(in srgb, var(--accent) 45%, var(--line));
}
.item-check {
  padding-top: 12px;
}
.moderation-content {
  min-width: 0;
}
.moderation-content header {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
}
.moderation-content header time {
  margin-left: auto;
  color: var(--muted);
  font-size: 10px;
}
.moderation-content :deep(.comment-body) {
  margin: 12px 0;
  color: var(--ink);
  line-height: 1.7;
}
.moderation-content footer {
  display: flex;
  flex-wrap: wrap;
  gap: 9px;
  align-items: center;
  color: var(--muted);
  font-size: 10px;
}
.moderation-content footer span {
  display: inline-flex;
  gap: 4px;
  align-items: center;
}
.title-badge,
.target-chip,
.reply-target,
.status-chip,
.pin-chip {
  padding: 3px 7px;
  border-radius: 999px;
  font-size: 9px;
}
.title-badge,
.target-chip {
  color: var(--accent);
  background: var(--accent-soft);
}
.target-chip {
  overflow: hidden;
  max-width: min(320px, 42vw);
  text-overflow: ellipsis;
  white-space: nowrap;
}
.reply-target {
  color: #8061b6;
  background: rgb(128 97 182 / 11%);
}
.status-chip {
  color: var(--muted);
  background: var(--accent-soft);
}
.status-chip.published {
  color: #267c59;
  background: rgb(70 189 132 / 14%);
}
.status-chip.pending {
  color: #a66c12;
  background: rgb(214 151 50 / 14%);
}
.pin-chip {
  color: var(--accent);
  background: var(--accent-soft);
}
.quick-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
}
.quick-actions button {
  display: inline-flex;
  gap: 4px;
  align-items: center;
  padding: 7px 9px;
  border: 0;
  border-radius: 9px;
  color: var(--accent);
  background: var(--accent-soft);
}
.quick-actions button.danger {
  color: var(--danger);
}
.more-menu {
  position: relative;
}
.more-menu summary {
  display: grid;
  width: 36px;
  height: 36px;
  cursor: pointer;
  list-style: none;
  place-items: center;
  border-radius: 11px;
  color: var(--muted);
}
.more-menu summary::-webkit-details-marker {
  display: none;
}
.more-menu > div {
  position: absolute;
  z-index: 20;
  top: 38px;
  right: 0;
  display: grid;
  width: 145px;
  padding: 6px;
  border-radius: 13px;
}
.more-menu button {
  display: flex;
  gap: 7px;
  align-items: center;
  padding: 8px;
  border: 0;
  border-radius: 8px;
  color: var(--ink);
  background: transparent;
  text-align: left;
}
.more-menu button:hover {
  background: var(--accent-soft);
}
.more-menu button.danger {
  color: var(--danger);
}
.moderation-item:has(.more-menu[open]),
.moderation-reply:has(.more-menu[open]) {
  position: relative;
  z-index: 30;
}
.moderation-thread {
  padding: 0 16px 16px 82px;
  border-top: 1px solid var(--line);
}
.thread-toggle {
  display: inline-flex;
  gap: 6px;
  align-items: center;
  min-height: 38px;
  padding: 8px 2px;
  border: 0;
  color: var(--accent);
  background: transparent;
  font-size: 12px;
  font-weight: 700;
}
.moderation-replies {
  display: grid;
  gap: 8px;
}
.moderation-reply {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto auto;
  gap: 10px;
  align-items: start;
  padding: 13px;
  border: 1px solid var(--line);
  border-radius: 16px;
  background: color-mix(in srgb, var(--glass-strong) 76%, transparent);
}
.moderation-reply .moderation-content :deep(.comment-body) {
  margin: 8px 0;
}
.reply-actions {
  justify-content: flex-end;
}
.reply-field {
  display: grid;
  gap: 7px;
  color: var(--muted);
  font-size: 12px;
  font-weight: 700;
}
.reply-field textarea {
  width: 100%;
  min-height: 132px;
  resize: vertical;
  padding: 12px;
  border: 1px solid var(--line);
  border-radius: 14px;
  color: var(--ink);
  background: var(--glass-strong);
  line-height: 1.65;
}
.reply-composer-tools {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: var(--muted);
  font-size: 11px;
}
.moderation-empty {
  padding: 50px;
  color: var(--muted);
  text-align: center;
}
@media (max-width: 760px) {
  .moderation-root {
    grid-template-columns: auto auto minmax(0, 1fr) auto;
    gap: 9px;
    padding: 13px;
  }
  .moderation-root > .quick-actions {
    grid-row: 2;
    grid-column: 2 / 4;
  }
  .moderation-root > .more-menu {
    grid-column: 4;
    grid-row: 1;
  }
  .moderation-thread {
    padding: 0 12px 12px;
  }
  .moderation-reply {
    grid-template-columns: auto minmax(0, 1fr) auto;
    padding: 11px;
  }
  .moderation-reply > .quick-actions {
    grid-row: 2;
    grid-column: 2 / 3;
    justify-content: flex-start;
  }
  .moderation-reply > .more-menu {
    grid-row: 1;
    grid-column: 3;
  }
  .moderation-content header time {
    width: 100%;
    margin: 0;
  }
  .target-chip {
    max-width: 52vw;
  }
  .moderation-batch {
    align-items: flex-start;
  }
}
</style>
