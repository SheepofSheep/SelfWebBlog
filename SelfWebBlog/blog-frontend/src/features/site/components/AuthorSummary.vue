<script setup>
import { ArrowRight } from 'lucide-vue-next'
import { computed } from 'vue'
import { optimizedImageUrl, toAbsoluteUrl } from '../../../utils/url'

const props = defineProps({ blogInfo: { type: Object, default: null } })
defineEmits(['about'])

const avatar = computed(() => toAbsoluteUrl(props.blogInfo?.avatarUrl || ''))
</script>

<template>
  <section v-if="blogInfo" class="author-summary">
    <header>
      <span class="avatar">
        <img
          v-if="avatar"
          :src="optimizedImageUrl(avatar, 160)"
          :alt="blogInfo.nickname || 'Gabriel'"
        />
        <span v-else>G</span>
      </span>
      <div>
        <small>关于作者</small>
        <h2>{{ blogInfo.nickname || 'Gabriel' }}</h2>
      </div>
    </header>
    <p v-if="blogInfo.bio">{{ blogInfo.bio }}</p>
    <button type="button" @click="$emit('about')">查看资料 <ArrowRight :size="15" /></button>
  </section>
</template>

<style scoped>
.author-summary {
  padding: 18px;
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-card);
  background: var(--surface-glass);
  backdrop-filter: blur(8px);
}
header {
  display: flex;
  align-items: center;
  gap: 12px;
}
.avatar {
  width: 48px;
  height: 48px;
  display: grid;
  flex: 0 0 auto;
  overflow: hidden;
  place-items: center;
  border-radius: var(--radius-card);
  background: var(--accent-soft);
  color: var(--accent-strong);
  font-family: var(--font-reading);
  font-weight: 800;
}
.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
small,
h2,
p {
  margin: 0;
}
small {
  color: var(--accent-strong);
  font-size: 0.68rem;
  font-weight: 800;
}
h2 {
  margin-top: 2px;
  font-family: var(--font-reading);
  font-size: 1.25rem;
}
p {
  margin-top: 14px;
  color: var(--text-secondary);
  font-size: 0.8rem;
  line-height: 1.7;
}
button {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  margin-top: 14px;
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--text-primary);
  font: inherit;
  font-size: 0.76rem;
  font-weight: 800;
  cursor: pointer;
}
</style>
