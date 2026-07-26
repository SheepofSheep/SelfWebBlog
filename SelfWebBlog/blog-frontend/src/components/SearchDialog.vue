<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { Search, X, ArrowUpRight } from 'lucide-vue-next'
import { api } from '@/api/client.js'
import { postTitle } from '@/core/format.js'

defineEmits(['close'])
const query = ref('')
const results = ref([])
const input = ref()
let timer
let requestId = 0
onMounted(() => input.value?.focus())
function search() {
  clearTimeout(timer)
  timer = setTimeout(async () => {
    const keyword = query.value.trim()
    const currentRequest = ++requestId
    if (!keyword) return (results.value = [])
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
  <div class="dialog-backdrop" @click.self="$emit('close')" @keydown.esc="$emit('close')">
    <section class="search-dialog glass" role="dialog" aria-modal="true" aria-label="搜索文章">
      <div class="search-input">
        <Search :size="20" /><input
          ref="input"
          v-model="query"
          placeholder="搜索标题、正文或标签"
          @input="search"
        /><button class="icon-button" aria-label="关闭" @click="$emit('close')">
          <X :size="19" />
        </button>
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
    </section>
  </div>
</template>
