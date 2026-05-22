<script setup>
import { computed } from 'vue'
import { useToast } from '../composables/toast'
import { CheckCircle, AlertTriangle, XCircle } from 'lucide-vue-next'

const { items } = useToast()

const iconOf = computed(() => (type) => {
  if (type === 'error') return XCircle
  if (type === 'warning') return AlertTriangle
  return CheckCircle
})
</script>

<template>
  <div class="toast-container">
    <div v-for="t in items" :key="t.id" class="toast-item" :class="'toast-' + t.type" role="status" aria-live="polite">
      <component :is="iconOf(t.type)" :size="16" class="toast-icon" />
      <span class="msg">{{ t.message }}</span>
    </div>
  </div>
</template>

<style scoped>
.toast-container {
  position: fixed;
  top: 1.5rem;
  left: 50%;
  transform: translateX(-50%);
  z-index: 9999;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  pointer-events: none;
}

.toast-item {
  pointer-events: auto;
  min-width: 260px;
  background: var(--paper);
  border: 1px solid var(--rule);
  padding: 0.75rem 1.25rem;
  display: flex;
  align-items: center;
  gap: 0.625rem;
  color: var(--ink);
  font-weight: var(--fw-medium);
  font-size: var(--fs-small);
  animation: toast-in 0.3s var(--ease-spring) forwards;
}

.toast-success .toast-icon { color: var(--sage); }
.toast-error .toast-icon { color: var(--danger); }
.toast-warning .toast-icon { color: var(--amber); }

@keyframes toast-in {
  from { opacity: 0; transform: translateY(-8px) scale(0.97); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}
</style>
