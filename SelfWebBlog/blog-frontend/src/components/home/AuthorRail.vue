<script setup>
import { computed } from 'vue'
import { CalendarDays, PenLine, Sparkles } from 'lucide-vue-next'
import { toAbsoluteUrl } from '../../utils/url'
import PersonalLinks from '../PersonalLinks.vue'
import MiniCalendar from '../MiniCalendar.vue'

const props = defineProps({
  blogInfo: { type: Object, default: null },
  posts: { type: Array, default: () => [] },
  calendarPosts: { type: Array, default: () => [] },
  totalPosts: { type: Number, default: 0 },
  isAdmin: { type: Boolean, default: false }
})

const emit = defineEmits(['write', 'archive', 'tag'])

const avatar = computed(() =>
  toAbsoluteUrl(
    props.blogInfo?.avatarUrl || 'https://api.dicebear.com/7.x/avataaars/svg?seed=Felix'
  )
)

const latestTags = computed(() => {
  const set = new Set()
  props.posts.forEach((p) => {
    if (!p?.tags) return
    p.tags
      .split(/[,，\s]+/)
      .filter(Boolean)
      .forEach((tag) => set.add(tag))
  })
  return [...set].slice(0, 6)
})
</script>

<template>
  <aside class="author-rail">
    <section class="rail-card author-card glass-card">
      <div class="author-top">
        <img :src="avatar" :alt="blogInfo?.nickname || '博主头像'" />
        <div>
          <p><Sparkles :size="13" /> SYSTEM PROFILE</p>
          <h2>{{ blogInfo?.nickname || 'Gabriel' }}</h2>
        </div>
      </div>
      <p class="author-bio">{{ blogInfo?.bio || '技术记录 / 项目实验 / 复习笔记 / 杂乱思考' }}</p>
      <div class="author-stats">
        <span
          ><strong>{{ totalPosts }}</strong> 篇文章</span
        >
        <span
          ><strong>{{ latestTags.length }}</strong> 个标签</span
        >
      </div>
      <PersonalLinks />
      <button v-if="isAdmin" class="rail-write" @click="emit('write')">
        <PenLine :size="16" />
        继续写作
      </button>
    </section>

    <section v-if="latestTags.length" class="rail-card tag-card glass-card">
      <p class="rail-title">TOPIC TOKENS</p>
      <div class="rail-tags">
        <button v-for="t in latestTags" :key="t" @click="emit('tag', t)"># {{ t }}</button>
      </div>
    </section>

    <section class="rail-card date-card glass-card">
      <p class="rail-title"><CalendarDays :size="14" /> ARCHIVE CALENDAR</p>
      <MiniCalendar :posts="calendarPosts" />
    </section>
  </aside>
</template>

<style scoped>
.author-rail {
  display: grid;
  gap: 12px;
}

.rail-card {
  padding: 14px;
  border-radius: var(--radius-xl);
}

.rail-card:hover {
  transform: none;
}

.author-top {
  display: grid;
  grid-template-columns: 58px minmax(0, 1fr);
  align-items: center;
  gap: 12px;
}

.author-top img {
  width: 58px;
  height: 58px;
  border: 1px solid var(--border-warm);
  border-radius: var(--radius-md);
  object-fit: cover;
}

.author-top p,
.rail-title {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin: 0;
  color: var(--primary-hover);
  font-family: var(--font-mono);
  font-size: 0.7rem;
  font-weight: 900;
  letter-spacing: 0.06em;
}

.author-top h2 {
  margin: 4px 0 0;
  color: var(--text-main);
  font-family: var(--font-serif);
  letter-spacing: 0;
}

.author-bio {
  margin: 14px 0 0;
  color: var(--text-secondary);
  font-size: 0.86rem;
  line-height: 1.72;
}

.author-stats {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  margin-top: 14px;
}

.author-stats span {
  padding: 10px;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  background: var(--surface-muted);
  color: var(--text-muted);
  font-size: 0.72rem;
  text-align: center;
}

.author-stats strong {
  display: block;
  color: var(--primary-hover);
  font-size: 1.15rem;
}

.rail-write {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  width: 100%;
  min-height: 42px;
  margin-top: 14px;
  border: 0;
  border-radius: var(--radius-md);
  background: var(--primary);
  color: var(--on-primary);
  font: inherit;
  font-weight: 900;
  cursor: pointer;
}

.rail-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.rail-tags button {
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: var(--surface-muted);
  color: var(--text-muted);
  font: inherit;
  font-size: 0.72rem;
  font-weight: 800;
  padding: 6px 9px;
  cursor: pointer;
}

.date-card {
  padding: 0;
  overflow: visible;
}

.date-card .rail-title {
  padding: 14px 14px 0;
}

.date-card :deep(.mini-calendar) {
  margin-top: 8px;
  box-shadow: none;
}
</style>
