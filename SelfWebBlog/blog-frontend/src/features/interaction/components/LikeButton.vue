<script setup>
import { Heart } from 'lucide-vue-next'
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from '../../../composables/toast'
import { applyOptimisticLike } from '../model/threadModel'
import { getLikeState, setLikeState } from '../api/interactions'

const props = defineProps({
  targetType: { type: String, required: true },
  targetId: { type: [String, Number], required: true },
  initialCount: { type: Number, default: 0 },
  initialLiked: { type: Boolean, default: false },
  loadState: { type: Boolean, default: true },
  label: { type: String, default: '点赞' }
})

const emit = defineEmits(['change'])
const route = useRoute()
const router = useRouter()
const liked = ref(props.initialLiked)
const count = ref(props.initialCount)
const busy = ref(false)

watch(
  () => [props.initialLiked, props.initialCount],
  ([nextLiked, nextCount]) => {
    liked.value = nextLiked
    count.value = nextCount
  }
)

async function load() {
  if (!props.loadState) return
  try {
    const state = await getLikeState(props.targetType, props.targetId)
    liked.value = Boolean(state.liked)
    count.value = Number(state.count ?? state.likeCount ?? 0)
  } catch {
    // Public reading should remain available if engagement state cannot load.
  }
}

async function toggle() {
  if (busy.value) return
  const previous = { liked: liked.value, likeCount: count.value }
  const optimistic = applyOptimisticLike(previous, !liked.value)
  liked.value = optimistic.next.liked
  count.value = optimistic.next.likeCount
  busy.value = true

  try {
    const state = await setLikeState(props.targetType, props.targetId, liked.value)
    liked.value = Boolean(state.liked)
    count.value = Number(state.count ?? state.likeCount ?? 0)
    emit('change', state)
  } catch (error) {
    liked.value = optimistic.rollback.liked
    count.value = optimistic.rollback.likeCount
    if (error.authRequired) {
      showToast('登录后可以点赞。', 'warning')
      await router.push({ name: 'login', query: { redirect: route.fullPath } })
    } else {
      showToast(error.message || '点赞失败，请稍后重试。', 'error')
    }
  } finally {
    busy.value = false
  }
}

onMounted(load)
</script>

<template>
  <button
    class="like-button"
    :class="{ active: liked }"
    type="button"
    :disabled="busy"
    :aria-pressed="liked"
    :aria-label="`${label}，当前 ${count} 个`"
    @click="toggle"
  >
    <Heart :size="17" :fill="liked ? 'currentColor' : 'none'" />
    <span>{{ count }}</span>
  </button>
</template>

<style scoped>
.like-button {
  min-width: 48px;
  height: 34px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 0 10px;
  border: 1px solid transparent;
  border-radius: var(--radius-control);
  background: transparent;
  color: var(--text-tertiary);
  font: inherit;
  font-size: 0.78rem;
  cursor: pointer;
  transition:
    color var(--motion-fast-duration),
    border-color var(--motion-fast-duration),
    background var(--motion-fast-duration),
    transform var(--motion-fast-duration);
}
.like-button:hover {
  border-color: var(--border-subtle);
  background: var(--surface-soft);
  color: var(--text-primary);
}
.like-button.active {
  color: var(--support-pink);
}
.like-button:active {
  transform: scale(0.96);
}
.like-button:disabled {
  cursor: wait;
  opacity: 0.6;
}
</style>
