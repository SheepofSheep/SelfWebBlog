<script setup>
import { computed, onMounted, ref } from 'vue'
import { Github, Mail, Link as LinkIcon, RefreshCw } from 'lucide-vue-next'
import MarkdownBody from '@/components/MarkdownBody.vue'
import AppAvatar from '@/components/AppAvatar.vue'
import { appStore } from '@/stores/app.js'
import { api } from '@/api/client.js'
import { formatDate, postPublicationTime, postTitle } from '@/core/format.js'
import { publicSiteLinks } from '@/core/siteLinkModel.js'
const links = computed(() => publicSiteLinks(appStore.site || {}))
const posts = ref([])
const recentError = ref('')
async function loadRecent() {
  recentError.value = ''
  try {
    posts.value = ((await api.posts(1, 6))?.records || []).slice(0, 6)
  } catch (error) {
    recentError.value = error?.message || '近期文章加载失败'
  }
}
onMounted(loadRecent)
</script>
<template>
  <div class="page narrow-page">
    <header class="about-head glass">
      <AppAvatar
        :src="appStore.site?.avatarUrl"
        :name="appStore.site?.nickname || '关于'"
        :size="96"
        rounded
      />
      <div>
        <h1>{{ appStore.site?.nickname || '关于' }}</h1>
        <p v-if="appStore.site?.bio">{{ appStore.site.bio }}</p>
        <div class="social-row">
          <a v-for="link in links" :key="link.id" :href="link.url" target="_blank" rel="noreferrer"
            ><Github v-if="link.iconKey === 'github'" :size="18" /><Mail
              v-else-if="link.iconKey === 'mail'"
              :size="18"
            /><LinkIcon v-else :size="18" />{{ link.label }}</a
          >
        </div>
      </div>
    </header>
    <section v-if="appStore.site?.aboutMarkdown" class="article-surface glass">
      <MarkdownBody :content="appStore.site.aboutMarkdown" />
    </section>
    <section v-if="posts.length" class="about-timeline glass">
      <div class="section-heading">
        <h2>近期</h2>
        <RouterLink to="/archive">全部</RouterLink>
      </div>
      <RouterLink v-for="post in posts" :key="post.id" :to="`/post/${post.id}`">
        <time>{{ formatDate(postPublicationTime(post), true) }}</time>
        <strong>{{ postTitle(post) }}</strong>
      </RouterLink>
    </section>
    <section v-else-if="recentError" class="about-recent-error glass" role="alert">
      <span>{{ recentError }}</span>
      <button class="secondary-button" type="button" @click="loadRecent">
        <RefreshCw :size="15" />重试
      </button>
    </section>
  </div>
</template>

<style scoped>
.about-recent-error {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
  padding: 18px 20px;
  border-radius: var(--radius-lg);
  color: var(--muted);
  font-size: 13px;
}
.about-recent-error button {
  display: inline-flex;
  gap: 6px;
  align-items: center;
}
</style>
