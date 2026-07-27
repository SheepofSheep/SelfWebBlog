<script setup>
import { computed } from 'vue'
import { ArrowUpRight } from 'lucide-vue-next'
import {
  firstPostMediaUrl,
  formatDate,
  postPublicationTime,
  postTags,
  postTitle
} from '@/core/format.js'

const props = defineProps({ post: { type: Object, required: true }, featured: Boolean })
const tags = computed(() => postTags(props.post))
const displayTitle = computed(() => postTitle(props.post))
const bodyImage = computed(() => {
  return firstPostMediaUrl(props.post.content)
})
const background = computed(() => {
  const p = props.post
  if (p.backgroundMode === 'CUSTOM_IMAGE' && p.backgroundValue)
    return {
      backgroundImage: `url("${p.backgroundValue}")`,
      backgroundPosition: `${p.backgroundFocusX ?? 50}% ${p.backgroundFocusY ?? 50}%`
    }
  if (
    (p.backgroundMode === 'COVER' || !p.backgroundMode) &&
    (p.coverUrl || p.imageUrl || bodyImage.value)
  )
    return {
      backgroundImage: `url("${p.coverUrl || p.imageUrl || bodyImage.value}")`,
      backgroundPosition: `${p.backgroundFocusX ?? 50}% ${p.backgroundFocusY ?? 50}%`
    }
  if (p.backgroundMode === 'SOLID') {
    const hex = String(p.backgroundValue || '#dcecff').replace('#', '')
    const rgb = [0, 2, 4].map((index) => Number.parseInt(hex.slice(index, index + 2), 16))
    const light = (rgb[0] * 299 + rgb[1] * 587 + rgb[2] * 114) / 1000 > 150
    return { backgroundColor: p.backgroundValue, color: light ? '#10243d' : '#ffffff' }
  }
  return {}
})
const preset = computed(() =>
  props.post.backgroundMode === 'GRADIENT' ? `preset-${props.post.backgroundValue}` : ''
)
</script>

<template>
  <RouterLink
    :to="`/post/${post.id}`"
    :class="['post-card', 'glass', { featured }, preset]"
    :style="background"
  >
    <div class="post-card-scrim" />
    <div class="post-card-content">
      <div class="post-meta">
        <span>{{ formatDate(postPublicationTime(post)) }}</span
        ><span v-if="post.category">{{ post.category }}</span>
      </div>
      <h2 v-if="displayTitle">{{ displayTitle }}</h2>
      <p v-if="post.summary">{{ post.summary }}</p>
      <div class="post-card-foot">
        <div class="tag-row">
          <span v-for="tag in tags.slice(0, 3)" :key="tag">{{ tag }}</span>
        </div>
        <ArrowUpRight :size="20" />
      </div>
    </div>
  </RouterLink>
</template>
