<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import {
  Activity,
  Database,
  Eye,
  FileText,
  MessageSquare,
  RefreshCw,
  Users,
  Image as ImageIcon
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
  { label: '已发布', value: overview.value.publishedCount, icon: FileText },
  { label: '草稿', value: overview.value.draftCount, icon: FileText },
  { label: '待处理', value: overview.value.pendingCount, icon: MessageSquare },
  { label: '用户', value: overview.value.userCount, icon: Users },
  { label: '媒体', value: overview.value.assetCount, icon: ImageIcon },
  { label: '孤立媒体', value: overview.value.orphanedCount, icon: Database }
])
const days = computed(() =>
  fillTrend(trend.value.days || [], range.value, trend.value.endDate || undefined)
)
const chartMaximum = computed(() =>
  Math.max(
    1,
    ...days.value.flatMap((day) => [day.views || 0, day.interactions || 0, day.posts || 0])
  )
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
        <article v-for="item in stats" :key="item.label" class="overview-stat glass">
          <span><component :is="item.icon" :size="16" />{{ item.label }}</span>
          <strong>{{ item.value ?? '—' }}</strong>
        </article>
      </section>

      <section class="trend-card glass">
        <header>
          <div>
            <span class="eyebrow">TREND</span>
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
          <svg viewBox="0 0 720 270" preserveAspectRatio="none" aria-hidden="true">
            <defs>
              <linearGradient id="view-fill" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0" stop-color="var(--accent)" stop-opacity=".2" />
                <stop offset="1" stop-color="var(--accent)" stop-opacity="0" />
              </linearGradient>
            </defs>
            <path v-if="viewLine" :d="`${viewLine} L 700 220 L 20 220 Z`" fill="url(#view-fill)" />
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

<style scoped>
.overview-panel {
  display: grid;
  gap: 15px;
}
.overview-error {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
  min-height: 72px;
  padding: 16px 18px;
  border-radius: 20px;
  color: var(--danger, #b84242);
  font-size: 13px;
}
.overview-error button {
  display: inline-flex;
  flex: 0 0 auto;
  gap: 6px;
  align-items: center;
}
.overview-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}
.overview-stat {
  display: grid;
  min-height: 126px;
  padding: 20px;
  align-content: space-between;
  border-radius: 22px;
}
.overview-stat > span {
  display: flex;
  gap: 7px;
  align-items: center;
  color: var(--muted);
  font-size: 12px;
}
.overview-stat strong {
  font-size: clamp(32px, 4vw, 48px);
  letter-spacing: -0.06em;
}
.trend-card {
  min-width: 0;
  padding: 22px;
  border-radius: 26px;
}
.trend-card > header {
  display: flex;
  gap: 18px;
  align-items: center;
  justify-content: space-between;
}
.trend-card h2 {
  margin: 2px 0 0;
  font-size: 25px;
}
.segmented {
  display: flex;
  padding: 4px;
  border-radius: 12px;
  background: var(--accent-soft);
}
.segmented button {
  padding: 7px 11px;
  border: 0;
  border-radius: 9px;
  color: var(--muted);
  background: transparent;
}
.segmented button.active {
  color: var(--ink);
  background: var(--glass-strong);
  box-shadow: var(--shadow);
}
.trend-legend {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin: 22px 0 2px;
  color: var(--muted);
  font-size: 12px;
}
.trend-legend span {
  display: inline-flex;
  gap: 5px;
  align-items: center;
}
.trend-legend .views {
  color: var(--accent);
}
.trend-legend .interactions {
  color: #9c6ade;
}
.trend-legend .posts {
  color: #28a879;
}
.trend-plot {
  width: 100%;
  overflow: hidden;
}
.trend-plot svg {
  display: block;
  width: 100%;
  height: 270px;
}
.trend-plot text {
  fill: var(--muted);
  font-size: 11px;
}
.line {
  fill: none;
  stroke-width: 3;
  stroke-linecap: round;
  stroke-linejoin: round;
  vector-effect: non-scaling-stroke;
}
.line.views {
  stroke: var(--accent);
}
.line.interactions {
  stroke: #9c6ade;
}
.line.posts {
  stroke: #28a879;
}
.trend-point {
  stroke: var(--glass-strong);
  stroke-width: 2;
  vector-effect: non-scaling-stroke;
  transition: r 160ms ease;
}
.trend-point:hover {
  r: 6px;
}
.trend-point.views {
  fill: var(--accent);
}
.trend-point.interactions {
  fill: #9c6ade;
}
.trend-point.posts {
  fill: #28a879;
}
.trend-summary {
  margin-top: 5px;
  color: var(--muted);
  font-size: 12px;
}
.trend-summary summary {
  width: max-content;
  cursor: pointer;
}
.trend-table {
  display: grid;
  max-height: 280px;
  margin-top: 12px;
  overflow: auto;
  border-top: 1px solid var(--line);
}
.trend-table > div {
  display: grid;
  grid-template-columns: 1fr repeat(3, 70px);
  gap: 8px;
  padding: 9px 4px;
  border-bottom: 1px solid var(--line);
  text-align: right;
}
.trend-table > div > :first-child {
  text-align: left;
}
.trend-table .head {
  position: sticky;
  top: 0;
  color: var(--ink);
  background: var(--glass-strong);
  font-weight: 700;
}
.health-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
.health-card {
  display: flex;
  gap: 12px;
  align-items: center;
  padding: 18px 20px;
  border-radius: 20px;
  color: var(--accent);
}
.health-card div {
  display: grid;
  gap: 3px;
}
.health-card strong {
  color: var(--ink);
}
.health-card span {
  color: var(--muted);
  font-size: 12px;
}
@media (max-width: 720px) {
  .overview-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .trend-card {
    padding: 16px;
  }
  .trend-card > header {
    align-items: flex-start;
  }
  .trend-plot svg {
    height: 220px;
  }
  .health-row {
    grid-template-columns: 1fr;
  }
}
</style>
