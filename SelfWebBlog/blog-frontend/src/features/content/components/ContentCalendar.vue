<script setup>
import { computed } from 'vue'

const props = defineProps({
  year: { type: Number, required: true },
  month: { type: Number, required: true },
  days: { type: Array, default: () => [] }
})
defineEmits(['select'])

const entries = computed(() => {
  const first = new Date(props.year, props.month - 1, 1)
  const total = new Date(props.year, props.month, 0).getDate()
  const offset = (first.getDay() + 6) % 7
  const byDay = new Map(props.days.map((entry) => [Number(String(entry.date).slice(-2)), entry]))
  return [
    ...Array.from({ length: offset }, (_, index) => ({ key: `blank-${index}`, blank: true })),
    ...Array.from({ length: total }, (_, index) => ({
      key: `day-${index + 1}`,
      day: index + 1,
      entry: byDay.get(index + 1) || null
    }))
  ]
})
</script>

<template>
  <div class="calendar-shell">
    <div class="calendar-weekdays" aria-hidden="true">
      <span v-for="label in ['一', '二', '三', '四', '五', '六', '日']" :key="label">{{
        label
      }}</span>
    </div>
    <div class="calendar-grid">
      <template v-for="item in entries" :key="item.key">
        <span v-if="item.blank" class="calendar-blank"></span>
        <button
          v-else
          type="button"
          :class="{ active: item.entry }"
          :disabled="!item.entry"
          @click="$emit('select', item.entry)"
        >
          <strong>{{ item.day }}</strong>
          <span v-if="item.entry">{{ item.entry.count }} 篇</span>
        </button>
      </template>
    </div>

    <div class="calendar-list">
      <button v-for="entry in days" :key="entry.date" type="button" @click="$emit('select', entry)">
        <time>{{ entry.date }}</time>
        <span>{{ entry.count }} 篇文章</span>
      </button>
    </div>
  </div>
</template>

<style scoped>
.calendar-weekdays,
.calendar-grid {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
}
.calendar-weekdays {
  border-block: 1px solid var(--border-strong);
}
.calendar-weekdays span {
  padding: 10px;
  color: var(--text-tertiary);
  font-size: 0.7rem;
  text-align: center;
}
.calendar-grid {
  border-left: 1px solid var(--border-subtle);
}
.calendar-grid > button,
.calendar-blank {
  min-height: 106px;
  border: 0;
  border-right: 1px solid var(--border-subtle);
  border-bottom: 1px solid var(--border-subtle);
  background: transparent;
}
.calendar-grid > button {
  display: grid;
  align-content: space-between;
  justify-items: start;
  padding: 11px;
  color: var(--text-tertiary);
  font: inherit;
  cursor: default;
}
.calendar-grid > button.active {
  background: var(--surface-glass);
  color: var(--text-primary);
  cursor: pointer;
}
.calendar-grid > button.active:hover {
  background: var(--accent-soft);
}
.calendar-grid strong {
  font-family: var(--font-reading);
  font-size: 1.05rem;
}
.calendar-grid span {
  color: var(--accent-strong);
  font-size: 0.68rem;
}
.calendar-list {
  display: none;
}
@media (max-width: 680px) {
  .calendar-weekdays,
  .calendar-grid {
    display: none;
  }
  .calendar-list {
    display: grid;
    border-top: 1px solid var(--border-strong);
  }
  .calendar-list button {
    min-height: 58px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
    padding: 0 4px;
    border: 0;
    border-bottom: 1px solid var(--border-subtle);
    background: transparent;
    color: var(--text-primary);
    font: inherit;
    cursor: pointer;
  }
  .calendar-list span {
    color: var(--text-tertiary);
    font-size: 0.72rem;
  }
}
</style>
