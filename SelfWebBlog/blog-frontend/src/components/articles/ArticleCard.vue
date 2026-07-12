<script setup>
import { Clock, Folder, Trash2 } from 'lucide-vue-next'
import { formatTime, getFirstImageUrl, toPlainText } from '../../utils/format'
import { toAbsoluteUrl } from '../../utils/url'

const props = defineProps({
  post: { type: Object, required: true },
  isAdmin: { type: Boolean, default: false }
})
defineEmits(['open', 'delete'])
const imageUrl = () =>
  toAbsoluteUrl(props.post.coverUrl || getFirstImageUrl(props.post.content || '') || '')
</script>

<template>
  <article class="article-card" @click="$emit('open', post.id)">
    <div class="article-image">
      <img v-if="imageUrl()" :src="imageUrl()" :alt="post.title" loading="lazy" />
      <span v-else>{{ post.title?.charAt(0) || '文' }}</span>
    </div>
    <div class="article-copy">
      <button
        v-if="isAdmin"
        class="article-delete"
        type="button"
        aria-label="删除文章"
        title="删除文章"
        @click.stop="$emit('delete', post.id)"
      >
        <Trash2 :size="14" />
      </button>
      <div class="article-meta">
        <span><Clock :size="13" /> {{ formatTime(post.createTime).slice(0, 11) }}</span>
        <span v-if="post.category"><Folder :size="13" /> {{ post.category }}</span>
      </div>
      <h3>{{ post.title }}</h3>
      <p>
        {{ post.summary || toPlainText(post.content || '').slice(0, 90) || '继续阅读这篇文章。' }}
      </p>
    </div>
  </article>
</template>

<style scoped>
.article-card {
  display: grid;
  min-width: 0;
  grid-template-columns: 178px minmax(0, 1fr);
  overflow: hidden;
  border-top: 1px solid var(--border-medium);
  cursor: pointer;
  transition:
    background var(--motion-normal),
    transform var(--motion-normal);
}
.article-card:hover {
  background: var(--surface-muted);
  transform: translateY(-2px);
}
.article-image {
  min-height: 155px;
  overflow: hidden;
  background: var(--surface-muted);
}
.article-image img {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
  transition: transform var(--motion-slow);
}
.article-card:hover img {
  transform: scale(1.03);
}
.article-image span {
  display: grid;
  height: 100%;
  place-items: center;
  color: var(--primary);
  font-family: var(--font-serif);
  font-size: 3rem;
}
.article-copy {
  min-width: 0;
  padding: 20px;
}
.article-copy {
  position: relative;
}
.article-delete {
  position: absolute;
  top: 14px;
  right: 14px;
  display: grid;
  width: 34px;
  height: 34px;
  place-items: center;
  border: 0;
  border-radius: 50%;
  background: var(--danger-soft);
  color: var(--danger);
  cursor: pointer;
}
.article-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  color: var(--text-muted);
  font-size: 0.72rem;
}
.article-meta span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
h3 {
  margin: 12px 0 8px;
  font-family: var(--font-serif);
  font-size: 1.28rem;
  line-height: 1.35;
}
p {
  display: -webkit-box;
  overflow: hidden;
  margin: 0;
  color: var(--text-secondary);
  font-size: 0.88rem;
  line-height: 1.7;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}
@media (max-width: 620px) {
  .article-card {
    grid-template-columns: 112px minmax(0, 1fr);
  }
  .article-image {
    min-height: 132px;
  }
  .article-copy {
    padding: 15px;
  }
  h3 {
    font-size: 1.05rem;
  }
  .article-meta span:nth-child(2),
  p {
    display: none;
  }
}
</style>
