<script setup>
import AppDialog from './AppDialog.vue'

defineProps({
  open: Boolean,
  title: { type: String, default: '确认操作' },
  message: { type: String, default: '' },
  confirmLabel: { type: String, default: '确认' },
  cancelLabel: { type: String, default: '取消' },
  busy: Boolean,
  danger: Boolean
})
defineEmits(['confirm', 'close'])
</script>

<template>
  <AppDialog
    :open="open"
    :title="title"
    size="small"
    :close-disabled="busy"
    @close="$emit('close')"
  >
    <p class="confirm-message">{{ message }}</p>
    <template #footer>
      <button class="secondary-button" type="button" :disabled="busy" @click="$emit('close')">
        {{ cancelLabel }}
      </button>
      <button
        :class="danger ? 'danger-button' : 'primary-button'"
        type="button"
        :disabled="busy"
        @click="$emit('confirm')"
      >
        {{ busy ? '处理中' : confirmLabel }}
      </button>
    </template>
  </AppDialog>
</template>

<style scoped>
.confirm-message {
  margin: 0;
  color: var(--muted);
  line-height: 1.75;
  white-space: pre-wrap;
}
.danger-button {
  padding: 10px 17px;
  border: 0;
  border-radius: 12px;
  color: white;
  background: var(--danger);
  font-weight: 700;
}
</style>
