<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getProfile } from '../api'
import AboutProfile from '../features/site/components/AboutProfile.vue'
import AuthorTimeline from '../features/site/components/AuthorTimeline.vue'
import EmptyState from '../components/ui/EmptyState.vue'

const router = useRouter()
const profile = ref(null)
const posts = ref([])
const loading = ref(true)

onMounted(async () => {
  try {
    const data = await getProfile({ page: 1, size: 8 })
    profile.value = data?.blogInfo || null
    posts.value = data?.posts || []
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <main class="about-page page-width">
    <div v-if="loading" class="about-loading loading-shimmer"></div>
    <template v-else-if="profile">
      <AboutProfile :profile="profile" />
      <AuthorTimeline :posts="posts" @open="router.push(`/post/${$event}`)" />
    </template>
    <EmptyState v-else title="作者资料暂时不可用" description="稍后刷新页面再试。" />
  </main>
</template>

<style scoped>
.about-page {
  min-height: calc(100vh - 70px);
}
.about-loading {
  min-height: 520px;
  margin-block: 30px;
  border-radius: var(--radius-card);
}
</style>
