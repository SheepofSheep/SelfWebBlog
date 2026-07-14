<script setup>
import { ArrowLeft, ArrowUpRight, Tag } from 'lucide-vue-next'
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { listPostsByTag, listTags } from '../api'
import EmptyState from '../components/ui/EmptyState.vue'
import { formatTime } from '../utils/format'

const route = useRoute()
const router = useRouter()
const posts = ref([])
const tag = ref(null)
const loading = ref(true)

async function load() {
  loading.value = true
  try {
    const [postResult, tags] = await Promise.all([
      listPostsByTag(route.params.slug),
      listTags()
    ])
    posts.value = postResult
    tag.value = tags.find((item) => item.slug === route.params.slug) || null
  } finally {
    loading.value = false
  }
}

onMounted(load)
watch(() => route.params.slug, load)
</script>

<template>
  <div class="tag-page page-width">
    <button class="back-action" type="button" @click="router.push('/tags')">
      <ArrowLeft :size="16" />全部标签
    </button>
    <header>
      <span><Tag :size="16" />主题</span>
      <h1>{{ tag?.name || route.params.slug }}</h1>
      <p>{{ posts.length }} 篇公开文章</p>
    </header>
    <div v-if="loading" class="loading-shimmer post-loading"></div>
    <div v-else-if="posts.length" class="tag-posts">
      <button v-for="post in posts" :key="post.id" type="button" @click="router.push(`/post/${post.id}`)">
        <time>{{ formatTime(post.createTime).slice(0, 11) }}</time>
        <span>
          <strong>{{ post.title }}</strong>
          <small>{{ post.summary || post.category || '文章' }}</small>
        </span>
        <ArrowUpRight :size="18" />
      </button>
    </div>
    <EmptyState v-else title="这个标签下暂无文章" description="可以返回全部标签继续浏览。" />
  </div>
</template>

<style scoped>
.tag-page {
  max-width: 920px;
  padding-block: 38px 80px;
}
.back-action {
  min-height: 36px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--text-secondary);
  font: inherit;
  font-size: 0.75rem;
  cursor: pointer;
}
header {
  margin: 32px 0 36px;
}
header > span {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--accent-strong);
  font-size: 0.72rem;
  font-weight: 800;
}
h1 {
  margin: 12px 0 8px;
  font-family: var(--font-reading);
  font-size: clamp(2.5rem, 8vw, 5rem);
  font-weight: 600;
}
header p {
  margin: 0;
  color: var(--text-tertiary);
  font-size: 0.76rem;
}
.tag-posts {
  border-top: 1px solid var(--border-strong);
}
.tag-posts button {
  width: 100%;
  min-height: 86px;
  display: grid;
  grid-template-columns: 112px minmax(0, 1fr) auto;
  align-items: center;
  gap: 16px;
  padding: 12px 4px;
  border: 0;
  border-bottom: 1px solid var(--border-subtle);
  background: transparent;
  color: var(--text-primary);
  font: inherit;
  text-align: left;
  cursor: pointer;
}
.tag-posts button:hover {
  background: var(--surface-glass);
}
time,
small {
  color: var(--text-tertiary);
  font-size: 0.7rem;
}
.tag-posts span {
  min-width: 0;
  display: grid;
  gap: 5px;
}
strong {
  overflow: hidden;
  font-family: var(--font-reading);
  font-size: 1.05rem;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.post-loading {
  height: 300px;
}
@media (max-width: 560px) {
  .tag-posts button {
    grid-template-columns: minmax(0, 1fr) auto;
  }
  time {
    grid-column: 1 / -1;
  }
}
</style>
