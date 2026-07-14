<script setup>
import { Activity, BookOpen, CalendarDays } from 'lucide-vue-next'
import { computed } from 'vue'
import { formatTime } from '../../../utils/format'

const props = defineProps({
  posts: { type: Array, default: () => [] },
  totalPosts: { type: Number, default: 0 }
})

const firstDate = computed(() => props.posts.at(-1)?.createTime)
const latestDate = computed(() => props.posts[0]?.createTime)
const runningDays = computed(() => {
  if (!firstDate.value) return 1
  return Math.max(1, Math.ceil((Date.now() - new Date(firstDate.value).getTime()) / 86400000))
})
</script>

<template>
  <section class="site-stats">
    <h2>站点概况</h2>
    <dl>
      <div>
        <BookOpen :size="16" />
        <dt>公开文章</dt>
        <dd>{{ totalPosts }}</dd>
      </div>
      <div>
        <CalendarDays :size="16" />
        <dt>记录天数</dt>
        <dd>{{ runningDays }}</dd>
      </div>
      <div>
        <Activity :size="16" />
        <dt>最近活动</dt>
        <dd>{{ latestDate ? formatTime(latestDate) : '暂无' }}</dd>
      </div>
    </dl>
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
dl {
  margin: 0;
  border-top: 1px solid var(--border-subtle);
}
dl div {
  min-height: 48px;
  display: grid;
  grid-template-columns: 22px minmax(0, 1fr) auto;
  align-items: center;
  gap: 7px;
  border-bottom: 1px solid var(--border-subtle);
  color: var(--text-tertiary);
}
dt,
dd {
  margin: 0;
  font-size: 0.74rem;
}
dd {
  color: var(--text-primary);
  font-weight: 700;
  text-align: right;
}
</style>
