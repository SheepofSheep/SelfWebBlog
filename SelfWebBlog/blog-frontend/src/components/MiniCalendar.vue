<script setup>
import { ref, computed } from 'vue'
import { ChevronLeft, ChevronRight } from 'lucide-vue-next'
import { navigate } from '../router'

const props = defineProps({
  posts: { type: Array, default: () => [] } // post objects with id, title, createTime
})

const today = new Date()
const current = ref(new Date(today.getFullYear(), today.getMonth(), 1))
const selectedDay = ref(null)

const year = computed(() => current.value.getFullYear())
const month = computed(() => current.value.getMonth())

const monthLabel = computed(() => `${year.value}年${month.value + 1}月`)

const postsByDay = computed(() => {
  const map = {}
  props.posts.forEach((p) => {
    if (!p.createTime) return
    const d = new Date(p.createTime)
    if (d.getFullYear() === year.value && d.getMonth() === month.value) {
      const day = d.getDate()
      if (!map[day]) map[day] = []
      map[day].push(p)
    }
  })
  return map
})

const selectedPosts = computed(() => {
  if (selectedDay.value === null) return []
  return postsByDay.value[selectedDay.value] || []
})

const weeks = computed(() => {
  const firstDay = new Date(year.value, month.value, 1)
  const lastDay = new Date(year.value, month.value + 1, 0)
  const startPad = firstDay.getDay()
  const days = []

  for (let i = 0; i < startPad; i++) days.push(null)
  for (let d = 1; d <= lastDay.getDate(); d++) days.push(d)

  const todayDate = today.getDate()

  const rows = []
  for (let i = 0; i < days.length; i += 7) {
    rows.push(
      days.slice(i, i + 7).map((d) => {
        if (d === null) return { num: '', isToday: false, hasPost: false, active: false }
        const isToday =
          d === todayDate && month.value === today.getMonth() && year.value === today.getFullYear()
        const hasPost = !!postsByDay.value[d]
        const active = selectedDay.value === d
        return { num: d, isToday, hasPost, active }
      })
    )
  }
  return rows
})

function clickDay(d) {
  if (!d || !d.hasPost || !d.num) return
  selectedDay.value = selectedDay.value === d.num ? null : Number(d.num)
}

function prevMonth() {
  current.value = new Date(year.value, month.value - 1, 1)
  selectedDay.value = null
}
function nextMonth() {
  current.value = new Date(year.value, month.value + 1, 1)
  selectedDay.value = null
}

function openPost(id) {
  navigate(`/post?id=${encodeURIComponent(String(id))}`)
}
</script>

<template>
  <div class="mini-calendar glass-card">
    <div class="cal-head">
      <button class="cal-nav" @click="prevMonth" aria-label="上个月">
        <ChevronLeft :size="14" />
      </button>
      <span class="cal-title">{{ monthLabel }}</span>
      <button class="cal-nav" @click="nextMonth" aria-label="下个月">
        <ChevronRight :size="14" />
      </button>
    </div>
    <div class="cal-grid">
      <span v-for="d in '日一二三四五六'" :key="d" class="cal-dow">{{ d }}</span>
      <span
        v-for="(cell, i) in weeks.flat()"
        :key="i"
        class="cal-day"
        :class="{
          today: cell.isToday,
          'has-post': cell.hasPost,
          active: cell.active,
          empty: !cell.num,
          clickable: cell.hasPost
        }"
        @click="clickDay(cell)"
        >{{ cell.num }}</span
      >
    </div>

    <!-- 选中日期的文章列表 -->
    <Transition name="cal-fade">
      <div v-if="selectedPosts.length" class="cal-posts">
        <div class="cal-posts-head">
          {{ monthLabel }} {{ selectedDay }}日 · {{ selectedPosts.length }} 篇
        </div>
        <div v-for="p in selectedPosts" :key="p.id" class="cal-post-item" @click="openPost(p.id)">
          <span class="cal-post-title">{{ p.title }}</span>
        </div>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.mini-calendar {
  padding: 14px;
  margin-top: 16px;
  user-select: none;
}

.cal-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}
.cal-title {
  font-size: 0.78rem;
  font-weight: 600;
  color: var(--text);
}
.cal-nav {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border: none;
  border-radius: 50%;
  background: transparent;
  color: var(--text-muted);
  cursor: pointer;
  transition:
    background var(--duration-fast),
    color var(--duration-fast);
}
.cal-nav:hover {
  background: var(--primary-soft);
  color: var(--primary-hover);
}

.cal-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 2px;
  text-align: center;
}
.cal-dow {
  font-size: 0.6rem;
  color: var(--text-faint);
  padding: 4px 0;
}
.cal-day {
  font-size: 0.7rem;
  color: var(--text-muted);
  padding: 4px 0;
  border-radius: 50%;
  transition: background var(--duration-fast);
}
.cal-day.empty {
  color: transparent;
}
.cal-day.today {
  background: var(--primary);
  color: var(--on-primary);
  font-weight: 600;
}
.cal-day.has-post {
  position: relative;
}
.cal-day.has-post::after {
  content: '';
  position: absolute;
  bottom: 1px;
  left: 50%;
  transform: translateX(-50%);
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: var(--primary);
}
.cal-day.today.has-post::after {
  background: rgba(255, 255, 255, 0.8);
}
.cal-day.clickable {
  cursor: pointer;
}
.cal-day.clickable:hover {
  background: var(--primary-soft);
}
.cal-day.active {
  background: var(--primary-soft);
  font-weight: 600;
  color: var(--primary-hover);
}

/* ═══ 文章列表 ═══ */
.cal-posts {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid var(--border-light);
}
.cal-posts-head {
  font-size: 0.68rem;
  color: var(--text-muted);
  font-weight: 600;
  margin-bottom: 6px;
}
.cal-post-item {
  padding: 5px 0;
  cursor: pointer;
  border-bottom: 1px solid var(--border-light);
  transition: color var(--duration-fast);
}
.cal-post-item:last-child {
  border-bottom: none;
}
.cal-post-item:hover {
  color: var(--primary-hover);
}
.cal-post-title {
  font-size: 0.72rem;
  color: var(--text);
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* ═══ Transition ═══ */
.cal-fade-enter-active {
  transition:
    opacity 0.25s var(--ease-out),
    max-height 0.25s var(--ease-out);
}
.cal-fade-leave-active {
  transition:
    opacity 0.15s var(--ease-out),
    max-height 0.15s var(--ease-out);
}
.cal-fade-enter-from,
.cal-fade-leave-to {
  opacity: 0;
  max-height: 0;
  overflow: hidden;
}
.cal-fade-enter-to,
.cal-fade-leave-from {
  max-height: 200px;
}
</style>
