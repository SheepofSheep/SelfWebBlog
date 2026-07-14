<script setup>
import { Clock, Eye, Folder, Trash2 } from 'lucide-vue-next'
import { formatTime, getFirstImageUrl, toPlainText } from '../../../utils/format'
import { optimizedImageUrl, toAbsoluteUrl } from '../../../utils/url'
import IconButton from '../../../components/ui/IconButton.vue'

defineProps({
  posts: { type: Array, default: () => [] },
  isAdmin: { type: Boolean, default: false }
})
defineEmits(['open', 'archive', 'delete'])

function postCover(post) {
  return toAbsoluteUrl(post.coverUrl || getFirstImageUrl(post.content || '') || '')
}
</script>

<template>
  <section class="post-feed">
    <header>
      <div>
        <span>更多内容</span>
        <h2>文章与随笔</h2>
      </div>
      <button type="button" @click="$emit('archive')">全部归档</button>
    </header>
    <TransitionGroup name="feed" tag="div" class="post-list">
      <article v-for="post in posts" :key="post.id" class="post-row">
        <button class="post-main" type="button" @click="$emit('open', post.id)">
          <span class="post-copy">
            <span class="post-category">{{ post.category || '随笔' }}</span>
            <strong>{{ post.title }}</strong>
            <small>{{ post.summary || toPlainText(post.content || '').slice(0, 92) }}</small>
            <span class="post-meta">
              <span><Clock :size="13" />{{ formatTime(post.createTime) }}</span>
              <span v-if="post.category"><Folder :size="13" />{{ post.category }}</span>
              <span v-if="post.viewCount != null"><Eye :size="13" />{{ post.viewCount }}</span>
            </span>
          </span>
          <span class="post-cover" :class="{ empty: !postCover(post) }">
            <img
              v-if="postCover(post)"
              :src="optimizedImageUrl(postCover(post), 480)"
              :alt="post.title"
              loading="lazy"
            />
            <span v-else>{{ post.title?.slice(0, 1) }}</span>
          </span>
        </button>
        <IconButton
          v-if="isAdmin"
          class="delete-button"
          label="删除文章"
          @click="$emit('delete', post.id)"
          ><Trash2 :size="16"
        /></IconButton>
      </article>
    </TransitionGroup>
  </section>
</template>

<style scoped>
.post-feed > header {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 18px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--border-strong);
}
header span {
  color: var(--accent-strong);
  font-size: 0.7rem;
  font-weight: 800;
}
header h2 {
  margin-top: 2px;
  font-family: var(--font-reading);
  font-size: 1.55rem;
  letter-spacing: 0;
}
header button {
  min-height: 38px;
  padding: 0 13px;
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-control);
  background: transparent;
  color: var(--text-secondary);
  font: inherit;
  font-size: 0.78rem;
  cursor: pointer;
}
.post-row {
  position: relative;
  border-bottom: 1px solid var(--border-subtle);
}
.post-main {
  width: 100%;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 168px;
  align-items: center;
  gap: 24px;
  padding: 22px 4px;
  border: 0;
  background: transparent;
  color: var(--text-primary);
  text-align: left;
  cursor: pointer;
}
.post-copy {
  min-width: 0;
  display: grid;
  gap: 7px;
}
.post-category {
  color: var(--accent-strong);
  font-size: 0.68rem;
  font-weight: 800;
}
.post-copy strong {
  font-family: var(--font-reading);
  font-size: 1.18rem;
  letter-spacing: 0;
}
.post-copy small {
  display: -webkit-box;
  overflow: hidden;
  color: var(--text-secondary);
  font-size: 0.8rem;
  line-height: 1.65;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}
.post-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  color: var(--text-tertiary);
  font-size: 0.68rem;
}
.post-meta > span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.post-cover {
  width: 168px;
  aspect-ratio: 16 / 10;
  overflow: hidden;
  display: grid;
  place-items: center;
  border-radius: var(--radius-card);
  background: var(--surface-soft);
  color: var(--accent-strong);
  font-family: var(--font-reading);
  font-size: 2rem;
}
.post-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform var(--motion-normal-duration) var(--motion-ease);
}
.post-main:hover .post-cover img {
  transform: scale(1.035);
}
.delete-button {
  position: absolute;
  top: 8px;
  right: 176px;
}
.feed-enter-active,
.feed-leave-active {
  transition:
    opacity var(--motion-normal-duration),
    transform var(--motion-normal-duration);
}
.feed-enter-from,
.feed-leave-to {
  opacity: 0;
  transform: translateY(8px);
}
@media (max-width: 560px) {
  .post-main {
    grid-template-columns: minmax(0, 1fr) 88px;
    gap: 14px;
  }
  .post-cover {
    width: 88px;
  }
  .post-copy small {
    -webkit-line-clamp: 1;
  }
  .delete-button {
    right: 92px;
  }
}
</style>
