<script setup>
import { Search, X } from 'lucide-vue-next'

defineProps({
  keyword: { type: String, default: '' },
  category: { type: String, default: '' },
  tag: { type: String, default: '' },
  categories: { type: Array, default: () => [] },
  tags: { type: Array, default: () => [] }
})

defineEmits(['update:keyword', 'update:category', 'update:tag', 'submit', 'clear'])
</script>

<template>
  <form class="filter-toolbar" role="search" @submit.prevent="$emit('submit')">
    <label class="search-field">
      <Search :size="17" />
      <span class="sr-only">搜索文章</span>
      <input
        :value="keyword"
        type="search"
        placeholder="搜索标题或内容"
        @input="$emit('update:keyword', $event.target.value)"
      />
    </label>
    <select
      :value="category"
      aria-label="文章分类"
      @change="$emit('update:category', $event.target.value)"
    >
      <option value="">全部分类</option>
      <option v-for="item in categories" :key="item" :value="item">{{ item }}</option>
    </select>
    <select :value="tag" aria-label="文章标签" @change="$emit('update:tag', $event.target.value)">
      <option value="">全部标签</option>
      <option v-for="item in tags" :key="item" :value="item">{{ item }}</option>
    </select>
    <button class="search-button" type="submit">查找</button>
    <button
      v-if="keyword || category || tag"
      class="clear-button"
      type="button"
      aria-label="清除筛选"
      title="清除筛选"
      @click="$emit('clear')"
    >
      <X :size="17" />
    </button>
  </form>
</template>

<style scoped>
.filter-toolbar {
  display: grid;
  grid-template-columns: minmax(180px, 1fr) minmax(120px, 0.35fr) minmax(120px, 0.35fr) auto auto;
  gap: 8px;
  padding: 10px;
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-card);
  background: var(--surface-glass);
  backdrop-filter: blur(14px);
}
.search-field {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 9px;
  padding-inline: 11px;
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-control);
  background: var(--surface-solid);
  color: var(--text-tertiary);
}
input,
select,
button {
  min-height: 40px;
  border-radius: var(--radius-control);
  font: inherit;
  font-size: 0.8rem;
}
input {
  min-width: 0;
  flex: 1;
  border: 0;
  outline: 0;
  background: transparent;
  color: var(--text-primary);
}
select {
  min-width: 0;
  padding: 0 10px;
  border: 1px solid var(--border-subtle);
  background: var(--surface-solid);
  color: var(--text-secondary);
}
button {
  border: 1px solid var(--border-subtle);
  cursor: pointer;
}
.search-button {
  padding: 0 18px;
  border-color: var(--accent);
  background: var(--accent);
  color: #2c1d06;
  font-weight: 800;
}
.clear-button {
  width: 40px;
  display: grid;
  place-items: center;
  padding: 0;
  background: transparent;
  color: var(--text-secondary);
}
@media (max-width: 760px) {
  .filter-toolbar {
    grid-template-columns: 1fr 1fr;
  }
  .search-field {
    grid-column: 1 / -1;
  }
  .search-button {
    grid-column: 1 / -1;
  }
}
</style>
