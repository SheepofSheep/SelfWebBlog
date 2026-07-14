<script setup>
import { ArrowUpRight, Clock, Folder } from 'lucide-vue-next'
import { formatTime, getFirstImageUrl, toPlainText } from '../../../utils/format'
import { toAbsoluteUrl } from '../../../utils/url'

const props = defineProps({ post: { type: Object, required: true } })
defineEmits(['open'])

const cover = toAbsoluteUrl(props.post.coverUrl || getFirstImageUrl(props.post.content || '') || '')
</script>

<template>
  <article class="featured-post">
    <button class="featured-cover" type="button" @click="$emit('open', post.id)">
      <img v-if="cover" :src="cover" :alt="post.title" />
      <span v-else>{{ post.title?.slice(0, 1) || '文' }}</span>
    </button>
    <div class="featured-copy">
      <span class="section-label">本期推荐</span>
      <button class="featured-title" type="button" @click="$emit('open', post.id)">
        <h1>{{ post.title }}</h1>
        <ArrowUpRight :size="21" />
      </button>
      <p>{{ post.summary || toPlainText(post.content || '').slice(0, 116) }}</p>
      <div class="featured-meta">
        <span><Clock :size="14" />{{ formatTime(post.createTime) }}</span>
        <span v-if="post.category"><Folder :size="14" />{{ post.category }}</span>
      </div>
    </div>
  </article>
</template>

<style scoped>
.featured-post {
  display: grid;
  grid-template-columns: minmax(0, 1.12fr) minmax(250px, 0.88fr);
  min-height: 350px;
  overflow: hidden;
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-card);
  background: var(--surface-solid);
  box-shadow: var(--shadow-panel);
}
.featured-cover {
  min-width: 0;
  overflow: hidden;
  padding: 0;
  border: 0;
  background: var(--surface-soft);
  cursor: pointer;
}
.featured-cover img {
  width: 100%;
  height: 100%;
  min-height: 350px;
  display: block;
  object-fit: cover;
  transition: transform var(--motion-slow-duration) var(--motion-ease);
}
.featured-cover > span {
  height: 100%;
  min-height: 350px;
  display: grid;
  place-items: center;
  color: var(--accent-strong);
  font-family: var(--font-reading);
  font-size: 5rem;
}
.featured-cover:hover img {
  transform: scale(1.025);
}
.featured-copy {
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: clamp(24px, 4vw, 40px);
}
.section-label {
  margin-bottom: 12px;
  color: var(--accent-strong);
  font-size: 0.72rem;
  font-weight: 800;
}
.featured-title {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: start;
  gap: 14px;
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--text-primary);
  text-align: left;
  cursor: pointer;
}
h1 {
  font-family: var(--font-reading);
  font-size: clamp(1.75rem, 3vw, 2.7rem);
  line-height: 1.22;
  letter-spacing: 0;
}
.featured-title svg {
  margin-top: 8px;
  color: var(--accent);
}
p {
  margin: 18px 0;
  color: var(--text-secondary);
  font-size: 0.9rem;
  line-height: 1.75;
}
.featured-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  color: var(--text-tertiary);
  font-size: 0.74rem;
}
.featured-meta span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}
@media (max-width: 720px) {
  .featured-post {
    grid-template-columns: 1fr;
  }
  .featured-cover,
  .featured-cover img,
  .featured-cover > span {
    min-height: 220px;
    max-height: 260px;
  }
}
</style>
