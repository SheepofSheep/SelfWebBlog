<script setup>
import { computed, ref } from 'vue'
import { Archive, Folder, PenLine, Tag } from 'lucide-vue-next'
import { toAbsoluteUrl } from '../../utils/url'
import { formatTime, getFirstImageUrl, toPlainText } from '../../utils/format'

const props = defineProps({
  posts: { type: Array, default: () => [] },
  blogInfo: { type: Object, default: null },
  totalPosts: { type: Number, default: 0 },
  isAdmin: { type: Boolean, default: false }
})

const emit = defineEmits(['open', 'delete', 'tag', 'category', 'write', 'archive'])

const hoveredId = ref(null)
const nickname = computed(() => props.blogInfo?.nickname || 'Gabriel')
const indexPosts = computed(() => props.posts.slice(0, 8))
const previewPost = computed(
  () => props.posts.find((post) => post.id === hoveredId.value) || props.posts[0] || null
)

const previewCover = computed(() => {
  if (!previewPost.value) return ''
  const first = getFirstImageUrl(previewPost.value.content || '')
  return previewPost.value.coverUrl || (first ? toAbsoluteUrl(first) : '')
})

const previewTags = computed(() => splitTags(previewPost.value).slice(0, 4))
const latestCategory = computed(
  () => previewPost.value?.category || previewPost.value?.categoryName || '未分类'
)

const digest = computed(() => {
  const source = previewPost.value?.summary || toPlainText(previewPost.value?.content || '')
  const limit = 138
  return source.slice(0, limit) + (source.length > limit ? '...' : '')
})

const archiveStats = computed(() => [
  { label: 'STATUS', value: 'ONLINE' },
  { label: 'POSTS', value: String(props.totalPosts || props.posts.length || 0) },
  { label: 'STACK', value: 'SPRING / VUE' }
])

function splitTags(post) {
  if (!post?.tags) return []
  return post.tags.split(/[,，\s]+/).filter(Boolean)
}

function rowNumber(index) {
  return `#${String(index + 1).padStart(3, '0')}`
}
</script>

<template>
  <section class="archive-hero-board">
    <div class="board-head">
      <div>
        <p class="board-kicker">LATEST ENTRY INDEX</p>
        <h2>最新文章索引</h2>
      </div>
      <div class="board-actions">
        <button v-if="isAdmin" class="board-action primary" @click="emit('write')">
          <PenLine :size="15" />
          写文章
        </button>
        <button class="board-action" @click="emit('archive')">
          <Archive :size="15" />
          全部归档
        </button>
      </div>
    </div>

    <div v-if="previewPost" class="archive-board">
      <div class="entry-list" aria-label="最新文章列表">
        <button
          v-for="(post, index) in indexPosts"
          :key="post.id"
          class="entry-row"
          :class="{ active: previewPost?.id === post.id }"
          @mouseenter="hoveredId = post.id"
          @focus="hoveredId = post.id"
          @click="emit('open', post.id)"
        >
          <span class="entry-no">{{ rowNumber(index) }}</span>
          <span class="entry-date">{{ formatTime(post.createTime) }}</span>
          <span class="entry-main">
            <strong>{{ post.title || '未命名文章' }}</strong>
            <small>{{ post.category || post.categoryName || '未分类' }}</small>
          </span>
          <span class="entry-tags">
            <span v-for="tagName in splitTags(post).slice(0, 2)" :key="tagName"
              >#{{ tagName }}</span
            >
          </span>
        </button>
      </div>

      <aside class="entry-preview">
        <div class="preview-meta">
          <span>{{ nickname }}</span>
          <strong>{{ formatTime(previewPost.createTime) }}</strong>
        </div>

        <div class="preview-cover" :class="{ empty: !previewCover }">
          <img v-if="previewCover" :src="previewCover" :alt="previewPost.title" />
          <span v-else>{{ previewPost.title?.charAt(0) || '文' }}</span>
        </div>

        <div class="preview-copy">
          <button
            v-if="latestCategory"
            class="preview-category"
            @click="emit('category', latestCategory)"
          >
            <Folder :size="13" />
            {{ latestCategory }}
          </button>
          <h3 @click="emit('open', previewPost.id)">{{ previewPost.title || '未命名文章' }}</h3>
          <p>{{ digest || '这篇文章还没有摘要，打开正文继续阅读。' }}</p>
          <div v-if="previewTags.length" class="preview-tags">
            <button v-for="tagName in previewTags" :key="tagName" @click="emit('tag', tagName)">
              <Tag :size="12" />
              {{ tagName }}
            </button>
          </div>
        </div>

        <div class="archive-stats">
          <span v-for="item in archiveStats" :key="item.label">
            <small>{{ item.label }}</small>
            <strong>{{ item.value }}</strong>
          </span>
        </div>
      </aside>
    </div>

    <div v-else class="empty-hero glass-card">
      <p class="board-kicker">NEW ARCHIVE</p>
      <h2>档案库还在等待第一篇记录</h2>
      <p>发布第一篇文章后，首页会自动生成编号索引、文章预览和归档入口。</p>
      <div class="board-actions">
        <button v-if="isAdmin" class="board-action primary" @click="emit('write')">
          <PenLine :size="15" />
          写第一篇
        </button>
        <button class="board-action" @click="emit('archive')">
          <Archive :size="15" />
          看归档
        </button>
      </div>
    </div>
  </section>
</template>

<style scoped>
.archive-hero-board {
  max-width: var(--magazine-max, 1180px);
  margin: 0 auto;
  padding: 8px var(--space-md) 0;
}

.board-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
}

.board-kicker {
  margin: 0 0 6px;
  color: var(--primary-hover);
  font-family: var(--font-mono);
  font-size: 0.72rem;
  font-weight: 900;
  letter-spacing: 0.08em;
}

.board-head h2,
.empty-hero h2 {
  margin: 0;
  color: var(--text-main);
  font-family: var(--font-serif);
  font-size: clamp(1.5rem, 3vw, 2.3rem);
  line-height: 1.18;
}

.board-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.board-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  min-height: 38px;
  padding: 0 13px;
  border: 1px solid var(--border-medium);
  border-radius: var(--radius-md);
  background: var(--surface-paper);
  color: var(--text-secondary);
  font: inherit;
  font-size: 0.82rem;
  font-weight: 900;
  cursor: pointer;
}

.board-action.primary {
  border-color: transparent;
  background: var(--primary);
  color: var(--on-primary);
}

.archive-board {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(320px, 0.72fr);
  overflow: hidden;
  border: 1px solid var(--border-medium);
  border-radius: var(--radius-xl);
  background:
    linear-gradient(
      180deg,
      color-mix(in srgb, var(--surface-paper) 94%, transparent),
      color-mix(in srgb, var(--surface-parchment) 36%, var(--surface-paper))
    ),
    var(--surface-paper);
  box-shadow: var(--shadow-paper);
}

.entry-list {
  display: grid;
  align-content: start;
  min-width: 0;
  border-right: 1px solid var(--border-medium);
}

.entry-row {
  display: grid;
  grid-template-columns: 64px 104px minmax(0, 1fr) minmax(96px, 0.45fr);
  align-items: center;
  gap: 14px;
  min-height: 76px;
  padding: 12px 16px;
  border: 0;
  border-bottom: 1px solid var(--border-light);
  background: transparent;
  color: var(--text-secondary);
  font: inherit;
  text-align: left;
  cursor: pointer;
}

.entry-row:hover,
.entry-row:focus-visible,
.entry-row.active {
  outline: none;
  background: color-mix(in srgb, var(--primary) 9%, transparent);
}

.entry-row.active {
  box-shadow: inset 3px 0 0 var(--primary);
}

.entry-no,
.entry-date,
.entry-tags {
  font-family: var(--font-mono);
  font-size: 0.72rem;
  font-weight: 800;
}

.entry-no {
  color: var(--primary-hover);
}

.entry-date,
.entry-tags {
  color: var(--text-muted);
}

.entry-main {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.entry-main strong {
  overflow: hidden;
  color: var(--text-main);
  font-family: var(--font-serif);
  font-size: 1rem;
  line-height: 1.32;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.entry-main small {
  color: var(--text-muted);
  font-size: 0.72rem;
}

.entry-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
  justify-content: flex-end;
  min-width: 0;
}

.entry-preview {
  display: grid;
  align-content: start;
  gap: 14px;
  padding: 16px;
  min-width: 0;
}

.preview-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: var(--text-muted);
  font-family: var(--font-mono);
  font-size: 0.72rem;
}

.preview-meta strong {
  color: var(--primary-hover);
}

.preview-cover {
  display: grid;
  aspect-ratio: 16 / 9;
  overflow: hidden;
  place-items: center;
  border: 1px solid var(--border-medium);
  border-radius: var(--radius-lg);
  background:
    repeating-linear-gradient(
      135deg,
      color-mix(in srgb, var(--primary) 12%, transparent) 0 1px,
      transparent 1px 16px
    ),
    var(--surface-parchment);
}

.preview-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  filter: saturate(0.92) contrast(1.02);
}

.preview-cover span {
  color: var(--primary-hover);
  font-family: var(--font-serif);
  font-size: clamp(2.4rem, 8vw, 5rem);
  font-weight: 900;
}

.preview-copy {
  min-width: 0;
}

.preview-category {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  min-height: 28px;
  padding: 0 9px;
  border: 1px solid var(--border-medium);
  border-radius: var(--radius-sm);
  background: color-mix(in srgb, var(--primary) 9%, transparent);
  color: var(--primary-hover);
  font: inherit;
  font-size: 0.72rem;
  font-weight: 900;
  cursor: pointer;
}

.preview-copy h3 {
  margin: 12px 0 0;
  color: var(--text-main);
  font-family: var(--font-serif);
  font-size: clamp(1.35rem, 3vw, 2rem);
  line-height: 1.26;
  cursor: pointer;
}

.preview-copy p {
  margin: 10px 0 0;
  color: var(--text-secondary);
  font-size: 0.9rem;
  line-height: 1.72;
}

.preview-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
  margin-top: 12px;
}

.preview-tags button {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  min-height: 29px;
  padding: 0 9px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: var(--surface-muted);
  color: var(--text-muted);
  font: inherit;
  font-size: 0.72rem;
  font-weight: 800;
  cursor: pointer;
}

.archive-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.archive-stats span {
  display: grid;
  gap: 3px;
  min-width: 0;
  padding: 10px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  background: color-mix(in srgb, var(--surface-strong) 76%, transparent);
}

.archive-stats small {
  color: var(--text-muted);
  font-family: var(--font-mono);
  font-size: 0.64rem;
  font-weight: 900;
}

.archive-stats strong {
  overflow: hidden;
  color: var(--text-main);
  font-family: var(--font-mono);
  font-size: 0.78rem;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.empty-hero {
  padding: clamp(28px, 6vw, 58px);
}

.empty-hero p {
  margin: 10px 0 18px;
  color: var(--text-secondary);
}

@media (max-width: 980px) {
  .archive-board {
    grid-template-columns: 1fr;
  }

  .entry-list {
    border-right: 0;
  }

  .entry-preview {
    border-top: 1px solid var(--border-medium);
  }
}

@media (max-width: 640px) {
  .archive-hero-board {
    padding-inline: var(--space-sm);
  }

  .board-head {
    align-items: flex-start;
    flex-direction: column;
  }

  .board-actions,
  .board-action {
    width: 100%;
  }

  .entry-row {
    grid-template-columns: 52px minmax(0, 1fr);
    gap: 8px 12px;
    min-height: auto;
    padding: 14px;
  }

  .entry-date {
    justify-self: end;
  }

  .entry-main,
  .entry-tags {
    grid-column: 1 / -1;
  }

  .entry-tags {
    justify-content: flex-start;
  }

  .entry-main strong {
    white-space: normal;
  }

  .entry-preview {
    display: none;
  }
}
</style>
