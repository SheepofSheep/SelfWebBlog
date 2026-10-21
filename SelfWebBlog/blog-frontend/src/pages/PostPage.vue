<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { Heart, Pencil, ArrowUp, RotateCcw, Share2 } from 'lucide-vue-next'
import QRCode from 'qrcode'
import { api } from '@/api/client.js'
import { appStore, notify } from '@/stores/app.js'
import { formatDate, postPublicationTime, postTags, postTitle } from '@/core/format.js'
import { headingId } from '@/core/readerModel.js'
import MarkdownBody from '@/components/MarkdownBody.vue'
import InteractionThread from '@/components/InteractionThread.vue'
import AppDialog from '@/components/AppDialog.vue'
import EmptyState from '@/components/EmptyState.vue'
const route = useRoute()
const post = ref(null)
const loading = ref(true)
const error = ref('')
const liked = ref(false)
const likes = ref(0)
const likeBusy = ref(false)
const progress = ref(0)
const readerRoot = ref(null)
const headings = ref([])
const lightbox = ref('')
const shareOpen = ref(false)
const qrCode = ref('')
const tags = computed(() => postTags(post.value))
let mounted = false
let loadTicket = 0
let scrollListening = false

function detachScroll() {
  if (!scrollListening) return
  removeEventListener('scroll', updateProgress)
  scrollListening = false
}

function resetReader() {
  detachScroll()
  post.value = null
  liked.value = false
  likes.value = 0
  likeBusy.value = false
  progress.value = 0
  headings.value = []
  lightbox.value = ''
  shareOpen.value = false
  qrCode.value = ''
}
async function toggleLike() {
  if (likeBusy.value) return
  likeBusy.value = true
  try {
    const data = await api.like('POST', post.value.id, !liked.value)
    liked.value = data.liked
    likes.value = data.count
  } catch (error) {
    notify(error.message)
  } finally {
    likeBusy.value = false
  }
}
function updateProgress() {
  const max = document.documentElement.scrollHeight - innerHeight
  progress.value = max > 0 ? (scrollY / max) * 100 : 0
}
function enhanceReader() {
  const root = readerRoot.value?.querySelector('.markdown-body')
  if (!root) return
  headings.value = [...root.querySelectorAll('h2,h3')].map((element, index) => {
    element.id = headingId(element.textContent, index)
    return { id: element.id, text: element.textContent, level: element.tagName.toLowerCase() }
  })
  root.querySelectorAll('pre').forEach((element) => {
    if (element.querySelector('.code-copy')) return
    const button = document.createElement('button')
    button.className = 'code-copy'
    button.type = 'button'
    button.textContent = '复制'
    element.append(button)
  })
}
async function readerClick(event) {
  const copy = event.target.closest('.code-copy')
  if (copy) {
    try {
      await navigator.clipboard.writeText(
        copy.parentElement.querySelector('code')?.textContent || ''
      )
      copy.textContent = '已复制'
      setTimeout(() => {
        copy.textContent = '复制'
      }, 1200)
    } catch {
      notify('复制失败，请手动选择代码')
    }
    return
  }
  const image = event.target.closest('.markdown-body img')
  if (image) lightbox.value = image.currentSrc || image.src
}
async function openShare() {
  try {
    qrCode.value = await QRCode.toDataURL(location.href, {
      width: 220,
      margin: 1,
      color: { dark: '#10243d', light: '#ffffff' }
    })
    shareOpen.value = true
  } catch {
    notify('分享二维码生成失败')
  }
}
async function copyShareLink() {
  try {
    await navigator.clipboard.writeText(location.href)
    notify('链接已复制')
  } catch {
    notify('复制失败，请从地址栏复制')
  }
}
async function loadPost() {
  const id = String(route.params.id || '')
  const ticket = ++loadTicket
  resetReader()
  loading.value = true
  error.value = ''
  try {
    const value = await api.post(id)
    if (!mounted || ticket !== loadTicket) return
    if (!value) throw new Error('文章不存在或不可访问')
    post.value = value
    likes.value = value.likeCount || 0
    loading.value = false
    const likeStateRequest = api.likeState('POST', id).catch(() => null)
    api.view(id).catch(() => {})
    await nextTick()
    if (!mounted || ticket !== loadTicket) return
    enhanceReader()
    addEventListener('scroll', updateProgress, { passive: true })
    scrollListening = true
    updateProgress()
    const likeState = await likeStateRequest
    if (!mounted || ticket !== loadTicket || !likeState) return
    liked.value = likeState.liked
    likes.value = likeState.count
  } catch (loadError) {
    if (!mounted || ticket !== loadTicket) return
    error.value = loadError?.message || '文章加载失败'
    loading.value = false
  }
}

onMounted(() => {
  mounted = true
  loadPost()
})
onBeforeUnmount(() => {
  mounted = false
  loadTicket += 1
  detachScroll()
})
</script>
<template>
  <div v-if="loading" class="page article-page">
    <div class="article-loading glass" aria-live="polite">加载中</div>
  </div>
  <div v-else-if="error" class="page article-page">
    <EmptyState :label="error">
      <button class="secondary-button" type="button" @click="loadPost">
        <RotateCcw :size="16" />重试
      </button>
    </EmptyState>
  </div>
  <div v-else-if="post" class="page article-page">
    <div class="reading-progress" :style="{ width: `${progress}%` }" />
    <header
      :class="['article-hero', 'glass', { 'has-cover': post.coverUrl || post.imageUrl }]"
      :style="
        post.coverUrl || post.imageUrl
          ? {
              backgroundImage: `url('${post.coverUrl || post.imageUrl}')`,
              backgroundPosition: `${post.backgroundFocusX ?? 50}% ${post.backgroundFocusY ?? 50}%`
            }
          : {}
      "
    >
      <div>
        <div class="post-meta">
          <span>{{ formatDate(postPublicationTime(post)) }}</span
          ><span v-if="post.category">{{ post.category }}</span>
        </div>
        <h1 v-if="postTitle(post)">{{ postTitle(post) }}</h1>
        <div class="tag-row">
          <RouterLink v-for="tag in tags" :key="tag" :to="`/tags?tag=${encodeURIComponent(tag)}`">{{
            tag
          }}</RouterLink>
        </div>
      </div>
    </header>
    <div class="article-layout">
      <article ref="readerRoot" class="article-surface glass" @click="readerClick">
        <MarkdownBody :content="post.content" />
      </article>
      <aside class="article-side">
        <nav v-if="headings.length" class="article-toc glass" aria-label="文章目录">
          <strong>目录</strong
          ><a
            v-for="heading in headings"
            :key="heading.id"
            :class="heading.level"
            :href="`#${heading.id}`"
            >{{ heading.text }}</a
          >
        </nav>
        <div class="article-tools glass">
          <button :class="{ active: liked }" :disabled="likeBusy" @click="toggleLike">
            <Heart :size="19" />{{ likes }}</button
          ><RouterLink v-if="appStore.user?.role === 'ADMIN'" :to="`/write/${post.id}`"
            ><Pencil :size="19" />编辑</RouterLink
          ><button @click="openShare"><Share2 :size="19" />分享</button
          ><button @click="scrollTo({ top: 0, behavior: 'smooth' })">
            <ArrowUp :size="19" />顶部
          </button>
        </div>
      </aside>
    </div>
    <InteractionThread :key="post.id" target-type="POST" :target-id="post.id" />
    <AppDialog :open="Boolean(lightbox)" title="图片预览" size="large" @close="lightbox = ''">
      <img class="article-lightbox-image" :src="lightbox" alt="文章图片预览" />
    </AppDialog>
    <AppDialog :open="shareOpen" title="分享" size="small" @close="shareOpen = false">
      <div class="article-share-content">
        <img :src="qrCode" alt="当前文章二维码" />
        <button class="secondary-button" type="button" @click="copyShareLink">复制链接</button>
      </div>
    </AppDialog>
  </div>
</template>

<style scoped>
.article-loading {
  display: grid;
  min-height: 360px;
  place-items: center;
  border-radius: var(--radius-xl);
  color: var(--muted);
}
.article-lightbox-image {
  width: 100%;
  max-height: min(72vh, 760px);
  border-radius: 18px;
  object-fit: contain;
}
.article-share-content {
  display: grid;
  gap: 18px;
  justify-items: center;
}
.article-share-content img {
  width: min(220px, 100%);
  border-radius: 16px;
}
</style>
