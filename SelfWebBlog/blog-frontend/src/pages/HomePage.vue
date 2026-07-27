<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowRight, RefreshCw } from 'lucide-vue-next'
import { api } from '@/api/client.js'
import { appStore } from '@/stores/app.js'
import PostCard from '@/components/PostCard.vue'
import EmptyState from '@/components/EmptyState.vue'
import CalendarWidget from '@/components/CalendarWidget.vue'
import { createAdminRequestGate } from '@/core/adminModel.js'

const posts = ref([])
const tags = ref([])
const loading = ref(true)
const postError = ref('')
const requestGate = createAdminRequestGate()
const route = useRoute()
const router = useRouter()
const featured = computed(() => posts.value[0])
const calendarOpen = computed(() => route.query.calendar === 'open')

function closeCalendar() {
  if (!calendarOpen.value) return
  const query = { ...route.query }
  delete query.calendar
  router.replace({ path: route.path, query })
}

async function load() {
  const ticket = requestGate.begin()
  loading.value = true
  postError.value = ''
  const [postData, tagData] = await Promise.allSettled([api.posts(1, 9), api.tags()])
  if (!requestGate.isCurrent(ticket)) return
  posts.value = postData.status === 'fulfilled' ? postData.value?.records || [] : []
  tags.value = tagData.status === 'fulfilled' ? tagData.value || [] : []
  if (postData.status === 'rejected') postError.value = postData.reason?.message || '文章加载失败'
  loading.value = false
}

onMounted(load)
onBeforeUnmount(() => requestGate.begin())
</script>

<template>
  <div class="page home-page">
    <section v-if="appStore.site?.currentStatus || appStore.site?.bio" class="home-note glass">
      <span class="note-orbit" aria-hidden="true" />
      <p>{{ appStore.site.currentStatus || appStore.site.bio }}</p>
    </section>
    <section v-if="loading" class="post-grid">
      <div v-for="i in 5" :key="i" class="skeleton-card glass" />
    </section>
    <template v-else-if="posts.length">
      <section class="feature-layout">
        <PostCard :post="featured" featured />
        <aside class="home-side">
          <section class="tag-cloud glass">
            <div class="section-heading">
              <h2>标签</h2>
              <RouterLink to="/tags">全部</RouterLink>
            </div>
            <div class="tag-row large">
              <RouterLink v-for="tag in tags.slice(0, 10)" :key="tag.slug" :to="`/tags/${tag.slug}`"
                >{{ tag.name }}<small>{{ tag.postCount }}</small></RouterLink
              >
            </div>
          </section>
          <CalendarWidget :initial-open="calendarOpen" @close="closeCalendar" />
        </aside>
      </section>
      <section class="section-block">
        <div class="section-heading">
          <h2>文章</h2>
          <RouterLink class="text-link" to="/archive"
            >查看归档 <ArrowRight :size="16"
          /></RouterLink>
        </div>
        <div class="post-grid">
          <PostCard v-for="post in posts.slice(1)" :key="post.id" :post="post" />
        </div>
      </section>
    </template>
    <EmptyState v-else :label="postError || '还没有发布文章'"
      ><button v-if="postError" class="secondary-button" type="button" @click="load">
        <RefreshCw :size="16" />重试</button
      ><RouterLink v-else-if="appStore.user?.role === 'ADMIN'" class="primary-button" to="/write"
        >写第一篇</RouterLink
      ></EmptyState
    >
  </div>
</template>
