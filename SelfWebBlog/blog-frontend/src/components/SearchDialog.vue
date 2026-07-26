<script setup>
import { onBeforeUnmount, ref } from 'vue'
import { Search, ArrowUpRight } from 'lucide-vue-next'
import { api } from '@/api/client.js'
import { postTitle } from '@/core/format.js'
import AppDialog from './AppDialog.vue'

defineEmits(['close'])
const query = ref('')
const results = ref([])
let timer
let requestId = 0
function search() {
  clearTimeout(timer)
  const currentRequest = ++requestId
  const keyword = query.value.trim()
  if (!keyword) {
    results.value = []
    return
  }
  timer = setTimeout(async () => {
    try {
      const value = await api.search(keyword)
      if (currentRequest === requestId) results.value = value?.records || value || []
    } catch {
      if (currentRequest === requestId) results.value = []
    }
  }, 180)
}
onBeforeUnmount(() => {
  clearTimeout(timer)
  requestId += 1
})
</script>

<template>
  <AppDialog :open="true" title="搜索文章" @close="$emit('close')">
    <div class="search-input">
      <Search :size="20" /><input
        v-model="query"
        autofocus
        aria-label="搜索关键词"
        placeholder="搜索标题、正文或标签"
        @input="search"
      />
    </div>
    <div class="search-results">
      <RouterLink
        v-for="post in results"
        :key="post.id"
        :to="`/post/${post.id}`"
        @click="$emit('close')"
        ><span>{{ postTitle(post) }}</span
        ><ArrowUpRight :size="17"
      /></RouterLink>
      <p v-if="query && !results.length" class="muted">没有匹配的文章</p>
    </div>
  </AppDialog>
</template>
