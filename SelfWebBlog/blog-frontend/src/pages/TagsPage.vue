<script setup>
import { onBeforeUnmount, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { RefreshCw } from 'lucide-vue-next'
import { api } from '@/api/client.js'
import PostCard from '@/components/PostCard.vue'
import EmptyState from '@/components/EmptyState.vue'
const route = useRoute()
const tags = ref([])
const posts = ref([])
const active = ref(null)
const loading = ref(true)
const error = ref('')
let requestId = 0

async function load() {
  const currentRequest = ++requestId
  const slug = String(route.params.slug || '')
  const name = String(route.query.tag || '').trim()
  loading.value = true
  error.value = ''
  posts.value = []
  try {
    const nextTags = (await api.tags()) || []
    if (currentRequest !== requestId) return
    const nextActive = nextTags.find(
      (tag) =>
        tag.slug === slug || (name && tag.name.toLocaleLowerCase() === name.toLocaleLowerCase())
    )
    tags.value = nextTags
    active.value = nextActive || null
    const nextPosts = nextActive ? await api.tagPosts(nextActive.slug) : []
    if (currentRequest !== requestId) return
    posts.value = Array.isArray(nextPosts) ? nextPosts : []
  } catch (loadError) {
    if (currentRequest !== requestId) return
    error.value = loadError?.message || '标签加载失败'
  } finally {
    if (currentRequest === requestId) loading.value = false
  }
}
watch(() => [route.params.slug, route.query.tag], load, { immediate: true })
onBeforeUnmount(() => {
  requestId += 1
})
</script>
<template>
  <div class="page">
    <header class="page-title">
      <div>
        <span>TAGS</span>
        <h1>标签</h1>
      </div>
    </header>
    <div class="tag-directory glass">
      <RouterLink
        v-for="tag in tags"
        :key="tag.id"
        :class="{ active: tag.slug === active?.slug }"
        :to="`/tags/${tag.slug}`"
        ><span>{{ tag.name }}</span
        ><small>{{ tag.postCount }}</small></RouterLink
      >
    </div>
    <div v-if="loading" class="tags-loading glass" aria-live="polite">加载中</div>
    <EmptyState v-else-if="error" :label="error">
      <button class="secondary-button" type="button" @click="load">
        <RefreshCw :size="16" />重试
      </button>
    </EmptyState>
    <section v-else-if="active" class="section-block">
      <div class="section-heading">
        <h2>{{ active?.name || '标签' }}</h2>
      </div>
      <div v-if="posts.length" class="post-grid">
        <PostCard v-for="post in posts" :key="post.id" :post="post" />
      </div>
      <EmptyState v-else />
    </section>
  </div>
</template>

<style scoped>
.tags-loading {
  display: grid;
  min-height: 240px;
  margin-top: 20px;
  place-items: center;
  border-radius: var(--radius-xl);
  color: var(--muted);
}
</style>
