<script setup>
import { MessageCircle, ShieldCheck } from 'lucide-vue-next'
import { computed } from 'vue'
import LikeButton from './LikeButton.vue'
import { optimizedImageUrl } from '../../../utils/url'

const props = defineProps({
  item: { type: Object, required: true },
  nested: { type: Boolean, default: false },
  canReply: { type: Boolean, default: true }
})

defineEmits(['reply'])

const displayName = computed(() => props.item.nickname || '访客')
const initial = computed(() => displayName.value.slice(0, 1).toUpperCase())
const roleLabel = computed(() => (props.item.role === 'ADMIN' ? '博主' : '访客'))

function relativeTime(value) {
  if (!value) return ''
  const delta = new Date(value).getTime() - Date.now()
  const minutes = Math.round(delta / 60000)
  const formatter = new Intl.RelativeTimeFormat('zh-CN', { numeric: 'auto' })
  if (Math.abs(minutes) < 60) return formatter.format(minutes, 'minute')
  const hours = Math.round(minutes / 60)
  if (Math.abs(hours) < 24) return formatter.format(hours, 'hour')
  const days = Math.round(hours / 24)
  if (Math.abs(days) < 30) return formatter.format(days, 'day')
  return new Date(value).toLocaleDateString('zh-CN')
}
</script>

<template>
  <article class="interaction-item" :class="{ nested, pending: item.status === 'PENDING' }">
    <img
      v-if="item.avatarUrl"
      class="avatar"
      :src="optimizedImageUrl(item.avatarUrl, 160)"
      :alt="displayName"
    />
    <span v-else class="avatar avatar-fallback" aria-hidden="true">{{ initial }}</span>
    <div class="interaction-body">
      <header>
        <strong>{{ displayName }}</strong>
        <span class="role" :class="{ owner: item.role === 'ADMIN' }">
          <ShieldCheck v-if="item.role === 'ADMIN'" :size="12" />{{ roleLabel }}
        </span>
        <span v-if="item.ipRegion" class="region">{{ item.ipRegion }}</span>
        <span v-if="item.status === 'PENDING'" class="status">待审核</span>
        <time :datetime="item.createTime">{{ relativeTime(item.createTime) }}</time>
      </header>
      <p>{{ item.content }}</p>
      <footer>
        <LikeButton
          target-type="COMMENT"
          :target-id="item.id"
          :initial-count="item.likeCount"
          :initial-liked="item.liked"
          :load-state="false"
          label="赞这条内容"
        />
        <button v-if="canReply" type="button" @click="$emit('reply', item)">
          <MessageCircle :size="15" /> 回复
        </button>
      </footer>
    </div>
  </article>
</template>

<style scoped>
.interaction-item {
  display: grid;
  grid-template-columns: 40px minmax(0, 1fr);
  gap: 12px;
  padding: 18px 0;
  border-top: 1px solid var(--border-subtle);
}
.interaction-item.nested {
  grid-template-columns: 32px minmax(0, 1fr);
  margin-left: 52px;
  padding: 14px 0;
}
.interaction-item.pending {
  opacity: 0.72;
}
.avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
}
.nested .avatar {
  width: 32px;
  height: 32px;
}
.avatar-fallback {
  display: grid;
  place-items: center;
  background: var(--accent-soft);
  color: var(--accent-strong);
  font-weight: 800;
}
.interaction-body {
  min-width: 0;
}
header,
footer,
header > span {
  display: flex;
  align-items: center;
}
header {
  min-height: 24px;
  flex-wrap: wrap;
  gap: 7px;
}
header strong {
  color: var(--text-primary);
  font-size: 0.88rem;
}
header > span {
  gap: 3px;
  font-size: 0.67rem;
}
.role,
.region,
.status {
  padding: 2px 6px;
  border: 1px solid var(--border-subtle);
  border-radius: 999px;
  color: var(--text-tertiary);
}
.role.owner {
  border-color: color-mix(in srgb, var(--accent) 42%, var(--border-subtle));
  color: var(--accent-strong);
}
.status {
  color: var(--support-blue);
}
time {
  margin-left: auto;
  color: var(--text-tertiary);
  font-size: 0.7rem;
}
p {
  margin: 8px 0 7px;
  color: var(--text-primary);
  font-size: 0.88rem;
  line-height: 1.75;
  overflow-wrap: anywhere;
  white-space: pre-wrap;
}
footer {
  gap: 4px;
}
footer > button {
  height: 34px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 0 9px;
  border: 1px solid transparent;
  border-radius: var(--radius-control);
  background: transparent;
  color: var(--text-tertiary);
  font: inherit;
  font-size: 0.75rem;
  cursor: pointer;
}
footer > button:hover {
  border-color: var(--border-subtle);
  background: var(--surface-soft);
  color: var(--text-primary);
}
@media (max-width: 620px) {
  .interaction-item.nested {
    margin-left: 24px;
  }
  time {
    width: 100%;
    margin-left: 0;
  }
}
</style>
