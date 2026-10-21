<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { RefreshCw, Search, X } from 'lucide-vue-next'
import { api } from '@/api/client.js'
import { filterArticles } from '@/core/articleFilter.js'
import { collectAllPosts } from '@/core/archiveModel.js'
import PostCard from '@/components/PostCard.vue'
import EmptyState from '@/components/EmptyState.vue'

const posts = ref([])
const route = useRoute()
const router = useRouter()
const query = computed({
  get: () => (typeof route.query.q === 'string' ? route.query.q : ''),
  set: (value) => router.replace({ query: { ...route.query, q: value || undefined } })
})
const loading = ref(true)
const error = ref('')
const filtered = computed(() => filterArticles(posts.value, query.value))

async function load() {
  loading.value = true
  error.value = ''
  try {
    posts.value = await collectAllPosts((page, size) => api.posts(page, size))
  } catch (loadError) {
    error.value = loadError?.message || '文章加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="page">
    <header class="page-title">
      <div>
        <h1>文章</h1>
        <p v-if="!loading">共 {{ filtered.length }} 篇</p>
      </div>
      <label class="filter-input glass">
        <Search :size="18" /><input
          v-model="query"
          aria-label="筛选文章"
          placeholder="搜索标题、正文或标签"
        />
        <button
          v-if="query"
          class="icon-button"
          type="button"
          aria-label="清空文章筛选"
          @click="query = ''"
        >
          <X :size="16" />
        </button>
      </label>
    </header>
    <div v-if="loading" class="articles-loading glass" aria-live="polite">加载中</div>
    <EmptyState v-else-if="error" :label="error">
      <button class="secondary-button" type="button" @click="load">
        <RefreshCw :size="16" />重试
      </button>
    </EmptyState>
    <template v-else-if="filtered.length">
      <p v-if="query.trim()" class="muted" aria-live="polite">找到 {{ filtered.length }} 篇文章</p>
      <div class="latest-list articles-list">
        <PostCard v-for="post in filtered" :key="post.id" :post="post" row />
      </div>
    </template>
    <EmptyState v-else :label="query.trim() ? '没有匹配的文章，换个关键词试试' : '还没有发布文章'">
      <button v-if="query" type="button" class="secondary-button" @click="query = ''">
        清空筛选
      </button>
    </EmptyState>
  </div>
</template>

<style scoped>
.articles-loading {
  display: grid;
  min-height: 280px;
  place-items: center;
  border-radius: var(--radius-xl);
  color: var(--muted);
}
</style>
