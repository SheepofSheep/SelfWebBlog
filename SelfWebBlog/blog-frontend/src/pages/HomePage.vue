<script setup>
import { computed, onMounted, ref, inject, nextTick } from 'vue'
import { getProfile, deletePost } from '../utils/api'
import { useToast } from '../composables/toast'
import { toAbsoluteUrl } from '../utils/url'
import { navigate } from '../router'
import { Trash2, PenLine, Clock, Tag, Folder, Eye, Sparkles } from 'lucide-vue-next'
import { gsap } from 'gsap'
import WriteModal from '../components/WriteModal.vue'
import PosterCarousel from '../components/PosterCarousel.vue'
import PersonalLinks from '../components/PersonalLinks.vue'
import SkeletonCard from '../components/SkeletonCard.vue'
import MiniCalendar from '../components/MiniCalendar.vue'
import { formatTime, toPlainText } from '../utils/format'

const { push } = useToast()
const user = inject('user', null)

const blogInfo = ref(null)
const posts = ref([])
const totalPosts = ref(0)
const showWrite = ref(false)
const editingInitial = ref(null)
const loading = ref(true)

const displayPosts = computed(() => posts.value)

const calendarPosts = computed(() => displayPosts.value.map(p => ({ id: p.id, title: p.title, createTime: p.createTime })))

function postTags(p) {
  if (!p?.tags) return []
  return p.tags.split(/[,，\s]+/).filter(Boolean).slice(0, 3)
}

function postSummary(p) {
  const text = p?.summary || toPlainText(p?.content || '')
  return text.slice(0, 100) + (text.length > 100 ? '…' : '')
}

function readMinutes(p) {
  if (!p?.content) return 1
  const text = p.content.replace(/[#*_`\[\]!\(\)>\-\s]/g, '')
  return Math.max(1, Math.ceil(text.length / 400))
}

async function refresh() {
  loading.value = true
  try {
    const data = await getProfile({ page: 1, size: 4 })
    if (data.blogInfo) blogInfo.value = data.blogInfo
    if (Array.isArray(data.posts)) posts.value = data.posts
    if (data.total) totalPosts.value = data.total
    else totalPosts.value = posts.value.length
    await nextTick()
    if (document.querySelector('.post-row')) {
      gsap.from('.post-row', { y: 16, opacity: 0, duration: 0.5, stagger: 0.06, ease: 'power2.out' })
    }
  } catch (e) {
    push(e?.message || '加载失败', 'error')
  } finally {
    loading.value = false
  }
}

function openPost(id) { navigate(`/post?id=${encodeURIComponent(String(id))}`) }
function openWriteModal() { navigate('/write') }

async function handleDeletePost(postId, event) {
  if (!confirm('确定要删除这篇文章吗？')) return
  const card = event.currentTarget.closest('.post-row')
  if (card) {
    gsap.to(card, {
      scale: 0.98, opacity: 0, duration: 0.25, ease: 'power2.out',
      onComplete: async () => {
        try { await deletePost(postId); await refresh(); push('删除成功') }
        catch (e) { push(e?.message || '删除失败', 'error') }
      }
    })
  } else {
    try { await deletePost(postId); await refresh(); push('删除成功') }
    catch (e) { push(e?.message || '删除失败', 'error') }
  }
}

onMounted(async () => { await refresh() })
</script>

<template>
  <main class="home-main">
    <!-- 海报轮播 — 全宽 -->
    <PosterCarousel />

    <div class="home-layout">
      <!-- 左侧：文章 -->
      <div class="home-content">
        <section class="posts-section glass-panel">
          <header class="section-head">
            <div>
              <h2 class="section-title">最新文章</h2>
              <p class="section-sub">共 {{ totalPosts }} 篇 · 随手记录的想法</p>
            </div>
          </header>

          <div class="posts-list">
            <SkeletonCard v-if="loading" :count="3" />

            <div v-else-if="displayPosts.length === 0" class="empty-state">
              <PenLine :size="36" class="empty-icon" />
              <p class="empty-title">还没有文章</p>
              <p class="empty-desc">写下你的第一篇博客吧</p>
            </div>

            <article
              v-else
              v-for="(p, index) in displayPosts"
              :key="p.id"
              class="post-row glass-card"
              :style="{ animationDelay: `${index * 0.06}s` }"
              @click="openPost(p.id)"
            >
              <div class="post-thumb" :class="{ 'no-cover': !p.coverUrl }">
                <img v-if="p.coverUrl" :src="p.coverUrl" :alt="p.title" loading="lazy" />
                <span v-else class="thumb-placeholder">{{ p.title?.charAt(0) || '文' }}</span>
              </div>

              <div class="post-body">
                <div class="post-meta">
                  <span class="meta-item"><Clock :size="12" /> {{ formatTime(p.createTime) }}</span>
                  <span v-if="p.category" class="meta-item"><Folder :size="12" /> {{ p.category }}</span>
                  <span class="meta-item"><Eye :size="12" /> {{ readMinutes(p) }} 分钟</span>
                </div>
                <h3 class="post-title">{{ p.title }}</h3>
                <p class="post-excerpt">{{ postSummary(p) }}</p>
                <div v-if="postTags(p).length" class="post-tags" @click.stop>
                  <span v-for="t in postTags(p)" :key="t" class="tag">{{ t }}</span>
                </div>
              </div>

              <div v-if="user?.role === 'ADMIN'" class="post-actions" @click.stop>
                <button class="delete-btn" @click="handleDeletePost(p.id, $event)" aria-label="删除文章">
                  <Trash2 :size="14" />
                </button>
              </div>
            </article>
          </div>
        </section>
      </div>

      <!-- 右侧：博主信息卡片 -->
      <aside class="home-aside">
        <section
          class="hero-card glass-panel"
          :class="{ 'has-bg': blogInfo?.bgUrl }"
          :style="blogInfo?.bgUrl ? { backgroundImage: 'url(' + toAbsoluteUrl(blogInfo.bgUrl) + ')' } : {}"
        >
          <div class="hero-inner">
            <div class="hero-avatar-wrap">
              <img
                class="hero-avatar avatar-img"
                :class="{ loaded: blogInfo?.avatarUrl }"
                :src="toAbsoluteUrl(blogInfo?.avatarUrl || 'https://api.dicebear.com/7.x/avataaars/svg?seed=Felix')"
                :alt="blogInfo?.nickname || '博主头像'"
                @load="(e) => e.target.classList.add('loaded')"
              />
            </div>
            <div class="hero-text">
              <p class="hero-greeting"><Sparkles :size="14" /> 你好呀，欢迎来逛</p>
              <h1 class="hero-name">{{ blogInfo?.nickname || '加载中...' }}</h1>
              <p class="hero-bio">{{ blogInfo?.bio || '记录生活与代码的小角落' }}</p>
              <div class="hero-meta">
                <span class="hero-stat"><strong>{{ totalPosts }}</strong> 篇文章</span>
                <PersonalLinks />
              </div>
            </div>
            <button v-if="user?.role === 'ADMIN'" class="pill-btn pill-btn-primary hero-write" @click="openWriteModal">
              <PenLine :size="16" />
              <span>写文章</span>
            </button>
          </div>
        </section>

        <MiniCalendar :posts="calendarPosts" />
      </aside>
    </div>

    <WriteModal v-model="showWrite" :initial="editingInitial" @success="refresh" />
  </main>
</template>

<style scoped>
.home-main {
  width: 100%;
  padding: var(--space-md) 0;
}

/* ═══ 左右布局 ═══ */
.home-layout {
  max-width: 1060px;
  margin: 0 auto;
  padding: 0 var(--space-md);
  display: flex;
  gap: 32px;
  align-items: flex-start;
}

.home-content {
  flex: 1;
  min-width: 0;
}

.home-aside {
  width: 260px;
  flex-shrink: 0;
  position: sticky;
  top: 20px;
}

/* ====== Hero ====== */
.hero-card {
  padding: var(--space-md);
  background-size: cover;
  background-position: center;
  position: relative;
  overflow: hidden;
  text-align: center;
}

.hero-card:hover {
  transform: none;
}

.hero-card.has-bg::before {
  content: '';
  position: absolute;
  inset: 0;
  background: rgba(255, 255, 255, 0.88);
  z-index: 0;
}
[data-theme='dark'] .hero-card.has-bg::before {
  background: rgba(26, 24, 24, 0.82);
}
.hero-card.has-bg > * { position: relative; z-index: 1; }

.hero-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-sm);
}

.hero-avatar-wrap {
  width: 80px;
  height: 80px;
  padding: 3px;
  border-radius: 50%;
  flex-shrink: 0;
  background: linear-gradient(135deg, var(--theme-pink), var(--theme-peach));
  margin-bottom: 4px;
}

.hero-avatar {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
  display: block;
  background: rgba(255, 248, 250, 0.95);
}

.hero-text {
  width: 100%;
  text-align: center;
}

.hero-greeting {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin: 0 0 6px;
  font-size: var(--font-size-xs);
  color: var(--theme-pink-hover);
  letter-spacing: 0.04em;
}

.hero-name {
  margin: 0 0 4px;
  font-size: var(--font-size-xl);
  font-weight: 600;
  letter-spacing: 0.02em;
  color: var(--text-main);
}

.hero-bio {
  margin: 0 0 10px;
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
  line-height: var(--line-height);
}

.hero-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--space-md);
}

.hero-stat {
  font-size: var(--font-size-xs);
  color: var(--text-muted);
}
.hero-stat strong {
  font-size: var(--font-size-md);
  color: var(--theme-pink-hover);
  margin-right: 4px;
}

.hero-write {
  width: 100%;
  margin-top: var(--space-xs);
}

/* ====== 文章区 ====== */
.posts-section {
  padding: var(--space-lg);
}

.posts-section:hover {
  transform: none;
}

.section-head {
  margin-bottom: var(--space-md);
  padding-bottom: var(--space-md);
  border-bottom: 1px solid rgba(244, 164, 184, 0.15);
}

.section-title {
  margin: 0 0 4px;
  font-size: var(--font-size-lg);
  font-weight: 600;
  letter-spacing: 0.02em;
  color: var(--text-main);
}

.section-sub {
  margin: 0;
  font-size: var(--font-size-xs);
  color: var(--text-muted);
}

.posts-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
  min-height: 120px;
}

/* ====== 横向文章卡片 ====== */
.post-row {
  display: flex;
  align-items: stretch;
  gap: var(--space-md);
  padding: var(--space-md);
  cursor: pointer;
  position: relative;
}

.post-row:hover {
  transform: translateY(-3px);
}

.post-thumb {
  width: 128px;
  height: 96px;
  flex-shrink: 0;
  border-radius: var(--radius-md);
  overflow: hidden;
  background: rgba(244, 164, 184, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.5);
  box-shadow: var(--shadow-inner);
}

.post-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center 30%;
  display: block;
  transition: transform var(--duration-slow) var(--ease-soft);
}

.post-row:hover .post-thumb img {
  transform: scale(1.08);
}

.post-thumb.no-cover {
  display: flex;
  align-items: center;
  justify-content: center;
}

.thumb-placeholder {
  font-size: 1.75rem;
  font-weight: 600;
  color: var(--theme-pink-hover);
  opacity: 0.6;
}

.post-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.post-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 6px;
  color: var(--text-muted);
  font-size: 0.68rem;
}

.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.post-title {
  margin: 0 0 6px;
  font-size: var(--font-size-md);
  font-weight: 600;
  color: var(--text-main);
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.post-excerpt {
  margin: 0;
  font-size: var(--font-size-xs);
  color: var(--text-secondary);
  line-height: 1.65;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.post-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
}

.post-tags .tag {
  padding: 2px 8px;
  font-size: 0.62rem;
}

.post-actions {
  position: absolute;
  top: 10px;
  right: 10px;
}

.delete-btn {
  background: rgba(255, 255, 255, 0.5);
  color: var(--theme-pink-hover);
  border: 1px solid rgba(244, 164, 184, 0.3);
  padding: 5px;
  cursor: pointer;
  border-radius: var(--radius-sm);
  display: flex;
  visibility: hidden;
  opacity: 0;
  transition: opacity var(--duration-normal) var(--ease-bounce),
              visibility var(--duration-normal) var(--ease-bounce),
              background var(--duration-normal) var(--ease-bounce);
}

.post-row:hover .delete-btn {
  visibility: visible;
  opacity: 1;
}

.delete-btn:hover {
  background: var(--danger);
  color: var(--on-primary);
  border-color: var(--danger);
}

.empty-state {
  text-align: center;
  padding: var(--space-xl) 0;
  color: var(--text-muted);
}

.empty-icon {
  opacity: 0.4;
  margin-bottom: var(--space-sm);
}

.empty-title {
  margin: 0 0 4px;
  font-weight: 600;
  color: var(--text-main);
}

.empty-desc {
  margin: 0;
  font-size: var(--font-size-sm);
}

@media (max-width: 860px) {
  .home-layout {
    flex-direction: column;
  }
  .home-aside {
    width: 100%;
    position: static;
  }
}

@media (max-width: 640px) {
  .hero-write {
    width: 100%;
  }

  .posts-section,
  .hero-card {
    padding: var(--space-md);
  }

  .post-row {
    flex-direction: column;
    gap: var(--space-sm);
  }

  .post-thumb {
    width: 100%;
    height: 140px;
  }
}
</style>
