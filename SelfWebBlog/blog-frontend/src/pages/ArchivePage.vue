<script setup>
import { computed, onMounted, ref } from 'vue'
import { Search } from 'lucide-vue-next'
import { api } from '@/api/client.js'
import { formatDate, postTitle, postTags } from '@/core/format.js'
import EmptyState from '@/components/EmptyState.vue'
const posts = ref([])
const query = ref('')
const filtered = computed(() => {
  const q = query.value.trim().toLowerCase()
  return q
    ? posts.value.filter((p) => [p.title, p.summary, p.tags].join(' ').toLowerCase().includes(q))
    : posts.value
})
const groups = computed(() =>
  filtered.value.reduce((result, post) => {
    const year = new Date(post.createTime).getFullYear()
    ;(result[year] ||= []).push(post)
    return result
  }, {})
)
onMounted(async () => {
  posts.value = (await api.posts(1, 100))?.records || []
})
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
    <div v-if="filtered.length" class="archive glass">
      <section v-for="(items, year) in groups" :key="year">
        <h2>{{ year }}</h2>
        <RouterLink v-for="post in items" :key="post.id" :to="`/post/${post.id}`"
          ><time>{{ formatDate(post.createTime, true) }}</time>
          <div>
            <strong>{{ postTitle(post) }}</strong
            ><span>{{ postTags(post).join(' · ') }}</span>
          </div></RouterLink
        >
      </section>
    </div>
    <EmptyState v-else />
  </div>
</template>
