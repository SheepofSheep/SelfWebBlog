<script setup>
import { ArrowUpRight, Clock, Folder } from 'lucide-vue-next'
import { formatTime, getFirstImageUrl, toPlainText } from '../../utils/format'
import { toAbsoluteUrl } from '../../utils/url'

const props = defineProps({ post: { type: Object, required: true } })
defineEmits(['open'])

function coverUrl() {
  return toAbsoluteUrl(props.post.coverUrl || getFirstImageUrl(props.post.content || '') || '')
}
</script>

<template>
  <article class="featured-article">
    <button class="featured-media" type="button" @click="$emit('open', post.id)">
      <img v-if="coverUrl()" :src="coverUrl()" :alt="post.title" />
      <span v-else>{{ post.title?.charAt(0) || '文' }}</span>
    </button>
    <div class="featured-copy">
      <p class="featured-label">本期推荐</p>
      <button class="featured-title" type="button" @click="$emit('open', post.id)">
        <h1>{{ post.title }}</h1>
        <ArrowUpRight :size="22" />
      </button>
      <p class="featured-summary">
        {{ post.summary || toPlainText(post.content || '').slice(0, 120) || '打开文章继续阅读。' }}
      </p>
      <div class="featured-meta">
        <span><Clock :size="14" /> {{ formatTime(post.createTime) }}</span>
        <span v-if="post.category"><Folder :size="14" /> {{ post.category }}</span>
      </div>
    </div>
  </article>
</template>

<style scoped>
.featured-article {
  display: grid;
  min-width: 0;
  grid-template-rows: 340px auto;
  overflow: hidden;
  border: 1px solid var(--border-medium);
  border-radius: 10px;
  background: var(--surface-paper);
}
.featured-media {
  position: relative;
  min-height: 0;
  overflow: hidden;
  border: 0;
  background: var(--surface-muted);
  cursor: pointer;
}
.featured-media::after {
  content: '';
  position: absolute;
  inset: auto 0 0;
  height: 42%;
  background: linear-gradient(transparent, rgba(18, 13, 7, 0.24));
}
.featured-media img {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
  transition: transform var(--motion-slow);
}
.featured-media:hover img {
  transform: scale(1.025);
}
.featured-media > span {
  display: grid;
  height: 100%;
  place-items: center;
  color: var(--primary);
  font-family: var(--font-serif);
  font-size: 6rem;
}
.featured-copy {
  padding: clamp(20px, 3vw, 34px);
}
.featured-label {
  margin: 0 0 10px;
  color: var(--primary-hover);
  font-size: 0.75rem;
  font-weight: 900;
}
.featured-title {
  width: 100%;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--text-main);
  text-align: left;
  cursor: pointer;
}
.featured-title h1 {
  margin: 0;
  font-family: var(--font-serif);
  font-size: clamp(2rem, 4vw, 3.5rem);
  line-height: 1.16;
}
.featured-title svg {
  flex: 0 0 auto;
  margin-top: 8px;
  color: var(--primary);
}
.featured-summary {
  max-width: 48ch;
  margin: 16px 0;
  color: var(--text-secondary);
  line-height: 1.8;
}
.featured-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  color: var(--text-muted);
  font-size: 0.78rem;
}
.featured-meta span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}
@media (max-width: 720px) {
  .featured-article {
    grid-template-rows: 230px auto;
  }
  .featured-title h1 {
    font-size: 2rem;
  }
}
</style>
