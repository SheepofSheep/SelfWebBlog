<script setup>
import { computed } from 'vue'
import { PenLine, Archive, Sparkles } from 'lucide-vue-next'
import MagazineArticleCard from './MagazineArticleCard.vue'
import { toAbsoluteUrl } from '../../utils/url'

const props = defineProps({
  posts: { type: Array, default: () => [] },
  blogInfo: { type: Object, default: null },
  totalPosts: { type: Number, default: 0 },
  isAdmin: { type: Boolean, default: false }
})

const emit = defineEmits(['open', 'delete', 'tag', 'category', 'write', 'archive'])

const featurePost = computed(() => props.posts[0] || null)
const sidePosts = computed(() => props.posts.slice(1, 4))
const nickname = computed(() => props.blogInfo?.nickname || 'Gabriel')
const avatar = computed(() =>
  toAbsoluteUrl(
    props.blogInfo?.avatarUrl || 'https://api.dicebear.com/7.x/avataaars/svg?seed=Felix'
  )
)

function forwardDelete(...args) {
  emit('delete', ...args)
}
</script>

<template>
  <section class="mag-hero">
    <div class="hero-label">
      <span>ガヴリールドロップアウト</span>
      <strong>{{ totalPosts }} POSTS</strong>
    </div>

    <div v-if="featurePost" class="hero-grid">
      <MagazineArticleCard
        :post="featurePost"
        variant="feature"
        :is-admin="isAdmin"
        class="hero-feature"
        @open="emit('open', $event)"
        @delete="forwardDelete"
        @tag="emit('tag', $event)"
        @category="emit('category', $event)"
      />

      <aside class="hero-stack">
        <div class="editor-card glass-card">
          <div class="editor-avatar">
            <img :src="avatar" :alt="nickname" />
          </div>
          <div>
            <p class="editor-kicker"><Sparkles :size="13" /> Editor</p>
            <h1>{{ nickname }}</h1>
            <p>{{ blogInfo?.bio || '记录生活与代码的小角落' }}</p>
          </div>
        </div>

        <div class="side-list">
          <MagazineArticleCard
            v-for="p in sidePosts"
            :key="p.id"
            :post="p"
            variant="compact"
            :is-admin="isAdmin"
            @open="emit('open', $event)"
            @delete="forwardDelete"
            @tag="emit('tag', $event)"
            @category="emit('category', $event)"
          />
        </div>

        <div class="hero-actions">
          <button v-if="isAdmin" class="hero-action primary" @click="emit('write')">
            <PenLine :size="16" />
            写文章
          </button>
          <button class="hero-action" @click="emit('archive')">
            <Archive :size="16" />
            翻归档
          </button>
        </div>
      </aside>
    </div>

    <div v-else class="empty-hero glass-card">
      <p class="editor-kicker"><Sparkles :size="13" /> New Issue</p>
      <h1>这本杂志还在排版中</h1>
      <p>第一篇文章发布后，首页会自动生成暗金杂志封面流。</p>
      <div class="hero-actions">
        <button v-if="isAdmin" class="hero-action primary" @click="emit('write')">
          <PenLine :size="16" />
          写第一篇
        </button>
        <button class="hero-action" @click="emit('archive')">
          <Archive :size="16" />
          看归档
        </button>
      </div>
    </div>
  </section>
</template>

<style scoped>
.mag-hero {
  max-width: var(--magazine-max, 1180px);
  margin: 0 auto;
  padding: 10px var(--space-md) 0;
}

.hero-label {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 12px;
  color: var(--text-muted);
  font-size: 0.72rem;
  font-weight: 800;
  letter-spacing: 0.13em;
}

.hero-label strong {
  color: var(--primary-hover);
}

.hero-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.58fr) minmax(320px, 0.92fr);
  gap: 18px;
  align-items: stretch;
}

.hero-stack {
  display: grid;
  min-width: 0;
  gap: 12px;
  align-content: start;
}

.editor-card {
  display: grid;
  grid-template-columns: 64px minmax(0, 1fr);
  gap: 14px;
  padding: 16px;
}

.editor-card:hover {
  transform: none;
}

.editor-avatar {
  width: 64px;
  height: 64px;
  padding: 2px;
  overflow: hidden;
  border: 1px solid var(--border);
  border-radius: 20px;
  background: var(--surface-paper);
}

.editor-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 17px;
}

.editor-kicker {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin: 0 0 5px;
  color: var(--primary-hover);
  font-size: 0.72rem;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.editor-card h1,
.empty-hero h1 {
  margin: 0;
  font-family: var(--font-serif);
  color: var(--text-main);
  letter-spacing: 0;
}

.editor-card p:last-child,
.empty-hero p {
  margin: 6px 0 0;
  color: var(--text-secondary);
  font-size: 0.85rem;
  line-height: 1.65;
}

.side-list {
  display: grid;
  gap: 10px;
}

.hero-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.hero-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  min-height: 42px;
  padding: 0 16px;
  border: 1px solid var(--border-warm);
  border-radius: var(--radius-pill);
  background: var(--surface-strong);
  color: var(--text-secondary);
  font: inherit;
  font-weight: 800;
  cursor: pointer;
}

[data-theme='dark'] .hero-action {
  background: var(--surface-strong);
}

.hero-action.primary {
  border-color: transparent;
  background: var(--primary);
  color: var(--on-primary);
  box-shadow: 0 8px 18px var(--primary-glow);
}

.empty-hero {
  padding: clamp(30px, 6vw, 72px);
}

@media (max-width: 980px) {
  .hero-grid {
    grid-template-columns: 1fr;
  }

  .hero-stack {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .mag-hero {
    padding-inline: var(--space-sm);
  }

  .hero-label {
    align-items: flex-start;
    flex-direction: column;
    gap: 4px;
  }

  .editor-card {
    grid-template-columns: 52px minmax(0, 1fr);
  }

  .editor-avatar {
    width: 52px;
    height: 52px;
    border-radius: 17px;
  }

  .hero-action {
    flex: 1 1 140px;
  }
}
</style>
