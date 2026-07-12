<script setup>
import { ArrowRight } from 'lucide-vue-next'
import { formatTime } from '../../utils/format'

defineProps({ posts: { type: Array, default: () => [] } })
defineEmits(['open', 'archive'])
</script>

<template>
  <section class="recent-list">
    <header>
      <div>
        <p>最近更新</p>
        <h2>继续往下读</h2>
      </div>
      <button type="button" aria-label="查看全部文章" @click="$emit('archive')">
        <ArrowRight :size="18" />
      </button>
    </header>
    <button
      v-for="(post, index) in posts"
      :key="post.id"
      class="recent-row"
      type="button"
      @click="$emit('open', post.id)"
    >
      <span class="recent-number">0{{ index + 1 }}</span>
      <span class="recent-main">
        <strong>{{ post.title }}</strong>
        <small>{{ post.summary || post.category || '随笔' }}</small>
      </span>
      <time>{{ formatTime(post.createTime).slice(0, 11) }}</time>
    </button>
    <button class="recent-all" type="button" @click="$emit('archive')">
      浏览全部文章 <ArrowRight :size="15" />
    </button>
  </section>
</template>

<style scoped>
.recent-list {
  display: flex;
  min-width: 0;
  flex-direction: column;
  border-block: 1px solid var(--border-medium);
}
.recent-list header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 18px 4px;
}
.recent-list header p {
  margin: 0 0 3px;
  color: var(--primary-hover);
  font-size: 0.72rem;
  font-weight: 900;
}
.recent-list h2 {
  margin: 0;
  font-family: var(--font-serif);
  font-size: 1.5rem;
}
.recent-list header button {
  display: grid;
  width: 38px;
  height: 38px;
  place-items: center;
  border: 1px solid var(--border-light);
  border-radius: 50%;
  background: transparent;
  color: var(--text-main);
  cursor: pointer;
}
.recent-row {
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
  min-height: 104px;
  padding: 18px 4px;
  border: 0;
  border-top: 1px solid var(--border-light);
  background: transparent;
  color: var(--text-main);
  text-align: left;
  cursor: pointer;
  transition:
    padding var(--motion-normal),
    background var(--motion-normal);
}
.recent-row:hover {
  padding-inline: 12px;
  background: var(--surface-muted);
}
.recent-number {
  color: var(--primary);
  font-family: var(--font-mono);
  font-size: 0.75rem;
}
.recent-main {
  display: grid;
  gap: 6px;
  min-width: 0;
}
.recent-main strong {
  overflow: hidden;
  font-family: var(--font-serif);
  font-size: 1.08rem;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.recent-main small,
.recent-row time {
  color: var(--text-muted);
  font-size: 0.72rem;
}
.recent-all {
  display: flex;
  min-height: 50px;
  align-items: center;
  justify-content: space-between;
  padding: 0 4px;
  border: 0;
  border-top: 1px solid var(--border-light);
  background: transparent;
  color: var(--text-main);
  font-weight: 800;
  cursor: pointer;
}
@media (max-width: 720px) {
  .recent-row {
    min-height: 86px;
  }
}
</style>
