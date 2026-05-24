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

watch(() => props.modelValue, (newValue) => {
  isVisible.value = newValue
})

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
    <div class="confirm-dialog" @click.stop>
      <h3 class="confirm-title">{{ title }}</h3>
      <p class="confirm-message">{{ message }}</p>
      <div class="confirm-footer">
        <button class="confirm-btn cancel-btn" @click="handleCancel">
          {{ cancelText }}
        </button>
        <button class="confirm-btn confirm-ok" @click="handleConfirm">
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
  background: var(--paper);
  border: 1px solid var(--rule);
  padding: 2rem;
  max-width: 400px;
  width: 90%;
  animation: scaleIn 0.25s var(--ease-out);
}

.confirm-title {
  margin: 0 0 0.75rem;
  font-family: var(--font-display);
  font-size: var(--fs-title);
  font-weight: var(--fw-bold);
  color: var(--ink);
}

.confirm-message {
  margin: 0 0 1.5rem;
  font-size: var(--fs-body);
  color: var(--ink-muted);
  line-height: var(--lh-body);
}

.confirm-footer {
  display: flex;
  gap: var(--space-sm);
  justify-content: flex-end;
}

.confirm-btn {
  padding: 8px 20px;
  font-size: var(--fs-body);
  font-weight: var(--fw-bold);
  font-family: var(--font-body);
  cursor: pointer;
  transition: background var(--duration-fast) var(--ease-out), color var(--duration-fast) var(--ease-out), border-color var(--duration-fast) var(--ease-out), transform var(--duration-fast) var(--ease-out);
  border: 1px solid var(--rule);
  background: transparent;
  color: var(--ink-muted);
}

.confirm-btn:hover {
  background: var(--paper-dim);
}

.confirm-ok {
  background: var(--red);
  color: var(--on-primary);
  border-color: var(--red);
}

.confirm-ok:hover {
  background: var(--red-hover);
  transform: translateY(-1px);
}

@keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
@keyframes scaleIn {
  from { opacity: 0; transform: scale(0.95); }
  to { opacity: 1; transform: scale(1); }
}

@media (max-width: 768px) {
  .confirm-dialog { padding: 1.5rem; width: 95%; }
}
</style>
