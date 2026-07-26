<script setup>
import { computed, onMounted, ref } from 'vue'
import { Search } from 'lucide-vue-next'
import { api } from '@/api/client.js'
import { filterArticles } from '@/core/articleFilter.js'
import PostCard from '@/components/PostCard.vue'
import EmptyState from '@/components/EmptyState.vue'

const posts = ref([])
const query = ref('')
const filtered = computed(() => filterArticles(posts.value, query.value))
onMounted(async () => {
  posts.value = (await api.posts(1, 100))?.records || []
})
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
    <div v-if="filtered.length" class="post-grid articles-grid">
      <PostCard v-for="post in filtered" :key="post.id" :post="post" />
    </div>
    <EmptyState v-else />
  </div>
</template>
