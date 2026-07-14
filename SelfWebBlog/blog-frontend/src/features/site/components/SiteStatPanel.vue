<script setup>
import { Activity, BookOpen, CalendarDays, Eye, MessageSquareText } from 'lucide-vue-next'
import { computed, onMounted, ref } from 'vue'
import { getSiteSummary } from '../../../api'
import { formatTime } from '../../../utils/format'

const props = defineProps({
  posts: { type: Array, default: () => [] },
  totalPosts: { type: Number, default: 0 }
})

const firstDate = computed(() => props.posts.at(-1)?.createTime)
const latestDate = computed(() => props.posts[0]?.createTime)
const summary = ref(null)
const runningDays = computed(() => {
  if (summary.value) return summary.value.runningDays
  if (!firstDate.value) return 1
  return Math.max(1, Math.ceil((Date.now() - new Date(firstDate.value).getTime()) / 86400000))
})

const activityLabel = computed(() => {
  const value = summary.value?.latestActivity || latestDate.value
  if (!value) return '暂无'
  const days = Math.max(0, Math.floor((Date.now() - new Date(value).getTime()) / 86400000))
  if (days === 0) return '今天'
  if (days === 1) return '1 天前'
  if (days < 30) return `${days} 天前`
  return formatTime(value)
})

onMounted(async () => {
  try {
    summary.value = await getSiteSummary()
  } catch {
    summary.value = null
  }
})
</script>

<template>
  <section class="site-stats">
    <h2>站点概况</h2>
    <div class="stat-list">
      <div>
        <BookOpen :size="16" /><span>公开文章</span
        ><strong>{{ summary?.postCount ?? totalPosts }}</strong>
      </div>
      <div>
        <Eye :size="16" /><span>累计浏览</span><strong>{{ summary?.viewCount ?? 0 }}</strong>
      </div>
      <div>
        <MessageSquareText :size="16" /><span>公开互动</span
        ><strong>{{ summary?.interactionCount ?? 0 }}</strong>
      </div>
      <div>
        <CalendarDays :size="16" /><span>记录天数</span><strong>{{ runningDays }}</strong>
      </div>
      <div>
        <Activity :size="16" /><span>最近活动</span><strong>{{ activityLabel }}</strong>
      </div>
    </div>
  </section>
</template>

<style scoped>
.site-stats {
  padding: 4px 0;
}
h2 {
  margin: 0 0 10px;
  font-size: 0.82rem;
}
.stat-list {
  margin: 0;
  border-top: 1px solid var(--border-subtle);
}
.stat-list > div {
  min-height: 48px;
  display: grid;
  grid-template-columns: 22px minmax(0, 1fr) auto;
  align-items: center;
  gap: 7px;
  border-bottom: 1px solid var(--border-subtle);
  color: var(--text-tertiary);
}
.stat-list span,
.stat-list strong {
  margin: 0;
  font-size: 0.74rem;
}
.stat-list strong {
  color: var(--text-primary);
  font-weight: 700;
  text-align: right;
}
</style>
