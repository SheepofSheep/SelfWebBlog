<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, ArrowRight, SearchX } from 'lucide-vue-next'
import { listCategories, listTags, searchPosts } from '../api'
import ArticleFilterBar from '../components/articles/ArticleFilterBar.vue'
import ArchiveTimeline from '../components/articles/ArchiveTimeline.vue'

const route = useRoute()
const router = useRouter()
const posts = ref([])
const categories = ref([])
const tags = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = 20
const loading = ref(false)
const keyword = ref('')
const category = ref('')
const tag = ref('')
const sort = ref('date')
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize)))
let debounceTimer

function initializeFromQuery() {
  keyword.value = String(route.query.keyword || '')
  category.value = String(route.query.category || '')
  tag.value = String(route.query.tag || '')
  sort.value = String(route.query.sort || 'date')
  currentPage.value = Math.max(1, Number(route.query.page || 1))
}

function archiveUrl() {
  const params = new URLSearchParams()
  if (keyword.value.trim()) params.set('keyword', keyword.value.trim())
  if (category.value) params.set('category', category.value)
  if (tag.value) params.set('tag', tag.value)
  if (sort.value !== 'date') params.set('sort', sort.value)
  if (currentPage.value > 1) params.set('page', String(currentPage.value))
  return params.size ? `/archive?${params}` : '/archive'
}

async function loadPosts({ resetPage = false } = {}) {
  if (resetPage) currentPage.value = 1
  loading.value = true
  try {
    const data = await searchPosts({
      keyword: keyword.value || undefined,
      category: category.value || undefined,
      tag: tag.value || undefined,
      sort: sort.value,
      page: currentPage.value,
      size: pageSize
    })
    posts.value = data.records || []
    total.value = data.total || 0
    const nextUrl = archiveUrl()
    if (route.fullPath !== nextUrl) await router.replace(nextUrl)
  } finally {
    loading.value = false
  }
}

function scheduleSearch() {
  clearTimeout(debounceTimer)
  debounceTimer = setTimeout(() => loadPosts({ resetPage: true }), 220)
}

function clearFilters() {
  keyword.value = ''
  category.value = ''
  tag.value = ''
  sort.value = 'date'
  loadPosts({ resetPage: true })
}

function changePage(nextPage) {
  if (nextPage < 1 || nextPage > totalPages.value) return
  currentPage.value = nextPage
  loadPosts()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

watch([category, tag, sort], scheduleSearch)
onBeforeUnmount(() => clearTimeout(debounceTimer))
onMounted(async () => {
  initializeFromQuery()
  await Promise.all([
    loadPosts(),
    listCategories()
      .then((data) => (categories.value = data || []))
      .catch(() => {}),
    listTags()
      .then((data) => (tags.value = data || []))
      .catch(() => {})
  ])
})
</script>

<template>
  <main class="archive-page">
    <header class="archive-header">
      <p>所有公开记录</p>
      <div>
        <h1>文章归档</h1>
        <span>{{ total }} 篇文章，按时间整理。</span>
      </div>
    </header>

    <section class="archive-filter">
      <ArticleFilterBar
        v-model:keyword="keyword"
        v-model:category="category"
        v-model:tag="tag"
        v-model:sort="sort"
        :categories="categories"
        :tags="tags"
        show-sort
        @submit="loadPosts({ resetPage: true })"
        @clear="clearFilters"
      />
    </section>

    <div v-if="loading" class="archive-loading">
      <div v-for="index in 5" :key="index" class="loading-shimmer"></div>
    </div>
    <ArchiveTimeline
      v-else-if="posts.length"
      :posts="posts"
      @open="router.push(`/post/${encodeURIComponent(String($event))}`)"
    />
    <section v-else class="archive-empty">
      <SearchX :size="30" />
      <h2>没有找到相关内容</h2>
      <p>换一个关键词，或者清除当前筛选。</p>
      <button type="button" @click="clearFilters">清除筛选</button>
    </section>

    <nav v-if="totalPages > 1" class="archive-pagination" aria-label="归档分页">
      <button type="button" :disabled="currentPage <= 1" @click="changePage(currentPage - 1)">
        <ArrowLeft :size="16" /> 上一页
      </button>
      <span>{{ currentPage }} / {{ totalPages }}</span>
      <button
        type="button"
        :disabled="currentPage >= totalPages"
        @click="changePage(currentPage + 1)"
      >
        下一页 <ArrowRight :size="16" />
      </button>
    </nav>
  </main>
</template>

<style scoped>
.archive-page {
  width: min(1120px, calc(100% - 40px));
  margin: 0 auto;
  padding: 42px 0 70px;
}
.archive-header {
  display: grid;
  grid-template-columns: 170px minmax(0, 1fr);
  gap: 24px;
  align-items: end;
  padding: 24px 0 34px;
}
.archive-header > p {
  margin: 0 0 9px;
  color: var(--primary-hover);
  font-size: 0.76rem;
  font-weight: 900;
}
.archive-header > div {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 20px;
}
.archive-header h1 {
  margin: 0;
  font-family: var(--font-serif);
  font-size: clamp(2.6rem, 6vw, 5.2rem);
  font-weight: 500;
  line-height: 1;
}
.archive-header span {
  color: var(--text-muted);
  font-size: 0.82rem;
}
.archive-filter {
  margin-bottom: 54px;
  padding: 22px 0;
  border-block: 1px solid var(--border-medium);
}
.archive-loading {
  display: grid;
  gap: 10px;
}
.archive-loading > div {
  height: 110px;
  border-radius: 8px;
}
.archive-empty {
  display: grid;
  min-height: 360px;
  place-items: center;
  align-content: center;
  gap: 8px;
  border-block: 1px solid var(--border-medium);
  text-align: center;
}
.archive-empty h2,
.archive-empty p {
  margin: 0;
}
.archive-empty p {
  color: var(--text-muted);
}
.archive-empty button {
  margin-top: 10px;
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
.archive-pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 18px;
  margin-top: 50px;
}
.archive-pagination button {
  display: inline-flex;
  min-height: 42px;
  align-items: center;
  gap: 7px;
  padding: 0 14px;
  border: 1px solid var(--border-medium);
  border-radius: 7px;
  background: transparent;
  color: var(--text-main);
  cursor: pointer;
}
.archive-pagination button:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}
.archive-pagination span {
  color: var(--text-muted);
  font-family: var(--font-mono);
  font-size: 0.78rem;
}
@media (max-width: 720px) {
  .archive-page {
    width: min(100% - 28px, 1120px);
    padding-top: 16px;
  }
  .archive-header {
    grid-template-columns: 1fr;
    gap: 6px;
  }
  .archive-header > div {
    display: grid;
    gap: 8px;
  }
  .archive-header h1 {
    font-size: 3rem;
  }
  .archive-filter {
    margin-bottom: 38px;
  }
}
</style>
