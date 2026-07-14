<script setup>
import { ref, watch } from 'vue'
import AppDialog from './ui/AppDialog.vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  title: { type: String, default: '确认操作' },
  message: { type: String, default: '确定要执行此操作吗？' },
  confirmText: { type: String, default: '确定' },
  cancelText: { type: String, default: '取消' },
  tone: { type: String, default: 'danger' },
  requiredText: { type: String, default: '' }
})

const emit = defineEmits(['update:modelValue', 'confirm', 'cancel'])
const typed = ref('')
watch(
  () => props.modelValue,
  (open) => {
    if (open) typed.value = ''
  }
)
function cancel() {
  emit('cancel')
  emit('update:modelValue', false)
}
function confirm() {
  if (props.requiredText && typed.value !== props.requiredText) return
  emit('confirm')
  emit('update:modelValue', false)
}
</script>

<template>
  <AppDialog :open="modelValue" :title="title" :description="message" @close="cancel">
    <p class="confirm-note">请确认这不是误操作。</p>
    <label v-if="requiredText" class="confirm-proof">
      <span
        >输入 <strong>{{ requiredText }}</strong> 继续</span
      >
      <input v-model="typed" type="text" autocomplete="off" autofocus />
    </label>
    <template #footer>
      <button class="confirm-button secondary" type="button" @click="cancel">
        {{ cancelText }}
      </button>
      <button
        :class="['confirm-button', tone]"
        type="button"
        :disabled="requiredText && typed !== requiredText"
        @click="confirm"
      >
        {{ confirmText }}
      </button>
    </template>
  </AppDialog>
</template>

<style scoped>
.confirm-note {
  margin: 0;
  color: var(--text-tertiary);
  font-size: 0.76rem;
}
.confirm-proof {
  display: grid;
  gap: 7px;
  margin-top: 14px;
  color: var(--text-secondary);
  font-size: 0.74rem;
}
.confirm-proof input {
  min-height: 40px;
  padding: 0 10px;
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-control);
  background: var(--surface-raised);
  color: var(--text-primary);
  font: inherit;
  outline: 0;
}
.confirm-proof input:focus {
  border-color: var(--accent);
  box-shadow: 0 0 0 3px var(--focus-ring);
}
.confirm-button {
  min-height: 40px;
  padding: 0 14px;
  border: 1px solid var(--border-subtle);
  border-radius: 6px;
  background: var(--surface-raised);
  color: var(--text-secondary);
  font: inherit;
  font-size: 0.75rem;
  font-weight: 800;
  cursor: pointer;
}
.confirm-button.primary {
  border-color: var(--accent);
  background: var(--accent);
  color: #2c1d06;
}
.confirm-button.danger {
  border-color: var(--danger);
  background: var(--danger);
  color: #fff;
}
.confirm-button:focus-visible {
  outline: 3px solid var(--focus-ring);
  outline-offset: 2px;
}
.confirm-button:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}
</style>
