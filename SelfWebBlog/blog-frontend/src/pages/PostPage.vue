<script setup>
import { ref, computed, onMounted, inject, nextTick, watch } from 'vue'
import { useRoute, navigate } from '../router'
// navigate used in template
import { getPost, addComment, deleteComment, getComments, togglePinComment } from '../utils/api'
import { showToast } from '../composables/toast'
import { renderArticleMarkdown, renderCommentMarkdown } from '../utils/marked'
import { toAbsoluteUrl } from '../utils/url'
import loadingStore from '../stores/loadingStore'
import { ArrowLeft, Send, Trash2, MessageCircle, Clock, MoreHorizontal, Pin, PinOff, Tag, Folder, RefreshCw, Eye, Smile, Edit3 } from 'lucide-vue-next'
import { gsap } from 'gsap'
import { formatTime } from '../utils/format'

function goBack() { window.history.back() }

function editPost() {
  if (!post.value) return
  sessionStorage.setItem('editingPost', JSON.stringify({
    id: post.value.id, title: post.value.title, content: post.value.content,
    summary: post.value.summary || '', coverUrl: post.value.coverUrl || '',
    category: post.value.category || '', tags: post.value.tags || '',
    postStatus: post.value.status || 'PUBLISHED'
  }))
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
const commentRefs = ref({})
const showEmoji = ref(false)

const emojis = [
  'love', 'love眼', 'wink', '卖萌', '吃瓜', '呵呵', '哇', '哦', '啊？', '喜欢', '喵',
  '嘲笑', '嘿嘿', '嫌弃', '害怕', '小狗农', '小生气', '尴尬', '我去', '搭讪', '摸头',
  '星星眼', '欸嘿', '求收留', '狐狸农', '王德法', '生气', '看热闹', '耶', '赞', '酷笑', '馋', '鬼迷日眼'
]

function insertEmoji(name) {
  commentContent.value += ` ![${name}](/images/emojis/${name}.jpg) `
  showEmoji.value = false
}

const renderedContent = computed(() => post.value ? renderArticleMarkdown(post.value.content) : '')

const readTime = computed(() => {
  if (!post.value?.content) return 1
  const text = post.value.content.replace(/[#*_`\[\]!\(\)>\-\s]/g, '')
  return Math.max(1, Math.ceil(text.length / 400))
})

const tagList = computed(() => {
  if (!post.value?.tags) return []
  return post.value.tags.split(/[,，\s]+/).filter(Boolean)
})

watch(renderedContent, async () => {
  await nextTick()
  if (document.querySelector('.post-content > *')) {
    gsap.from('.post-content > *', { opacity: 0, y: 15, duration: 0.6, stagger: 0.05, ease: 'power2.out' })
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
    comments.value = user?.value ? result.records.map(c => ({
      ...c,
      avatarUrl: c.role === 'ADMIN' && user.value ? user.value.avatarUrl : c.avatarUrl
    })) : result.records
  } catch { comments.value = [] }
}

async function handleAddComment() {
  if (!commentContent.value.trim()) return
  commentSubmitting.value = true
  try {
    await addComment({ postId: postId.value, content: commentContent.value })
    commentContent.value = ''
    await loadComments()
    showToast('评论发表成功')
  } catch (error) {
    showToast('评论发表失败：' + error.message)
  } finally { commentSubmitting.value = false }
}

const openMenuId = ref(null)

function toggleMenu(id) { openMenuId.value = openMenuId.value === id ? null : id }

async function handleDeleteComment(commentId) {
  if (!confirm('确定要删除这条评论吗？')) return
  try { await deleteComment(commentId); openMenuId.value = null; await loadComments(); showToast('评论删除成功') }
  catch { comments.value = comments.value.filter(c => c.id !== commentId) }
}

async function handleTogglePin(comment) {
  try { await togglePinComment(comment.id); openMenuId.value = null; await loadComments() }
  catch (e) { showToast(e?.message || '操作失败', 'error') }
}

function formatDate(dateString) { return new Date(dateString).toLocaleString() }

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

onMounted(async () => {
  if (refreshUser) await refreshUser()
  loadPost(); loadComments()
})
</script>

<template>
  <div class="post-page">
    <div v-if="post" class="post-container glass-card">
      <div class="post-top-actions">
        <button class="back-btn" @click="goBack">
          <ArrowLeft :size="16" /> 返回
        </button>
        <button v-if="user?.role === 'ADMIN'" class="edit-btn" @click="editPost" aria-label="编辑文章">
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
        <span v-if="post.updateTime && post.updateTime !== post.createTime" class="meta-item"><RefreshCw :size="14" /> {{ formatTime(post.updateTime) }}</span>
        <span class="meta-item"><Eye :size="14" /> 约 {{ readTime }} 分钟</span>
        <span v-if="post.category" class="meta-item"><Folder :size="14" /> {{ post.category }}</span>
      </div>

      <!-- 标签 -->
      <div v-if="tagList.length" class="post-tags">
        <Tag :size="14" />
        <span v-for="t in tagList" :key="t" class="tag">{{ t }}</span>
      </div>

      <div class="post-content" v-html="renderedContent"></div>

      <!-- 评论区 -->
      <div class="comments-section">
        <h2 class="comments-title">
          <MessageCircle :size="18" /> 评论
        </h2>

        <div v-if="user" class="comment-form">
          <div class="comment-input-wrap">
            <textarea v-model="commentContent" class="field-control" placeholder="写下你的评论..." rows="3"></textarea>
            <div class="emoji-picker-wrap">
              <button class="emoji-btn" @click="showEmoji = !showEmoji" aria-label="表情" title="表情"><Smile :size="18" /></button>
              <Transition name="fade">
                <div v-if="showEmoji" class="emoji-popover glass-card">
                  <div class="emoji-grid">
                    <button
                      v-for="e in emojis" :key="e"
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
          <button class="pill-btn pill-btn-primary" :disabled="!commentContent.trim() || commentSubmitting" @click="handleAddComment">
            <Send :size="14" /> {{ commentSubmitting ? '发布中...' : '发表评论' }}
          </button>
        </div>
        <div v-else class="comment-login-hint glass-card">
          <p><a @click="navigate('/login')" style="cursor:pointer;color:var(--primary)">登录</a> 后即可发表评论</p>
        </div>

        <div v-if="comments.length > 0" class="comments-list">
          <div v-for="c in comments" :key="c.id" class="comment-item" :class="{ 'admin-comment': c.role === 'ADMIN', 'pinned': c.pinned }">
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
                <span v-if="c.titleName" :class="['title-badge', 'title-' + (c.titleStyle || 'default')]">{{ c.titleName }}</span>
                <span v-if="c.role === 'ADMIN'" class="comment-badge">博主</span>
                <span class="comment-time">{{ formatRelativeTime(c.createTime) }}</span>
              </div>
              <p class="comment-text" v-html="renderCommentMarkdown(c.content)"></p>
            </div>
            <div v-if="user?.role === 'ADMIN'" class="comment-actions" @click.stop>
              <button class="menu-btn" @click="toggleMenu(c.id)" :aria-label="openMenuId === c.id ? '关闭菜单' : '评论操作'">
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
        <div v-else class="comments-empty">还没有评论，来发表第一条吧</div>
      </div>
    </div>

    <div v-else class="loading-state glass-panel">
      <div class="loading-spin loading-shimmer"></div>
      <p>加载中...</p>
    </div>
  </div>
</template>

<style scoped>
.post-page { max-width: 800px; margin: 0 auto; width: 100%; }

.post-container { padding: var(--space-lg); }
.back-btn {
  display: inline-flex; align-items: center; gap: 6px;
  background: rgba(255, 255, 255, 0.35);
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: var(--radius-pill);
  padding: 8px 18px; font-size: var(--font-size-sm); cursor: pointer;
  color: var(--text-secondary);
  transition: border-color var(--duration-normal) var(--ease-bounce),
              color var(--duration-normal) var(--ease-bounce),
              background var(--duration-normal) var(--ease-bounce),
              transform var(--duration-normal) var(--ease-bounce);
}
.post-top-actions {
  display: flex; align-items: center; gap: 10px;
  margin-bottom: var(--space-md);
}

.back-btn:hover {
  border-color: var(--theme-pink);
  color: var(--theme-pink-hover);
  background: rgba(244, 164, 184, 0.12);
  transform: translateY(-2px);
}

.edit-btn {
  display: inline-flex; align-items: center; gap: 5px;
  background: var(--primary-soft);
  border: 1px solid rgba(244, 164, 184, 0.3);
  border-radius: var(--radius-pill);
  padding: 8px 18px; font-size: var(--font-size-sm); cursor: pointer;
  color: var(--primary-hover);
  transition: border-color var(--duration-normal) var(--ease-bounce),
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
  max-height: 360px;
}

.post-title {
  font-family: var(--font-sans);
  font-size: var(--font-size-xl);
  font-weight: 600;
  letter-spacing: 0.02em;
  color: var(--text-main);
  line-height: 1.35;
  margin: 0 0 var(--space-sm);
}

.post-summary {
  margin: 0 0 16px;
  font-size: 0.95rem; line-height: 1.65;
  color: var(--text-muted);
  padding: 12px 16px;
  background: var(--primary-soft);
  border-left: 3px solid var(--primary);
  border-radius: 0 var(--radius-sm) var(--radius-sm) 0;
}

.post-meta {
  display: flex; align-items: center; flex-wrap: wrap;
  gap: 16px; color: var(--text-muted); font-size: 0.8rem;
  margin-bottom: 8px;
}
.meta-item { display: flex; align-items: center; gap: 5px; }

.post-tags {
  display: flex; align-items: center; gap: 8px;
  margin-bottom: var(--space-lg); flex-wrap: wrap;
  color: var(--text-muted);
}
.post-tags .tag {
  padding: 4px 12px;
  font-size: 0.72rem;
}

.post-content { font-size: var(--font-size-md); line-height: var(--line-height); color: var(--text-main); }
.post-content h2 { font-family: var(--font-sans); font-size: var(--font-size-lg); font-weight: 600; margin: 2rem 0 1rem; letter-spacing: 0.02em; }
.post-content p { margin-bottom: 1rem; }

/* ====== 评论区 ====== */
.comments-section { margin-top: 3rem; padding-top: 2rem; border-top: 1px solid var(--border); }
.comments-title {
  display: flex; align-items: center; gap: 8px;
  font-family: var(--font-sans); font-size: var(--font-size-lg);
  font-weight: 600; letter-spacing: 0.02em;
  margin-bottom: 1.2rem; color: var(--text-main);
}

.comment-form { margin-bottom: 1.5rem; }
.comment-form textarea { margin-bottom: 10px; min-height: 96px; resize: vertical; }

.comment-input-wrap { position: relative; }
.emoji-picker-wrap { position: absolute; right: 8px; bottom: 16px; }
.emoji-btn {
  display: flex; align-items: center; justify-content: center;
  width: 32px; height: 32px; border: 1px solid var(--border); border-radius: 50%;
  background: var(--surface); color: var(--text-muted); cursor: pointer;
  transition: color var(--duration-fast), border-color var(--duration-fast);
}
.emoji-btn:hover { color: var(--primary-hover); border-color: var(--primary); }

.emoji-popover {
  position: absolute; right: 0; bottom: 40px;
  width: 320px; max-height: 280px; overflow-y: auto;
  padding: 12px; z-index: 100;
}
.emoji-grid {
  display: grid; grid-template-columns: repeat(6, 1fr); gap: 6px;
}
.emoji-item {
  width: 44px; height: 44px; padding: 3px; border: 1px solid var(--border-light);
  border-radius: var(--radius-sm); background: transparent; cursor: pointer;
  overflow: hidden; transition: border-color var(--duration-fast), transform var(--duration-fast);
}
.emoji-item:hover { border-color: var(--primary); transform: scale(1.1); }
.emoji-item img { width: 100%; height: 100%; object-fit: cover; border-radius: 4px; }

.comment-login-hint {
  padding: 20px; text-align: center;
  margin-bottom: 1.5rem; font-size: 0.88rem; color: var(--text-muted);
}

.comments-list { display: flex; flex-direction: column; gap: 12px; }

.comment-item {
  display: flex; gap: 12px; align-items: flex-start;
  padding: var(--space-md);
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.35);
  border: 1px solid rgba(255, 255, 255, 0.45);
  backdrop-filter: blur(8px);
  transition: transform var(--duration-normal) var(--ease-bounce),
              box-shadow var(--duration-normal) var(--ease-bounce);
}
.comment-item:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-soft);
}
.admin-comment { border-color: rgba(244, 164, 184, 0.35); background: rgba(244, 164, 184, 0.08); }
.comment-body { flex: 1; min-width: 0; }
.comment-head { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; }
.comment-name { font-weight: 600; font-size: 0.85rem; color: var(--text-main); }
.comment-time { margin-left: auto; font-size: 0.72rem; color: var(--text-muted); }
.comment-text { margin: 0; font-size: 0.88rem; color: var(--text-secondary); line-height: var(--line-height); }
.comment-text :deep(img) {
  display: inline; width: 40px; height: 40px;
  vertical-align: middle; margin: 0 2px;
  border-radius: 4px; object-fit: cover;
}
/* ====== 管理菜单 ====== */
.comment-actions { position: relative; flex-shrink: 0; }
.menu-btn {
  display: flex; align-items: center; justify-content: center;
  width: 32px; height: 32px;
  border: none; border-radius: 50%;
  background: transparent; color: var(--text-muted);
  cursor: pointer;
  transition: background var(--duration-normal) var(--ease-bounce),
              color var(--duration-normal) var(--ease-bounce);
}
.menu-btn:hover { background: var(--primary-soft); color: var(--primary-hover); }

[data-theme="dark"] .action-menu { background: rgba(30,26,28,0.95); }
.action-menu button {
  display: flex; align-items: center; gap: 8px;
  background: transparent; color: var(--ink); font-size: 0.82rem;
  font-family: var(--font-body); cursor: pointer;
  transition: background var(--duration-fast), color var(--duration-fast);
}
.action-menu button:hover { background: var(--red-soft); }
.action-menu button.danger:hover { background: rgba(186,0,52,0.1); color: var(--red); }

.pinned-badge {
  display: inline-flex; align-items: center; gap: 3px;
  font-size: 0.6rem; font-weight: 600; color: var(--theme-pink-hover);
  background: rgba(244, 164, 184, 0.15); padding: 2px 8px;
  border-radius: var(--radius-pill);
  margin-right: 6px; vertical-align: middle;
}
.pinned { border-color: rgba(244, 164, 184, 0.35); }

.fade-enter-active, .fade-leave-active { transition: opacity 0.35s var(--ease-bounce); }
.fade-enter-from, .fade-leave-to { opacity: 0; }

.comments-empty { text-align: center; padding: 2rem; color: var(--text-muted); font-size: 0.88rem; }

.loading-state {
  text-align: center;
  padding: 4rem var(--space-lg);
  color: var(--text-muted);
}
.loading-spin {
  width: 48px; height: 48px;
  margin: 0 auto var(--space-md);
  border-radius: 50%;
}
@keyframes spin { to { transform: rotate(360deg); } }

@media (max-width: 640px) {
  .post-container { padding: var(--space-md); }
  .post-title { font-size: var(--font-size-lg); }
}
</style>
