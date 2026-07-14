<script setup>
import { computed, inject, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { PenLine } from 'lucide-vue-next'
import { deletePost } from '../api'
import { useHomeData } from '../composables/useHomeData'
import { useToast } from '../composables/toast'
import ConfirmDialog from '../components/ConfirmDialog.vue'
import EmptyState from '../components/ui/EmptyState.vue'
import DeferredMount from '../components/ui/DeferredMount.vue'
import FeaturedPost from '../features/content/components/FeaturedPost.vue'
import RecentPostRows from '../features/content/components/RecentPostRows.vue'
import PostFilterToolbar from '../features/content/components/PostFilterToolbar.vue'
import PostFeed from '../features/content/components/PostFeed.vue'
import ContentCalendarMini from '../features/content/components/ContentCalendarMini.vue'
import PopularTags from '../features/content/components/PopularTags.vue'
import AuthorSummary from '../features/site/components/AuthorSummary.vue'
import SiteStatPanel from '../features/site/components/SiteStatPanel.vue'

defineOptions({ name: 'HomePage' })

const router = useRouter()
const user = inject('user', null)
const { push } = useToast()
const {
  blogInfo,
  posts,
  totalPosts,
  loading,
  error,
  featuredPost,
  recentPosts,
  feedPosts,
  categories,
  tags,
  load,
  dispose
} = useHomeData()
const keyword = ref('')
const category = ref('')
const tag = ref('')
const isAdmin = computed(() => user?.value?.role === 'ADMIN' || user?.role === 'ADMIN')
const confirmDialog = ref({ open: false, postId: null })

function openPost(id) {
  router.push(`/post/${encodeURIComponent(String(id))}`)
}

function openArchive(query = {}) {
  router.push({ name: 'archive', query })
}

function submitFilter() {
  openArchive({
    ...(keyword.value.trim() ? { keyword: keyword.value.trim() } : {}),
    ...(category.value ? { category: category.value } : {}),
    ...(tag.value ? { tag: tag.value } : {})
  })
}

function clearFilter() {
  keyword.value = ''
  category.value = ''
  tag.value = ''
}

function askDelete(postId) {
  confirmDialog.value = { open: true, postId }
}

async function confirmDelete() {
  const postId = confirmDialog.value.postId
  confirmDialog.value.open = false
  if (!postId) return
  try {
    await deletePost(postId)
    await load()
    push('文章已删除')
  } catch (requestError) {
    push(requestError?.message || '文章暂时没有删掉。', 'error')
  }
}

onMounted(load)
onBeforeUnmount(dispose)
</script>

<template>
  <div class="home-page page-width">
    <header class="home-status">
      <div>
        <span>Gabriel's Blog</span>
        <p v-if="blogInfo?.currentStatus">{{ blogInfo.currentStatus }}</p>
      </div>
      <button v-if="isAdmin" type="button" @click="router.push('/write')">
        <PenLine :size="16" />写文章
      </button>
      <small v-else>{{ totalPosts }} 篇公开文章</small>
    </header>

    <div v-if="loading" class="home-skeleton">
      <div class="loading-shimmer"></div>
      <div class="loading-shimmer"></div>
      <div class="loading-shimmer"></div>
    </div>

    <div v-else-if="featuredPost" class="home-grid">
      <div class="content-column">
        <FeaturedPost :post="featuredPost" @open="openPost" />
        <RecentPostRows :posts="recentPosts" @open="openPost" @archive="openArchive" />
        <PostFilterToolbar
          v-model:keyword="keyword"
          v-model:category="category"
          v-model:tag="tag"
          :categories="categories"
          :tags="tags"
          @submit="submitFilter"
          @clear="clearFilter"
        />
        <PostFeed
          :posts="feedPosts"
          :is-admin="isAdmin"
          @open="openPost"
          @archive="openArchive"
          @delete="askDelete"
        />
      </div>

      <aside class="sidebar-column">
        <AuthorSummary :blog-info="blogInfo" @about="router.push('/about')" />
        <div class="sidebar-stack">
          <SiteStatPanel :posts="posts" :total-posts="totalPosts" />
          <DeferredMount min-height="180px">
            <ContentCalendarMini :posts="posts" @open="router.push('/calendar')" />
          </DeferredMount>
          <DeferredMount min-height="140px">
            <PopularTags
              :tags="tags"
              @select="openArchive({ tag: $event })"
              @open="router.push('/tags')"
            />
          </DeferredMount>
        </div>
      </aside>
    </div>

    <EmptyState v-else :title="error || '还没有公开文章'" description="发布文章后会在这里显示。">
      <template #icon><PenLine :size="21" /></template>
      <button v-if="isAdmin" class="empty-write" type="button" @click="router.push('/write')">
        开始写作
      </button>
    </EmptyState>

    <ConfirmDialog
      v-model="confirmDialog.open"
      title="删除文章"
      message="确定删除这篇文章吗？删除后访客将无法继续阅读。"
      confirm-text="删除文章"
      @confirm="confirmDelete"
    />
  </div>
</template>

<style scoped>
.home-page {
  padding-block: 20px 64px;
}
.home-status {
  min-height: 58px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 18px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--border-subtle);
}
.home-status > div {
  min-width: 0;
  display: flex;
  align-items: baseline;
  gap: 14px;
}
.home-status span {
  flex: 0 0 auto;
  color: var(--accent-strong);
  font-size: 0.72rem;
  font-weight: 900;
}
.home-status p,
.home-status small {
  margin: 0;
  overflow: hidden;
  color: var(--text-secondary);
  font-size: 0.76rem;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.home-status button,
.empty-write {
  min-height: 38px;
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 7px;
  padding: 0 14px;
  border: 1px solid var(--accent);
  border-radius: var(--radius-control);
  background: var(--accent);
  color: #2c1d06;
  font: inherit;
  font-size: 0.76rem;
  font-weight: 800;
  cursor: pointer;
}
.home-grid {
  display: grid;
  grid-template-columns: minmax(0, 2fr) minmax(250px, 1fr);
  align-items: start;
  gap: clamp(24px, 3.5vw, 42px);
}
.content-column {
  min-width: 0;
  display: grid;
  gap: 34px;
}
.sidebar-column {
  min-width: 0;
  display: grid;
  gap: 22px;
  position: sticky;
  top: 92px;
}
.sidebar-stack {
  display: grid;
  gap: 22px;
  padding: 18px;
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-card);
  background: var(--surface-solid);
}
.sidebar-stack > * + * {
  padding-top: 20px;
  border-top: 1px solid var(--border-subtle);
}
.home-skeleton {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 24px;
  min-height: 620px;
}
.home-skeleton > :first-child {
  grid-row: span 2;
}
.home-skeleton > div {
  border-radius: var(--radius-card);
}
@media (max-width: 900px) {
  .home-grid,
  .home-skeleton {
    grid-template-columns: 1fr;
  }
  .sidebar-column {
    position: static;
    grid-template-columns: 1fr 1fr;
  }
}
@media (max-width: 620px) {
  .home-page {
    width: min(100% - 24px, var(--layout-max));
    padding-top: 8px;
  }
  .home-status > div {
    display: grid;
    gap: 2px;
  }
  .content-column {
    gap: 26px;
  }
  .sidebar-column {
    grid-template-columns: 1fr;
  }
}
</style>
