<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { CalendarDays, ChevronLeft, ChevronRight, Maximize2, RotateCcw, X } from 'lucide-vue-next'
import { api } from '@/api/client.js'
import {
  buildMonthCells,
  createCalendarRequestGate,
  currentCalendarMonth,
  selectableCalendarDate,
  shiftCalendarMonth
} from '@/core/calendarModel.js'
import { postTitle } from '@/core/format.js'
import AppDialog from './AppDialog.vue'

const props = defineProps({ initialOpen: Boolean })
const emit = defineEmits(['close'])
const today = new Date()
const cursor = ref({ year: today.getFullYear(), month: today.getMonth() + 1 })
const data = ref({ days: [] })
const loading = ref(false)
const error = ref('')
const expanded = ref(false)
const selectedDate = ref('')
const compactPopoverOpen = ref(false)
const openedFromRoute = ref(false)
const requestGate = createCalendarRequestGate()
const weekdays = ['一', '二', '三', '四', '五', '六', '日']

const cells = computed(() =>
  buildMonthCells(cursor.value.year, cursor.value.month, data.value.days || [])
)
const selectedDay = computed(
  () => cells.value.find((cell) => cell?.date === selectedDate.value) || null
)
const monthLabel = computed(() => `${cursor.value.year} · ${cursor.value.month}`)

async function load() {
  const version = requestGate.begin()
  loading.value = true
  error.value = ''
  try {
    const result = (await api.calendar(cursor.value.year, cursor.value.month)) || { days: [] }
    if (!requestGate.isCurrent(version)) return
    data.value = { ...result, days: Array.isArray(result.days) ? result.days : [] }
    const selectedInMonth = cells.value.some((cell) => cell?.date === selectedDate.value)
    if (!selectedInMonth) {
      selectedDate.value =
        cells.value.find((cell) => cell?.count)?.date || cells.value.find(Boolean)?.date || ''
    }
  } catch (loadError) {
    if (!requestGate.isCurrent(version)) return
    error.value = loadError?.message || '日历加载失败'
    data.value = { days: [] }
    compactPopoverOpen.value = false
  } finally {
    if (requestGate.isCurrent(version)) loading.value = false
  }
}

function move(delta) {
  compactPopoverOpen.value = false
  cursor.value = shiftCalendarMonth(cursor.value.year, cursor.value.month, delta)
  load()
}

function goCurrentMonth() {
  compactPopoverOpen.value = false
  const current = currentCalendarMonth()
  if (cursor.value.year === current.year && cursor.value.month === current.month) return
  cursor.value = current
  load()
}

function openDate(cell) {
  const date = selectableCalendarDate(cell)
  if (!date) return
  selectedDate.value = date
  compactPopoverOpen.value = true
}

function openExpanded() {
  if (!selectedDate.value) selectedDate.value = cells.value.find(Boolean)?.date || ''
  compactPopoverOpen.value = false
  expanded.value = true
}

function closeExpanded() {
  expanded.value = false
  emit('close')
}

function closeCompactPopover() {
  compactPopoverOpen.value = false
}

function onWidgetKeydown(event) {
  if (event.key !== 'Escape' || !compactPopoverOpen.value) return
  event.preventDefault()
  closeCompactPopover()
}

watch(
  () => props.initialOpen,
  (open) => {
    if (open) {
      openedFromRoute.value = true
      openExpanded()
    } else if (openedFromRoute.value) {
      openedFromRoute.value = false
      expanded.value = false
    }
  },
  { immediate: true }
)

onMounted(load)
</script>

<template>
  <section class="calendar-widget glass" aria-label="文章日历" @keydown="onWidgetKeydown">
    <header class="calendar-widget-head">
      <h2>日历</h2>
      <div class="calendar-widget-controls">
        <button type="button" aria-label="上个月" @click="move(-1)">
          <ChevronLeft :size="16" />
        </button>
        <strong>{{ monthLabel }}</strong>
        <button type="button" aria-label="下个月" @click="move(1)">
          <ChevronRight :size="16" />
        </button>
        <button type="button" aria-label="回到本月" title="回到本月" @click="goCurrentMonth">
          <CalendarDays :size="15" />
        </button>
        <button type="button" aria-label="放大日历" @click="openExpanded">
          <Maximize2 :size="15" />
        </button>
      </div>
    </header>
    <div v-if="error" class="calendar-widget-error">
      <span>{{ error }}</span>
      <button type="button" @click="load"><RotateCcw :size="14" />重试</button>
    </div>
    <template v-else>
      <div class="calendar-week" aria-hidden="true">
        <span v-for="day in weekdays" :key="day">{{ day }}</span>
      </div>
      <div :class="['calendar-compact-grid', { loading }]">
        <span v-for="(cell, index) in cells" :key="cell?.date || `blank-${index}`">
          <button
            v-if="cell"
            type="button"
            :class="{ marked: cell.count, selected: cell.date === selectedDate }"
            :disabled="!selectableCalendarDate(cell)"
            :aria-label="`${cell.date}${cell.count ? `，${cell.count} 篇文章` : ''}`"
            @click="openDate(cell)"
          >
            {{ cell.day }}
            <i v-if="cell.count" aria-hidden="true" />
          </button>
        </span>
      </div>
    </template>

    <Transition name="calendar-popover">
      <section
        v-if="compactPopoverOpen && selectedDay?.posts?.length"
        class="calendar-compact-popover glass"
        role="region"
        :aria-label="`${selectedDate} 文章`"
      >
        <header>
          <time>{{ selectedDate }}</time>
          <button type="button" aria-label="关闭日期文章" @click="closeCompactPopover">
            <X :size="15" />
          </button>
        </header>
        <div>
          <RouterLink
            v-for="post in selectedDay.posts"
            :key="post.id"
            :to="`/post/${post.id}`"
            @click="closeCompactPopover"
          >
            {{ postTitle(post) }}
          </RouterLink>
        </div>
      </section>
    </Transition>
  </section>

  <AppDialog :open="expanded" title="日历" size="large" @close="closeExpanded">
    <div v-if="error" class="calendar-expanded-error" role="alert">
      <span>{{ error }}</span>
      <button class="secondary-button" type="button" @click="load">
        <RotateCcw :size="15" />重试
      </button>
    </div>
    <div v-else class="calendar-expanded" :aria-busy="loading">
      <div class="calendar-expanded-main">
        <div class="calendar-expanded-nav">
          <button type="button" aria-label="上个月" @click="move(-1)">
            <ChevronLeft :size="19" />
          </button>
          <strong>{{ monthLabel }}</strong>
          <button type="button" aria-label="下个月" @click="move(1)">
            <ChevronRight :size="19" />
          </button>
          <button type="button" aria-label="回到本月" title="回到本月" @click="goCurrentMonth">
            <CalendarDays :size="18" />
          </button>
        </div>
        <div class="calendar-week" aria-hidden="true">
          <span v-for="day in weekdays" :key="day">{{ day }}</span>
        </div>
        <div class="calendar-large-grid">
          <span v-for="(cell, index) in cells" :key="cell?.date || `large-blank-${index}`">
            <button
              v-if="cell"
              type="button"
              :class="{ marked: cell.count, selected: cell.date === selectedDate }"
              :aria-label="`${cell.date}${cell.count ? `，${cell.count} 篇文章` : ''}`"
              @click="selectedDate = cell.date"
            >
              <b>{{ cell.day }}</b>
              <small v-if="cell.count">{{ cell.count }}</small>
            </button>
          </span>
        </div>
      </div>
      <aside class="calendar-day-posts">
        <time>{{ selectedDate || monthLabel }}</time>
        <div v-if="selectedDay?.posts?.length">
          <RouterLink
            v-for="post in selectedDay.posts"
            :key="post.id"
            :to="`/post/${post.id}`"
            @click="closeExpanded"
          >
            {{ postTitle(post) }}
          </RouterLink>
        </div>
        <p v-else>当日无文章</p>
      </aside>
    </div>
  </AppDialog>
</template>

<style scoped>
.calendar-widget {
  position: relative;
  display: block;
  padding: 20px 22px;
  border-radius: var(--radius-lg);
}
.calendar-widget-head,
.calendar-widget-controls,
.calendar-expanded-nav {
  display: flex;
  align-items: center;
}
.calendar-widget-head {
  gap: 12px;
  justify-content: space-between;
  margin-bottom: 12px;
}
.calendar-widget-head h2 {
  margin: 0;
  font-size: 17px;
}
.calendar-widget-controls {
  gap: 2px;
}
.calendar-widget-controls strong {
  min-width: 58px;
  color: var(--muted);
  font-size: 11px;
  text-align: center;
}
.calendar-widget-controls button,
.calendar-expanded-nav button {
  display: grid;
  width: 30px;
  height: 30px;
  padding: 0;
  place-items: center;
  border: 0;
  border-radius: 10px;
  color: var(--muted);
  background: transparent;
}
.calendar-widget-controls button:hover,
.calendar-expanded-nav button:hover {
  color: var(--accent);
  background: var(--accent-soft);
}
.calendar-week,
.calendar-compact-grid,
.calendar-large-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
}
.calendar-week span {
  display: grid;
  min-height: 24px;
  place-items: center;
  color: var(--muted);
  font-size: 10px;
}
.calendar-compact-grid {
  transition: opacity 160ms ease;
}
.calendar-compact-grid.loading {
  opacity: 0.45;
}
.calendar-compact-grid > span {
  display: grid;
  min-height: 29px;
  place-items: center;
}
.calendar-compact-grid button {
  position: relative;
  display: grid;
  width: 27px;
  height: 27px;
  padding: 0;
  place-items: center;
  border: 0;
  border-radius: 9px;
  color: color-mix(in srgb, var(--ink) 76%, transparent);
  background: transparent;
  font-size: 11px;
}
.calendar-compact-grid button:disabled {
  cursor: default;
}
.calendar-compact-grid button:hover:not(:disabled),
.calendar-compact-grid button.selected {
  color: var(--ink);
  background: var(--accent-soft);
}
.calendar-compact-grid button.marked {
  color: var(--primary-ink);
  background: var(--accent);
  box-shadow: 0 5px 14px color-mix(in srgb, var(--accent) 28%, transparent);
}
.calendar-compact-grid i {
  position: absolute;
  right: 3px;
  bottom: 3px;
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: currentColor;
}
.calendar-compact-popover {
  position: absolute;
  z-index: 20;
  top: calc(100% - 8px);
  right: 12px;
  left: 12px;
  padding: 13px;
  border-radius: 16px;
  box-shadow: var(--shadow);
}
.calendar-compact-popover header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 9px;
}
.calendar-compact-popover time {
  color: var(--muted);
  font-size: 11px;
}
.calendar-compact-popover header button {
  display: grid;
  width: 26px;
  height: 26px;
  padding: 0;
  place-items: center;
  border: 0;
  border-radius: 9px;
  color: var(--muted);
  background: transparent;
}
.calendar-compact-popover header button:hover {
  color: var(--accent);
  background: var(--accent-soft);
}
.calendar-compact-popover div {
  display: grid;
  gap: 6px;
}
.calendar-compact-popover a {
  overflow-wrap: anywhere;
  padding: 8px 10px;
  border-radius: 10px;
  background: var(--glass-strong);
  font-size: 12px;
}
.calendar-compact-popover a:hover {
  color: var(--accent);
  transform: translateX(2px);
}
.calendar-popover-enter-active,
.calendar-popover-leave-active {
  transition:
    opacity 160ms ease,
    transform 180ms ease;
}
.calendar-popover-enter-from,
.calendar-popover-leave-to {
  opacity: 0;
  transform: translateY(-5px) scale(0.98);
}
.calendar-widget-error {
  display: flex;
  min-height: 130px;
  flex-direction: column;
  gap: 10px;
  align-items: center;
  justify-content: center;
  color: var(--muted);
  font-size: 12px;
}
.calendar-widget-error button {
  display: inline-flex;
  gap: 5px;
  align-items: center;
  padding: 7px 10px;
  border: 1px solid var(--line);
  border-radius: 10px;
  color: var(--ink);
  background: var(--glass-strong);
}
.calendar-expanded {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(210px, 0.34fr);
  gap: 24px;
}
.calendar-expanded-error {
  display: grid;
  min-height: 300px;
  place-content: center;
  justify-items: center;
  gap: 12px;
  color: var(--muted);
  text-align: center;
}
.calendar-expanded-error button {
  display: inline-flex;
  gap: 6px;
  align-items: center;
}
.calendar-expanded-nav {
  justify-content: center;
  gap: 12px;
  margin-bottom: 10px;
}
.calendar-expanded-nav strong {
  min-width: 110px;
  text-align: center;
}
.calendar-large-grid {
  overflow: hidden;
  border: 1px solid var(--line);
  border-radius: 20px;
}
.calendar-large-grid > span {
  min-width: 0;
  aspect-ratio: 1;
  border-right: 1px solid var(--line);
  border-bottom: 1px solid var(--line);
}
.calendar-large-grid > span:nth-child(7n) {
  border-right: 0;
}
.calendar-large-grid button {
  position: relative;
  width: 100%;
  height: 100%;
  min-height: 54px;
  padding: 8px;
  border: 0;
  color: var(--muted);
  background: transparent;
  text-align: left;
}
.calendar-large-grid button:hover,
.calendar-large-grid button.selected {
  color: var(--ink);
  background: var(--accent-soft);
}
.calendar-large-grid button.marked::after {
  position: absolute;
  right: 9px;
  bottom: 9px;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--accent);
  content: '';
}
.calendar-large-grid small {
  position: absolute;
  top: 8px;
  right: 8px;
  color: var(--accent);
}
.calendar-day-posts {
  padding: 18px;
  border: 1px solid var(--line);
  border-radius: 20px;
  background: color-mix(in srgb, var(--accent-soft) 45%, transparent);
}
.calendar-day-posts time {
  color: var(--muted);
  font-size: 12px;
}
.calendar-day-posts div {
  display: grid;
  gap: 8px;
  margin-top: 15px;
}
.calendar-day-posts a {
  padding: 10px 12px;
  border-radius: 12px;
  background: var(--glass-strong);
}
.calendar-day-posts a:hover {
  color: var(--accent);
  transform: translateX(3px);
}
.calendar-day-posts p {
  margin: 18px 0 0;
  color: var(--muted);
  font-size: 13px;
}
@media (max-width: 720px) {
  .calendar-compact-popover {
    position: static;
    margin-top: 10px;
  }
  .calendar-expanded {
    grid-template-columns: 1fr;
  }
  .calendar-large-grid button {
    min-height: 42px;
    padding: 6px;
  }
}
</style>
