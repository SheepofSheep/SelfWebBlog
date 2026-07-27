<script setup>
import { computed, onMounted, ref } from 'vue'
import { ShieldCheck } from 'lucide-vue-next'
import { api } from '@/api/client.js'
import { formatDate } from '@/core/format.js'
import { notify } from '@/stores/app.js'
import AdminToolbar from './AdminToolbar.vue'

const events = ref([])
const query = ref('')
const loading = ref(false)
const filtered = computed(() => {
  const keyword = query.value.trim().toLowerCase()
  if (!keyword) return events.value
  return events.value.filter((event) =>
    [event.action, event.targetType, event.targetId, event.detail]
      .filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(keyword))
  )
})

async function load() {
  loading.value = true
  try {
    events.value = await api.security()
  } catch (error) {
    notify(error.message)
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <section class="security-panel">
    <AdminToolbar
      v-model="query"
      placeholder="搜索动作、对象或详情"
      :busy="loading"
      @refresh="load"
    />
    <div class="event-list glass" :aria-busy="loading">
      <article v-for="event in filtered" :key="event.id">
        <span class="event-icon"><ShieldCheck :size="17" /></span>
        <div>
          <strong>{{ event.action }}</strong
          ><span
            >{{ event.targetType || 'SYSTEM'
            }}<template v-if="event.targetId"> · {{ event.targetId }}</template
            ><template v-if="event.detail"> · {{ event.detail }}</template></span
          >
        </div>
        <time :datetime="event.createTime">{{ formatDate(event.createTime) }}</time>
      </article>
      <p v-if="!loading && !filtered.length">没有符合条件的安全事件</p>
    </div>
  </section>
</template>

<style scoped>
.event-list {
  padding: 8px 16px;
  border-radius: 24px;
}
.event-list article {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  gap: 12px;
  align-items: center;
  padding: 14px 3px;
  border-bottom: 1px solid var(--line);
}
.event-icon {
  display: grid;
  width: 38px;
  height: 38px;
  place-items: center;
  border-radius: 12px;
  color: var(--accent);
  background: var(--accent-soft);
}
.event-list article > div {
  display: grid;
  min-width: 0;
  gap: 3px;
}
.event-list span,
.event-list time {
  overflow: hidden;
  color: var(--muted);
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.event-list > p {
  padding: 45px;
  color: var(--muted);
  text-align: center;
}
@media (max-width: 720px) {
  .event-list article {
    grid-template-columns: auto minmax(0, 1fr);
  }
  .event-list time {
    grid-column: 2;
  }
}
</style>
