<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import {
  Activity,
  Database,
  Eye,
  FileText,
  MessageSquare,
  RefreshCw,
  ArrowRight
} from 'lucide-vue-next'
import { api } from '@/api/client.js'
import { chartGeometry, createAdminRequestGate, fillTrend } from '@/core/adminModel.js'
import { formatDate } from '@/core/format.js'
import { notify } from '@/stores/app.js'

const overview = ref({})
const trend = ref({ days: [] })
const range = ref(14)
const loading = ref(true)
const error = ref('')
const hasData = ref(false)
const requestGate = createAdminRequestGate()

const stats = computed(() => [
  {
    label: '已发布',
    value: overview.value.publishedCount,
    to: '/admin?tab=content&status=PUBLISHED'
  },
  { label: '草稿', value: overview.value.draftCount, to: '/admin?tab=content&status=DRAFT' },
  {
    label: '待处理评论',
    value: overview.value.pendingCount,
    to: '/admin?tab=interactions&status=PENDING'
  },
  { label: '用户', value: overview.value.userCount, to: '/admin?tab=users' },
  { label: '媒体', value: overview.value.assetCount, to: '/admin?tab=assets' },
  {
    label: '孤立媒体',
    value: overview.value.orphanedCount,
    to: '/admin?tab=assets&status=ORPHANED'
  }
])
const days = computed(() =>
  fillTrend(trend.value.days || [], range.value, trend.value.endDate || undefined)
)
const chartMaximum = computed(() => {
  const maximum = Math.max(
    1,
    ...days.value.flatMap((day) => [day.views || 0, day.interactions || 0, day.posts || 0])
  )
  return Math.max(4, Math.ceil((maximum + 1) / 4) * 4)
})
const ticks = computed(() =>
  Array.from({ length: 5 }, (_, index) => ({
    value: (chartMaximum.value * index) / 4,
    y: 220 - index * 50
  }))
)
const geometry = (key) =>
  chartGeometry(
    days.value.map((day) => day[key]),
    720,
    240,
    20,
    chartMaximum.value
  )
const viewGeometry = computed(() => geometry('views'))
const interactionGeometry = computed(() => geometry('interactions'))
const postGeometry = computed(() => geometry('posts'))
const viewLine = computed(() => viewGeometry.value.path)
const interactionLine = computed(() => interactionGeometry.value.path)
const postLine = computed(() => postGeometry.value.path)
const chartSeries = computed(() => [
  { key: 'views', label: '独立浏览', className: 'views', points: viewGeometry.value.points },
  {
    key: 'interactions',
    label: '互动',
    className: 'interactions',
    points: interactionGeometry.value.points
  },
  { key: 'posts', label: '发布', className: 'posts', points: postGeometry.value.points }
])
const labels = computed(() => {
  if (!days.value.length) return []
  const indexes = [...new Set([0, Math.floor((days.value.length - 1) / 2), days.value.length - 1])]
  return indexes.map((index) => ({
    index,
    text: days.value[index].date.slice(5).replace('-', '/')
  }))
})

async function load(targetRange = range.value) {
  const ticket = requestGate.begin()
  loading.value = true
  error.value = ''
  try {
    const [overviewValue, trendValue] = await Promise.all([api.overview(), api.trends(targetRange)])
    if (!requestGate.isCurrent(ticket)) return false
    overview.value = overviewValue
    trend.value = trendValue
    range.value = targetRange
    hasData.value = true
    return true
  } catch (loadError) {
    if (requestGate.isCurrent(ticket)) {
      error.value = loadError?.message || '概览加载失败'
      notify(error.value)
    }
    return false
  } finally {
    requestGate.runIfCurrent(ticket, () => {
      loading.value = false
    })
  }
}

async function setRange(value) {
  if (range.value === value && !error.value) return
  await load(value)
}

onMounted(load)
onBeforeUnmount(() => requestGate.begin())
</script>

<template>
  <div class="overview-panel" :aria-busy="loading">
    <section v-if="error" class="overview-error glass" role="alert">
      <span>{{ error }}</span>
      <button class="secondary-button" type="button" :disabled="loading" @click="load(range)">
        <RefreshCw :size="16" />重试
      </button>
    </section>
    <template v-if="hasData">
      <section class="overview-stats">
        <RouterLink v-for="item in stats" :key="item.label" :to="item.to" class="overview-stat">
          <span>{{ item.label }}<ArrowRight :size="14" /></span>
          <strong>{{ item.value ?? '—' }}</strong>
        </RouterLink>
      </section>

      <section class="trend-card glass">
        <header>
          <div>
            <h2>近期趋势</h2>
          </div>
          <div class="segmented" aria-label="趋势时间范围">
            <button
              type="button"
              :disabled="loading"
              :class="{ active: range === 14 }"
              @click="setRange(14)"
            >
              14 天
            </button>
            <button
              type="button"
              :disabled="loading"
              :class="{ active: range === 30 }"
              @click="setRange(30)"
            >
              30 天
            </button>
          </div>
        </header>

        <div class="trend-legend" aria-label="图表图例">
          <span class="views"><Eye :size="15" />独立浏览 {{ trend.totals?.views ?? '—' }}</span>
          <span class="interactions"
            ><MessageSquare :size="15" />互动 {{ trend.totals?.interactions ?? '—' }}</span
          >
          <span class="posts"><FileText :size="15" />发布 {{ trend.totals?.posts ?? '—' }}</span>
        </div>

        <div class="trend-plot" role="img" :aria-label="`最近 ${range} 天浏览、互动和文章发布趋势`">
          <svg viewBox="0 0 760 270" preserveAspectRatio="none" aria-hidden="true">
            <g v-for="tick in ticks" :key="tick.value">
              <line
                x1="20"
                x2="700"
                :y1="tick.y"
                :y2="tick.y"
                stroke="var(--line)"
                stroke-width="1"
              />
              <text x="704" :y="tick.y + 4" text-anchor="start">{{ tick.value }}</text>
            </g>
            <path :d="viewLine" class="line views" />
            <path :d="interactionLine" class="line interactions" />
            <path :d="postLine" class="line posts" />
            <g v-for="series in chartSeries" :key="series.key">
              <circle
                v-for="(point, index) in series.points"
                :key="`${series.key}-${days[index]?.date}`"
                :class="['trend-point', series.className]"
                :cx="point.x"
                :cy="point.y"
                r="4"
              >
                <title>
                  {{ days[index]?.date }} · {{ series.label }} {{ days[index]?.[series.key] || 0 }}
                </title>
              </circle>
            </g>
            <text
              v-for="label in labels"
              :key="label.index"
              :x="20 + (label.index / Math.max(1, days.length - 1)) * 680"
              y="258"
              :text-anchor="
                label.index === 0 ? 'start' : label.index === days.length - 1 ? 'end' : 'middle'
              "
            >
              {{ label.text }}
            </text>
          </svg>
        </div>

        <details class="trend-summary">
          <summary>查看每日数据</summary>
          <div class="trend-table" role="table" aria-label="每日趋势数据">
            <div role="row" class="head">
              <span role="columnheader">日期</span><span role="columnheader">浏览</span
              ><span role="columnheader">互动</span><span role="columnheader">发布</span>
            </div>
            <div v-for="day in days" :key="day.date" role="row">
              <time role="cell" :datetime="day.date">{{ day.date }}</time
              ><span role="cell">{{ day.views }}</span
              ><span role="cell">{{ day.interactions }}</span
              ><span role="cell">{{ day.posts }}</span>
            </div>
          </div>
        </details>
      </section>

      <section class="health-row">
        <article class="health-card glass">
          <Database :size="20" />
          <div>
            <strong>数据库</strong
            ><span>{{
              overview.databaseStatus === 'UP'
                ? '运行正常'
                : overview.databaseStatus === 'DOWN'
                  ? '需要检查'
                  : '状态未知'
            }}</span>
          </div>
        </article>
        <article class="health-card glass">
          <Activity :size="20" />
          <div>
            <strong>最近活动</strong
            ><span>{{ formatDate(overview.latestActivity) || '暂无记录' }}</span>
          </div>
        </article>
      </section>
    </template>
  </div>
</template>
