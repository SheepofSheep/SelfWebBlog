<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ExternalLink, Github, Mail, Tv } from 'lucide-vue-next'
import { getProfile, getSiteAbout } from '../api'
import { renderArticleMarkdown } from '../utils/marked'
import AboutProfile from '../features/site/components/AboutProfile.vue'
import AuthorTimeline from '../features/site/components/AuthorTimeline.vue'
import EmptyState from '../components/ui/EmptyState.vue'

const router = useRouter()
const profile = ref(null)
const posts = ref([])
const loading = ref(true)
const aboutHtml = computed(() => renderArticleMarkdown(profile.value?.aboutMarkdown || ''))
const socialLinks = computed(() =>
  Object.entries(profile.value?.socialLinks || {}).filter(([, value]) => value)
)
const socialIcons = { github: Github, bilibili: Tv, email: Mail, website: ExternalLink }
const socialLabels = { github: 'GitHub', bilibili: '哔哩哔哩', email: '邮箱', website: '个人网站' }

function socialHref(key, value) {
  return key === 'email' ? `mailto:${value}` : value
}

onMounted(async () => {
  try {
    const [site, data] = await Promise.all([getSiteAbout(), getProfile({ page: 1, size: 8 })])
    profile.value = site
    posts.value = data?.posts || []
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="about-page page-width">
    <div v-if="loading" class="about-loading loading-shimmer"></div>
    <template v-else-if="profile">
      <AboutProfile :profile="profile" />
      <section v-if="aboutHtml || socialLinks.length" class="about-details">
        <article v-if="aboutHtml" class="markdown-body" v-html="aboutHtml"></article>
        <nav v-if="socialLinks.length" aria-label="作者链接">
          <a
            v-for="[key, value] in socialLinks"
            :key="key"
            :href="socialHref(key, value)"
            :target="key === 'email' ? undefined : '_blank'"
            rel="noopener noreferrer"
          >
            <component :is="socialIcons[key] || ExternalLink" :size="16" />{{
              socialLabels[key] || key
            }}
          </a>
        </nav>
      </section>
      <AuthorTimeline :posts="posts" @open="router.push(`/post/${$event}`)" />
    </template>
    <EmptyState v-else title="作者资料暂时不可用" description="稍后刷新页面再试。" />
  </div>
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
.about-details {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 190px;
  gap: clamp(28px, 6vw, 72px);
  padding-block: clamp(28px, 5vw, 58px);
  border-bottom: 1px solid var(--border-strong);
}
.about-details article {
  min-width: 0;
}
.about-details nav {
  display: grid;
  align-content: start;
  border-top: 1px solid var(--border-subtle);
}
.about-details nav a {
  min-height: 44px;
  display: flex;
  align-items: center;
  gap: 8px;
  border-bottom: 1px solid var(--border-subtle);
  color: var(--text-secondary);
  font-size: 0.75rem;
  font-weight: 700;
  text-decoration: none;
}
.about-details nav a:hover {
  color: var(--accent-strong);
}
@media (max-width: 680px) {
  .about-details {
    grid-template-columns: 1fr;
  }
}
</style>
