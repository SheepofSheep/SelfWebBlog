<script setup>
import { computed } from 'vue'
import DOMPurify from 'dompurify'
import { editorMediaUrlPattern } from '@/core/markdownPolicy.js'
import {
  markdownParser,
  markdownSanitizeOptions,
  cleanMarkdownTree
} from '@/core/markdownRenderer.js'
import { notify } from '@/stores/app.js'
const props = defineProps({
  content: { type: String, default: '' },
  allowLocalMedia: Boolean
})
const html = computed(() => {
  const fragment = DOMPurify.sanitize(markdownParser.parse(props.content || ''), {
    ...markdownSanitizeOptions,
    RETURN_DOM_FRAGMENT: true,
    ...(props.allowLocalMedia ? { ALLOWED_URI_REGEXP: editorMediaUrlPattern } : {})
  })
  const root = document.createElement('div')
  root.append(cleanMarkdownTree(fragment))
  root.querySelectorAll('pre').forEach((pre) => {
    const button = document.createElement('button')
    button.type = 'button'
    button.className = 'code-copy'
    button.textContent = '复制'
    button.setAttribute('aria-label', '复制代码')
    pre.append(button)
  })
  return root.innerHTML
})
async function copyCode(event) {
  const button = event.target.closest('.code-copy')
  if (!button) return
  event.stopPropagation()
  try {
    await navigator.clipboard.writeText(
      button.parentElement.querySelector('code')?.textContent || ''
    )
    notify('代码已复制')
  } catch {
    notify('复制失败，请手动选择代码')
  }
}
</script>
<template><article class="markdown-body" @click="copyCode" v-html="html" /></template>
