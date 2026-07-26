<script setup>
import { computed, onMounted, ref } from 'vue'
import { Github, Mail, Link as LinkIcon } from 'lucide-vue-next'
import MarkdownBody from '@/components/MarkdownBody.vue'
import { appStore } from '@/stores/app.js'
import { api } from '@/api/client.js'
import { formatDate, postTitle } from '@/core/format.js'
const links = computed(() => Object.entries(appStore.site?.socialLinks || {}))
const posts = ref([])
onMounted(async () => {
  posts.value = ((await api.posts(1, 6).catch(() => null))?.records || []).slice(0, 6)
})
</script>
<template>
  <div class="page narrow-page">
    <header class="about-head glass">
      <img v-if="appStore.site?.avatarUrl" :src="appStore.site.avatarUrl" alt="" />
      <div>
        <h1>{{ appStore.site?.nickname || '关于' }}</h1>
        <p v-if="appStore.site?.bio">{{ appStore.site.bio }}</p>
        <div class="social-row">
          <a
            v-for="link in links"
            :key="link[0]"
            :href="link[0] === 'email' ? `mailto:${link[1]}` : link[1]"
            target="_blank"
            rel="noreferrer"
            ><Github v-if="link[0] === 'github'" :size="18" /><Mail
              v-else-if="link[0] === 'email'"
              :size="18"
            /><LinkIcon v-else :size="18" />{{ link[0] }}</a
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
        <time>{{ formatDate(post.createTime, true) }}</time>
        <strong>{{ postTitle(post) }}</strong>
      </RouterLink>
    </section>
  </div>
</template>
