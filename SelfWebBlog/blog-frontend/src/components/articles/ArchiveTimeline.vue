<script setup>
import { computed } from 'vue'
import { ArrowUpRight, Clock, Folder } from 'lucide-vue-next'
import { formatTime, getFirstImageUrl } from '../../utils/format'
import { toAbsoluteUrl } from '../../utils/url'

const props = defineProps({ posts: { type: Array, default: () => [] } })
defineEmits(['open'])

const groups = computed(() => {
  const grouped = new Map()
  for (const post of props.posts) {
    const date = new Date(post.createTime)
    const validDate = !Number.isNaN(date.getTime())
    const key = validDate
      ? `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`
      : 'unknown'
    if (!grouped.has(key)) {
      grouped.set(key, {
        key,
        year: validDate ? date.getFullYear() : '未定',
        month: validDate ? `${date.getMonth() + 1} 月` : '时间未定',
        posts: []
      })
    }
    grouped.get(key).posts.push(post)
  }
  return [...grouped.values()]
})

function imageUrl(post) {
  return toAbsoluteUrl(post.coverUrl || getFirstImageUrl(post.content || '') || '')
}
</script>

<template>
  <div class="archive-timeline">
    <section v-for="group in groups" :key="group.key" class="timeline-group">
      <header>
        <strong>{{ group.year }}</strong>
        <span>{{ group.month }}</span>
      </header>
      <div class="timeline-list">
        <button
          v-for="post in group.posts"
          :key="post.id"
          class="timeline-row"
          type="button"
          @click="$emit('open', post.id)"
        >
          <span class="timeline-date">{{ formatTime(post.createTime).slice(5, 11) }}</span>
          <span class="timeline-copy">
            <strong>{{ post.title }}</strong>
            <small>{{ post.summary || '打开文章继续阅读' }}</small>
          </span>
          <span class="timeline-meta">
            <span v-if="post.category"><Folder :size="13" /> {{ post.category }}</span>
            <span><Clock :size="13" /> {{ formatTime(post.createTime).slice(-5) }}</span>
          </span>
          <span v-if="imageUrl(post)" class="timeline-thumb">
            <img :src="imageUrl(post)" :alt="post.title" loading="lazy" />
          </span>
          <ArrowUpRight class="timeline-arrow" :size="18" />
        </button>
      </div>
    </section>
  </div>
</template>

<style scoped>
.archive-timeline {
  display: grid;
  gap: 44px;
}
.timeline-group {
  display: grid;
  grid-template-columns: 150px minmax(0, 1fr);
  gap: 26px;
}
.timeline-group > header {
  position: sticky;
  top: 96px;
  align-self: start;
  display: grid;
  gap: 2px;
}
.timeline-group > header strong {
  font-family: var(--font-serif);
  font-size: 2.4rem;
  font-weight: 500;
}
.timeline-group > header span {
  color: var(--primary-hover);
  font-size: 0.78rem;
  font-weight: 900;
}
.timeline-list {
  border-top: 1px solid var(--border-medium);
}
.timeline-row {
  width: 100%;
  display: grid;
  grid-template-columns: 72px minmax(0, 1fr) auto 88px 24px;
  align-items: center;
  gap: 18px;
  min-height: 112px;
  padding: 14px 8px;
  border: 0;
  border-bottom: 1px solid var(--border-light);
  background: transparent;
  color: var(--text-main);
  text-align: left;
  cursor: pointer;
  transition:
    padding var(--motion-normal),
    background var(--motion-normal);
}
.timeline-row:hover {
  padding-inline: 16px;
  background: var(--surface-muted);
}
.timeline-date {
  color: var(--primary);
  font-family: var(--font-mono);
  font-size: 0.78rem;
}
.timeline-copy {
  display: grid;
  min-width: 0;
  gap: 5px;
}
.timeline-copy strong {
  overflow: hidden;
  font-family: var(--font-serif);
  font-size: 1.16rem;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.timeline-copy small {
  overflow: hidden;
  color: var(--text-muted);
  font-size: 0.78rem;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.timeline-meta {
  display: flex;
  gap: 12px;
  color: var(--text-muted);
  font-size: 0.7rem;
}
.timeline-meta span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.timeline-thumb {
  width: 88px;
  height: 64px;
  overflow: hidden;
  border-radius: 6px;
}
.timeline-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.timeline-arrow {
  color: var(--text-muted);
}
@media (max-width: 820px) {
  .timeline-group {
    grid-template-columns: 1fr;
    gap: 12px;
  }
  .timeline-group > header {
    position: static;
    display: flex;
    align-items: baseline;
    gap: 8px;
  }
  .timeline-row {
    grid-template-columns: 52px minmax(0, 1fr) 70px;
    min-height: 94px;
    gap: 12px;
  }
  .timeline-meta,
  .timeline-arrow {
    display: none;
  }
  .timeline-thumb {
    width: 70px;
    height: 58px;
  }
}
</style>
