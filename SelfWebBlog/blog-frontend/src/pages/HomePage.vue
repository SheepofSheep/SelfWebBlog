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
import AppAvatar from '@/components/AppAvatar.vue'

const posts = ref([])
const tags = ref([])
const loading = ref(true)
const postError = ref('')
const requestGate = createAdminRequestGate()
const route = useRoute()
const router = useRouter()
const featured = computed(() => posts.value[0])
const calendarOpen = computed(() => route.query.calendar === 'open')
const query = ref('')
function searchArticles() {
  router.push({ path: '/articles', query: query.value.trim() ? { q: query.value.trim() } : {} })
}

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
    <h1 v-if="!featured" class="sr-only">{{ appStore.site?.nickname || '博客' }}</h1>
    <div class="home-stage">
      <section class="home-intro">
        <p class="home-description">
          {{ appStore.site?.bio || '把所学写成文章，把日常留在这里。' }}
        </p>
        <p v-if="appStore.site?.currentStatus" class="home-status">
          <span />{{ appStore.site.currentStatus }}
        </p>
        <form class="home-search glass" role="search" @submit.prevent="searchArticles">
          <input
            v-model="query"
            aria-label="搜索博客文章"
            placeholder="想读点什么？搜索文章、标签…"
          />
          <button type="submit" class="round-tool" aria-label="查找文章">
            <ArrowRight :size="20" />
          </button>
        </form>
        <div v-if="featured && !loading" class="home-current">
          <div class="section-heading">
            <h2>最近写下</h2>
            <RouterLink to="/articles">所有文章 <ArrowRight :size="15" /></RouterLink>
          </div>
          <PostCard :post="featured" featured />
        </div>
      </section>
      <aside class="home-side">
        <section class="author-card glass">
          <AppAvatar
            :src="appStore.site?.avatarUrl"
            :name="appStore.site?.nickname || '博主'"
            :size="56"
          />
          <div>
            <span>你好，我是</span>
            <h2>{{ appStore.site?.nickname || '博主' }}</h2>
          </div>
          <p>{{ appStore.site?.currentStatus || appStore.site?.bio || '欢迎来这里坐坐。' }}</p>
          <RouterLink class="text-link" to="/about"
            >更多关于我 <ArrowRight :size="16"
          /></RouterLink>
        </section>
        <section class="tag-cloud glass">
          <div class="section-heading">
            <h2>从兴趣开始</h2>
            <RouterLink to="/tags">全部</RouterLink>
          </div>
          <div class="tag-row large">
            <RouterLink v-for="tag in tags.slice(0, 8)" :key="tag.slug" :to="`/tags/${tag.slug}`"
              >{{ tag.name }}<small>{{ tag.postCount }}</small></RouterLink
            >
            <p v-if="!tags.length" class="muted">标签还在慢慢整理。</p>
          </div>
        </section>
        <CalendarWidget :initial-open="calendarOpen" @close="closeCalendar" />
      </aside>
    </div>
    <section class="section-block latest-posts" aria-label="最新文章">
      <div class="section-heading">
        <div>
          <h2>继续阅读</h2>
          <p>散落的想法，慢慢整理成篇。</p>
        </div>
        <RouterLink class="text-link" to="/archive">时间归档 <ArrowRight :size="16" /></RouterLink>
      </div>
      <div v-if="loading" class="post-grid">
        <div v-for="i in 4" :key="i" class="skeleton-card glass" />
      </div>
      <div v-else-if="posts.length > 1" class="latest-list">
        <PostCard v-for="post in posts.slice(1)" :key="post.id" :post="post" row />
      </div>
      <EmptyState
        v-else
        :label="postError || (posts.length ? '新的文章正在酝酿中' : '还没有发布文章')"
        ><button v-if="postError" class="secondary-button" type="button" @click="load">
          <RefreshCw :size="16" />重试</button
        ><RouterLink v-else-if="appStore.user?.role === 'ADMIN'" class="primary-button" to="/write"
          >写下一篇</RouterLink
        ></EmptyState
      >
    </section>
  </div>
</template>
