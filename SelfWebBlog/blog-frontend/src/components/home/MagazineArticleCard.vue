<script setup>
import { computed } from 'vue'
import { Clock, Eye, Folder, Tag, Trash2 } from 'lucide-vue-next'
import { toAbsoluteUrl } from '../../utils/url'
import { formatTime, getFirstImageUrl, toPlainText } from '../../utils/format'

const props = defineProps({
  post: { type: Object, required: true },
  variant: { type: String, default: 'compact' },
  isAdmin: { type: Boolean, default: false }
})

const emit = defineEmits(['open', 'delete', 'tag', 'category'])

const categoryName = computed(() => props.post?.category || props.post?.categoryName || '')

const tags = computed(() => {
  if (!props.post?.tags) return []
  return props.post.tags
    .split(/[,，\s]+/)
    .filter(Boolean)
    .slice(0, props.variant === 'feature' ? 4 : 3)
})

const summary = computed(() => {
  const text = props.post?.summary || toPlainText(props.post?.content || '')
  const limit = props.variant === 'feature' ? 150 : 86
  return text.slice(0, limit) + (text.length > limit ? '…' : '')
})

const cover = computed(() => {
  const first = getFirstImageUrl(props.post?.content || '')
  return props.post?.coverUrl || (first ? toAbsoluteUrl(first) : '')
})

const readMinutes = computed(() => {
  if (!props.post?.content) return 1
  const text = props.post.content.replace(/[#*_`[\]!()>-\s]/g, '')
  return Math.max(1, Math.ceil(text.length / 400))
})

function openPost() {
  emit('open', props.post.id)
}
</script>

<template>
  <article :class="['mag-card', 'mag-card-' + variant]" @click="openPost">
    <div class="mag-cover" :class="{ empty: !cover }">
      <img v-if="cover" :src="cover" :alt="post.title" loading="lazy" />
      <div v-else class="mag-cover-fallback">
        <span>{{ post.title?.charAt(0) || '文' }}</span>
      </div>
      <button
        v-if="isAdmin"
        class="mag-delete"
        aria-label="删除文章"
        @click.stop="emit('delete', post.id, $event)"
      >
        <Trash2 :size="14" />
      </button>
      <button v-if="categoryName" class="mag-category" @click.stop="emit('category', categoryName)">
        <Folder :size="12" />
        {{ categoryName }}
      </button>
    </div>

    <div class="mag-body">
      <div class="mag-meta">
        <span><Clock :size="12" /> {{ formatTime(post.createTime) }}</span>
        <span><Eye :size="12" /> {{ readMinutes }} 分钟</span>
      </div>
      <h3 class="mag-title">{{ post.title || '未命名文章' }}</h3>
      <p class="mag-summary">{{ summary }}</p>
      <div v-if="tags.length" class="mag-tags" @click.stop>
        <button v-for="t in tags" :key="t" @click.stop="emit('tag', t)">
          <Tag :size="11" />
          {{ t }}
        </button>
      </div>
    </div>
  </article>
</template>

<style scoped>
.mag-card {
  position: relative;
  display: grid;
  overflow: hidden;
  border: 1px solid var(--border);
  background:
    linear-gradient(
      180deg,
      color-mix(in srgb, var(--surface-paper) 94%, transparent),
      color-mix(in srgb, var(--surface-parchment) 24%, var(--surface-paper))
    ),
    var(--surface-paper);
  box-shadow: var(--shadow-paper);
  cursor: pointer;
  transition:
    transform var(--duration-normal) var(--ease-soft),
    border-color var(--duration-normal) var(--ease-soft),
    box-shadow var(--duration-normal) var(--ease-soft);
}

[data-theme='dark'] .mag-card {
  background:
    linear-gradient(180deg, color-mix(in srgb, var(--surface-paper) 92%, transparent), #18140d),
    var(--surface-paper);
  border-color: var(--anime-line, var(--border));
}

.mag-card:hover {
  transform: translateY(-2px);
  border-color: var(--border-warm);
  box-shadow: var(--shadow-lift);
}

.mag-card-compact {
  grid-template-columns: 132px minmax(0, 1fr);
  min-height: 150px;
  border-radius: var(--radius-xl);
}

.mag-card-grid {
  grid-template-rows: 160px auto;
  border-radius: var(--radius-xl);
}

.mag-cover {
  position: relative;
  min-width: 0;
  overflow: hidden;
  background: linear-gradient(135deg, rgba(201, 145, 45, 0.12), rgba(111, 127, 69, 0.08)), #1b150d;
}

.mag-cover img {
  width: 100%;
  height: 100%;
  min-height: inherit;
  object-fit: cover;
  display: block;
  filter: saturate(0.96) contrast(1.01);
  transition: transform var(--duration-slow) var(--ease-soft);
}

.mag-card:hover .mag-cover img {
  transform: scale(1.06);
}

.mag-cover::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, transparent 50%, rgba(17, 16, 13, 0.44));
  pointer-events: none;
}

.mag-cover-fallback {
  display: grid;
  width: 100%;
  height: 100%;
  min-height: inherit;
  place-items: center;
  background:
    repeating-linear-gradient(135deg, rgba(212, 160, 68, 0.1) 0 1px, transparent 1px 18px),
    linear-gradient(135deg, #2b2114, #15110c);
}

.mag-cover-fallback span {
  font-family: var(--font-serif);
  font-size: clamp(2rem, 8vw, 5rem);
  color: rgba(246, 196, 91, 0.72);
}

.mag-delete {
  position: absolute;
  top: 12px;
  right: 12px;
  z-index: 2;
  display: grid;
  width: 34px;
  height: 34px;
  place-items: center;
  border: 1px solid rgba(255, 226, 177, 0.35);
  border-radius: var(--radius-sm);
  background: rgba(17, 16, 13, 0.58);
  color: #ffd1c4;
  cursor: pointer;
}

.mag-delete:hover {
  background: var(--danger);
  color: white;
}

.mag-category {
  position: absolute;
  left: 12px;
  bottom: 12px;
  z-index: 2;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  max-width: calc(100% - 24px);
  padding: 7px 10px;
  border: 1px solid rgba(255, 226, 177, 0.32);
  border-radius: var(--radius-sm);
  background: rgba(17, 16, 13, 0.62);
  color: #ffe3ad;
  font-size: 0.72rem;
  font-weight: 700;
  cursor: pointer;
}

.mag-body {
  min-width: 0;
  padding: 16px;
}

.mag-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 8px;
  color: var(--text-muted);
  font-size: 0.72rem;
}

.mag-meta span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.mag-title {
  margin: 0;
  color: var(--text-main);
  font-family: var(--font-serif);
  font-size: 1.05rem;
  line-height: 1.45;
  letter-spacing: 0;
}

.mag-summary {
  display: -webkit-box;
  margin: 10px 0 0;
  overflow: hidden;
  color: var(--text-secondary);
  font-size: 0.86rem;
  line-height: 1.72;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.mag-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
  margin-top: 14px;
}

.mag-tags button {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  max-width: 120px;
  padding: 5px 9px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: var(--surface-muted);
  color: var(--text-muted);
  font: inherit;
  font-size: 0.72rem;
  cursor: pointer;
}

.mag-tags button:hover {
  color: var(--primary-hover);
  border-color: var(--border-warm);
}

@media (max-width: 720px) {
  .mag-card-compact {
    grid-template-columns: 96px minmax(0, 1fr);
    min-height: 124px;
  }

  .mag-card-compact .mag-body {
    padding: 13px;
  }

  .mag-card-compact .mag-title {
    font-size: 0.94rem;
  }

  .mag-card-compact .mag-summary,
  .mag-card-compact .mag-tags {
    display: none;
  }
}
</style>
