<script setup>
import { computed } from 'vue'
import { parseComment } from '@/core/emojiCatalog.js'

const props = defineProps({ content: { type: String, default: '' } })
const segments = computed(() => parseComment(props.content))
</script>

<template>
  <div class="comment-body">
    <template v-for="(segment, index) in segments" :key="`${segment.kind}-${index}`">
      <span v-if="segment.kind === 'text'">{{ segment.text }}</span>
      <img
        v-else
        class="comment-sticker"
        :src="segment.src"
        :alt="`表情：${segment.name}`"
        :title="segment.name"
        loading="lazy"
      />
    </template>
  </div>
</template>

<style scoped>
.comment-body {
  color: var(--ink);
  line-height: 1.75;
  overflow-wrap: anywhere;
  white-space: pre-wrap;
}
.comment-sticker {
  display: inline-block;
  width: clamp(64px, 12vw, 94px);
  height: auto;
  margin: 5px 7px 5px 2px;
  border-radius: 15px;
  vertical-align: middle;
  box-shadow: 0 9px 24px rgb(16 36 61 / 12%);
}
</style>
