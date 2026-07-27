<script setup>
import { computed } from 'vue'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import { editorMediaUrlPattern } from '@/core/markdownPolicy.js'
const props = defineProps({
  content: { type: String, default: '' },
  allowLocalMedia: Boolean
})
marked.setOptions({ gfm: true, breaks: true })
const html = computed(() =>
  DOMPurify.sanitize(marked.parse(props.content || ''), {
    ADD_TAGS: ['video', 'audio'],
    ADD_ATTR: ['controls', 'playsinline'],
    ...(props.allowLocalMedia ? { ALLOWED_URI_REGEXP: editorMediaUrlPattern } : {})
  })
)
</script>
<template><article class="markdown-body" v-html="html" /></template>
