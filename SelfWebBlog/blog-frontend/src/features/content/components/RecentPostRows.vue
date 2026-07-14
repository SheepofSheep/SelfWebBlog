<script setup>
import { ArrowRight } from 'lucide-vue-next'
import { formatTime } from '../../../utils/format'
import IconButton from '../../../components/ui/IconButton.vue'

defineProps({ posts: { type: Array, default: () => [] } })
defineEmits(['open', 'archive'])
</script>

<template>
  <section class="recent-posts">
    <header>
      <div>
        <span>刚刚更新</span>
        <h2>最近文章</h2>
      </div>
      <IconButton label="查看全部文章" @click="$emit('archive')"
        ><ArrowRight :size="18"
      /></IconButton>
    </header>
    <button
      v-for="(post, index) in posts"
      :key="post.id"
      class="recent-row"
      type="button"
      @click="$emit('open', post.id)"
    >
      <span class="index">{{ String(index + 1).padStart(2, '0') }}</span>
      <span class="copy">
        <strong>{{ post.title }}</strong>
        <small>{{ post.summary || post.category || '随笔' }}</small>
      </span>
      <time>{{ formatTime(post.createTime).slice(0, 11) }}</time>
    </button>
  </section>
</template>

<style scoped>
.recent-posts {
  border-block: 1px solid var(--border-subtle);
}
header {
  min-height: 74px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
}
header span,
header h2 {
  display: inline;
  margin: 0;
}
header span {
  margin-right: 12px;
  color: var(--accent-strong);
  font-size: 0.7rem;
  font-weight: 800;
}
header h2 {
  font-size: 1.08rem;
}
.recent-row {
  width: 100%;
  min-height: 88px;
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
  padding: 14px 8px;
  border: 0;
  border-top: 1px solid var(--border-subtle);
  background: transparent;
  color: var(--text-primary);
  text-align: left;
  cursor: pointer;
  transition:
    padding var(--motion-normal-duration) var(--motion-ease),
    background var(--motion-normal-duration);
}
.recent-row:hover {
  padding-inline: 16px;
  background: var(--surface-soft);
}
.index {
  color: var(--accent);
  font-family: var(--font-mono);
  font-size: 0.72rem;
}
.copy {
  min-width: 0;
  display: grid;
  gap: 5px;
}
.copy strong,
.copy small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.copy strong {
  font-family: var(--font-reading);
  font-size: 1rem;
}
.copy small,
time {
  color: var(--text-tertiary);
  font-size: 0.7rem;
}
@media (max-width: 560px) {
  .recent-row {
    grid-template-columns: 28px minmax(0, 1fr);
  }
  time {
    display: none;
  }
}
</style>
