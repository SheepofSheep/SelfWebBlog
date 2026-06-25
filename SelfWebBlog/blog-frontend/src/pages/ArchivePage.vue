<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { searchPosts, listCategories, listTags } from '../utils/api'
import { navigate, useRoute } from '../router'
import { formatTime } from '../utils/format'
import { Search, Clock, Eye, Folder, Tag, ArrowUpDown, X } from 'lucide-vue-next'

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
const { query } = useRoute()

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize)))
const quickTags = computed(() => tags.value.slice(0, 12))
const activeFilters = computed(() =>
  [
    keyword.value ? { key: 'keyword', label: `搜索：${keyword.value}` } : null,
    category.value ? { key: 'category', label: `分类：${category.value}` } : null,
    tag.value ? { key: 'tag', label: `标签：${tag.value}` } : null
  ].filter(Boolean)
)

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

function buildArchiveQuery() {
  const params = new URLSearchParams()
  if (keyword.value.trim()) params.set('keyword', keyword.value.trim())
  if (category.value) params.set('category', category.value)
  if (tag.value.trim()) params.set('tag', tag.value.trim())
  if (sort.value !== 'date') params.set('sort', sort.value)
  if (currentPage.value > 1) params.set('page', String(currentPage.value))
  return params.toString()
}

function syncArchiveUrl() {
  const next = buildArchiveQuery()
  const target = next ? `/archive?${next}` : '/archive'
  const current = window.location.hash.replace(/^#/, '') || '/'
  if (current !== target) navigate(target)
}

function initializeFromQuery() {
  keyword.value = query.value.get('keyword') || ''
  category.value = query.value.get('category') || ''
  tag.value = query.value.get('tag') || ''
  sort.value = query.value.get('sort') || 'date'
  currentPage.value = Number(query.value.get('page') || 1)
}

async function loadPosts() {
  loading.value = true
  syncArchiveUrl()
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

async function loadTags() {
  try {
    tags.value = await listTags()
  } catch {
    tags.value = []
  }
}

function clearFilters() {
  keyword.value = ''
  category.value = ''
  tag.value = ''
  sort.value = 'date'
  loadPosts()
}

function removeFilter(key) {
  if (key === 'keyword') keyword.value = ''
  if (key === 'category') category.value = ''
  if (key === 'tag') tag.value = ''
  doSearch()
}

function clearKeyword() {
  keyword.value = ''
  doSearch()
}

function clearTag() {
  tag.value = ''
  doSearch()
}

function chooseTag(name) {
  tag.value = name
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
  initializeFromQuery()
  loadPosts()
  loadCategories()
  loadTags()
})
</script>

<template>
  <main class="archive-page">
    <header class="archive-hero">
      <span class="archive-kicker">GABRIEL.ARCHIVE / SEARCH</span>
      <h1 class="archive-title">档案检索台</h1>
      <p class="archive-sub">共 {{ total }} 篇文章 · 搜索、分类和标签会保留在地址里</p>
    </header>

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

      <div v-if="quickTags.length" class="quick-tags" aria-label="常用标签">
        <span class="quick-label">常用标签</span>
        <button
          v-for="t in quickTags"
          :key="t.id || t.name"
          :class="['quick-tag', { active: tag === t.name }]"
          @click="chooseTag(t.name)"
        >
          {{ t.name }}
        </button>
      </div>

      <div v-if="activeFilters.length" class="filter-chips">
        <button
          v-for="item in activeFilters"
          :key="item.key"
          class="filter-chip"
          @click="removeFilter(item.key)"
        >
          {{ item.label }}
          <X :size="12" />
        </button>
      </div>
    </div>

    <!-- 文章卡片网格 -->
    <div v-if="loading" class="archive-grid">
      <div v-for="i in 6" :key="i" class="archive-card archive-skeleton loading-shimmer"></div>
    </div>
    <div v-else-if="posts.length === 0" class="empty-state">
      <p class="empty-title">没有找到相关内容</p>
      <p class="empty-desc">换个关键词看看，或者先清除筛选。</p>
      <button class="pill-btn pill-btn-primary" @click="clearFilters">
        <X :size="14" />
        清除筛选
      </button>
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
        <div v-else class="post-cover-photo fallback-cover">
          <div class="cover-frame">
            <span>{{ p.title?.charAt(0) || '文' }}</span>
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
  max-width: var(--magazine-max, 1180px);
  margin: 0 auto;
  width: 100%;
  padding: 14px var(--space-md) var(--space-xl);
}

.archive-hero {
  position: relative;
  margin-bottom: 18px;
  padding: 22px 4px 6px;
}

.archive-kicker {
  display: inline-flex;
  margin-bottom: 8px;
  color: var(--primary-hover);
  font-size: 0.72rem;
  font-weight: 900;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.archive-title {
  font-family: var(--font-serif);
  font-size: clamp(2rem, 5vw, 4.2rem);
  font-weight: 700;
  color: var(--text);
  margin: 0 0 10px;
  letter-spacing: 0;
  line-height: 1.05;
}
.archive-sub {
  margin: 0;
  color: var(--text-muted);
  font-size: 0.9rem;
}

/* ═══ 工具栏 ═══ */
.archive-toolbar {
  padding: 16px;
  margin-bottom: 24px;
  border-radius: var(--radius-xl);
}

.archive-toolbar:hover {
  transform: none;
}

.search-box {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 8px;
  min-height: 50px;
  padding: 8px 14px;
  border-radius: var(--radius-md);
  background: rgba(255, 253, 247, 0.72);
  border: 1px solid var(--border-medium);
  margin-bottom: 12px;
  color: var(--text-muted);
}
[data-theme='dark'] .search-box {
  background: rgba(17, 16, 13, 0.38);
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
  min-height: 36px;
  padding: 6px 12px;
  border: 1px solid var(--border-medium);
  border-radius: var(--radius-sm);
  background: rgba(255, 253, 247, 0.72);
  color: var(--text);
  font-size: 0.78rem;
  font-family: var(--font-body);
  cursor: pointer;
  outline: none;
}

[data-theme='dark'] .filter-select,
[data-theme='dark'] .tag-input-wrap {
  background: rgba(17, 16, 13, 0.42);
  border-color: var(--border-medium);
}

.tag-input-wrap {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  min-height: 36px;
  border: 1px solid var(--border-medium);
  border-radius: var(--radius-sm);
  background: rgba(255, 253, 247, 0.72);
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
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-width: 96px;
  padding: 8px 16px;
  border: 1px solid var(--border-warm);
  border-radius: var(--radius-sm);
  background: rgba(255, 247, 227, 0.9);
  color: var(--text-secondary);
  font-size: 0.82rem;
  font-family: var(--font-body);
  font-weight: 600;
  line-height: 1.2;
  cursor: pointer;
  transition:
    border-color var(--duration-fast) var(--ease-soft),
    color var(--duration-fast) var(--ease-soft),
    background var(--duration-fast) var(--ease-soft),
    transform var(--duration-fast) var(--ease-soft);
}
[data-theme='dark'] .sort-btn {
  background: rgba(39, 31, 21, 0.7);
  border-color: var(--border-medium);
  color: var(--text-secondary);
}
.sort-btn:hover,
.sort-btn:focus-visible {
  border-color: var(--primary);
  color: var(--primary-hover);
  background: rgba(255, 245, 215, 0.96);
  transform: translateY(-1px);
}
[data-theme='dark'] .sort-btn:hover,
[data-theme='dark'] .sort-btn:focus-visible {
  background: rgba(227, 170, 52, 0.14);
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
  border-radius: var(--radius-sm);
  background: rgba(255, 250, 238, 0.58);
  color: var(--text-muted);
  font-size: 0.76rem;
  font-family: var(--font-body);
  cursor: pointer;
  transition:
    color var(--duration-fast),
    border-color var(--duration-fast);
}

.quick-tags,
.filter-chips {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.quick-label {
  color: var(--text-muted);
  font-size: 0.72rem;
  font-weight: 700;
}

.quick-tag,
.filter-chip {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  min-height: 28px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: rgba(255, 250, 238, 0.62);
  color: var(--text-secondary);
  cursor: pointer;
  font-family: var(--font-body);
  font-size: 0.72rem;
  padding: 5px 11px;
  transition:
    color var(--duration-fast),
    border-color var(--duration-fast),
    background var(--duration-fast),
    transform var(--duration-fast);
}

.quick-tag:hover,
.quick-tag.active,
.filter-chip:hover {
  color: var(--primary-hover);
  border-color: var(--border-warm);
  background: var(--primary-soft);
  transform: translateY(-1px);
}
.clear-filter-btn:hover {
  color: var(--danger);
  border-color: var(--danger);
}

/* ═══ 卡片网格 ═══ */
.archive-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 18px;
}

.archive-card {
  cursor: pointer;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 0;
  min-height: 280px;
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
.fallback-cover .cover-frame {
  display: grid;
  place-items: center;
  background:
    repeating-linear-gradient(135deg, rgba(246, 196, 91, 0.14) 0 1px, transparent 1px 18px),
    radial-gradient(circle at 25% 12%, rgba(246, 196, 91, 0.3), transparent 34%),
    linear-gradient(135deg, #2b2114, #15110c);
}
.fallback-cover span {
  color: rgba(246, 196, 91, 0.72);
  font-family: var(--font-serif);
  font-size: 3.4rem;
  font-weight: 900;
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
  font-family: var(--font-serif);
  font-size: 1.05rem;
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
  padding: 48px 20px;
  color: var(--text-muted);
}

.archive-skeleton {
  min-height: 280px;
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
}

.empty-state {
  border: 1px dashed var(--border-warm);
  border-radius: var(--radius-lg);
  background: rgba(255, 250, 238, 0.56);
}
[data-theme='dark'] .empty-state {
  background: rgba(17, 16, 13, 0.36);
}

.empty-title {
  margin: 0 0 6px;
  color: var(--text-main);
  font-weight: 700;
}

.empty-desc {
  margin: 0 0 18px;
}

@media (max-width: 640px) {
  .archive-page {
    padding-inline: var(--space-sm);
  }
  .archive-grid {
    grid-template-columns: 1fr;
    gap: 14px;
  }
  .search-box {
    grid-template-columns: auto minmax(0, 1fr);
  }
  .filter-row {
    gap: 6px;
  }
  .sort-btn {
    flex: 1 1 120px;
    justify-content: center;
  }
  .tag-input {
    width: 70px;
  }
}
</style>
