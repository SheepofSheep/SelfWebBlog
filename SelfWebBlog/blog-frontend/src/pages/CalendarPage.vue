<script setup>
import { ChevronLeft, ChevronRight } from 'lucide-vue-next'
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getContentCalendar } from '../api'
import ContentCalendar from '../features/content/components/ContentCalendar.vue'

const router = useRouter()
const cursor = ref(new Date())
const data = ref({ days: [], postCount: 0 })
const loading = ref(true)
const selected = ref(null)
const title = computed(() => `${cursor.value.getFullYear()} 年 ${cursor.value.getMonth() + 1} 月`)

async function load() {
  loading.value = true
  selected.value = null
  try {
    data.value = await getContentCalendar(cursor.value.getFullYear(), cursor.value.getMonth() + 1)
  } finally {
    loading.value = false
  }
}

async function moveMonth(delta) {
  cursor.value = new Date(cursor.value.getFullYear(), cursor.value.getMonth() + delta, 1)
  await load()
}

onMounted(load)
</script>

<template>
  <div class="calendar-page page-width">
    <header>
      <div>
        <span>Archive Calendar</span>
        <h1>内容日历</h1>
      </div>
      <div class="month-control">
        <button type="button" aria-label="上个月" title="上个月" @click="moveMonth(-1)">
          <ChevronLeft :size="18" />
        </button>
        <strong>{{ title }}</strong>
        <button type="button" aria-label="下个月" title="下个月" @click="moveMonth(1)">
          <ChevronRight :size="18" />
        </button>
      </div>
    </header>

    <div v-if="loading" class="calendar-loading loading-shimmer"></div>
    <ContentCalendar
      v-else
      :year="cursor.getFullYear()"
      :month="cursor.getMonth() + 1"
      :days="data.days"
      @select="selected = $event"
    />

    <section v-if="selected" class="selected-day">
      <h2>
        {{ selected.date }} <small>{{ selected.count }} 篇</small>
      </h2>
      <button
        v-for="post in selected.posts"
        :key="post.id"
        type="button"
        @click="router.push(`/post/${post.id}`)"
      >
        <strong>{{ post.title }}</strong>
        <span>{{ post.summary || post.category || '阅读文章' }}</span>
      </button>
    </section>
  </div>
</template>

<style scoped>
.calendar-page {
  padding-block: clamp(42px, 7vw, 78px);
}
header {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 30px;
}
header span {
  color: var(--accent-strong);
  font-size: 0.72rem;
  font-weight: 800;
}
h1 {
  margin: 8px 0 0;
  font-family: var(--font-reading);
  font-size: clamp(2.2rem, 6vw, 4.4rem);
  font-weight: 600;
}
.month-control {
  display: grid;
  grid-template-columns: 38px minmax(120px, auto) 38px;
  align-items: center;
  gap: 6px;
}
.month-control button {
  width: 38px;
  height: 38px;
  display: grid;
  place-items: center;
  padding: 0;
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-control);
  background: transparent;
  color: var(--text-primary);
  cursor: pointer;
}
.month-control strong {
  font-size: 0.82rem;
  text-align: center;
}
.calendar-loading {
  height: 530px;
  border-radius: var(--radius-card);
}
.selected-day {
  max-width: 760px;
  margin: 38px auto 0;
  padding-top: 24px;
  border-top: 1px solid var(--border-strong);
}
.selected-day h2 {
  margin: 0 0 12px;
  font-family: var(--font-reading);
  font-size: 1.3rem;
}
.selected-day h2 small {
  color: var(--text-tertiary);
  font-family: var(--font-interface);
  font-size: 0.7rem;
  font-weight: 400;
}
.selected-day > button {
  width: 100%;
  min-height: 64px;
  display: grid;
  align-content: center;
  gap: 5px;
  padding: 8px 4px;
  border: 0;
  border-bottom: 1px solid var(--border-subtle);
  background: transparent;
  color: var(--text-primary);
  font: inherit;
  text-align: left;
  cursor: pointer;
}
.selected-day span {
  color: var(--text-tertiary);
  font-size: 0.72rem;
}
@media (max-width: 620px) {
  header {
    align-items: start;
    flex-direction: column;
  }
}
</style>
