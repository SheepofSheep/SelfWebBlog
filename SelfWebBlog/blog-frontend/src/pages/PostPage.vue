<script setup>
import {
  ref,
  computed,
  onMounted,
  onBeforeUnmount,
  inject,
  nextTick,
  watch,
  defineAsyncComponent
} from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getPost } from '../api'
import { showToast } from '../composables/toast'
import { renderArticleMarkdown } from '../utils/marked'
import loadingStore from '../stores/loadingStore'
import InteractionThread from '../features/interaction/components/InteractionThread.vue'
import LikeButton from '../features/interaction/components/LikeButton.vue'
import { recordPostView } from '../features/interaction/api/interactions'
import { ArrowLeft, Clock, Tag, Folder, RefreshCw, Eye, Share2, Edit3 } from 'lucide-vue-next'
import { formatTime } from '../utils/format'
import { optimizedImageSrcset, optimizedImageUrl } from '../utils/url'

const route = useRoute()
const router = useRouter()
const navigate = (to) => router.push(to)

function goBack() {
  window.history.back()
}

function editPost() {
  if (!post.value) return
  sessionStorage.setItem(
    'editingPost',
    JSON.stringify({
      id: post.value.id,
      title: post.value.title,
      content: post.value.content,
      summary: post.value.summary || '',
      coverUrl: post.value.coverUrl || '',
      category: post.value.category || '',
      tags: post.value.tags || '',
      postStatus: post.value.status || 'PUBLISHED'
    })
  )
  navigate('/write')
}

const postId = computed(() => String(route.params.id || ''))
const post = ref(null)
const user = inject('user', null)
const refreshUser = inject('refreshUser', null)
const articleRef = ref(null)
const readingProgress = ref(0)
const activeHeading = ref('')
const previewImage = ref('')
const shareOpen = ref(false)
const ShareCardDialog = defineAsyncComponent(
  () => import('../features/site/components/ShareCardDialog.vue')
)
const shareUrl = computed(() =>
  typeof window === 'undefined' ? '' : `${window.location.origin}${route.fullPath}`
)

const renderedContent = computed(() =>
  post.value ? renderArticleMarkdown(post.value.content) : ''
)

const readTime = computed(() => {
  if (!post.value?.content) return 1
  const text = post.value.content.replace(/[#*_`[\]!()>\-\s]/g, '')
  return Math.max(1, Math.ceil(text.length / 400))
})

const tagList = computed(() => {
  if (!post.value?.tags) return []
  return post.value.tags.split(/[,，\s]+/).filter(Boolean)
})

const tocItems = computed(() => {
  if (!post.value?.content) return []
  const headings = []
  const pattern = /^(#{2,3})\s+(.+)$/gm
  let match
  while ((match = pattern.exec(post.value.content))) {
    const text = match[2].replace(/[#*_`]/g, '').trim()
    if (!text) continue
    headings.push({
      id: `section-${headings.length + 1}`,
      text,
      level: match[1].length
    })
  }
  return headings.slice(0, 12)
})

watch(renderedContent, async () => {
  await nextTick()
  prepareArticleEnhancements()
})

async function loadPost() {
  if (!postId.value) return
  try {
    post.value = await getPost(postId.value)
    loadingStore.setRouterLoading(false)
  } catch (error) {
    showToast('加载文章失败：' + error.message)
    loadingStore.setRouterLoading(false)
  }
}

function prepareArticleEnhancements() {
  const root = articleRef.value
  if (!root) return
  const headings = [...root.querySelectorAll('h2, h3')]
  headings.forEach((heading, index) => {
    if (tocItems.value[index]) heading.id = tocItems.value[index].id
  })
  root.querySelectorAll('img').forEach((img) => {
    img.setAttribute('loading', 'lazy')
    img.classList.add('clickable-image')
  })
}

function scrollToHeading(id) {
  const target = document.getElementById(id)
  if (!target) return
  target.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function updateReadingProgress() {
  const root = articleRef.value
  if (!root) return
  const rect = root.getBoundingClientRect()
  const total = Math.max(1, root.scrollHeight - window.innerHeight * 0.55)
  const read = Math.min(total, Math.max(0, -rect.top + 120))
  readingProgress.value = Math.round((read / total) * 100)

  let current = ''
  for (const item of tocItems.value) {
    const el = document.getElementById(item.id)
    if (el && el.getBoundingClientRect().top <= 160) current = item.id
  }
  activeHeading.value = current || tocItems.value[0]?.id || ''
}

function handleArticleClick(event) {
  const target = event.target
  if (target?.tagName === 'IMG') {
    previewImage.value = target.getAttribute('src') || ''
  }
}

onMounted(async () => {
  if (refreshUser) await refreshUser()
  await loadPost()
  if (post.value) {
    recordPostView(postId.value).catch(() => {})
  }
  await nextTick()
  prepareArticleEnhancements()
  updateReadingProgress()
  window.addEventListener('scroll', updateReadingProgress, { passive: true })
})

onBeforeUnmount(() => {
  window.removeEventListener('scroll', updateReadingProgress)
})
</script>

<template>
  <div class="post-page">
    <div class="reading-progress" aria-hidden="true">
      <span :style="{ width: readingProgress + '%' }"></span>
    </div>

    <div v-if="post" class="post-container glass-card">
      <div class="post-top-actions">
        <button class="back-btn" @click="goBack"><ArrowLeft :size="16" /> 返回</button>
        <button
          v-if="user?.role === 'ADMIN'"
          class="edit-btn"
          aria-label="编辑文章"
          @click="editPost"
        >
          <Edit3 :size="14" /> 编辑
        </button>
      </div>

      <h1 class="post-title">{{ post.title }}</h1>

      <!-- 摘要 -->
      <p v-if="post.summary" class="post-summary">{{ post.summary }}</p>

      <!-- 元信息 -->
      <div class="post-meta">
        <span class="meta-item"><Clock :size="14" /> {{ formatTime(post.createTime) }}</span>
        <span v-if="post.updateTime && post.updateTime !== post.createTime" class="meta-item"
          ><RefreshCw :size="14" /> {{ formatTime(post.updateTime) }}</span
        >
        <span class="meta-item"
          ><Eye :size="14" /> {{ post.viewCount || 0 }} 次浏览 · 约 {{ readTime }} 分钟</span
        >
        <span v-if="post.category" class="meta-item"
          ><Folder :size="14" /> {{ post.category }}</span
        >
      </div>

      <!-- 标签 -->
      <div v-if="tagList.length" class="post-tags">
        <Tag :size="14" />
        <span v-for="t in tagList" :key="t" class="tag">{{ t }}</span>
      </div>

      <div class="post-engagement">
        <LikeButton
          target-type="POST"
          :target-id="post.id"
          :initial-count="post.likeCount || 0"
          label="喜欢这篇文章"
        />
        <button type="button" @click="shareOpen = true"><Share2 :size="17" />分享</button>
      </div>

      <div v-if="post.coverUrl" class="post-cover-photo">
        <div class="cover-frame">
          <img
            :src="optimizedImageUrl(post.coverUrl, 1280)"
            :srcset="optimizedImageSrcset(post.coverUrl) || undefined"
            sizes="(max-width: 760px) calc(100vw - 28px), 900px"
            :alt="post.title"
            fetchpriority="high"
            decoding="async"
          />
        </div>
      </div>

      <div class="article-layout" :class="{ 'has-toc': tocItems.length }">
        <aside v-if="tocItems.length" class="post-toc" aria-label="文章目录">
          <p class="toc-title">目录</p>
          <button
            v-for="item in tocItems"
            :key="item.id"
            :class="['toc-link', 'level-' + item.level, { active: activeHeading === item.id }]"
            @click="scrollToHeading(item.id)"
          >
            {{ item.text }}
          </button>
        </aside>
        <div
          ref="articleRef"
          class="post-content"
          @click="handleArticleClick"
          v-html="renderedContent"
        ></div>
      </div>

      <div class="comments-section">
        <InteractionThread target-type="POST" :target-id="post.id" title="评论" />
      </div>
    </div>

    <div v-else class="loading-state glass-panel">
      <div class="loading-spin loading-shimmer"></div>
      <p>加载中...</p>
    </div>

    <Teleport to="body">
      <Transition name="fade">
        <div v-if="previewImage" class="image-preview" @click="previewImage = ''">
          <button class="image-preview-close" aria-label="关闭图片预览">关闭</button>
          <img :src="previewImage" alt="文章图片预览" />
        </div>
      </Transition>
    </Teleport>

    <ShareCardDialog
      v-if="post"
      :open="shareOpen"
      :post="post"
      :url="shareUrl"
      @close="shareOpen = false"
    />
  </div>
</template>

<style scoped>
.post-page {
  max-width: var(--magazine-max, 1180px);
  margin: 0 auto;
  width: 100%;
  padding: 0 var(--space-md) var(--space-xl);
}

.reading-progress {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  z-index: 300;
  background: transparent;
}

.reading-progress span {
  display: block;
  height: 100%;
  background: linear-gradient(90deg, var(--brand-primary-soft), var(--primary));
  box-shadow: 0 0 18px rgba(217, 154, 29, 0.28);
  transition: width 0.15s var(--ease-soft);
}

.post-container {
  padding: clamp(20px, 4vw, 44px);
  border-radius: 8px;
}
.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: rgba(255, 250, 238, 0.7);
  border: 1px solid var(--border);
  border-radius: var(--radius-pill);
  padding: 8px 18px;
  font-size: var(--font-size-sm);
  cursor: pointer;
  color: var(--text-secondary);
  transition:
    border-color var(--duration-normal) var(--ease-bounce),
    color var(--duration-normal) var(--ease-bounce),
    background var(--duration-normal) var(--ease-bounce),
    transform var(--duration-normal) var(--ease-bounce);
}
.post-top-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: var(--space-md);
}

.back-btn:hover {
  border-color: var(--border-warm);
  color: var(--primary-hover);
  background: var(--primary-soft);
  transform: translateY(-2px);
}

.edit-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  background: var(--primary-soft);
  border: 1px solid var(--border-warm);
  border-radius: var(--radius-pill);
  padding: 8px 18px;
  font-size: var(--font-size-sm);
  cursor: pointer;
  color: var(--primary-hover);
  transition:
    border-color var(--duration-normal) var(--ease-bounce),
    color var(--duration-normal) var(--ease-bounce),
    background var(--duration-normal) var(--ease-bounce),
    transform var(--duration-normal) var(--ease-bounce);
}
.edit-btn:hover {
  background: var(--primary);
  border-color: var(--primary);
  color: var(--on-primary);
  transform: translateY(-2px);
}

.post-cover-photo .cover-frame img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.post-cover-photo {
  margin: 26px 0 42px;
  padding: 0;
}

.post-cover-photo .cover-frame {
  width: 100%;
  max-height: 420px;
  aspect-ratio: 16 / 7;
}

.post-title {
  font-family: var(--font-serif);
  font-size: clamp(2rem, 5vw, 4.2rem);
  font-weight: 700;
  letter-spacing: 0;
  color: var(--text-main);
  line-height: 1.12;
  margin: 8px 0 var(--space-sm);
}

.post-summary {
  margin: 0 0 16px;
  max-width: 760px;
  font-size: 1rem;
  line-height: 1.78;
  color: var(--text-secondary);
  padding: 0;
  background: transparent;
  border: 0;
  border-radius: 0;
}

.post-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
  color: var(--text-muted);
  font-size: 0.8rem;
  margin-bottom: 8px;
}
.meta-item {
  display: flex;
  align-items: center;
  gap: 5px;
}

.post-tags {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 0;
  flex-wrap: wrap;
  color: var(--text-muted);
}
.post-tags .tag {
  padding: 4px 12px;
  font-size: 0.72rem;
}

.post-engagement {
  min-height: 46px;
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 10px;
}
.post-engagement > button {
  min-height: 34px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 0 10px;
  border: 1px solid transparent;
  border-radius: var(--radius-control);
  background: transparent;
  color: var(--text-tertiary);
  font: inherit;
  font-size: 0.76rem;
  cursor: pointer;
}
.post-engagement > button:hover {
  border-color: var(--border-subtle);
  background: var(--surface-soft);
  color: var(--text-primary);
}

.article-layout.has-toc {
  display: grid;
  grid-template-columns: minmax(0, 760px) 220px;
  justify-content: center;
  gap: 34px;
  align-items: start;
}

.article-layout:not(.has-toc) {
  max-width: 780px;
  margin: 0 auto;
}

.post-content :deep(> *) {
  animation: article-content-enter var(--motion-normal) both;
}

@keyframes article-content-enter {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.post-toc {
  position: sticky;
  top: 104px;
  order: 2;
  padding: 16px;
  border: 1px solid var(--border-warm);
  border-radius: 8px;
  background: var(--surface-muted);
}
[data-theme='dark'] .post-toc {
  background: var(--surface-muted);
}

.toc-title {
  margin: 0 0 8px;
  color: var(--text-muted);
  font-size: 0.72rem;
  font-weight: 700;
}

.toc-link {
  display: block;
  width: 100%;
  border: none;
  border-left: 2px solid transparent;
  background: transparent;
  color: var(--text-muted);
  cursor: pointer;
  font-family: var(--font-body);
  font-size: 0.72rem;
  line-height: 1.35;
  padding: 6px 0 6px 8px;
  text-align: left;
  transition:
    color var(--duration-fast),
    border-color var(--duration-fast),
    background var(--duration-fast);
}

.toc-link.level-3 {
  padding-left: 18px;
}

.toc-link:hover,
.toc-link.active {
  color: var(--primary-hover);
  border-left-color: var(--primary);
  background: rgba(217, 154, 29, 0.08);
}

.post-content {
  min-width: 0;
  font-size: 1.03rem;
  line-height: 1.9;
  color: var(--text-main);
}
.post-content h2 {
  font-family: var(--font-serif);
  font-size: clamp(1.35rem, 3vw, 2rem);
  font-weight: 600;
  margin: 2.4rem 0 1rem;
  letter-spacing: 0;
  scroll-margin-top: 110px;
}

.post-content h3 {
  scroll-margin-top: 110px;
}

.post-content :deep(img.clickable-image) {
  cursor: zoom-in;
}
.post-content p {
  margin-bottom: 1rem;
}

/* ====== 评论区 ====== */
.comments-section {
  max-width: 820px;
  margin: 3.2rem auto 0;
  padding-top: 2rem;
  border-top: 1px solid var(--border);
}
.comments-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-family: var(--font-sans);
  font-size: var(--font-size-lg);
  font-weight: 600;
  letter-spacing: 0.02em;
  margin-bottom: 1.2rem;
  color: var(--text-main);
}

.comment-form {
  margin-bottom: 1.5rem;
}
.comment-form textarea {
  margin-bottom: 10px;
  min-height: 96px;
  resize: vertical;
}

.comment-input-wrap {
  position: relative;
}
.emoji-picker-wrap {
  position: absolute;
  right: 8px;
  bottom: 16px;
}
.emoji-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: 1px solid var(--border);
  border-radius: 50%;
  background: var(--surface);
  color: var(--text-muted);
  cursor: pointer;
  transition:
    color var(--duration-fast),
    border-color var(--duration-fast);
}
.emoji-btn:hover {
  color: var(--primary-hover);
  border-color: var(--primary);
}

.emoji-popover {
  position: absolute;
  right: 0;
  bottom: 40px;
  width: 320px;
  max-height: 280px;
  overflow-y: auto;
  padding: 12px;
  z-index: 100;
}
.emoji-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 6px;
}
.emoji-item {
  width: 44px;
  height: 44px;
  padding: 3px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-sm);
  background: transparent;
  cursor: pointer;
  overflow: hidden;
  transition:
    border-color var(--duration-fast),
    transform var(--duration-fast);
}
.emoji-item:hover {
  border-color: var(--primary);
  transform: scale(1.1);
}
.emoji-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 4px;
}

.comment-login-hint {
  padding: 20px;
  text-align: center;
  margin-bottom: 1.5rem;
  font-size: 0.88rem;
  color: var(--text-muted);
}

.comment-login-hint:hover {
  transform: none;
}

.login-hint-title {
  margin: 0 0 4px;
  color: var(--text-main);
  font-weight: 700;
}

.login-hint-desc {
  margin: 0 0 14px;
}

.comments-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.comment-item {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  padding: var(--space-md);
  border-radius: var(--radius-md);
  background: rgba(255, 250, 238, 0.62);
  border: 1px solid var(--border);
  transition:
    transform var(--duration-normal) var(--ease-bounce),
    box-shadow var(--duration-normal) var(--ease-bounce);
}
[data-theme='dark'] .comment-item {
  background: rgba(17, 16, 13, 0.34);
}
.comment-item:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-soft);
}
.admin-comment {
  border-color: var(--border-warm);
  background: rgba(217, 154, 29, 0.08);
}
.comment-body {
  flex: 1;
  min-width: 0;
}
.comment-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}
.comment-name {
  font-weight: 600;
  font-size: 0.85rem;
  color: var(--text-main);
}
.comment-time {
  margin-left: auto;
  font-size: 0.72rem;
  color: var(--text-muted);
}
.comment-text {
  margin: 0;
  font-size: 0.88rem;
  color: var(--text-secondary);
  line-height: var(--line-height);
}
.comment-text :deep(img) {
  display: inline;
  width: 40px;
  height: 40px;
  vertical-align: middle;
  margin: 0 2px;
  border-radius: 4px;
  object-fit: cover;
}
/* ====== 管理菜单 ====== */
.comment-actions {
  position: relative;
  flex-shrink: 0;
}
.menu-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 50%;
  background: transparent;
  color: var(--text-muted);
  cursor: pointer;
  transition:
    background var(--duration-normal) var(--ease-bounce),
    color var(--duration-normal) var(--ease-bounce);
}
.menu-btn:hover {
  background: var(--primary-soft);
  color: var(--primary-hover);
}

[data-theme='dark'] .action-menu {
  background: rgba(39, 31, 21, 0.96);
}
.action-menu button {
  display: flex;
  align-items: center;
  gap: 8px;
  background: transparent;
  color: var(--ink);
  font-size: 0.82rem;
  font-family: var(--font-body);
  cursor: pointer;
  transition:
    background var(--duration-fast),
    color var(--duration-fast);
}
.action-menu button:hover {
  background: var(--primary-soft);
}
.action-menu button.danger:hover {
  background: var(--danger-soft);
  color: var(--danger);
}

.pinned-badge {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  font-size: 0.6rem;
  font-weight: 600;
  color: var(--primary-hover);
  background: var(--primary-soft);
  padding: 2px 8px;
  border-radius: var(--radius-pill);
  margin-right: 6px;
  vertical-align: middle;
}
.pinned {
  border-color: var(--border-warm);
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.35s var(--ease-bounce);
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.comments-empty {
  text-align: center;
  padding: 2rem;
  color: var(--text-muted);
  font-size: 0.88rem;
}

.loading-state {
  text-align: center;
  padding: 4rem var(--space-lg);
  color: var(--text-muted);
}
.loading-spin {
  width: 48px;
  height: 48px;
  margin: 0 auto var(--space-md);
  border-radius: 50%;
}

.image-preview {
  position: fixed;
  inset: 0;
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px;
  background: rgba(32, 24, 16, 0.82);
  backdrop-filter: blur(6px);
}

.image-preview img {
  max-width: min(94vw, 1200px);
  max-height: 90vh;
  border-radius: var(--radius-lg);
  box-shadow: 0 24px 80px rgba(0, 0, 0, 0.32);
}

.image-preview-close {
  position: fixed;
  top: 20px;
  right: 20px;
  border: 1px solid rgba(255, 250, 238, 0.3);
  border-radius: var(--radius-pill);
  background: rgba(255, 250, 238, 0.16);
  color: #fff9ec;
  cursor: pointer;
  padding: 8px 14px;
}
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 640px) {
  .post-page {
    padding-inline: var(--space-sm);
  }
  .post-container {
    padding: var(--space-md);
    border-radius: 8px;
  }
  .post-title {
    font-size: clamp(1.8rem, 10vw, 2.5rem);
  }
  .article-layout,
  .article-layout.has-toc {
    display: block;
  }
  .post-toc {
    position: static;
    margin-bottom: var(--space-md);
  }
  .post-meta {
    gap: 8px;
  }
  .emoji-popover {
    right: -8px;
    width: min(320px, calc(100vw - 48px));
  }
}
</style>
