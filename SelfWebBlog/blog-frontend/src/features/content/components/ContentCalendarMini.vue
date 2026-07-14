<script setup>
import { CalendarDays } from 'lucide-vue-next'
import { computed } from 'vue'

const props = defineProps({ posts: { type: Array, default: () => [] } })
defineEmits(['open'])

const now = new Date()
const year = now.getFullYear()
const month = now.getMonth()
const monthLabel = `${year} 年 ${month + 1} 月`
const firstWeekday = new Date(year, month, 1).getDay()
const daysInMonth = new Date(year, month + 1, 0).getDate()
const activeDays = computed(() => {
  const result = new Set()
  props.posts.forEach((post) => {
    const date = new Date(post.createTime)
    if (date.getFullYear() === year && date.getMonth() === month) result.add(date.getDate())
  })
  return result
})
const days = computed(() => [
  ...Array.from({ length: firstWeekday }, () => null),
  ...Array.from({ length: daysInMonth }, (_, index) => index + 1)
])
</script>

<template>
  <section class="mini-calendar">
    <header>
      <span><CalendarDays :size="15" />内容日历</span>
      <button type="button" @click="$emit('open')">{{ monthLabel }}</button>
    </header>
    <div class="weekdays" aria-hidden="true">
      <span v-for="day in '日一二三四五六'" :key="day">{{ day }}</span>
    </div>
    <div class="days">
      <span
        v-for="(day, index) in days"
        :key="`${index}-${day}`"
        :class="{ active: activeDays.has(day) }"
      >
        {{ day || '' }}
      </span>
    </div>
  </section>
</template>

<style scoped>
.mini-calendar {
  padding-top: 4px;
}
header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}
header span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 0.8rem;
  font-weight: 800;
}
header button {
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--text-tertiary);
  font: inherit;
  font-size: 0.68rem;
  cursor: pointer;
}
.weekdays,
.days {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
}
.weekdays span,
.days span {
  min-height: 28px;
  display: grid;
  place-items: center;
  color: var(--text-tertiary);
  font-size: 0.66rem;
}
.days span.active {
  border-radius: var(--radius-control);
  background: var(--accent-soft);
  color: var(--accent-strong);
  font-weight: 800;
}
</style>
