<script setup>
import { computed, onMounted, ref } from 'vue'
import { RefreshCw, Search } from 'lucide-vue-next'
import { api } from '@/api/client.js'
import {
  archivePostDate,
  collectAllPosts,
  filterArchivePosts,
  groupArchive
} from '@/core/archiveModel.js'
import { formatDate, postTitle, postTags } from '@/core/format.js'
import EmptyState from '@/components/EmptyState.vue'

const posts = ref([])
const query = ref('')
const loading = ref(true)
const error = ref('')
const filtered = computed(() => filterArchivePosts(posts.value, query.value))
const groups = computed(() => groupArchive(filtered.value))

async function load() {
  loading.value = true
  error.value = ''
  try {
    posts.value = await collectAllPosts((page, size) => api.posts(page, size))
  } catch (loadError) {
    error.value = loadError?.message || '归档加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
<template>
  <div class="page narrow-page">
    <header class="page-title">
      <div>
        <span>ARCHIVE</span>
        <h1>文章</h1>
      </div>
      <label class="filter-input glass"
        ><Search :size="18" /><input v-model="query" placeholder="筛选文章"
      /></label>
    </header>
    <div v-if="loading" class="archive-loading glass" aria-live="polite">加载中</div>
    <EmptyState v-else-if="error" :label="error">
      <button class="secondary-button" type="button" @click="load">
        <RefreshCw :size="16" />重试
      </button>
    </EmptyState>
    <div v-else-if="filtered.length" class="archive glass">
      <section v-for="group in groups" :key="group.year">
        <h2>{{ group.year }}</h2>
        <div class="archive-year-posts">
          <RouterLink v-for="post in group.posts" :key="post.id" :to="`/post/${post.id}`"
            ><time>{{ formatDate(archivePostDate(post), true) }}</time>
            <div>
              <strong>{{ postTitle(post) }}</strong
              ><span>{{ postTags(post).join(' · ') }}</span>
            </div></RouterLink
          >
        </div>
      </section>
    </div>
    <EmptyState v-else label="暂无文章" />
  </div>
</template>

<style scoped>
.archive-loading {
  display: grid;
  min-height: 260px;
  place-items: center;
  border-radius: var(--radius-xl);
  color: var(--muted);
}
.archive-year-posts {
  min-width: 0;
}
.archive-year-posts a {
  width: 100%;
}
@media (max-width: 680px) {
  .archive :deep(section) {
    grid-template-columns: 1fr;
    gap: 10px;
  }
}
</style>
