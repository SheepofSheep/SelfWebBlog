<script setup>
import { formatTime } from '../../../utils/format'

defineProps({ posts: { type: Array, default: () => [] } })
defineEmits(['open'])
</script>

<template>
  <section v-if="posts.length" class="author-timeline">
    <header>
      <p>最近记录</p>
      <h2>写过的内容</h2>
    </header>
    <div>
      <button
        v-for="post in posts.slice(0, 6)"
        :key="post.id"
        type="button"
        @click="$emit('open', post.id)"
      >
        <time>{{ formatTime(post.createTime).slice(0, 11) }}</time>
        <strong>{{ post.title }}</strong>
        <span>{{ post.category || '随笔' }}</span>
      </button>
    </div>
  </section>
</template>

<style scoped>
.author-timeline {
  display: grid;
  grid-template-columns: minmax(150px, 0.35fr) minmax(0, 1fr);
  gap: clamp(24px, 6vw, 70px);
  padding-block: 46px 70px;
}
header p,
header h2 {
  margin: 0;
}
header p {
  color: var(--accent-strong);
  font-size: 0.7rem;
  font-weight: 900;
}
header h2 {
  margin-top: 5px;
  font-family: var(--font-reading);
  font-size: 1.55rem;
}
.author-timeline > div {
  border-top: 1px solid var(--border-strong);
}
button {
  width: 100%;
  min-height: 72px;
  display: grid;
  grid-template-columns: 105px minmax(0, 1fr) auto;
  align-items: center;
  gap: 14px;
  padding: 12px 4px;
  border: 0;
  border-bottom: 1px solid var(--border-subtle);
  background: transparent;
  color: var(--text-primary);
  text-align: left;
  cursor: pointer;
}
button:hover {
  background: var(--surface-soft);
}
time,
span {
  color: var(--text-tertiary);
  font-size: 0.7rem;
}
strong {
  overflow: hidden;
  font-family: var(--font-reading);
  text-overflow: ellipsis;
  white-space: nowrap;
}
@media (max-width: 700px) {
  .author-timeline {
    grid-template-columns: 1fr;
  }
  button {
    grid-template-columns: 80px minmax(0, 1fr);
  }
  button span {
    display: none;
  }
}
</style>
