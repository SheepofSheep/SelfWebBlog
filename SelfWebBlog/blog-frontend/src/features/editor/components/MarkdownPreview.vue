<script setup>
import { computed } from 'vue'
import { renderArticleMarkdown } from '../../../utils/marked'

const props = defineProps({ content: { type: String, default: '' } })
const rendered = computed(() => renderArticleMarkdown(props.content))
</script>

<template>
  <article v-if="content" class="markdown-preview markdown-body" v-html="rendered"></article>
  <div v-else class="preview-empty">预览会显示在这里。</div>
</template>

<style scoped>
.markdown-preview,
.preview-empty {
  min-height: 520px;
  padding: clamp(22px, 4vw, 48px);
}
.markdown-preview {
  color: var(--text-primary);
  font-family: var(--font-reading);
  font-size: 1rem;
  line-height: 1.95;
}
.preview-empty {
  display: grid;
  place-items: center;
  color: var(--text-tertiary);
  font-size: 0.84rem;
}
</style>
