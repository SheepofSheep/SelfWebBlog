<script setup>
import { nextTick, ref, watch } from 'vue'
import { Search } from 'lucide-vue-next'
import { useRouter } from 'vue-router'
import AppDialog from './ui/AppDialog.vue'

const props = defineProps({ open: { type: Boolean, default: false } })
const emit = defineEmits(['close'])
const router = useRouter()
const keyword = ref('')
const input = ref(null)

watch(
  () => props.open,
  async (open) => {
    if (!open) return
    await nextTick()
    input.value?.focus()
  }
)

async function submit() {
  const value = keyword.value.trim()
  emit('close')
  await router.push({ name: 'archive', query: value ? { keyword: value } : {} })
  keyword.value = ''
}
</script>

<template>
  <AppDialog
    :open="open"
    title="搜索文章"
    description="按标题、摘要或正文关键词查找。"
    @close="emit('close')"
  >
    <form class="site-search-form" role="search" @submit.prevent="submit">
      <label>
        <Search :size="18" />
        <span class="sr-only">搜索关键词</span>
        <input ref="input" v-model="keyword" type="search" placeholder="输入关键词" autofocus />
      </label>
      <button type="submit">查找</button>
    </form>
  </AppDialog>
</template>

<style scoped>
.site-search-form {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
}
.site-search-form label {
  min-height: 44px;
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 0 12px;
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-control);
  color: var(--text-tertiary);
  background: var(--surface-raised);
}
.site-search-form input {
  min-width: 0;
  width: 100%;
  border: 0;
  outline: 0;
  background: transparent;
  color: var(--text-primary);
  font: inherit;
}
.site-search-form button {
  min-height: 44px;
  padding: 0 18px;
  border: 1px solid var(--accent);
  border-radius: var(--radius-control);
  background: var(--accent);
  color: #2c1d06;
  font: inherit;
  font-weight: 800;
  cursor: pointer;
}
@media (max-width: 480px) {
  .site-search-form {
    grid-template-columns: 1fr;
  }
}
</style>
