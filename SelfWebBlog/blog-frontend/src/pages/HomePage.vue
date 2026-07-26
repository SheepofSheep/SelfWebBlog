<script setup>
import { computed, onMounted, ref } from 'vue'
import { ArrowRight } from 'lucide-vue-next'
import { api } from '@/api/client.js'
import { buildMonthCells } from '@/core/calendarModel.js'
import { appStore } from '@/stores/app.js'
import PostCard from '@/components/PostCard.vue'
import EmptyState from '@/components/EmptyState.vue'

const posts = ref([])
const tags = ref([])
const calendarDays = ref([])
const loading = ref(true)
const today = new Date()
const calendarCells = computed(() =>
  buildMonthCells(today.getFullYear(), today.getMonth() + 1, calendarDays.value)
)
const featured = computed(() => posts.value[0])
onMounted(async () => {
  const [postData, tagData, calendarData] = await Promise.allSettled([
    api.posts(1, 9),
    api.tags(),
    api.calendar(today.getFullYear(), today.getMonth() + 1)
  ])
  posts.value = postData.status === 'fulfilled' ? postData.value?.records || [] : []
  tags.value = tagData.status === 'fulfilled' ? tagData.value || [] : []
  calendarDays.value = calendarData.status === 'fulfilled' ? calendarData.value?.days || [] : []
  loading.value = false
})
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
          <RouterLink class="mini-calendar glass" to="/calendar" aria-label="打开内容日历">
            <div class="section-heading">
              <h2>日历</h2>
              <span>{{ today.getFullYear() }} · {{ today.getMonth() + 1 }}</span>
            </div>
            <div class="mini-calendar-week" aria-hidden="true">
              <span v-for="day in ['一', '二', '三', '四', '五', '六', '日']" :key="day">{{
                day
              }}</span>
            </div>
            <div class="mini-calendar-days">
              <span
                v-for="(cell, index) in calendarCells"
                :key="index"
                :class="{ empty: !cell, active: cell?.count }"
                >{{ cell?.day }}</span
              >
            </div>
          </RouterLink>
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
    <EmptyState v-else label="还没有发布文章"
      ><RouterLink v-if="appStore.user?.role === 'ADMIN'" class="primary-button" to="/write"
        >写第一篇</RouterLink
      ></EmptyState
    >
  </div>
</template>
