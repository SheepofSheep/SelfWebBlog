import { computed, ref } from 'vue'
import { getProfile } from '../api'

export function useHomeData() {
  const blogInfo = ref(null)
  const posts = ref([])
  const totalPosts = ref(0)
  const loading = ref(false)
  const error = ref('')

  const featuredPost = computed(() => posts.value[0] || null)
  const recentPosts = computed(() => posts.value.slice(1, 4))
  const feedPosts = computed(() => posts.value.slice(4))
  const categories = computed(() => [
    ...new Set(posts.value.map((post) => post.category).filter(Boolean))
  ])
  const tags = computed(() => [
    ...new Set(posts.value.flatMap((post) => (post.tags || '').split(/[,，\s]+/).filter(Boolean)))
  ])

  async function load() {
    loading.value = true
    error.value = ''
    try {
      const data = await getProfile({ page: 1, size: 16 })
      blogInfo.value = data?.blogInfo || null
      posts.value = Array.isArray(data?.posts) ? data.posts : []
      totalPosts.value = data?.total ?? posts.value.length
    } catch (requestError) {
      error.value = requestError?.message || '文章暂时没有加载出来。'
    } finally {
      loading.value = false
    }
  }

  return {
    blogInfo,
    posts,
    totalPosts,
    loading,
    error,
    featuredPost,
    recentPosts,
    feedPosts,
    categories,
    tags,
    load
  }
}
