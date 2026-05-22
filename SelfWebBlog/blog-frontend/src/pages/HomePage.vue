<script setup>
import { computed, onMounted, ref, inject, nextTick } from 'vue'
import { getProfile, deletePost } from '../utils/api'
import { useToast } from '../composables/toast'
import { toAbsoluteUrl } from '../utils/url'
import { navigate } from '../router'
import { Trash2, PenLine, Clock } from 'lucide-vue-next'
import { gsap } from 'gsap'
import WriteModal from '../components/WriteModal.vue'
import PosterCarousel from '../components/PosterCarousel.vue'
import PersonalLinks from '../components/PersonalLinks.vue'
import SkeletonCard from '../components/SkeletonCard.vue'
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

async function refresh() {
  loading.value = true
  try {
    const data = await getProfile({ page: 1, size: 4 })
    if (data.blogInfo) blogInfo.value = data.blogInfo
    if (Array.isArray(data.posts)) posts.value = data.posts
    if (data.total) totalPosts.value = data.total
    else totalPosts.value = posts.value.length
    await nextTick()
    if (document.querySelector('.post-card')) {
      gsap.from('.post-card', { y: 20, opacity: 0, duration: 0.5, stagger: 0.08, ease: 'power2.out' })
    }
  } catch (e) {
    push(e?.message || '加载失败', 'error')
  } finally {
    loading.value = false
  }
}

function openPost(id) { navigate(`/post?id=${encodeURIComponent(String(id))}`) }
function goToProfile() { navigate('/profile') }
function openWriteModal() { navigate('/write') }

function maybeOpenEditing() {
  // 由 WritePage 处理，此处不再需要
}

async function handleDeletePost(postId, event) {
  if (!confirm('确定要删除这篇文章吗？')) return
  const card = event.currentTarget.closest('.post-card')
  if (card) {
    gsap.to(card, {
      scale: 0.9, opacity: 0, duration: 0.25, ease: 'power2.out',
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

onMounted(async () => { await refresh(); maybeOpenEditing() })
</script>

<template>
  <main class="home-main">
    <!-- 海报轮播 -->
    <PosterCarousel />

    <!-- 内容区 -->
    <div class="home-layout">
      <!-- 侧边栏 -->
      <aside class="home-aside">
        <div class="glass-card profile-card">
          <div class="avatar-ring">
            <img
              class="avatar-img"
              :class="{ loaded: blogInfo?.avatarUrl }"
              :src="toAbsoluteUrl(blogInfo?.avatarUrl || 'https://api.dicebear.com/7.x/avataaars/svg?seed=Felix')"
              :alt="blogInfo?.nickname || '博主头像'"
              @load="(e) => e.target.classList.add('loaded')"
            />
          </div>
          <h2 class="profile-name">{{ blogInfo?.nickname || '加载中...' }}</h2>
          <p class="profile-bio">{{ blogInfo?.bio || 'OvO' }}</p>
          <div class="profile-stats">
            <div class="stat-item">
              <span class="stat-num">{{ totalPosts }}</span>
              <span class="stat-label">篇文章</span>
            </div>
          </div>
          <PersonalLinks />
        </div>
      </aside>

      <!-- 文章 Feed -->
      <section class="home-feed custom-scrollbar">
        <div class="feed-topbar">
          <button v-if="user?.role === 'ADMIN'" class="pill-btn pill-btn-primary" @click="openWriteModal">
            <PenLine :size="18" />
            <span>写文章</span>
          </button>
        </div>

        <div class="glass-card posts-panel">
          <div class="panel-header">
            <h2 class="panel-title">最新文章</h2>
            <span class="badge-count">{{ totalPosts }} 篇</span>
          </div>

          <div class="posts-list">
            <SkeletonCard v-if="loading" :count="3" />
            <div v-else-if="displayPosts.length === 0" class="empty-state">
              <div class="empty-icon">
                <PenLine :size="40" />
              </div>
              <div class="empty-title">还没有文章</div>
              <div class="empty-desc">写下你的第一篇博客吧</div>
            </div>
            <div v-else class="post-grid">
              <article
                v-for="(p, index) in displayPosts"
                :key="p.id"
                class="glass-card post-card"
                :style="{ animationDelay: `${index * 0.08}s` }"
                @click="openPost(p.id)"
              >
                <div class="post-accent"></div>
                <div class="post-inner">
                  <div class="post-meta">
                    <Clock :size="13" />
                    <span class="post-date">{{ formatTime(p.createTime) }}</span>
                  </div>
                  <h3 class="post-title">{{ p.title }}</h3>
                  <p class="post-excerpt">
                    {{ (() => { const t = toPlainText(p.content); return t.length > 120 ? t.slice(0, 120) + '...' : t })() }}
                  </p>
                </div>
                <div v-if="user?.role === 'ADMIN'" class="post-actions" @click.stop>
                  <button class="delete-btn" @click="handleDeletePost(p.id, $event)" aria-label="删除文章">
                    <Trash2 :size="14" />
                  </button>
                </div>
              </article>
            </div>
          </div>

          <div v-if="totalPosts > posts.length" class="panel-footer">
            <button class="pill-btn pill-btn-ghost" @click="goToProfile">
              查看全部 {{ totalPosts }} 篇文章
            </button>
          </div>
        </div>
      </section>
    </div>

    <WriteModal v-model="showWrite" :initial="editingInitial" @success="refresh" />
  </main>
</template>

<style scoped>
.home-main {
  max-width: 1140px;
  margin: 0 auto;
  width: 100%;
}

.home-layout {
  display: flex;
  gap: 32px;
  align-items: flex-start;
  justify-content: center;
}

/* ====== 侧边栏（右侧） ====== */
.home-aside {
  width: 240px;
  flex-shrink: 0;
  position: sticky;
  top: 24px;
  order: 1;
}

.profile-card {
  padding: 28px 20px 20px;
  text-align: center;
}

.avatar-ring {
  width: 88px; height: 88px;
  padding: 3px;
  margin: 0 auto 14px;
  background: linear-gradient(135deg, var(--red), var(--red-hover), var(--secondary-light));
}

.avatar-img {
  width: 100%; height: 100%;
  object-fit: cover;
  display: block;
  background: #fff;
}

.profile-name {
  margin: 0 0 4px;
  font-family: var(--font-display);
  font-size: 1.1rem;
  font-weight: 700;
  background: linear-gradient(135deg, var(--red), var(--secondary-light));
  background-clip: text;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.profile-bio {
  margin: 0 0 14px;
  font-size: 0.78rem;
  color: var(--ink-muted);
  line-height: 1.6;
}

.profile-stats {
  display: flex;
  justify-content: center;
  gap: 24px;
  margin-bottom: 2px;
}
.stat-item { display: flex; flex-direction: column; align-items: center; }
.stat-num { font-family: var(--font-display); font-size: 1.3rem; color: var(--red); font-weight: 700; }
.stat-label { font-size: 0.65rem; color: var(--ink-muted); margin-top: 2px; }

/* ====== Feed ====== */
.home-feed { flex: 1; min-width: 0; max-width: 680px; }

.feed-topbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 20px;
}

.posts-panel { padding: 28px; }

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 18px;
  margin-bottom: 20px;
  border-bottom: 1px solid var(--rule);
}

.panel-title {
  margin: 0;
  font-family: var(--font-display);
  font-size: 1.15rem;
  font-weight: 700;
}

.badge-count {
  display: inline-flex;
  font-size: 0.7rem;
  background: var(--red-soft);
  color: var(--red);
  padding: 4px 10px;
  font-weight: 600;
}

.posts-list { min-height: 200px; }

.post-grid { display: grid; gap: 14px; }

/* ====== 文章卡片 ====== */
.post-card {
  position: relative;
  padding: 0;
  cursor: pointer;
  display: flex;
  overflow: hidden;
}

.post-accent {
  width: 4px;
  flex-shrink: 0;
  background: linear-gradient(180deg, var(--red) 0%, var(--red-hover) 50%, var(--secondary-light) 100%);
  opacity: 0.5;
  transition: opacity var(--duration-normal);
}
.post-card:hover .post-accent { opacity: 1; }

.post-inner { flex: 1; padding: 20px; position: relative; z-index: 1; }

.post-actions { position: absolute; top: 12px; right: 12px; z-index: 2; }
.delete-btn {
  background: var(--red-wash);
  color: var(--red);
  border: 1px solid var(--rule);
  padding: 5px;
  cursor: pointer;
  transition: opacity var(--duration-fast), visibility var(--duration-fast), background var(--duration-fast), color var(--duration-fast), border-color var(--duration-fast);
  display: flex;
  visibility: hidden;
  opacity: 0;
}
.post-card:hover .delete-btn {
  visibility: visible;
  opacity: 1;
}
.delete-btn:hover { background: var(--red); color: var(--on-primary); border-color: var(--red); }

.post-meta { display: flex; align-items: center; gap: 6px; margin-bottom: 10px; color: var(--ink-muted); }
.post-date { font-size: 0.7rem; font-weight: 600; letter-spacing: 0.04em; }

.post-title {
  margin: 0 0 10px;
  font-family: var(--font-display);
  font-size: 1.05rem;
  font-weight: 700;
  color: var(--ink);
  line-height: 1.35;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.post-excerpt {
  margin: 0;
  color: var(--ink-muted);
  font-size: 0.82rem;
  line-height: 1.7;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* ====== 空状态 ====== */
.empty-state { text-align: center; padding: 48px 0; }
.empty-icon { color: var(--ink-muted); margin-bottom: 12px; }
.empty-title { font-family: var(--font-display); font-size: 1.1rem; font-weight: 700; color: var(--ink); margin-bottom: 6px; }
.empty-desc { font-size: 0.85rem; color: var(--ink-muted); }

.panel-footer { margin-top: 20px; padding-top: 18px; border-top: 1px solid var(--rule); text-align: center; }

/* ====== 响应式 ====== */
@media (max-width: 900px) {
  .home-layout { flex-direction: column; }
  .home-aside { width: 100%; position: static; }
  .profile-card { display: flex; flex-wrap: wrap; align-items: center; gap: 14px; text-align: left; padding: 18px; }
  .avatar-ring { width: 60px; height: 60px; margin: 0; }
  .profile-stats { margin-left: auto; }
}
@media (max-width: 640px) {
  .posts-panel { padding: 16px; }
}
</style>
