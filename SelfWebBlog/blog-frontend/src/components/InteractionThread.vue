<script setup>
import { onMounted, ref } from 'vue'
import {
  Heart,
  MessageCircle,
  MoreHorizontal,
  Pin,
  Trash2,
  ChevronDown,
  RefreshCw
} from 'lucide-vue-next'
import { api } from '@/api/client.js'
import { appStore, notify } from '@/stores/app.js'
import { formatDate } from '@/core/format.js'

const props = defineProps({
  targetType: { type: String, required: true },
  targetId: { type: [String, Number], required: true },
  guestbook: Boolean
})
const items = ref([])
const content = ref('')
const replyTo = ref(null)
const replyContent = ref('')
const openReplies = ref(new Set())
const busy = ref(false)
const guestName = ref('')
const captchaAnswer = ref('')
const captcha = ref(null)

async function loadCaptcha() {
  if (props.guestbook && !appStore.user) captcha.value = await api.captcha('guestbook')
}
async function load() {
  try {
    items.value = (await api.thread(props.targetType, props.targetId))?.items || []
    await loadCaptcha()
  } catch {
    items.value = []
  }
}
async function send() {
  if (!content.value.trim()) return
  busy.value = true
  try {
    await api.comment({
      targetType: props.targetType,
      targetId: props.targetId,
      content: content.value.trim(),
      guestName: props.guestbook && !appStore.user ? guestName.value : undefined,
      captchaId: captcha.value?.challengeId,
      captchaAnswer: captchaAnswer.value
    })
    content.value = ''
    captchaAnswer.value = ''
    await load()
    notify('已发送')
  } catch (error) {
    notify(error.message)
    await loadCaptcha()
  } finally {
    busy.value = false
  }
}
async function sendReply(item) {
  if (!replyContent.value.trim()) return
  try {
    await api.reply(item.id, replyContent.value.trim())
    replyContent.value = ''
    replyTo.value = null
    openReplies.value = new Set([...openReplies.value, item.id])
    await load()
  } catch (error) {
    notify(error.message)
  }
}
async function toggleLike(item) {
  const previousLiked = item.liked
  const previousCount = item.likeCount || 0
  item.liked = !previousLiked
  item.likeCount = Math.max(0, previousCount + (item.liked ? 1 : -1))
  try {
    const state = await api.like('COMMENT', item.id, item.liked)
    item.liked = state.liked
    item.likeCount = state.count
  } catch (error) {
    item.liked = previousLiked
    item.likeCount = previousCount
    notify(error.message)
  }
}
function toggleReplies(id) {
  const next = new Set(openReplies.value)
  next.has(id) ? next.delete(id) : next.add(id)
  openReplies.value = next
}
async function adminAction(item, action) {
  try {
    if (action === 'pin') await api.pin(item.id, !item.pinned)
    else await api.deleteInteraction(item.id)
    await load()
  } catch (error) {
    notify(error.message)
  }
}
onMounted(load)
</script>

<template>
  <section class="interaction-section">
    <div class="section-heading">
      <h2>{{ guestbook ? '留言' : '评论' }}</h2>
      <span>{{ items.length }}</span>
    </div>
    <form class="composer glass" @submit.prevent="send">
      <div v-if="guestbook && !appStore.user" class="guest-fields">
        <input v-model="guestName" placeholder="昵称" maxlength="30" required />
        <div class="captcha-inline">
          <img v-if="captcha" :src="captcha.imageDataUrl" alt="验证码" /><button
            type="button"
            class="icon-button"
            aria-label="更换验证码"
            @click="loadCaptcha"
          >
            <RefreshCw :size="16" /></button
          ><input v-model="captchaAnswer" placeholder="验证码" maxlength="12" required />
        </div>
      </div>
      <textarea
        v-model="content"
        :placeholder="appStore.user || guestbook ? '写下内容' : '登录后可以评论'"
        :disabled="!appStore.user && !guestbook"
        maxlength="2000"
      />
      <div>
        <RouterLink v-if="!appStore.user && !guestbook" to="/login" class="text-link"
          >登录</RouterLink
        ><button class="primary-button" :disabled="busy || (!appStore.user && !guestbook)">
          发送
        </button>
      </div>
    </form>
    <div class="thread-list">
      <article
        v-for="item in items"
        :key="item.id"
        :class="['interaction-item', 'glass', { pinned: item.pinned }]"
      >
        <div class="interaction-head">
          <img v-if="item.avatarUrl" :src="item.avatarUrl" alt="" /><span
            v-else
            class="avatar-fallback"
            >{{ (item.nickname || item.guestName || '访').slice(0, 1) }}</span
          >
          <div>
            <strong>{{ item.nickname || item.guestName || '访客' }}</strong
            ><span
              v-if="item.titleName"
              :class="['title-badge', `title-${item.titleStyle || 'default'}`]"
              >{{ item.titleName }}</span
            ><time>{{ formatDate(item.createTime) }}</time>
            <small v-if="item.ipRegion">{{ item.ipRegion }}</small>
          </div>
          <details v-if="appStore.user?.role === 'ADMIN'" class="more-menu">
            <summary aria-label="更多操作"><MoreHorizontal :size="19" /></summary>
            <div class="glass">
              <button @click="adminAction(item, 'pin')">
                <Pin :size="15" />{{ item.pinned ? '取消置顶' : '置顶' }}</button
              ><button class="danger" @click="adminAction(item, 'delete')">
                <Trash2 :size="15" />删除
              </button>
            </div>
          </details>
        </div>
        <p class="interaction-content">{{ item.content }}</p>
        <div class="interaction-actions">
          <button :class="{ active: item.liked }" @click="toggleLike(item)">
            <Heart :size="16" />{{ item.likeCount || 0 }}
          </button>
          <button v-if="appStore.user" @click="replyTo = replyTo === item.id ? null : item.id">
            <MessageCircle :size="16" />回复
          </button>
          <button v-if="item.replies?.length" @click="toggleReplies(item.id)">
            <ChevronDown :class="{ rotated: openReplies.has(item.id) }" :size="16" />{{
              item.replies.length
            }}
            条回复
          </button>
        </div>
        <form v-if="replyTo === item.id" class="reply-composer" @submit.prevent="sendReply(item)">
          <input v-model="replyContent" placeholder="回复内容" maxlength="2000" /><button>
            发送
          </button>
        </form>
        <Transition name="replies"
          ><div v-if="openReplies.has(item.id)" class="reply-list">
            <article v-for="reply in item.replies" :key="reply.id">
              <div>
                <strong>{{ reply.nickname || '用户' }}</strong
                ><span v-if="reply.titleName" class="title-badge">{{ reply.titleName }}</span
                ><time>{{ formatDate(reply.createTime) }}</time
                ><small v-if="reply.ipRegion">{{ reply.ipRegion }}</small>
              </div>
              <p>{{ reply.content }}</p>
            </article>
          </div></Transition
        >
      </article>
      <p v-if="!items.length" class="muted thread-empty">还没有内容</p>
    </div>
  </section>
</template>
