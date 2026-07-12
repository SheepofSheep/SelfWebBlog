<script setup>
import { Github, PenLine } from 'lucide-vue-next'
import PersonalLinks from '../PersonalLinks.vue'
import { toAbsoluteUrl } from '../../utils/url'

defineProps({
  blogInfo: { type: Object, default: null },
  totalPosts: { type: Number, default: 0 },
  isAdmin: { type: Boolean, default: false }
})
defineEmits(['write'])
</script>

<template>
  <section class="author-strip">
    <img
      :src="
        toAbsoluteUrl(blogInfo?.avatarUrl) ||
        'https://api.dicebear.com/7.x/avataaars/svg?seed=Gabriel'
      "
      :alt="blogInfo?.nickname || 'Gabriel'"
    />
    <div class="author-copy">
      <p>关于作者</p>
      <h2>{{ blogInfo?.nickname || 'Gabriel' }}</h2>
      <span>{{ blogInfo?.bio || '记录代码、学习和偶尔偏离主题的生活。' }}</span>
    </div>
    <strong>{{ totalPosts }}<small> 篇公开文章</small></strong>
    <PersonalLinks />
    <button v-if="isAdmin" type="button" @click="$emit('write')">
      <PenLine :size="15" /> 写新文章
    </button>
    <a v-else href="https://github.com/SheepofSheep" target="_blank" rel="noopener noreferrer">
      <Github :size="15" /> GitHub
    </a>
  </section>
</template>

<style scoped>
.author-strip {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto auto auto;
  align-items: center;
  gap: 18px;
  padding: 22px 0;
  border-block: 1px solid var(--border-medium);
}
.author-strip > img {
  width: 58px;
  height: 58px;
  border-radius: 50%;
  object-fit: cover;
}
.author-copy {
  min-width: 0;
}
.author-copy p {
  margin: 0 0 2px;
  color: var(--primary-hover);
  font-size: 0.7rem;
  font-weight: 900;
}
.author-copy h2 {
  margin: 0;
  font-family: var(--font-serif);
  font-size: 1.28rem;
}
.author-copy span {
  display: block;
  overflow: hidden;
  color: var(--text-muted);
  font-size: 0.82rem;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.author-strip > strong {
  font-family: var(--font-serif);
  font-size: 1.35rem;
}
.author-strip > strong small {
  color: var(--text-muted);
  font-family: var(--font-sans);
  font-size: 0.72rem;
  font-weight: 400;
}
.author-strip :deep(.personal-links) {
  margin: 0;
}
.author-strip > button,
.author-strip > a {
  display: inline-flex;
  min-height: 40px;
  align-items: center;
  gap: 7px;
  padding: 0 13px;
  border: 1px solid var(--border-medium);
  border-radius: 7px;
  background: transparent;
  color: var(--text-main);
  font: inherit;
  font-size: 0.78rem;
  font-weight: 700;
  text-decoration: none;
  cursor: pointer;
}
@media (max-width: 760px) {
  .author-strip {
    grid-template-columns: auto minmax(0, 1fr) auto;
  }
  .author-strip > strong,
  .author-strip :deep(.personal-links) {
    display: none;
  }
}
</style>
