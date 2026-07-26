<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { api } from '@/api/client.js'
import PostCard from '@/components/PostCard.vue'
import EmptyState from '@/components/EmptyState.vue'
const route = useRoute()
const tags = ref([])
const posts = ref([])
const active = ref(null)
async function load() {
  tags.value = await api.tags()
  const slug = route.params.slug
  const name = String(route.query.tag || '').trim()
  active.value = tags.value.find(
    (tag) =>
      tag.slug === slug || (name && tag.name.toLocaleLowerCase() === name.toLocaleLowerCase())
  )
  posts.value = active.value ? await api.tagPosts(active.value.slug) : []
}
onMounted(load)
watch(() => [route.params.slug, route.query.tag], load)
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
    <section v-if="active" class="section-block">
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
