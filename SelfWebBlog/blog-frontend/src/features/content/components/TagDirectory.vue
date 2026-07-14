<script setup>
import { ArrowUpRight, Tag } from 'lucide-vue-next'
import { formatTime } from '../../../utils/format'

defineProps({ tags: { type: Array, default: () => [] } })
defineEmits(['open'])
</script>

<template>
  <div class="tag-directory">
    <button v-for="item in tags" :key="item.id || item.slug" type="button" @click="$emit('open', item)">
      <span class="tag-mark"><Tag :size="16" /></span>
      <span class="tag-copy">
        <strong>{{ item.name }}</strong>
        <small>{{ item.latestPostTime ? `最近更新 ${formatTime(item.latestPostTime)}` : '暂无公开文章' }}</small>
      </span>
      <span class="tag-count">{{ item.postCount || 0 }} 篇</span>
      <ArrowUpRight :size="17" />
    </button>
  </div>
</template>

<style scoped>
.tag-directory {
  border-top: 1px solid var(--border-strong);
}
.tag-directory > button {
  width: 100%;
  min-height: 76px;
  display: grid;
  grid-template-columns: 38px minmax(0, 1fr) auto auto;
  align-items: center;
  gap: 14px;
  padding: 12px 4px;
  border: 0;
  border-bottom: 1px solid var(--border-subtle);
  background: transparent;
  color: var(--text-primary);
  font: inherit;
  text-align: left;
  cursor: pointer;
  transition:
    background var(--motion-fast-duration),
    padding var(--motion-fast-duration);
}
.tag-directory > button:hover {
  padding-inline: 10px;
  background: var(--surface-glass);
}
.tag-mark {
  width: 34px;
  height: 34px;
  display: grid;
  place-items: center;
  border-radius: var(--radius-control);
  background: var(--accent-soft);
  color: var(--accent-strong);
}
.tag-copy {
  min-width: 0;
  display: grid;
  gap: 4px;
}
.tag-copy strong {
  font-size: 0.9rem;
}
.tag-copy small,
.tag-count {
  color: var(--text-tertiary);
  font-size: 0.7rem;
}
@media (max-width: 560px) {
  .tag-directory > button {
    grid-template-columns: 34px minmax(0, 1fr) auto;
  }
  .tag-count {
    display: none;
  }
}
</style>
