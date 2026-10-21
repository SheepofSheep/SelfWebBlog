<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { RefreshCw, Search, X } from 'lucide-vue-next'
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
const route = useRoute()
const router = useRouter()
const query = computed({
  get: () => (typeof route.query.q === 'string' ? route.query.q : ''),
  set: (value) => router.replace({ query: { ...route.query, q: value || undefined } })
})
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
        <h1>归档</h1>
        <p v-if="!loading">共 {{ filtered.length }} 篇</p>
      </div>
      <label class="filter-input glass"
        ><Search :size="18" /><input
          v-model="query"
          aria-label="筛选归档文章"
          placeholder="筛选文章"
        />
        <button
          v-if="query"
          type="button"
          class="icon-button"
          aria-label="清空归档筛选"
          @click="query = ''"
        >
          <X :size="16" />
        </button>
      </label>
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
    <EmptyState v-else :label="query.trim() ? '没有匹配的归档文章' : '暂无文章'">
      <button v-if="query" type="button" class="secondary-button" @click="query = ''">
        清空筛选
      </button>
    </EmptyState>
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
