<script setup>
import { Search, X } from 'lucide-vue-next'

defineProps({
  keyword: { type: String, default: '' },
  category: { type: String, default: '' },
  tag: { type: String, default: '' },
  sort: { type: String, default: 'date' },
  categories: { type: Array, default: () => [] },
  tags: { type: Array, default: () => [] },
  showSort: { type: Boolean, default: false }
})
const emit = defineEmits([
  'update:keyword',
  'update:category',
  'update:tag',
  'update:sort',
  'submit',
  'clear'
])
</script>

<template>
  <form class="article-filter" role="search" @submit.prevent="emit('submit')">
    <label class="filter-search">
      <Search :size="17" />
      <span class="sr-only">搜索文章</span>
      <input
        :value="keyword"
        type="search"
        placeholder="搜索文章、项目或随笔"
        @input="emit('update:keyword', $event.target.value)"
      />
    </label>
    <select
      :value="category"
      aria-label="文章分类"
      @change="emit('update:category', $event.target.value)"
    >
      <option value="">全部分类</option>
      <option v-for="item in categories" :key="item" :value="item">{{ item }}</option>
    </select>
    <select
      v-if="showSort"
      :value="sort"
      aria-label="文章排序"
      @change="emit('update:sort', $event.target.value)"
    >
      <option value="date">最新发布</option>
      <option value="title">标题顺序</option>
    </select>
    <button class="filter-submit" type="submit">查找</button>
    <button
      v-if="keyword || category || tag"
      class="filter-clear"
      type="button"
      aria-label="清除筛选"
      @click="emit('clear')"
    >
      <X :size="16" />
    </button>
    <div v-if="tags.length" class="filter-tags">
      <button
        v-for="item in tags.slice(0, 8)"
        :key="item.id || item.name || item"
        type="button"
        :class="{ active: tag === (item.name || item) }"
        @click="emit('update:tag', tag === (item.name || item) ? '' : item.name || item)"
      >
        # {{ item.name || item }}
      </button>
    </div>
  </form>
</template>

<style scoped>
.article-filter {
  display: grid;
  grid-template-columns: minmax(240px, 1fr) 170px auto auto;
  gap: 8px;
  align-items: center;
}
.filter-search {
  min-width: 0;
  display: flex;
  height: 46px;
  align-items: center;
  gap: 9px;
  padding: 0 14px;
  border: 1px solid var(--border-medium);
  border-radius: 8px;
  background: var(--surface-strong);
  color: var(--text-muted);
}
.filter-search input {
  min-width: 0;
  flex: 1;
  border: 0;
  outline: 0;
  background: transparent;
  color: var(--text-main);
  font: inherit;
}
select,
.filter-submit,
.filter-clear {
  height: 46px;
  border: 1px solid var(--border-medium);
  border-radius: 8px;
  background: var(--surface-strong);
  color: var(--text-main);
  font: inherit;
}
select {
  min-width: 0;
  padding: 0 12px;
}
.filter-submit {
  padding: 0 20px;
  border-color: var(--primary);
  background: var(--primary);
  color: var(--on-primary);
  font-weight: 800;
  cursor: pointer;
}
.filter-clear {
  display: grid;
  width: 46px;
  place-items: center;
  cursor: pointer;
}
.filter-tags {
  grid-column: 1 / -1;
  display: flex;
  gap: 6px;
  overflow-x: auto;
  padding-top: 4px;
  scrollbar-width: none;
}
.filter-tags button {
  flex: 0 0 auto;
  min-height: 34px;
  padding: 0 11px;
  border: 1px solid var(--border-light);
  border-radius: 999px;
  background: transparent;
  color: var(--text-muted);
  cursor: pointer;
}
.filter-tags button.active,
.filter-tags button:hover {
  border-color: var(--primary);
  color: var(--primary-hover);
}
.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
}
@media (max-width: 720px) {
  .article-filter {
    grid-template-columns: minmax(0, 1fr) auto;
  }
  .filter-search {
    grid-column: 1 / -1;
  }
  select {
    width: 100%;
  }
  .filter-submit {
    padding: 0 16px;
  }
}
</style>
