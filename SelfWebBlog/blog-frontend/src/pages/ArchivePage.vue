<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { searchPosts, listCategories } from '../utils/api'
import { navigate } from '../router'
import { formatTime } from '../utils/format'
import { Search, Clock, Eye, Folder, Tag, ArrowUpDown, X } from 'lucide-vue-next'

const posts = ref([])
const categories = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = 20
const loading = ref(false)

const keyword = ref('')
const category = ref('')
const tag = ref('')
const sort = ref('date')

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize)))

function postTags(p) {
  if (!p?.tags) return []
  return p.tags.split(/[,，\s]+/).filter(Boolean)
}

function postSummary(p) {
  return p?.summary || ''
}

function readMinutes(p) {
  if (!p?.content) return 1
  const text = p.content.replace(/[#*_`\[\]!\(\)>\-\s]/g, '')
  return Math.max(1, Math.ceil(text.length / 400))
}

let debounceTimer = null
function doSearch(resetPage = true) {
  if (resetPage) currentPage.value = 1
  clearTimeout(debounceTimer)
  debounceTimer = setTimeout(() => loadPosts(), 200)
}

async function loadPosts() {
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
  } catch {
    posts.value = []
  } finally {
    loading.value = false
  }
}

async function loadCategories() {
  try {
    categories.value = await listCategories()
  } catch {
    categories.value = []
  }
}

function clearFilters() {
  keyword.value = ''
  category.value = ''
  tag.value = ''
  sort.value = 'date'
  loadPosts()
}

function clearKeyword() {
  keyword.value = ''
  doSearch()
}

function clearTag() {
  tag.value = ''
  doSearch()
}

function prevPage() {
  if (currentPage.value > 1) {
    currentPage.value--
    loadPosts()
  }
}
function nextPage() {
  if (currentPage.value < totalPages.value) {
    currentPage.value++
    loadPosts()
  }
}

watch([category, sort], () => doSearch())

onMounted(() => {
  loadPosts()
  loadCategories()
})
</script>

<template>
  <main class="archive-page">
    <h1 class="archive-title">全部文章</h1>
    <p class="archive-sub">共 {{ total }} 篇文章</p>

    <!-- 搜索与筛选栏 -->
    <div class="archive-toolbar glass-card">
      <div class="search-box">
        <Search :size="16" />
        <input
          v-model="keyword"
          type="text"
          placeholder="搜索文章标题或摘要..."
          @input="doSearch()"
          class="search-input"
        />
        <button v-if="keyword" class="clear-btn" @click="clearKeyword" aria-label="清除搜索">
          <X :size="14" />
        </button>
      </div>

      <div class="filter-row">
        <select v-model="category" class="filter-select">
          <option value="">全部分类</option>
          <option v-for="c in categories" :key="c" :value="c">{{ c }}</option>
        </select>

        <div class="tag-input-wrap">
          <Tag :size="14" />
          <input
            v-model="tag"
            type="text"
            placeholder="标签筛选..."
            @input="doSearch()"
            class="tag-input"
          />
          <button v-if="tag" class="clear-btn" @click="clearTag" aria-label="清除标签">
            <X :size="14" />
          </button>
        </div>

        <button :class="['sort-btn', { active: sort === 'date' }]" @click="sort = 'date'">
          <Clock :size="14" /> 最新
        </button>
        <button :class="['sort-btn', { active: sort === 'title' }]" @click="sort = 'title'">
          <ArrowUpDown :size="14" /> 标题
        </button>

        <button v-if="keyword || category || tag" class="clear-filter-btn" @click="clearFilters">
          <X :size="14" /> 清除筛选
        </button>
      </div>
    </div>

    <!-- 文章卡片网格 -->
    <div v-if="loading" class="loading-state">加载中...</div>
    <div v-else-if="posts.length === 0" class="empty-state">
      <p>没有找到匹配的文章</p>
    </div>
    <div v-else class="archive-grid">
      <article
        v-for="p in posts"
        :key="p.id"
        class="glass-card archive-card"
        @click="navigate(`/post?id=${p.id}`)"
      >
        <div v-if="p.coverUrl" class="post-cover-photo">
          <div class="cover-frame">
            <img :src="p.coverUrl" :alt="p.title" />
          </div>
        </div>

        <div class="card-body">
          <div class="card-meta">
            <span class="meta-item"><Clock :size="12" /> {{ formatTime(p.createTime) }}</span>
            <span v-if="p.category" class="meta-item"><Folder :size="12" /> {{ p.category }}</span>
            <span class="meta-item"><Eye :size="12" /> {{ readMinutes(p) }} min</span>
          </div>

          <h2 class="card-title">{{ p.title }}</h2>

          <p v-if="postSummary(p)" class="card-summary">{{ postSummary(p) }}</p>

          <div v-if="postTags(p).length" class="card-tags">
            <span v-for="t in postTags(p)" :key="t" class="tag">{{ t }}</span>
          </div>
        </div>
      </article>
    </div>

    <!-- 分页 -->
    <div v-if="totalPages > 1" class="pagination">
      <button class="pill-btn pill-btn-ghost" :disabled="currentPage === 1" @click="prevPage">
        上一页
      </button>
      <span class="page-info">{{ currentPage }} / {{ totalPages }}</span>
      <button
        class="pill-btn pill-btn-ghost"
        :disabled="currentPage >= totalPages"
        @click="nextPage"
      >
        下一页
      </button>
    </div>
  </main>
</template>

<style scoped>
.archive-page {
  max-width: 1060px;
  margin: 0 auto;
  width: 100%;
  padding: 20px 0;
}

.archive-title {
  font-family: var(--font-sans);
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--text);
  margin: 0 0 4px;
}
.archive-sub {
  margin: 0 0 24px;
  color: var(--text-muted);
  font-size: 0.85rem;
}

/* ═══ 工具栏 ═══ */
.archive-toolbar {
  padding: 16px 20px;
  margin-bottom: 24px;
}

.search-box {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 14px;
  border-radius: var(--radius-pill);
  background: var(--surface-muted);
  border: 1px solid var(--border);
  margin-bottom: 12px;
  color: var(--text-muted);
}
.search-input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  font-size: 0.88rem;
  font-family: var(--font-body);
  color: var(--text);
}
.search-input::placeholder {
  color: var(--text-faint);
}
.clear-btn {
  display: flex;
  border: none;
  background: none;
  color: var(--text-muted);
  cursor: pointer;
  padding: 2px;
  border-radius: 50%;
}
.clear-btn:hover {
  color: var(--text);
  background: var(--surface-muted);
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.filter-select {
  padding: 6px 12px;
  border: 1px solid var(--border);
  border-radius: var(--radius-pill);
  background: var(--surface-muted);
  color: var(--text);
  font-size: 0.78rem;
  font-family: var(--font-body);
  cursor: pointer;
  outline: none;
}

.tag-input-wrap {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border: 1px solid var(--border);
  border-radius: var(--radius-pill);
  background: var(--surface-muted);
  color: var(--text-muted);
}
.tag-input {
  border: none;
  outline: none;
  background: transparent;
  font-size: 0.78rem;
  font-family: var(--font-body);
  color: var(--text);
  width: 100px;
}
.tag-input::placeholder {
  color: var(--text-faint);
}

.sort-btn {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 6px 14px;
  border: 1px solid var(--border);
  border-radius: var(--radius-pill);
  background: transparent;
  color: var(--text-muted);
  font-size: 0.78rem;
  font-family: var(--font-body);
  cursor: pointer;
  transition:
    border-color var(--duration-fast),
    color var(--duration-fast),
    background var(--duration-fast);
}
.sort-btn:hover {
  border-color: var(--primary);
  color: var(--primary-hover);
}
.sort-btn.active {
  background: var(--primary-soft);
  border-color: var(--primary);
  color: var(--primary-hover);
}

.clear-filter-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  border: 1px solid var(--border);
  border-radius: var(--radius-pill);
  background: transparent;
  color: var(--text-muted);
  font-size: 0.76rem;
  font-family: var(--font-body);
  cursor: pointer;
  transition:
    color var(--duration-fast),
    border-color var(--duration-fast);
}
.clear-filter-btn:hover {
  color: var(--danger);
  border-color: var(--danger);
}

/* ═══ 卡片网格 ═══ */
.archive-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 18px;
}

.archive-card {
  cursor: pointer;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 0;
}

.archive-card .post-cover-photo {
  border-radius: var(--radius-lg) var(--radius-lg) 0 0;
  overflow: hidden;
  margin: 0;
}
.archive-card .cover-frame {
  height: 170px;
  overflow: hidden;
}
.archive-card .cover-frame img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform var(--duration-slow) var(--ease-soft);
}
.archive-card:hover .cover-frame img {
  transform: scale(1.04);
}

.card-body {
  padding: 16px;
  flex: 1;
  display: flex;
  flex-direction: column;
}

.card-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  color: var(--text-muted);
  font-size: 0.66rem;
  margin-bottom: 6px;
}
.meta-item {
  display: flex;
  align-items: center;
  gap: 3px;
}

.card-title {
  margin: 0 0 6px;
  font-family: var(--font-sans);
  font-size: 0.95rem;
  font-weight: 700;
  color: var(--text);
  line-height: 1.35;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-summary {
  margin: 0 0 8px;
  color: var(--text-muted);
  font-size: 0.76rem;
  line-height: 1.55;
  flex: 1;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
  padding-top: 8px;
  border-top: 1px solid var(--border-light);
}
.card-tags .tag {
  padding: 2px 8px;
  font-size: 0.62rem;
}

/* ═══ 分页 ═══ */
.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-top: 32px;
  padding-top: 24px;
  border-top: 1px solid var(--border);
}
.page-info {
  font-size: 0.85rem;
  color: var(--text-muted);
}

.loading-state,
.empty-state {
  text-align: center;
  padding: 48px 0;
  color: var(--text-muted);
}

@media (max-width: 640px) {
  .archive-grid {
    grid-template-columns: 1fr;
    gap: 14px;
  }
  .filter-row {
    gap: 6px;
  }
  .tag-input {
    width: 70px;
  }
}
</style>
