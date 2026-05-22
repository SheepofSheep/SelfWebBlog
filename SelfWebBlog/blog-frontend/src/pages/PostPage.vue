<script setup>
import { ref, computed, onMounted, inject, nextTick, watch } from 'vue'
import { useRoute, navigate } from '../router'
import { getPost, addComment, deleteComment, getComments, togglePinComment } from '../utils/api'
import { showToast } from '../composables/toast'
import { renderMarkdown } from '../utils/marked'
import { toAbsoluteUrl } from '../utils/url'
import loadingStore from '../stores/loadingStore'
import { ArrowLeft, Send, Trash2, MessageCircle, Clock, MoreHorizontal, Pin, PinOff } from 'lucide-vue-next'
import { gsap } from 'gsap'

function goBack() { window.history.back() }

const { query } = useRoute()
const postId = computed(() => query.value.get('id'))
const post = ref(null)
const comments = ref([])
const commentContent = ref('')
const user = inject('user', null)
const refreshUser = inject('refreshUser', null)
const commentSubmitting = ref(false)
const commentRefs = ref({})

const renderedContent = computed(() => post.value ? renderMarkdown(post.value.content) : '')

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
      <button class="back-btn" @click="goBack">
        <ArrowLeft :size="16" /> 返回
      </button>
      <h1 class="post-title">{{ post.title }}</h1>
      <div class="post-meta">
        <Clock :size="14" />
        <span>{{ formatDate(post.createTime) }}</span>
      </div>
      <div class="post-content" v-html="renderedContent"></div>

      <!-- 评论区 -->
      <div class="comments-section">
        <h2 class="comments-title">
          <MessageCircle :size="18" /> 评论
        </h2>

        <div class="comment-form">
          <textarea v-model="commentContent" placeholder="写下你的评论..." rows="3"></textarea>
          <button class="pill-btn pill-btn-primary" :disabled="!commentContent.trim() || commentSubmitting" @click="handleAddComment">
            <Send :size="14" /> {{ commentSubmitting ? '发布中...' : '发表评论' }}
          </button>
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
              <p class="comment-text">{{ c.content }}</p>
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

    <div v-else class="loading-state">
      <div class="loading-spin"></div>
      <p>加载中...</p>
    </div>
  </div>
</template>

<style scoped>
.post-page { max-width: 800px; margin: 0 auto; width: 100%; }

.post-container { padding: 32px; }
.back-btn {
  display: inline-flex; align-items: center; gap: 6px;
  background: none; border: 1px solid var(--rule);
  padding: 6px 16px; font-size: 0.82rem; cursor: pointer;
  margin-bottom: 20px; transition: border-color var(--duration-fast), color var(--duration-fast), background var(--duration-fast);
}
.back-btn:hover { border-color: var(--red); color: var(--red); background: var(--red-soft); }

.post-title {
  font-family: var(--font-display); font-size: 2rem; font-weight: 800;
  color: var(--ink); line-height: 1.3; margin: 0 0 12px;
}

.post-meta { display: flex; align-items: center; gap: 6px; color: var(--ink-muted); font-size: 0.82rem; margin-bottom: 28px; }

.post-content { font-size: 1rem; line-height: 1.85; color: var(--ink); }
.post-content h2 { font-family: var(--font-display); font-size: 1.3rem; margin: 2rem 0 1rem; }
.post-content p { margin-bottom: 1rem; }

/* ====== 评论区 ====== */
.comments-section { margin-top: 3rem; padding-top: 2rem; border-top: 1px solid var(--rule); }
.comments-title { display: flex; align-items: center; gap: 8px; font-family: var(--font-display); font-size: 1.1rem; margin-bottom: 1.2rem; color: var(--ink); }

.comment-form { margin-bottom: 1.5rem; }
.comment-form textarea {
  width: 100%; box-sizing: border-box; padding: 12px 16px;
  resize: vertical; font-size: 0.9rem; font-family: var(--font-body);
  background: var(--paper); color: var(--ink); outline: none;
  margin-bottom: 10px; transition: border-color var(--duration-fast);
}
.comment-form textarea:focus { border-color: var(--red); }

.comments-list { display: flex; flex-direction: column; gap: 12px; }

.comment-item {
  display: flex; gap: 12px; align-items: flex-start;
  background: rgba(0,0,0,0.02); border: 1px solid var(--rule);
}
.admin-comment { border-color: var(--red-soft); background: var(--red-soft); }
.comment-body { flex: 1; min-width: 0; }
.comment-head { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; }
.comment-name { font-weight: 600; font-size: 0.85rem; color: var(--ink); }
.comment-time { margin-left: auto; font-size: 0.72rem; color: var(--ink-muted); }
.comment-text { margin: 0; font-size: 0.88rem; color: var(--ink-muted); line-height: 1.6; }
/* ====== 管理菜单 ====== */
.comment-actions { position: relative; flex-shrink: 0; }
.menu-btn {
  display: flex; align-items: center; justify-content: center;
  background: transparent; color: var(--ink-muted);
  cursor: pointer; transition: background var(--duration-fast), color var(--duration-fast);
}
.menu-btn:hover { background: rgba(0,0,0,0.04); color: var(--ink); }

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
  font-size: 0.6rem; font-weight: 600; color: var(--red);
  background: var(--red-soft); padding: 2px 8px;
  margin-right: 6px; vertical-align: middle;
}
.pinned { border-color: var(--red-soft); background: rgba(186,0,52,0.02); }

.fade-enter-active, .fade-leave-active { transition: opacity 0.15s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

.comments-empty { text-align: center; padding: 2rem; color: var(--ink-muted); font-size: 0.88rem; }

.loading-state { text-align: center; padding: 4rem; color: var(--ink-muted); }
@keyframes spin { to { transform: rotate(360deg); } }

@media (max-width: 640px) {
  .post-container { padding: 20px; }
  .post-title { font-size: 1.5rem; }
}
</style>
