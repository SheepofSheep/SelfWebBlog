<script setup>
import { computed, ref } from 'vue'
import { Archive, Search, Tags } from 'lucide-vue-next'

const props = defineProps({
  posts: { type: Array, default: () => [] }
})

const emit = defineEmits(['search', 'tag', 'category', 'archive'])
const keyword = ref('')

const categories = computed(() => {
  const set = new Set()
  props.posts.forEach((p) => {
    const name = p?.category || p?.categoryName
    if (name) set.add(name)
  })
  return [...set].slice(0, 8)
})

const tags = computed(() => {
  const set = new Set()
  props.posts.forEach((p) => {
    if (!p?.tags) return
    p.tags
      .split(/[,，\s]+/)
      .filter(Boolean)
      .forEach((tag) => set.add(tag))
  })
  return [...set].slice(0, 10)
})

function submit() {
  const value = keyword.value.trim()
  if (value) emit('search', value)
  else emit('archive')
}
</script>

<template>
  <section class="editorial-index glass-card">
    <div class="index-head">
      <p>EDITORIAL INDEX</p>
      <button @click="emit('archive')">
        <Archive :size="15" />
        全部归档
      </button>
    </div>

    <form class="index-search" @submit.prevent="submit">
      <Search :size="16" />
      <input v-model="keyword" type="search" placeholder="搜索文章、标题或片段..." />
      <button type="submit">搜索</button>
    </form>

    <div v-if="categories.length" class="index-row">
      <span>分类</span>
      <button v-for="c in categories" :key="c" @click="emit('category', c)">
        {{ c }}
      </button>
    </div>

    <div v-if="tags.length" class="index-row tag-row">
      <span><Tags :size="13" /> 标签</span>
      <button v-for="t in tags" :key="t" @click="emit('tag', t)"># {{ t }}</button>
    </div>
  </section>
</template>

<style scoped>
.editorial-index {
  max-width: var(--magazine-max, 1180px);
  margin: 18px auto 0;
  padding: 16px;
}

.editorial-index:hover {
  transform: none;
}

.index-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.index-head p {
  margin: 0;
  color: var(--primary-hover);
  font-size: 0.72rem;
  font-weight: 900;
  letter-spacing: 0.12em;
}

.index-head button,
.index-row button,
.index-search button {
  border: 1px solid var(--border);
  border-radius: var(--radius-pill);
  background: var(--surface-muted);
  color: var(--text-secondary);
  font: inherit;
  font-size: 0.78rem;
  font-weight: 800;
  cursor: pointer;
}

.index-head button {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 34px;
  padding: 0 12px;
}

.index-search {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  min-height: 48px;
  padding: 0 8px 0 14px;
  border: 1px solid var(--border-medium);
  border-radius: 18px;
  background: var(--surface-strong);
  color: var(--text-muted);
}

[data-theme='dark'] .index-search {
  background: var(--surface-strong);
}

.index-search input {
  min-width: 0;
  border: 0;
  outline: 0;
  background: transparent;
  color: var(--text-main);
  font: inherit;
}

.index-search button {
  min-height: 34px;
  padding: 0 14px;
  border-color: var(--border-warm);
  color: var(--primary-hover);
}

.index-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.index-row span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  margin-right: 2px;
  color: var(--text-muted);
  font-size: 0.76rem;
  font-weight: 900;
}

.index-row button {
  max-width: 150px;
  min-height: 32px;
  padding: 0 11px;
}

.index-row button:hover,
.index-head button:hover {
  color: var(--primary-hover);
  border-color: var(--border-warm);
}

@media (max-width: 640px) {
  .editorial-index {
    margin-top: 14px;
    padding: 13px;
  }

  .index-search {
    grid-template-columns: auto minmax(0, 1fr);
    padding-block: 10px;
  }

  .index-search button {
    grid-column: 1 / -1;
    width: 100%;
  }
}
</style>
