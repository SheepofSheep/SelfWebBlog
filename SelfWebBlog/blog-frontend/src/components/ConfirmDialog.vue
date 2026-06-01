<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  title: { type: String, default: '确认操作' },
  message: { type: String, default: '确定要执行此操作吗？' },
  confirmText: { type: String, default: '确定' },
  cancelText: { type: String, default: '取消' }
})

const emit = defineEmits(['update:modelValue', 'confirm', 'cancel'])

const isVisible = ref(props.modelValue)

watch(
  () => props.modelValue,
  (newValue) => {
    isVisible.value = newValue
  }
)

function handleConfirm() {
  emit('confirm')
  emit('update:modelValue', false)
}

function handleCancel() {
  emit('cancel')
  emit('update:modelValue', false)
}
</script>

<template>
  <div v-if="isVisible" class="confirm-backdrop" @click="handleCancel">
    <div class="confirm-dialog glass-card" role="dialog" aria-modal="true" @click.stop>
      <h3 class="confirm-title">{{ title }}</h3>
      <p class="confirm-message">{{ message }}</p>
      <div class="confirm-footer">
        <button class="pill-btn pill-btn-ghost" @click="handleCancel">
          {{ cancelText }}
        </button>
        <button class="pill-btn confirm-ok" @click="handleConfirm">
          {{ confirmText }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.confirm-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(60, 45, 45, 0.18);
  backdrop-filter: blur(12px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  animation: fadeIn 0.3s ease;
}

.confirm-dialog {
  padding: 2rem;
  max-width: 400px;
  width: 90%;
  animation: scaleIn 0.25s var(--ease-out);
}

.confirm-title {
  margin: 0 0 0.75rem;
  font-size: var(--font-size-lg);
  font-weight: 600;
  color: var(--text-main);
}

.confirm-message {
  margin: 0 0 1.5rem;
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
  line-height: var(--line-height);
}

.confirm-footer {
  display: flex;
  gap: var(--space-sm);
  justify-content: flex-end;
}

.confirm-ok {
  background: var(--danger);
  color: var(--on-primary);
  border-color: var(--danger);
}

.confirm-ok:hover {
  background: #c95f68;
  transform: translateY(-1px);
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}
@keyframes scaleIn {
  from {
    opacity: 0;
    transform: scale(0.95);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

@media (max-width: 768px) {
  .confirm-dialog {
    padding: 1.5rem;
    width: 95%;
  }
}
</style>
