<script setup>
import { computed, onMounted, ref, inject, nextTick } from 'vue'
import { getProfile, deletePost } from '../utils/api'
import { useToast } from '../composables/toast'
import { navigate } from '../router'
import { ArrowRight, PenLine } from 'lucide-vue-next'
import { gsap } from 'gsap'
import PosterCarousel from '../components/PosterCarousel.vue'
import ConfirmDialog from '../components/ConfirmDialog.vue'
import MagazineHero from '../components/home/MagazineHero.vue'
import MagazineArticleCard from '../components/home/MagazineArticleCard.vue'
import EditorialIndex from '../components/home/EditorialIndex.vue'
import AuthorRail from '../components/home/AuthorRail.vue'

const { push } = useToast()
const user = inject('user', null)

const blogInfo = ref(null)
const posts = ref([])
const totalPosts = ref(0)
const loading = ref(true)
const confirmDialog = ref({
  open: false,
  title: '',
  message: '',
  confirmText: '确认',
  onConfirm: null
})

const displayPosts = computed(() => posts.value)
const gridPosts = computed(() =>
  displayPosts.value.length > 4 ? displayPosts.value.slice(4) : displayPosts.value.slice(1)
)
const isAdmin = computed(() => user?.value?.role === 'ADMIN' || user?.role === 'ADMIN')

const calendarPosts = ref([])

async function loadCalendarPosts() {
  try {
    const data = await getProfile({ page: 1, size: 50 })
    if (Array.isArray(data?.posts)) {
      calendarPosts.value = data.posts.map((p) => ({
        id: p.id,
        title: p.title,
        createTime: p.createTime
      }))
    }
  } catch {}
}

async function refresh() {
  loading.value = true
  try {
    const data = await getProfile({ page: 1, size: 10 })
    if (data.blogInfo) blogInfo.value = data.blogInfo
    if (Array.isArray(data.posts)) posts.value = data.posts
    if (data.total) totalPosts.value = data.total
    else totalPosts.value = posts.value.length
    await nextTick()
    if (document.querySelector('.mag-card')) {
      gsap.from('.mag-card', {
        y: 16,
        opacity: 0,
        duration: 0.5,
        stagger: 0.06,
        ease: 'power2.out'
      })
    }
  } catch (e) {
    push(e?.message || '加载失败', 'error')
  } finally {
    loading.value = false
  }
}

function openPost(id) {
  navigate(`/post?id=${encodeURIComponent(String(id))}`)
}
function openWriteModal() {
  navigate('/write')
}

function openArchiveWithSearch(keyword) {
  navigate(`/archive?keyword=${encodeURIComponent(keyword)}`)
}

function openArchiveWithTag(tag) {
  navigate(`/archive?tag=${encodeURIComponent(tag)}`)
}

function openArchiveWithCategory(category) {
  navigate(`/archive?category=${encodeURIComponent(category)}`)
}

async function handleDeletePost(postId, event) {
  const card = event?.currentTarget?.closest('.mag-card')
  askConfirm({
    title: '删除文章',
    message: '确定要删除这篇文章吗？删除后访客将无法继续阅读。',
    confirmText: '删除文章',
    onConfirm: async () => deletePostWithAnimation(postId, card)
  })
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

async function deletePostWithAnimation(postId, card) {
  if (card) {
    gsap.to(card, {
      scale: 0.98,
      opacity: 0,
      duration: 0.25,
      ease: 'power2.out',
      onComplete: async () => {
        try {
          await deletePost(postId)
          await refresh()
          push('文章已删除')
        } catch (e) {
          push(e?.message || '这篇文章暂时没有删掉，稍后再试一次。', 'error')
        }
      }
    })
  } else {
    try {
      await deletePost(postId)
      await refresh()
      push('文章已删除')
    } catch (e) {
      push(e?.message || '这篇文章暂时没有删掉，稍后再试一次。', 'error')
    }
  }
}

onMounted(async () => {
  await Promise.all([refresh(), loadCalendarPosts()])
})
</script>

<template>
  <main class="home-main">
    <section class="home-cover">
      <div class="cover-copy">
        <p class="cover-kicker">GABRIEL MAGAZINE</p>
        <h1>Gabriel</h1>
        <p>欢 迎 OvO。</p>
      </div>
      <button v-if="isAdmin" class="cover-write" @click="openWriteModal">
        <PenLine :size="16" />
        写新文章
      </button>
    </section>

    <div v-if="loading" class="home-loading">
      <div v-for="i in 4" :key="i" class="loading-shimmer loading-card"></div>
    </div>

    <template v-else>
      <MagazineHero
        :posts="displayPosts"
        :blog-info="blogInfo"
        :total-posts="totalPosts"
        :is-admin="isAdmin"
        @open="openPost"
        @delete="handleDeletePost"
        @tag="openArchiveWithTag"
        @category="openArchiveWithCategory"
        @write="openWriteModal"
        @archive="navigate('/archive')"
      />

      <EditorialIndex
        :posts="displayPosts"
        @search="openArchiveWithSearch"
        @tag="openArchiveWithTag"
        @category="openArchiveWithCategory"
        @archive="navigate('/archive')"
      />

      <section class="magazine-body">
        <div class="article-flow">
          <header class="flow-head">
            <div>
              <p class="flow-kicker">LATEST ROLL</p>
              <h2>继续阅读</h2>
            </div>
            <button class="flow-more" @click="navigate('/archive')">
              全部文章
              <ArrowRight :size="15" />
            </button>
          </header>

          <div v-if="gridPosts.length" class="article-grid">
            <MagazineArticleCard
              v-for="p in gridPosts"
              :key="p.id"
              :post="p"
              variant="grid"
              :is-admin="isAdmin"
              @open="openPost"
              @delete="handleDeletePost"
              @tag="openArchiveWithTag"
              @category="openArchiveWithCategory"
            />
          </div>

          <div v-else class="empty-state glass-card">
            <PenLine :size="34" />
            <h2>还没有文章</h2>
            <p>发布第一篇后，首页会自动生成杂志封面和文章流。</p>
            <button v-if="isAdmin" class="cover-write" @click="openWriteModal">开始写作</button>
          </div>
        </div>

        <AuthorRail
          :blog-info="blogInfo"
          :posts="displayPosts"
          :calendar-posts="calendarPosts"
          :total-posts="totalPosts"
          :is-admin="isAdmin"
          @write="openWriteModal"
          @archive="navigate('/archive')"
          @tag="openArchiveWithTag"
        />

        <div class="poster-strip">
          <PosterCarousel />
        </div>
      </section>
    </template>

    <ConfirmDialog
      v-model="confirmDialog.open"
      :title="confirmDialog.title"
      :message="confirmDialog.message"
      :confirm-text="confirmDialog.confirmText"
      @confirm="handleConfirmAction"
    />
  </main>
</template>

<style scoped>
.home-main {
  width: 100%;
  padding: 0 0 var(--space-xl);
}

.home-cover {
  max-width: var(--magazine-max, 1180px);
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 16px var(--space-md) 14px;
}

.cover-kicker,
.flow-kicker {
  margin: 0 0 6px;
  color: var(--primary-hover);
  font-size: 0.72rem;
  font-weight: 900;
  letter-spacing: 0.14em;
}

.cover-copy h1 {
  margin: 0;
  font-family: var(--font-serif);
  font-size: clamp(2rem, 5vw, 4.6rem);
  line-height: 1;
  color: var(--text-main);
}

.cover-copy p:last-child {
  margin: 10px 0 0;
  color: var(--text-secondary);
  font-size: 0.95rem;
}

.cover-write,
.flow-more {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  min-height: 42px;
  padding: 0 16px;
  border: 1px solid var(--border-warm);
  border-radius: var(--radius-pill);
  background: var(--primary);
  color: var(--on-primary);
  font: inherit;
  font-weight: 900;
  cursor: pointer;
  box-shadow: 0 8px 18px var(--primary-glow);
}

.home-loading {
  display: grid;
  grid-template-columns: minmax(0, 1.5fr) minmax(280px, 0.8fr);
  gap: 16px;
  max-width: var(--magazine-max, 1180px);
  margin: 0 auto;
  padding: 0 var(--space-md);
}

.loading-card {
  min-height: 180px;
  border-radius: 26px;
  background: var(--surface-muted);
}

.magazine-body {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 290px;
  gap: 18px;
  max-width: var(--magazine-max, 1180px);
  margin: 18px auto 0;
  padding: 0 var(--space-md);
  align-items: start;
}

.article-flow {
  min-width: 0;
}

.flow-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
}

.flow-head h2 {
  margin: 0;
  font-family: var(--font-serif);
  font-size: clamp(1.5rem, 3vw, 2.25rem);
  color: var(--text-main);
}

.flow-more {
  min-height: 38px;
  padding-inline: 13px;
  background: var(--surface-muted);
  color: var(--text-secondary);
  box-shadow: none;
}

.article-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.poster-strip {
  grid-column: 1 / -1;
  margin-top: 2px;
}

.empty-state {
  display: grid;
  justify-items: center;
  gap: 10px;
  padding: 54px 20px;
  text-align: center;
}

@media (max-width: 980px) {
  .magazine-body {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .home-cover {
    align-items: flex-start;
    flex-direction: column;
    padding-inline: var(--space-sm);
  }

  .cover-write {
    width: 100%;
  }

  .home-loading,
  .magazine-body {
    padding-inline: var(--space-sm);
  }

  .home-loading,
  .article-grid {
    grid-template-columns: 1fr;
  }
}
</style>
