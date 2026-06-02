<script setup>
import { ref, computed, onMounted, onBeforeUnmount, inject, nextTick, watch } from 'vue'
import { useRoute, navigate } from '../router'
// navigate used in template
import { getPost, addComment, deleteComment, getComments, togglePinComment } from '../utils/api'
import { showToast } from '../composables/toast'
import { renderArticleMarkdown, renderCommentMarkdown } from '../utils/marked'
import loadingStore from '../stores/loadingStore'
import ConfirmDialog from '../components/ConfirmDialog.vue'
import {
  ArrowLeft,
  Send,
  Trash2,
  MessageCircle,
  Clock,
  MoreHorizontal,
  Pin,
  PinOff,
  Tag,
  Folder,
  RefreshCw,
  Eye,
  Smile,
  Edit3
} from 'lucide-vue-next'
import { gsap } from 'gsap'
import { formatTime } from '../utils/format'

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

const { query } = useRoute()
const postId = computed(() => query.value.get('id'))
const post = ref(null)
const comments = ref([])
const commentContent = ref('')
const user = inject('user', null)
const refreshUser = inject('refreshUser', null)
const commentSubmitting = ref(false)
const showEmoji = ref(false)
const articleRef = ref(null)
const readingProgress = ref(0)
const activeHeading = ref('')
const previewImage = ref('')
const confirmDialog = ref({
  open: false,
  title: '',
  message: '',
  confirmText: '确认',
  onConfirm: null
})

const emojis = [
  'love',
  'love眼',
  'wink',
  '卖萌',
  '吃瓜',
  '呵呵',
  '哇',
  '哦',
  '啊？',
  '喜欢',
  '喵',
  '嘲笑',
  '嘿嘿',
  '嫌弃',
  '害怕',
  '小狗农',
  '小生气',
  '尴尬',
  '我去',
  '搭讪',
  '摸头',
  '星星眼',
  '欸嘿',
  '求收留',
  '狐狸农',
  '王德法',
  '生气',
  '看热闹',
  '耶',
  '赞',
  '酷笑',
  '馋',
  '鬼迷日眼'
]

function insertEmoji(name) {
  commentContent.value += ` ![${name}](/images/emojis/${name}.jpg) `
  showEmoji.value = false
}

const renderedContent = computed(() =>
  post.value ? renderArticleMarkdown(post.value.content) : ''
)

const readTime = computed(() => {
  if (!post.value?.content) return 1
  const text = post.value.content.replace(/[#*_`\[\]!\(\)>\-\s]/g, '')
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
  if (document.querySelector('.post-content > *')) {
    gsap.from('.post-content > *', {
      opacity: 0,
      y: 15,
      duration: 0.6,
      stagger: 0.05,
      ease: 'power2.out'
    })
  }
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

async function loadComments() {
  if (!postId.value) return
  try {
    const result = await getComments(postId.value)
    comments.value = user?.value
      ? result.records.map((c) => ({
          ...c,
          avatarUrl: c.role === 'ADMIN' && user.value ? user.value.avatarUrl : c.avatarUrl
        }))
      : result.records
  } catch {
    comments.value = []
  }
}

async function handleAddComment() {
  if (!commentContent.value.trim()) return
  if (!user?.value) {
    showToast('登录后就可以发表评论。')
    navigate('/login')
    return
  }
  commentSubmitting.value = true
  try {
    await addComment({ postId: postId.value, content: commentContent.value })
    commentContent.value = ''
    await loadComments()
    showToast('评论发表成功')
  } catch (error) {
    const message =
      error?.status === 429 || error?.code === 429
        ? '评论太快啦，稍等一下再发。'
        : error?.status === 401 || error?.code === 401
          ? '登录状态过期了，重新登录一下就好。'
          : error?.message || '评论没有发出去，稍后再试一次。'
    showToast(message, 'error')
  } finally {
    commentSubmitting.value = false
  }
}

const openMenuId = ref(null)

function toggleMenu(id) {
  openMenuId.value = openMenuId.value === id ? null : id
}

async function handleDeleteComment(commentId) {
  askConfirm({
    title: '删除评论',
    message: '确定要删除这条评论吗？删除后访客将看不到这条内容。',
    confirmText: '删除评论',
    onConfirm: async () => {
      await deleteCommentById(commentId)
    }
  })
}

async function deleteCommentById(commentId) {
  try {
    await deleteComment(commentId)
    openMenuId.value = null
    await loadComments()
    showToast('评论删除成功')
  } catch {
    comments.value = comments.value.filter((c) => c.id !== commentId)
  }
}

function askConfirm({ title, message, confirmText = '确认', onConfirm }) {
  confirmDialog.value = { open: true, title, message, confirmText, onConfirm }
}

async function handleConfirmAction() {
  const action = confirmDialog.value.onConfirm
  confirmDialog.value.open = false
  confirmDialog.value.onConfirm = null
  if (action) await action()
}

async function handleTogglePin(comment) {
  try {
    await togglePinComment(comment.id)
    openMenuId.value = null
    await loadComments()
  } catch (e) {
    showToast(e?.message || '这次没有处理成功，稍后再试一次。', 'error')
  }
}

function formatRelativeTime(dateString) {
  const diff = Date.now() - new Date(dateString).getTime()
  const d = Math.floor(diff / 86400000)
  const h = Math.floor(diff / 3600000)
  const m = Math.floor(diff / 60000)
  if (d > 0) return `${d}天前`
  if (h > 0) return `${h}小时前`
  if (m > 0) return `${m}分钟前`
  return '刚刚'
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
  await loadComments()
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
          @click="editPost"
          aria-label="编辑文章"
        >
          <Edit3 :size="14" /> 编辑
        </button>
      </div>

      <!-- 封面图 — 手账相纸风 -->
      <div v-if="post.coverUrl" class="post-cover-photo">
        <div class="cover-frame">
          <img :src="post.coverUrl" :alt="post.title" />
        </div>
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
        <span class="meta-item"><Eye :size="14" /> 约 {{ readTime }} 分钟</span>
        <span v-if="post.category" class="meta-item"
          ><Folder :size="14" /> {{ post.category }}</span
        >
      </div>

      <!-- 标签 -->
      <div v-if="tagList.length" class="post-tags">
        <Tag :size="14" />
        <span v-for="t in tagList" :key="t" class="tag">{{ t }}</span>
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
          v-html="renderedContent"
          @click="handleArticleClick"
        ></div>
      </div>

      <!-- 评论区 -->
      <div class="comments-section">
        <h2 class="comments-title"><MessageCircle :size="18" /> 评论</h2>

        <div v-if="user" class="comment-form">
          <div class="comment-input-wrap">
            <textarea
              v-model="commentContent"
              class="field-control"
              placeholder="写下你的评论..."
              rows="3"
            ></textarea>
            <div class="emoji-picker-wrap">
              <button
                class="emoji-btn"
                @click="showEmoji = !showEmoji"
                aria-label="表情"
                title="表情"
              >
                <Smile :size="18" />
              </button>
              <Transition name="fade">
                <div v-if="showEmoji" class="emoji-popover glass-card">
                  <div class="emoji-grid">
                    <button
                      v-for="e in emojis"
                      :key="e"
                      class="emoji-item"
                      :title="e"
                      @click="insertEmoji(e)"
                    >
                      <img :src="`/images/emojis/${e}.jpg`" :alt="e" loading="lazy" />
                    </button>
                  </div>
                </div>
              </Transition>
            </div>
          </div>
          <button
            class="pill-btn pill-btn-primary"
            :disabled="!commentContent.trim() || commentSubmitting"
            @click="handleAddComment"
          >
            <Send :size="14" /> {{ commentSubmitting ? '发布中...' : '发表评论' }}
          </button>
        </div>
        <div v-else class="comment-login-hint glass-card">
          <p class="login-hint-title">想留下一点想法？</p>
          <p class="login-hint-desc">登录后就可以发表评论，头像和昵称会一起显示在评论区。</p>
          <button class="pill-btn pill-btn-primary" @click="navigate('/login')">去登录</button>
        </div>

        <div v-if="comments.length > 0" class="comments-list">
          <div
            v-for="c in comments"
            :key="c.id"
            class="comment-item"
            :class="{ 'admin-comment': c.role === 'ADMIN', pinned: c.pinned }"
          >
            <img
              :src="c.avatarUrl || 'https://api.dicebear.com/7.x/avataaars/svg?seed=guest'"
              :alt="c.nickname + '的头像'"
              class="comment-avatar avatar-img"
              :class="{ loaded: c.avatarUrl }"
              @load="(e) => e.target.classList.add('loaded')"
            />
            <div class="comment-body">
              <div class="comment-head">
                <span v-if="c.pinned" class="pinned-badge"><Pin :size="10" /> 置顶</span>
                <span class="comment-name">{{ c.nickname }}</span>
                <span
                  v-if="c.titleName"
                  :class="['title-badge', 'title-' + (c.titleStyle || 'default')]"
                  >{{ c.titleName }}</span
                >
                <span v-if="c.role === 'ADMIN'" class="comment-badge">博主</span>
                <span class="comment-time">{{ formatRelativeTime(c.createTime) }}</span>
              </div>
              <p class="comment-text" v-html="renderCommentMarkdown(c.content)"></p>
            </div>
            <div v-if="user?.role === 'ADMIN'" class="comment-actions" @click.stop>
              <button
                class="menu-btn"
                @click="toggleMenu(c.id)"
                :aria-label="openMenuId === c.id ? '关闭菜单' : '评论操作'"
              >
                <MoreHorizontal :size="16" />
              </button>
              <Transition name="fade">
                <div v-if="openMenuId === c.id" class="action-menu glass-card">
                  <button @click="handleTogglePin(c)">
                    <PinOff v-if="c.pinned" :size="14" />
                    <Pin v-else :size="14" />
                    {{ c.pinned ? '取消置顶' : '置顶' }}
                  </button>
                  <button class="danger" @click="handleDeleteComment(c.id)">
                    <Trash2 :size="14" />
                    删除
                  </button>
                </div>
              </Transition>
            </div>
          </div>
        </div>
        <div v-else class="comments-empty">还没有评论，留下一点想法吧。</div>
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

    <ConfirmDialog
      v-model="confirmDialog.open"
      :title="confirmDialog.title"
      :message="confirmDialog.message"
      :confirm-text="confirmDialog.confirmText"
      @confirm="handleConfirmAction"
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
  border-radius: 30px;
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
  max-height: 460px;
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
  padding: 14px 18px;
  background: rgba(217, 154, 29, 0.1);
  border: 1px solid var(--border-warm);
  border-left: 4px solid var(--primary);
  border-radius: 18px;
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
  margin-bottom: var(--space-lg);
  flex-wrap: wrap;
  color: var(--text-muted);
}
.post-tags .tag {
  padding: 4px 12px;
  font-size: 0.72rem;
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

.post-toc {
  position: sticky;
  top: 104px;
  order: 2;
  padding: 16px;
  border: 1px solid var(--border-warm);
  border-radius: 20px;
  background:
    linear-gradient(180deg, rgba(255, 250, 238, 0.7), rgba(255, 240, 207, 0.48)),
    var(--surface-muted);
}
[data-theme='dark'] .post-toc {
  background:
    linear-gradient(180deg, rgba(39, 31, 21, 0.72), rgba(17, 16, 13, 0.45)), var(--surface-muted);
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
    border-radius: 24px;
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
