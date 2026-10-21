<script setup>
import { onBeforeUnmount, ref } from 'vue'
import { Search, ArrowUpRight, X } from 'lucide-vue-next'
import { api } from '@/api/client.js'
import { postTitle } from '@/core/format.js'
import AppDialog from './AppDialog.vue'

defineEmits(['close'])
const query = ref('')
const input = ref(null)
const results = ref([])
const loading = ref(false)
const error = ref('')
let timer
let requestId = 0
function clearSearch() {
  query.value = ''
  search()
  input.value?.focus()
}
function search() {
  clearTimeout(timer)
  const currentRequest = ++requestId
  const keyword = query.value.trim()
  error.value = ''
  results.value = []
  loading.value = Boolean(keyword)
  if (!keyword) {
    results.value = []
    return
  }
  timer = setTimeout(async () => {
    try {
      const value = await api.search(keyword)
      if (currentRequest === requestId) results.value = value?.records || value || []
    } catch (failure) {
      if (currentRequest === requestId) error.value = failure.message || '搜索失败，请重试'
    } finally {
      if (currentRequest === requestId) loading.value = false
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
        ref="input"
        v-model="query"
        autofocus
        aria-label="搜索关键词"
        placeholder="搜索标题、正文或标签"
        @input="search"
      />
      <button
        v-if="query"
        type="button"
        class="icon-button"
        aria-label="清空搜索"
        @click="clearSearch"
      >
        <X :size="16" />
      </button>
    </div>
    <div class="search-results" :aria-busy="loading">
      <p v-if="loading" class="muted" aria-live="polite">正在搜索…</p>
      <div v-else-if="error" role="alert">
        <p class="error-text">{{ error }}</p>
        <button type="button" class="secondary-button" @click="search">重试搜索</button>
      </div>
      <RouterLink
        v-for="post in results"
        :key="post.id"
        :to="`/post/${post.id}`"
        @click="$emit('close')"
        ><span>{{ postTitle(post) }}</span
        ><ArrowUpRight :size="17"
      /></RouterLink>
      <p v-if="!loading && !error && query.trim() && !results.length" class="muted">
        没有匹配的文章，可以试试更短的关键词
      </p>
      <p v-if="!query.trim()" class="muted">输入标题、正文或标签关键词，按 Esc 关闭</p>
    </div>
  </AppDialog>
</template>
