<script setup>
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import {
  ArrowDown,
  ArrowUp,
  ChevronDown,
  Heart,
  MessageCircle,
  MoreHorizontal,
  Pin,
  RefreshCw,
  Trash2
} from 'lucide-vue-next'
import { api } from '@/api/client.js'
import { appStore, notify } from '@/stores/app.js'
import { formatDate } from '@/core/format.js'
import { insertStickerToken } from '@/core/emojiCatalog.js'
import {
  appendThreadReply,
  createLatestLikeQueue,
  mergeThreadRoots,
  patchThreadItem,
  replyTargetName,
  scrollBehaviorForMotion,
  sortPinned,
  toggleReplyGroup
} from '@/core/threadModel.js'
import AppAvatar from './AppAvatar.vue'
import CommentBody from './CommentBody.vue'
import ConfirmDialog from './ConfirmDialog.vue'
import EmojiPicker from './EmojiPicker.vue'

const props = defineProps({
  targetType: { type: String, required: true },
  targetId: { type: [String, Number], required: true },
  guestbook: Boolean
})
const items = ref([])
const page = ref(1)
const size = ref(20)
const total = ref(0)
const loading = ref(false)
const loadingMore = ref(false)
const loadError = ref('')
const content = ref('')
const contentInput = ref(null)
const replyTarget = ref(null)
const replyContent = ref('')
const replyInput = ref(null)
const openReplies = ref(new Set())
const busy = ref(false)
const replyBusy = ref(false)
const guestName = ref('')
const captchaAnswer = ref('')
const captcha = ref(null)
const captchaLoading = ref(false)
const captchaError = ref('')
const deleteTarget = ref(null)
const deleteBusy = ref(false)
const likePendingIds = ref(new Set())
const hasMore = computed(() => items.value.length < total.value)
const likeQueues = new Map()
const replyElements = new Map()
let loadGeneration = 0
let captchaGeneration = 0

function displayName(item) {
  return item?.nickname || item?.guestName || '访客'
}

async function loadCaptcha() {
  if (!props.guestbook || appStore.user) return
  const generation = ++captchaGeneration
  captcha.value = null
  captchaAnswer.value = ''
  captchaLoading.value = true
  captchaError.value = ''
  try {
    const value = await api.captcha('guestbook')
    if (generation === captchaGeneration) captcha.value = value
  } catch (error) {
    if (generation === captchaGeneration) captchaError.value = error.message || '验证码加载失败'
  } finally {
    if (generation === captchaGeneration) captchaLoading.value = false
  }
}

async function fetchPage(nextPage, append, generation) {
  loadError.value = ''
  try {
    const response =
      (await api.thread(props.targetType, props.targetId, nextPage, size.value)) || {}
    if (generation !== loadGeneration) return false
    const roots = Array.isArray(response.items) ? response.items : []
    items.value = append ? mergeThreadRoots(items.value, roots) : sortPinned(roots)
    page.value = Number(response.page || nextPage)
    size.value = Number(response.size || size.value)
    total.value = Number(response.total ?? items.value.length)
    return true
  } catch (error) {
    if (generation !== loadGeneration) return false
    if (!append) {
      items.value = []
      total.value = 0
    }
    loadError.value = error.message || '内容加载失败，请重试'
    return false
  }
}

async function resetThread() {
  const generation = ++loadGeneration
  captchaGeneration += 1
  likeQueues.clear()
  likePendingIds.value = new Set()
  replyElements.clear()
  items.value = []
  page.value = 1
  size.value = 20
  total.value = 0
  openReplies.value = new Set()
  replyTarget.value = null
  replyContent.value = ''
  content.value = ''
  captcha.value = null
  captchaLoading.value = false
  captchaError.value = ''
  loadingMore.value = false
  loading.value = true
  loadError.value = ''
  await fetchPage(1, false, generation)
  if (generation !== loadGeneration) return
  loading.value = false
  await loadCaptcha()
}

async function loadMore() {
  if (loadingMore.value || !hasMore.value) return
  loadingMore.value = true
  const generation = loadGeneration
  try {
    await fetchPage(page.value + 1, true, generation)
  } finally {
    if (generation === loadGeneration) loadingMore.value = false
  }
}

async function refreshLoadedPages() {
  const generation = ++loadGeneration
  loadError.value = ''
  const pagesToRefresh = Math.max(1, page.value)
  const pageSize = size.value
  let refreshed = []
  let refreshedTotal = total.value
  let refreshedPage = 1
  loading.value = true
  loadingMore.value = false
  try {
    for (let nextPage = 1; nextPage <= pagesToRefresh; nextPage += 1) {
      const response =
        (await api.thread(props.targetType, props.targetId, nextPage, pageSize)) || {}
      if (generation !== loadGeneration) return false
      refreshed = mergeThreadRoots(refreshed, Array.isArray(response.items) ? response.items : [])
      refreshedTotal = Number(response.total ?? refreshed.length)
      refreshedPage = Number(response.page || nextPage)
      if (nextPage * pageSize >= refreshedTotal) break
    }
    items.value = refreshed
    total.value = refreshedTotal
    page.value = refreshedPage
    return true
  } catch (error) {
    if (generation === loadGeneration) loadError.value = error.message || '内容加载失败，请重试'
    return false
  } finally {
    if (generation === loadGeneration) loading.value = false
  }
}

async function insertSticker(model, input, sticker) {
  const element = input.value
  const from = element?.selectionStart ?? model.value.length
  const to = element?.selectionEnd ?? from
  const next = insertStickerToken(model.value, from, to, sticker.name)
  model.value = next.text
  await nextTick()
  element?.focus()
  element?.setSelectionRange(next.caret, next.caret)
}

function insertContentSticker(sticker) {
  return insertSticker(content, contentInput, sticker)
}

function insertReplySticker(sticker) {
  const input = Array.isArray(replyInput.value) ? replyInput.value[0] : replyInput.value
  return insertSticker(replyContent, { value: input }, sticker)
}

async function send() {
  if (busy.value || !content.value.trim()) return
  if (props.guestbook && !appStore.user && (!captcha.value || captchaLoading.value))
    return notify('请先加载验证码')
  const submitted = content.value
  const generation = loadGeneration
  busy.value = true
  try {
    const created = await api.comment({
      targetType: props.targetType,
      targetId: props.targetId,
      content: submitted.trim(),
      guestName: props.guestbook && !appStore.user ? guestName.value : undefined,
      captchaId: captcha.value?.challengeId,
      captchaAnswer: captchaAnswer.value
    })
    if (generation !== loadGeneration) return
    if (created) {
      const alreadyLoaded = items.value.some((item) => String(item.id) === String(created.id))
      items.value = mergeThreadRoots(items.value, [
        { ...created, replies: Array.isArray(created.replies) ? created.replies : [] }
      ])
      if (!alreadyLoaded) total.value += 1
    }
    if (content.value === submitted) content.value = ''
    captchaAnswer.value = ''
    await loadCaptcha()
    notify('已发送')
  } catch (error) {
    if (generation === loadGeneration) {
      notify(error.message)
      await loadCaptcha()
    }
  } finally {
    busy.value = false
  }
}

async function beginReply(root, target) {
  if (replyBusy.value) return
  const sameTarget = replyTarget.value?.targetId === target.id
  replyTarget.value = sameTarget
    ? null
    : {
        rootId: root.id,
        targetId: target.id,
        name: displayName(target)
      }
  if (sameTarget) return
  openReplies.value = new Set([...openReplies.value, root.id])
  await nextTick()
  const input = Array.isArray(replyInput.value) ? replyInput.value[0] : replyInput.value
  input?.focus()
}

async function sendReply() {
  if (replyBusy.value || !replyContent.value.trim() || !replyTarget.value) return
  const rootId = replyTarget.value.rootId
  const submitted = replyContent.value
  const generation = loadGeneration
  replyBusy.value = true
  try {
    const created = await api.reply(replyTarget.value.targetId, submitted.trim())
    if (generation !== loadGeneration) return
    if (created) items.value = appendThreadReply(items.value, rootId, created)
    if (replyContent.value === submitted) {
      replyContent.value = ''
      replyTarget.value = null
    }
    openReplies.value = new Set([...openReplies.value, rootId])
    notify('已回复')
    if (created?.id != null) await focusNewReply(created.id)
  } catch (error) {
    if (generation === loadGeneration) notify(error.message)
  } finally {
    replyBusy.value = false
  }
}

function setLikePending(id, pending) {
  const next = new Set(likePendingIds.value)
  pending ? next.add(String(id)) : next.delete(String(id))
  likePendingIds.value = next
}

function likePending(id) {
  return likePendingIds.value.has(String(id))
}

function toggleLike(item) {
  const id = String(item.id)
  let queue = likeQueues.get(id)
  if (!queue) {
    queue = createLatestLikeQueue(
      { liked: item.liked, count: item.likeCount },
      {
        request: (liked) => api.like('COMMENT', item.id, liked),
        onChange: (state) => {
          items.value = patchThreadItem(items.value, id, (current) => ({
            ...current,
            liked: state.liked,
            likeCount: state.count
          }))
          setLikePending(id, state.pending)
          if (!state.pending && likeQueues.get(id) === queue) likeQueues.delete(id)
        },
        onError: (error) => notify(error.message)
      }
    )
    likeQueues.set(id, queue)
  }
  queue.toggle()
}

function setReplyElement(id, element) {
  const key = String(id)
  element ? replyElements.set(key, element) : replyElements.delete(key)
}

async function focusNewReply(id) {
  await nextTick()
  const element = replyElements.get(String(id))
  if (!element) return
  const reducedMotion =
    document.documentElement.dataset.motion === 'reduced' ||
    window.matchMedia('(prefers-reduced-motion: reduce)').matches
  element.focus({ preventScroll: true })
  element.scrollIntoView({
    behavior: scrollBehaviorForMotion(reducedMotion),
    block: 'nearest'
  })
}

function toggleReplies(id) {
  openReplies.value = toggleReplyGroup(openReplies.value, id)
}

function closeMenu(event) {
  event?.currentTarget?.closest('details')?.removeAttribute('open')
}

async function pinItem(item, event) {
  closeMenu(event)
  try {
    await api.pin(item.id, !item.pinned)
    await refreshLoadedPages()
    notify(item.pinned ? '已取消置顶' : '已置顶')
  } catch (error) {
    notify(error.message)
  }
}

async function movePinned(item, direction, event) {
  closeMenu(event)
  try {
    await api.movePin(item.id, direction)
    await refreshLoadedPages()
  } catch (error) {
    notify(error.message)
  }
}

function requestDelete(item, event) {
  closeMenu(event)
  deleteTarget.value = item
}

async function confirmDelete() {
  if (!deleteTarget.value || deleteBusy.value) return
  const target = deleteTarget.value
  deleteBusy.value = true
  try {
    await api.deleteInteraction(target.id)
    const targetId = String(target.id)
    const deletingRoot = items.value.some((item) => String(item.id) === targetId)
    if (deletingRoot) {
      const refreshed = await refreshLoadedPages()
      if (!refreshed) {
        items.value = items.value.filter((item) => String(item.id) !== targetId)
        total.value = Math.max(0, total.value - 1)
        page.value = Math.max(0, page.value - 1)
      }
    } else {
      items.value = items.value.map((item) => ({
        ...item,
        replies: (item.replies || []).filter((reply) => String(reply.id) !== targetId)
      }))
    }
    deleteTarget.value = null
    notify('已删除')
  } catch (error) {
    notify(error.message)
  } finally {
    deleteBusy.value = false
  }
}

watch([() => props.targetType, () => props.targetId], resetThread, { immediate: true })
onBeforeUnmount(() => {
  loadGeneration += 1
  captchaGeneration += 1
})
</script>

<template>
  <section class="interaction-section">
    <div class="section-heading">
      <h2>{{ guestbook ? '留言' : '评论' }}</h2>
      <span>{{ total }}</span>
    </div>

    <form class="composer glass" @submit.prevent="send">
      <div v-if="guestbook && !appStore.user" class="guest-fields">
        <input
          v-model="guestName"
          aria-label="留言昵称"
          placeholder="昵称"
          maxlength="30"
          required
        />
        <div class="captcha-inline">
          <img v-if="captcha" :src="captcha.imageDataUrl" alt="验证码" />
          <button
            type="button"
            class="icon-button"
            aria-label="更换验证码"
            :disabled="captchaLoading || busy"
            @click="loadCaptcha"
          >
            <RefreshCw :size="16" />
          </button>
          <input
            v-model="captchaAnswer"
            aria-label="留言验证码"
            placeholder="验证码"
            maxlength="12"
            :disabled="captchaLoading || !captcha || busy"
            autocomplete="off"
            required
          />
        </div>
        <p v-if="captchaLoading" class="muted" aria-live="polite">正在加载验证码…</p>
        <p v-if="captchaError" class="error-text" role="alert">
          {{ captchaError }}，请点击刷新按钮重试
        </p>
      </div>
      <textarea
        ref="contentInput"
        v-model="content"
        aria-label="评论或留言内容"
        :placeholder="appStore.user || guestbook ? '写下内容' : '登录后可以评论'"
        :disabled="!appStore.user && !guestbook"
        maxlength="2000"
      />
      <div class="composer-footer">
        <div>
          <EmojiPicker v-if="appStore.user || guestbook" @select="insertContentSticker" />
          <RouterLink
            v-if="!appStore.user && !guestbook"
            :to="{ path: '/login', query: { redirect: $route.fullPath } }"
            class="text-link"
          >
            登录
          </RouterLink>
        </div>
        <button
          class="primary-button"
          :disabled="
            busy ||
            !content.trim() ||
            (!appStore.user && !guestbook) ||
            (guestbook && !appStore.user && (!captcha || captchaLoading))
          "
        >
          {{ busy ? '发送中' : '发送' }}
        </button>
      </div>
    </form>

    <div class="thread-list">
      <article
        v-for="item in items"
        :key="item.id"
        :data-interaction-id="String(item.id)"
        :class="['interaction-item', 'glass', { pinned: item.pinned }]"
      >
        <div class="interaction-head">
          <AppAvatar :src="item.avatarUrl" :name="displayName(item)" :size="40" rounded />
          <div>
            <strong>{{ displayName(item) }}</strong>
            <span
              v-if="item.titleName"
              :class="['title-badge', `title-${item.titleStyle || 'default'}`]"
            >
              {{ item.titleName }}
            </span>
            <span v-if="item.pinned" class="pin-badge"><Pin :size="11" />置顶</span>
            <time>{{ formatDate(item.createTime) }}</time>
            <small v-if="item.ipRegion">{{ item.ipRegion }}</small>
          </div>
          <details v-if="appStore.user?.role === 'ADMIN'" class="more-menu">
            <summary aria-label="更多操作"><MoreHorizontal :size="19" /></summary>
            <div class="glass">
              <button type="button" @click="pinItem(item, $event)">
                <Pin :size="15" />{{ item.pinned ? '取消置顶' : '置顶' }}
              </button>
              <template v-if="item.pinned">
                <button type="button" @click="movePinned(item, 'UP', $event)">
                  <ArrowUp :size="15" />上移
                </button>
                <button type="button" @click="movePinned(item, 'DOWN', $event)">
                  <ArrowDown :size="15" />下移
                </button>
              </template>
              <button class="danger" type="button" @click="requestDelete(item, $event)">
                <Trash2 :size="15" />删除
              </button>
            </div>
          </details>
        </div>

        <CommentBody class="interaction-content" :content="item.content" />
        <div class="interaction-actions">
          <button
            type="button"
            :class="{ active: item.liked }"
            :aria-busy="likePending(item.id)"
            @click="toggleLike(item)"
          >
            <Heart :size="16" :fill="item.liked ? 'currentColor' : 'none'" />{{
              item.likeCount || 0
            }}
          </button>
          <button
            v-if="appStore.user"
            type="button"
            :disabled="replyBusy"
            @click="beginReply(item, item)"
          >
            <MessageCircle :size="16" />回复
          </button>
          <button v-if="item.replies?.length" type="button" @click="toggleReplies(item.id)">
            <ChevronDown :class="{ rotated: openReplies.has(item.id) }" :size="16" />
            {{ item.replies.length }} 条回复
          </button>
        </div>

        <form
          v-if="replyTarget?.rootId === item.id"
          class="reply-composer"
          @submit.prevent="sendReply"
        >
          <div class="reply-composer-head">
            <span>回复 {{ replyTarget.name }}</span>
            <button type="button" :disabled="replyBusy" @click="replyTarget = null">取消</button>
          </div>
          <textarea
            ref="replyInput"
            v-model="replyContent"
            placeholder="回复内容"
            maxlength="2000"
          />
          <footer>
            <div :inert="replyBusy"><EmojiPicker @select="insertReplySticker" /></div>
            <button class="primary-button" :disabled="replyBusy || !replyContent.trim()">
              {{ replyBusy ? '发送中' : '发送' }}
            </button>
          </footer>
        </form>

        <Transition name="replies">
          <div v-if="openReplies.has(item.id)" class="reply-list">
            <article
              v-for="reply in item.replies"
              :key="reply.id"
              :ref="(element) => setReplyElement(reply.id, element)"
              :data-interaction-id="String(reply.id)"
              tabindex="-1"
            >
              <div class="reply-head">
                <AppAvatar :src="reply.avatarUrl" :name="displayName(reply)" :size="34" rounded />
                <div>
                  <strong>{{ displayName(reply) }}</strong>
                  <span
                    v-if="reply.titleName"
                    :class="['title-badge', `title-${reply.titleStyle || 'default'}`]"
                  >
                    {{ reply.titleName }}
                  </span>
                  <span v-if="replyTargetName(item, reply, item.replies)" class="reply-context">
                    回复 {{ replyTargetName(item, reply, item.replies) }}
                  </span>
                  <time>{{ formatDate(reply.createTime) }}</time>
                  <small v-if="reply.ipRegion">{{ reply.ipRegion }}</small>
                </div>
                <details v-if="appStore.user?.role === 'ADMIN'" class="more-menu">
                  <summary aria-label="更多操作"><MoreHorizontal :size="18" /></summary>
                  <div class="glass">
                    <button class="danger" type="button" @click="requestDelete(reply, $event)">
                      <Trash2 :size="15" />删除
                    </button>
                  </div>
                </details>
              </div>
              <CommentBody class="reply-content" :content="reply.content" />
              <div class="interaction-actions reply-actions">
                <button
                  type="button"
                  :class="{ active: reply.liked }"
                  :aria-busy="likePending(reply.id)"
                  @click="toggleLike(reply)"
                >
                  <Heart :size="15" :fill="reply.liked ? 'currentColor' : 'none'" />{{
                    reply.likeCount || 0
                  }}
                </button>
                <button
                  v-if="appStore.user"
                  type="button"
                  :disabled="replyBusy"
                  @click="beginReply(item, reply)"
                >
                  <MessageCircle :size="15" />回复
                </button>
              </div>
            </article>
          </div>
        </Transition>
      </article>
      <p v-if="loading" class="muted thread-empty" aria-live="polite">
        正在加载{{ guestbook ? '留言' : '评论' }}…
      </p>
      <div v-else-if="loadError" class="thread-empty" role="alert">
        <p class="error-text">{{ loadError }}</p>
        <button type="button" class="secondary-button" @click="refreshLoadedPages">重新加载</button>
      </div>
      <p v-else-if="!items.length" class="muted thread-empty">
        {{ guestbook ? '还没有留言，来打个招呼吧' : '还没有评论，欢迎分享你的看法' }}
      </p>
    </div>

    <div v-if="hasMore" class="thread-load-more">
      <button type="button" :disabled="loadingMore" @click="loadMore">
        {{ loadingMore ? '加载中…' : `加载更多${guestbook ? '留言' : '评论'}` }}
        <span>{{ items.length }} / {{ total }}</span>
      </button>
    </div>

    <ConfirmDialog
      :open="Boolean(deleteTarget)"
      title="删除内容"
      :message="deleteTarget ? `确认删除“${displayName(deleteTarget)}”的内容？` : ''"
      confirm-label="删除"
      danger
      :busy="deleteBusy"
      @close="!deleteBusy && (deleteTarget = null)"
      @confirm="confirmDelete"
    />
  </section>
</template>

<style scoped>
.composer-footer,
.composer-footer > div,
.reply-composer footer,
.reply-composer-head,
.reply-head {
  display: flex;
  align-items: center;
}
.composer-footer {
  justify-content: space-between;
}
.composer-footer > div {
  gap: 8px;
}
.interaction-head :deep(.app-avatar) {
  border-radius: 14px;
}
.pin-badge,
.reply-context {
  display: inline-flex;
  gap: 3px;
  align-items: center;
  color: var(--accent);
  font-size: 10px;
}
.interaction-content {
  margin: 15px 0;
}
.reply-composer {
  display: grid;
  gap: 8px;
  margin-top: 13px;
  padding: 12px;
  border: 1px solid var(--line);
  border-radius: 16px;
  background: color-mix(in srgb, var(--accent-soft) 38%, transparent);
}
.reply-composer-head {
  justify-content: space-between;
  color: var(--muted);
  font-size: 12px;
}
.reply-composer-head button {
  padding: 4px 7px;
  border: 0;
  color: var(--muted);
  background: transparent;
  font-size: 11px;
}
.reply-composer textarea {
  width: 100%;
  min-height: 76px;
  resize: vertical;
  padding: 10px 12px;
  border: 1px solid var(--line);
  border-radius: 12px;
  outline: 0;
  background: var(--glass-strong);
}
.reply-composer textarea:focus {
  border-color: var(--accent);
}
.reply-composer footer {
  justify-content: space-between;
}
.reply-composer footer .primary-button {
  padding: 8px 14px;
}
.reply-list article {
  position: relative;
}
.reply-head {
  gap: 9px;
  align-items: flex-start;
}
.reply-head > div {
  display: flex;
  min-width: 0;
  flex: 1;
  flex-wrap: wrap;
  gap: 5px;
  align-items: center;
}
.reply-head time {
  width: 100%;
}
.reply-head small {
  color: var(--muted);
  font-size: 10px;
}
.reply-content {
  margin: 9px 0 4px 43px;
}
.reply-actions {
  margin-left: 35px;
}
.more-menu .danger {
  color: var(--danger);
}
.interaction-item:has(.more-menu[open]),
.reply-list article:has(.more-menu[open]) {
  position: relative;
  z-index: 30;
}
.thread-load-more {
  display: flex;
  justify-content: center;
  margin-top: 18px;
}
.thread-load-more button {
  display: inline-flex;
  gap: 8px;
  align-items: center;
  padding: 9px 14px;
  border: 1px solid var(--line);
  border-radius: 999px;
  color: var(--ink);
  background: var(--glass-strong);
}
.thread-load-more button:hover:not(:disabled) {
  border-color: var(--accent);
  color: var(--accent);
}
.thread-load-more span {
  color: var(--muted);
  font-size: 11px;
}
@media (max-width: 620px) {
  .interaction-item {
    padding: 16px;
  }
  .reply-list {
    margin-left: 8px;
    padding-left: 10px;
  }
  .reply-content {
    margin-left: 0;
  }
  .reply-actions {
    margin-left: -8px;
  }
}
</style>
