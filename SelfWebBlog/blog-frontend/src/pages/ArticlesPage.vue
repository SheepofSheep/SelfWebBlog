<script setup>
import { computed, onMounted, ref } from 'vue'
import { RefreshCw, Search } from 'lucide-vue-next'
import { api } from '@/api/client.js'
import { filterArticles } from '@/core/articleFilter.js'
import { collectAllPosts } from '@/core/archiveModel.js'
import PostCard from '@/components/PostCard.vue'
import EmptyState from '@/components/EmptyState.vue'

const posts = ref([])
const query = ref('')
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
        <span>ARTICLES</span>
        <h1>文章</h1>
      </div>
      <label class="filter-input glass">
        <Search :size="18" /><input v-model="query" placeholder="搜索正文或标签" />
      </label>
    </header>
    <div v-if="loading" class="articles-loading glass" aria-live="polite">加载中</div>
    <EmptyState v-else-if="error" :label="error">
      <button class="secondary-button" type="button" @click="load">
        <RefreshCw :size="16" />重试
      </button>
    </EmptyState>
    <div v-else-if="filtered.length" class="post-grid articles-grid">
      <PostCard v-for="post in filtered" :key="post.id" :post="post" />
    </div>
    <EmptyState v-else />
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
