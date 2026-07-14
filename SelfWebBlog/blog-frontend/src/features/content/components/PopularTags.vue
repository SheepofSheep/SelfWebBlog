<script setup>
import { Tag } from 'lucide-vue-next'

defineProps({ tags: { type: Array, default: () => [] } })
defineEmits(['select', 'open'])
</script>

<template>
  <section v-if="tags.length" class="popular-tags">
    <header>
      <span><Tag :size="15" />热门标签</span>
      <button type="button" @click="$emit('open')">全部</button>
    </header>
    <div>
      <button
        v-for="tag in tags.slice(0, 10)"
        :key="tag"
        type="button"
        @click="$emit('select', tag)"
      >
        {{ tag }}
      </button>
    </div>
  </section>
</template>

<style scoped>
header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}
header span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 0.8rem;
  font-weight: 800;
}
header button {
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--text-tertiary);
  font: inherit;
  font-size: 0.7rem;
  cursor: pointer;
}
.popular-tags > div {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
}
.popular-tags > div button {
  min-height: 30px;
  padding: 0 10px;
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-control);
  background: var(--surface-soft);
  color: var(--text-secondary);
  font: inherit;
  font-size: 0.7rem;
  cursor: pointer;
}
.popular-tags > div button:hover {
  border-color: var(--accent);
  color: var(--accent-strong);
}
</style>
