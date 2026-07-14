<script setup>
import { CircleAlert, CircleCheck, LoaderCircle, RotateCcw, X } from 'lucide-vue-next'

defineProps({ tasks: { type: Array, default: () => [] } })
defineEmits(['retry', 'dismiss'])
</script>

<template>
  <TransitionGroup v-if="tasks.length" name="queue" tag="div" class="upload-queue">
    <div v-for="task in tasks" :key="task.id" class="upload-task" :class="task.status">
      <LoaderCircle v-if="task.status === 'uploading'" :size="15" class="spin" />
      <CircleCheck v-else-if="task.status === 'completed'" :size="15" />
      <CircleAlert v-else-if="task.status === 'failed'" :size="15" />
      <span>{{ task.file?.name || '图片' }}</span>
      <small v-if="task.status === 'uploading'">{{ task.progress }}%</small>
      <button
        v-if="task.status === 'failed'"
        type="button"
        title="重试"
        @click="$emit('retry', task.id)"
      >
        <RotateCcw :size="14" />
      </button>
      <button
        v-else-if="task.status === 'completed'"
        type="button"
        title="关闭"
        @click="$emit('dismiss', task.id)"
      >
        <X :size="14" />
      </button>
    </div>
  </TransitionGroup>
</template>

<style scoped>
.upload-queue {
  display: grid;
  gap: 6px;
  padding: 8px 10px;
  border-top: 1px solid var(--border-subtle);
}
.upload-task {
  min-height: 34px;
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto auto;
  align-items: center;
  gap: 8px;
  padding: 0 8px;
  border-radius: var(--radius-control);
  background: var(--surface-soft);
  color: var(--text-secondary);
  font-size: 0.75rem;
}
.upload-task.failed {
  color: var(--danger-color);
}
.upload-task.completed {
  color: var(--success-color);
}
.upload-task span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.upload-task button {
  width: 28px;
  height: 28px;
  display: grid;
  place-items: center;
  padding: 0;
  border: 0;
  background: transparent;
  color: currentColor;
  cursor: pointer;
}
.spin {
  animation: spin 0.8s linear infinite;
}
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
.queue-enter-active,
.queue-leave-active {
  transition:
    opacity var(--motion-fast-duration),
    transform var(--motion-fast-duration);
}
.queue-enter-from,
.queue-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}
</style>
