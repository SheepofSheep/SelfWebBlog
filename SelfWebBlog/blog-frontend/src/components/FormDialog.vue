<script setup>
import AppDialog from './AppDialog.vue'

defineProps({
  open: Boolean,
  title: { type: String, default: '' },
  description: { type: String, default: '' },
  submitLabel: { type: String, default: '保存' },
  cancelLabel: { type: String, default: '取消' },
  busy: Boolean,
  valid: { type: Boolean, default: true },
  size: { type: String, default: 'small' }
})
defineEmits(['submit', 'close'])
</script>

<template>
  <AppDialog
    :open="open"
    :title="title"
    :description="description"
    :size="size"
    :close-disabled="busy"
    @close="$emit('close')"
  >
    <form class="form-dialog" @submit.prevent="!busy && valid && $emit('submit')">
      <fieldset class="form-dialog-fields" :disabled="busy"><slot /></fieldset>
      <div class="form-dialog-actions">
        <button class="secondary-button" type="button" :disabled="busy" @click="$emit('close')">
          {{ cancelLabel }}
        </button>
        <button class="primary-button" :disabled="busy || !valid">
          {{ busy ? '保存中' : submitLabel }}
        </button>
      </div>
    </form>
  </AppDialog>
</template>

<style scoped>
.form-dialog {
  display: grid;
  gap: 16px;
}
.form-dialog-fields {
  display: grid;
  gap: 16px;
  min-width: 0;
  margin: 0;
  padding: 0;
  border: 0;
}
.form-dialog-actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  padding-top: 4px;
}
</style>
