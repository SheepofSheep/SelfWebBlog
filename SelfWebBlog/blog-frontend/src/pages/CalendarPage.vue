<script setup>
import { computed, onMounted, ref } from 'vue'
import { ChevronLeft, ChevronRight } from 'lucide-vue-next'
import { api } from '@/api/client.js'
import { buildMonthCells } from '@/core/calendarModel.js'
import { postTitle } from '@/core/format.js'

const cursor = ref(new Date())
const data = ref({ days: [] })
const year = computed(() => cursor.value.getFullYear())
const month = computed(() => cursor.value.getMonth() + 1)
const cells = computed(() => buildMonthCells(year.value, month.value, data.value.days))
async function load() {
  data.value = await api.calendar(year.value, month.value)
}
function move(delta) {
  cursor.value = new Date(year.value, month.value - 1 + delta, 1)
  load()
}
onMounted(load)
</script>
<template>
  <div class="page narrow-page">
    <header class="page-title">
      <div>
        <span>CALENDAR</span>
        <h1>日历</h1>
      </div>
      <div class="year-switch glass">
        <button aria-label="上个月" @click="move(-1)"><ChevronLeft /></button
        ><strong>{{ year }} · {{ month }}</strong
        ><button aria-label="下个月" @click="move(1)"><ChevronRight /></button>
      </div>
    </header>
    <section class="calendar-panel glass">
      <div class="calendar-week">
        <span v-for="day in ['一', '二', '三', '四', '五', '六', '日']" :key="day">{{ day }}</span>
      </div>
      <div class="month-grid">
        <div
          v-for="(cell, index) in cells"
          :key="index"
          :class="['month-cell', { empty: !cell, active: cell?.count }]"
        >
          <template v-if="cell"
            ><span>{{ cell.day }}</span
            ><RouterLink
              v-for="post in cell.posts.slice(0, 2)"
              :key="post.id"
              :to="`/post/${post.id}`"
              >{{ postTitle(post) }}</RouterLink
            ><small v-if="cell.count > 2">+{{ cell.count - 2 }}</small></template
          >
        </div>
      </div>
    </section>
  </div>
</template>
