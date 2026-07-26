<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { Heart, Pencil, ArrowUp, Share2, X } from 'lucide-vue-next'
import QRCode from 'qrcode'
import { api } from '@/api/client.js'
import { appStore, notify } from '@/stores/app.js'
import { formatDate, postTags, postTitle } from '@/core/format.js'
import { headingId } from '@/core/readerModel.js'
import MarkdownBody from '@/components/MarkdownBody.vue'
import InteractionThread from '@/components/InteractionThread.vue'
const route = useRoute()
const post = ref(null)
const liked = ref(false)
const likes = ref(0)
const progress = ref(0)
const readerRoot = ref(null)
const headings = ref([])
const lightbox = ref('')
const shareOpen = ref(false)
const qrCode = ref('')
const tags = computed(() => postTags(post.value))
async function toggleLike() {
  try {
    const data = await api.like('POST', post.value.id, !liked.value)
    liked.value = data.liked
    likes.value = data.count
  } catch (error) {
    notify(error.message)
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
    await navigator.clipboard.writeText(copy.parentElement.querySelector('code')?.textContent || '')
    copy.textContent = '已复制'
    setTimeout(() => {
      copy.textContent = '复制'
    }, 1200)
    return
  }
  const image = event.target.closest('.markdown-body img')
  if (image) lightbox.value = image.currentSrc || image.src
}
async function openShare() {
  qrCode.value = await QRCode.toDataURL(location.href, {
    width: 220,
    margin: 1,
    color: { dark: '#10243d', light: '#ffffff' }
  })
  shareOpen.value = true
}
async function copyShareLink() {
  await navigator.clipboard.writeText(location.href)
  notify('链接已复制')
}
onMounted(async () => {
  post.value = await api.post(route.params.id)
  likes.value = post.value.likeCount || 0
  const likeState = await api.likeState('POST', route.params.id).catch(() => null)
  if (likeState) {
    liked.value = likeState.liked
    likes.value = likeState.count
  }
  api.view(route.params.id).catch(() => {})
  await nextTick()
  enhanceReader()
  addEventListener('scroll', updateProgress, { passive: true })
})
onBeforeUnmount(() => removeEventListener('scroll', updateProgress))
</script>
<template>
  <div v-if="post" class="page article-page">
    <div class="reading-progress" :style="{ width: `${progress}%` }" />
    <header
      class="article-hero glass"
      :style="
        post.coverUrl || post.imageUrl
          ? {
              backgroundImage: `linear-gradient(180deg, transparent, rgba(5,16,32,.72)),url('${post.coverUrl || post.imageUrl}')`,
              backgroundPosition: `${post.backgroundFocusX ?? 50}% ${post.backgroundFocusY ?? 50}%`
            }
          : {}
      "
    >
      <div>
        <div class="post-meta">
          <span>{{ formatDate(post.createTime) }}</span
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
          <button :class="{ active: liked }" @click="toggleLike">
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
    <InteractionThread target-type="POST" :target-id="post.id" />
    <div
      v-if="lightbox"
      class="lightbox"
      role="dialog"
      aria-modal="true"
      @click.self="lightbox = ''"
    >
      <button aria-label="关闭图片" @click="lightbox = ''"><X /></button
      ><img :src="lightbox" alt="" />
    </div>
    <div v-if="shareOpen" class="dialog-backdrop" @click.self="shareOpen = false">
      <section class="share-dialog glass" role="dialog" aria-modal="true">
        <button class="icon-button" aria-label="关闭分享" @click="shareOpen = false"><X /></button
        ><img :src="qrCode" alt="当前文章二维码" /><button
          class="secondary-button"
          @click="copyShareLink"
        >
          复制链接
        </button>
      </section>
    </div>
  </div>
</template>
