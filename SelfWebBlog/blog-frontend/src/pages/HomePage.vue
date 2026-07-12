<script setup>
import { computed, inject, onMounted, ref } from 'vue'
import { PenLine } from 'lucide-vue-next'
import { deletePost } from '../api'
import { useHomeData } from '../composables/useHomeData'
import { useToast } from '../composables/toast'
import { navigate } from '../router'
import ArticleFeed from '../components/articles/ArticleFeed.vue'
import ArticleFilterBar from '../components/articles/ArticleFilterBar.vue'
import AuthorStrip from '../components/articles/AuthorStrip.vue'
import FeaturedArticle from '../components/articles/FeaturedArticle.vue'
import RecentArticleList from '../components/articles/RecentArticleList.vue'
import ConfirmDialog from '../components/ConfirmDialog.vue'

defineOptions({ name: 'HomePage' })

const user = inject('user', null)
const { push } = useToast()
const {
  blogInfo,
  totalPosts,
  loading,
  error,
  featuredPost,
  recentPosts,
  feedPosts,
  categories,
  tags,
  load
} = useHomeData()
const keyword = ref('')
const category = ref('')
const tag = ref('')
const isAdmin = computed(() => user?.value?.role === 'ADMIN' || user?.role === 'ADMIN')
const confirmDialog = ref({ open: false, postId: null })

function openPost(id) {
  navigate(`/post?id=${encodeURIComponent(String(id))}`)
}

function openArchive() {
  navigate('/archive')
}

function submitFilter() {
  const query = new URLSearchParams()
  if (keyword.value.trim()) query.set('keyword', keyword.value.trim())
  if (category.value) query.set('category', category.value)
  if (tag.value) query.set('tag', tag.value)
  navigate(query.size ? `/archive?${query}` : '/archive')
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
</script>

<template>
  <main class="home-page">
    <section class="home-intro">
      <div>
        <p>Gabriel 的个人博客</p>
        <h1>代码、学习与生活，<br />整理成可以继续阅读的记录。</h1>
      </div>
      <div class="intro-note">
        <strong>{{ totalPosts }}</strong>
        <span>篇公开文章<br />持续更新中</span>
      </div>
    </section>

    <div v-if="loading" class="home-skeleton">
      <div class="loading-shimmer"></div>
      <div class="loading-shimmer"></div>
    </div>

    <section v-else-if="featuredPost" class="first-edition">
      <FeaturedArticle :post="featuredPost" @open="openPost" />
      <RecentArticleList :posts="recentPosts" @open="openPost" @archive="openArchive" />
    </section>

    <section v-else class="home-empty">
      <PenLine :size="30" />
      <h2>{{ error || '第一篇文章正在路上' }}</h2>
      <button v-if="isAdmin" type="button" @click="navigate('/write')">开始写作</button>
    </section>

    <section v-if="featuredPost" class="discovery-band">
      <div class="section-heading">
        <p>找到想读的内容</p>
        <h2>从主题开始</h2>
      </div>
      <ArticleFilterBar
        v-model:keyword="keyword"
        v-model:category="category"
        v-model:tag="tag"
        :categories="categories"
        :tags="tags"
        @submit="submitFilter"
        @clear="clearFilter"
      />
    </section>

    <ArticleFeed
      v-if="feedPosts.length"
      :posts="feedPosts"
      :is-admin="isAdmin"
      @open="openPost"
      @archive="openArchive"
      @delete="askDelete"
    />

    <AuthorStrip
      v-if="blogInfo"
      :blog-info="blogInfo"
      :total-posts="totalPosts"
      :is-admin="isAdmin"
      @write="navigate('/write')"
    />

    <ConfirmDialog
      v-model="confirmDialog.open"
      title="删除文章"
      message="确定删除这篇文章吗？删除后访客将无法继续阅读。"
      confirm-text="删除文章"
      @confirm="confirmDelete"
    />
  </main>
</template>

<style scoped>
.home-page {
  width: min(1180px, calc(100% - 40px));
  margin: 0 auto;
  padding: 34px 0 60px;
}
.home-intro {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: end;
  gap: 32px;
  padding: 22px 0 30px;
}
.home-intro p,
.section-heading p {
  margin: 0 0 8px;
  color: var(--primary-hover);
  font-size: 0.76rem;
  font-weight: 900;
}
.home-intro h1 {
  max-width: 840px;
  margin: 0;
  font-family: var(--font-serif);
  font-size: clamp(2.4rem, 4.8vw, 4.5rem);
  font-weight: 500;
  line-height: 1.14;
}
.intro-note {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-bottom: 8px;
}
.intro-note strong {
  font-family: var(--font-serif);
  font-size: 2.5rem;
  color: var(--primary);
}
.intro-note span {
  color: var(--text-muted);
  font-size: 0.72rem;
  line-height: 1.5;
}
.first-edition {
  display: grid;
  grid-template-columns: minmax(0, 1.55fr) minmax(320px, 0.72fr);
  gap: clamp(24px, 4vw, 54px);
}
.home-skeleton {
  display: grid;
  grid-template-columns: 1.55fr 0.72fr;
  gap: 40px;
  min-height: 620px;
}
.home-skeleton > div {
  border-radius: 10px;
}
.discovery-band {
  display: grid;
  grid-template-columns: 210px minmax(0, 1fr);
  gap: 28px;
  align-items: start;
  margin: 76px 0 64px;
  padding: 26px 0;
  border-block: 1px solid var(--border-medium);
}
.section-heading h2 {
  margin: 0;
  font-family: var(--font-serif);
  font-size: 1.65rem;
}
.home-empty {
  display: grid;
  min-height: 360px;
  place-items: center;
  align-content: center;
  gap: 12px;
  border-block: 1px solid var(--border-medium);
  text-align: center;
}
.home-empty h2 {
  margin: 0;
  font-family: var(--font-serif);
}
.home-empty button {
  min-height: 42px;
  padding: 0 16px;
  border: 0;
  border-radius: 7px;
  background: var(--primary);
  color: var(--on-primary);
  font: inherit;
  font-weight: 800;
  cursor: pointer;
}
.home-page > :deep(.article-feed) {
  margin-bottom: 70px;
}
@media (max-width: 900px) {
  .home-intro {
    grid-template-columns: 1fr;
  }
  .intro-note {
    display: none;
  }
  .first-edition,
  .home-skeleton {
    grid-template-columns: 1fr;
    min-height: auto;
  }
  .discovery-band {
    grid-template-columns: 1fr;
  }
}
@media (max-width: 620px) {
  .home-page {
    width: min(100% - 28px, 1180px);
    padding-top: 10px;
  }
  .home-intro {
    padding: 20px 0 24px;
  }
  .home-intro h1 {
    font-size: 2.35rem;
  }
  .first-edition {
    gap: 34px;
  }
  .discovery-band {
    margin: 48px 0;
  }
}
</style>
