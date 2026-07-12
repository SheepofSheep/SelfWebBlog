<script setup>
import ArticleCard from './ArticleCard.vue'

defineProps({
  posts: { type: Array, default: () => [] },
  isAdmin: { type: Boolean, default: false }
})
defineEmits(['open', 'archive', 'delete'])
</script>

<template>
  <section class="article-feed">
    <header>
      <div>
        <p>更多记录</p>
        <h2>文章与随笔</h2>
      </div>
      <button type="button" @click="$emit('archive')">查看归档</button>
    </header>
    <TransitionGroup v-if="posts.length" name="feed" tag="div" class="feed-grid">
      <ArticleCard
        v-for="post in posts"
        :key="post.id"
        :post="post"
        :is-admin="isAdmin"
        @open="$emit('open', $event)"
        @delete="$emit('delete', $event)"
      />
    </TransitionGroup>
  </section>
</template>

<style scoped>
.article-feed {
  min-width: 0;
}
.article-feed > header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 18px;
  border-bottom: 1px solid var(--border-medium);
}
header p {
  margin: 0 0 4px;
  color: var(--primary-hover);
  font-size: 0.74rem;
  font-weight: 900;
}
header h2 {
  margin: 0;
  font-family: var(--font-serif);
  font-size: clamp(1.6rem, 3vw, 2.25rem);
}
header button {
  min-height: 40px;
  padding: 0 14px;
  border: 1px solid var(--border-medium);
  border-radius: 7px;
  background: transparent;
  color: var(--text-main);
  font: inherit;
  cursor: pointer;
}
.feed-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  column-gap: 24px;
}
.feed-enter-active,
.feed-leave-active,
.feed-move {
  transition:
    opacity var(--motion-normal),
    transform var(--motion-normal);
}
.feed-enter-from,
.feed-leave-to {
  opacity: 0;
  transform: translateY(10px);
}
@media (max-width: 920px) {
  .feed-grid {
    grid-template-columns: 1fr;
  }
}
</style>
