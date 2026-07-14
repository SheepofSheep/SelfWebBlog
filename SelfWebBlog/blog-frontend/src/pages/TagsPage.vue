<script setup>
import { Tags } from 'lucide-vue-next'
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { listTags } from '../api'
import EmptyState from '../components/ui/EmptyState.vue'
import TagDirectory from '../features/content/components/TagDirectory.vue'

const router = useRouter()
const tags = ref([])
const loading = ref(true)

onMounted(async () => {
  try {
    tags.value = await listTags()
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="directory-page page-width">
    <header>
      <span><Tags :size="17" /> Topics</span>
      <h1>标签</h1>
      <p>{{ tags.length }} 个主题</p>
    </header>
    <div v-if="loading" class="loading-shimmer tag-loading"></div>
    <TagDirectory
      v-else-if="tags.length"
      :tags="tags"
      @open="router.push({ name: 'tag-detail', params: { slug: $event.slug } })"
    />
    <EmptyState v-else title="还没有标签" description="文章添加标签后会在这里出现。">
      <template #icon><Tags :size="20" /></template>
    </EmptyState>
  </div>
</template>

<style scoped>
.directory-page {
  max-width: 920px;
  padding-block: clamp(44px, 8vw, 90px);
}
header {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: end;
  gap: 6px 20px;
  margin-bottom: 30px;
}
header > span {
  grid-column: 1 / -1;
  display: flex;
  align-items: center;
  gap: 7px;
  color: var(--accent-strong);
  font-size: 0.74rem;
  font-weight: 800;
}
h1,
p {
  margin: 0;
}
h1 {
  font-family: var(--font-reading);
  font-size: clamp(2.4rem, 7vw, 4.8rem);
  font-weight: 600;
}
p {
  color: var(--text-tertiary);
  font-size: 0.75rem;
}
.tag-loading {
  height: 320px;
  border-radius: var(--radius-card);
}
</style>
