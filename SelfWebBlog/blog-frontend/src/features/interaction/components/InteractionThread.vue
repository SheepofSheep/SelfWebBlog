<script setup>
import { LoaderCircle, LogIn, MessageSquareText, RefreshCw, X } from 'lucide-vue-next'
import { computed, nextTick, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import EmptyState from '../../../components/ui/EmptyState.vue'
import InlineNotice from '../../../components/ui/InlineNotice.vue'
import { showToast } from '../../../composables/toast'
import { useAuthStore } from '../../../stores/authStore'
import {
  createInteraction,
  getInteractionThread,
  replyToInteraction
} from '../api/interactions'
import { insertReply, normalizeThread } from '../model/threadModel'
import GuestbookComposer from './GuestbookComposer.vue'
import InteractionItem from './InteractionItem.vue'
import ReplyComposer from './ReplyComposer.vue'

const props = defineProps({
  targetType: { type: String, required: true },
  targetId: { type: [String, Number], required: true },
  title: { type: String, default: '评论' }
})

const { user } = useAuthStore()
const route = useRoute()
const router = useRouter()
const roots = ref([])
const total = ref(0)
const page = ref(1)
const loading = ref(false)
const loadingMore = ref(false)
const submitting = ref(false)
const errorMessage = ref('')
const replyingTo = ref(null)
const loggedComposer = ref(null)
const pageSize = 20

const isGuestbook = computed(() => props.targetType.toUpperCase() === 'GUESTBOOK')
const canLoadMore = computed(() => roots.value.length < total.value)
const composerLabel = computed(() =>
  replyingTo.value ? `回复 ${replyingTo.value.nickname || '访客'}` : props.title
)

async function load(reset = true) {
  if (reset) {
    loading.value = true
    page.value = 1
  } else {
    loadingMore.value = true
  }
  errorMessage.value = ''
  try {
    const data = await getInteractionThread(props.targetType, props.targetId, page.value, pageSize)
    const incoming = normalizeThread(data)
    roots.value = reset ? incoming : [...roots.value, ...incoming]
    total.value = Number(data.total || roots.value.length)
  } catch (error) {
    errorMessage.value = error.message || '内容加载失败。'
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

async function loadMore() {
  if (!canLoadMore.value || loadingMore.value) return
  page.value += 1
  await load(false)
}

async function beginReply(item) {
  replyingTo.value = item
  if (user.value) {
    await nextTick()
    loggedComposer.value?.focus()
  }
}

function acceptCreated(created) {
  if (created.replyToId) {
    roots.value = insertReply(roots.value, created.rootId || created.replyToId, created)
  } else {
    roots.value = [{ ...created, replies: created.replies || [] }, ...roots.value]
    total.value += 1
  }
  replyingTo.value = null
}

async function submitLogged(content, clear) {
  submitting.value = true
  try {
    const created = replyingTo.value
      ? await replyToInteraction(replyingTo.value.id, content)
      : await createInteraction({
          targetType: props.targetType.toUpperCase(),
          targetId: Number(props.targetId),
          content
        })
    acceptCreated(created)
    clear()
    showToast(created.status === 'PENDING' ? '内容已提交审核。' : '发布成功。', 'success')
  } catch (error) {
    showToast(error.message || '发送失败，请稍后重试。', 'error')
  } finally {
    submitting.value = false
  }
}

function goToLogin() {
  router.push({ name: 'login', query: { redirect: route.fullPath } })
}

watch(
  () => [props.targetType, props.targetId],
  () => load(true),
  { immediate: true }
)
</script>

<template>
  <section class="interaction-thread" :aria-labelledby="`thread-${targetType}-${targetId}`">
    <header class="thread-heading">
      <div>
        <p>交流</p>
        <h2 :id="`thread-${targetType}-${targetId}`">{{ title }}</h2>
      </div>
      <span>{{ total }} 条</span>
    </header>

    <div class="composer-shell">
      <div v-if="replyingTo" class="reply-context">
        <span>{{ composerLabel }}</span>
        <button type="button" aria-label="取消回复" title="取消回复" @click="replyingTo = null">
          <X :size="15" />
        </button>
      </div>

      <ReplyComposer
        v-if="user"
        ref="loggedComposer"
        :placeholder="replyingTo ? '写下回复...' : isGuestbook ? '写下留言...' : '参与这篇文章的讨论...'"
        :submit-label="replyingTo ? '回复' : isGuestbook ? '发布留言' : '发布评论'"
        :busy="submitting"
        @submit="submitLogged"
      />
      <GuestbookComposer
        v-else-if="isGuestbook"
        :target-id="targetId"
        :reply-to-id="replyingTo?.id"
        :submit-label="replyingTo ? '发布回复' : '发布留言'"
        @created="acceptCreated"
      />
      <InlineNotice v-else>
        文章评论需要登录。阅读不受影响，登录后可参与讨论。
        <button class="notice-login" type="button" @click="goToLogin">
          <LogIn :size="15" /> 登录
        </button>
      </InlineNotice>
    </div>

    <InlineNotice v-if="errorMessage" tone="danger">
      {{ errorMessage }}
      <button class="notice-login" type="button" @click="load(true)">
        <RefreshCw :size="15" /> 重试
      </button>
    </InlineNotice>

    <div v-if="loading" class="thread-loading" role="status">
      <LoaderCircle :size="22" /> 正在加载
    </div>
    <EmptyState
      v-else-if="!roots.length && !errorMessage"
      :title="isGuestbook ? '还没有留言' : '还没有评论'"
      :description="isGuestbook ? '第一条留言可以从这里开始。' : '登录后可以留下第一条评论。'"
    >
      <template #icon><MessageSquareText :size="20" /></template>
    </EmptyState>
    <div v-else class="thread-list">
      <template v-for="root in roots" :key="root.id">
        <InteractionItem :item="root" @reply="beginReply" />
        <InteractionItem
          v-for="reply in root.replies || []"
          :key="reply.id"
          :item="reply"
          nested
          @reply="beginReply"
        />
      </template>
    </div>

    <button v-if="canLoadMore" class="load-more" type="button" :disabled="loadingMore" @click="loadMore">
      {{ loadingMore ? '加载中' : '加载更多' }}
    </button>
  </section>
</template>

<style scoped>
.interaction-thread {
  display: grid;
  gap: 20px;
}
.thread-heading,
.reply-context,
.thread-loading,
.notice-login {
  display: flex;
  align-items: center;
}
.thread-heading {
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--border-strong);
}
.thread-heading p,
.thread-heading h2 {
  margin: 0;
}
.thread-heading p {
  margin-bottom: 3px;
  color: var(--accent-strong);
  font-size: 0.7rem;
  font-weight: 800;
}
.thread-heading h2 {
  font-family: var(--font-reading);
  font-size: 1.35rem;
}
.thread-heading > span {
  color: var(--text-tertiary);
  font-size: 0.75rem;
}
.composer-shell {
  padding: 16px;
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-card);
  background: color-mix(in srgb, var(--surface-glass) 88%, transparent);
}
.reply-context {
  justify-content: space-between;
  gap: 10px;
  margin: -3px 0 11px;
  color: var(--accent-strong);
  font-size: 0.76rem;
  font-weight: 700;
}
.reply-context button {
  width: 30px;
  height: 30px;
  display: grid;
  place-items: center;
  padding: 0;
  border: 0;
  border-radius: var(--radius-control);
  background: transparent;
  color: var(--text-secondary);
  cursor: pointer;
}
.notice-login {
  gap: 5px;
  margin-top: 8px;
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--accent-strong);
  font: inherit;
  font-weight: 750;
  cursor: pointer;
}
.thread-loading {
  min-height: 180px;
  justify-content: center;
  gap: 9px;
  color: var(--text-secondary);
  font-size: 0.82rem;
}
.thread-loading svg {
  animation: spin 0.8s linear infinite;
}
.thread-list {
  border-bottom: 1px solid var(--border-subtle);
}
.load-more {
  min-height: 40px;
  justify-self: center;
  padding: 0 16px;
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-control);
  background: transparent;
  color: var(--text-secondary);
  font: inherit;
  cursor: pointer;
}
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
