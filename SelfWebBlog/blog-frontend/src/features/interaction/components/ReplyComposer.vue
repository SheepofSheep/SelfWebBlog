<script setup>
import { Send, X } from 'lucide-vue-next'
import { nextTick, ref } from 'vue'

defineProps({
  placeholder: { type: String, default: '写下回复...' },
  submitLabel: { type: String, default: '回复' },
  busy: { type: Boolean, default: false },
  cancelable: { type: Boolean, default: false }
})

const emit = defineEmits(['submit', 'cancel'])
const content = ref('')
const textarea = ref(null)

async function submit() {
  const value = content.value.trim()
  if (!value) return
  emit('submit', value, () => {
    content.value = ''
  })
}

async function focus() {
  await nextTick()
  textarea.value?.focus()
}

defineExpose({ focus })
</script>

<template>
  <form class="reply-composer" @submit.prevent="submit">
    <textarea
      ref="textarea"
      v-model="content"
      :placeholder="placeholder"
      rows="3"
      maxlength="2000"
      :disabled="busy"
      required
    />
    <div class="composer-footer">
      <span>{{ content.length }} / 2000</span>
      <div>
        <button v-if="cancelable" class="quiet-action" type="button" @click="emit('cancel')">
          <X :size="15" /> 取消
        </button>
        <button class="submit-action" type="submit" :disabled="busy || !content.trim()">
          <Send :size="15" /> {{ busy ? '发送中' : submitLabel }}
        </button>
      </div>
    </div>
  </form>
</template>

<style scoped>
.reply-composer {
  display: grid;
  gap: 9px;
}
textarea {
  width: 100%;
  min-height: 88px;
  resize: vertical;
  padding: 13px 14px;
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-control);
  background: var(--surface-solid);
  color: var(--text-primary);
  font: inherit;
  line-height: 1.7;
  transition:
    border-color var(--motion-fast-duration),
    box-shadow var(--motion-fast-duration);
}
textarea:focus {
  border-color: var(--accent);
  box-shadow: 0 0 0 3px var(--focus-ring);
  outline: none;
}
.composer-footer,
.composer-footer > div,
button {
  display: flex;
  align-items: center;
}
.composer-footer {
  justify-content: space-between;
  gap: 12px;
  color: var(--text-tertiary);
  font-size: 0.72rem;
}
.composer-footer > div {
  gap: 8px;
}
button {
  min-height: 34px;
  gap: 6px;
  padding: 0 11px;
  border-radius: var(--radius-control);
  font: inherit;
  font-size: 0.78rem;
  cursor: pointer;
}
.quiet-action {
  border: 1px solid var(--border-subtle);
  background: transparent;
  color: var(--text-secondary);
}
.submit-action {
  border: 1px solid var(--accent);
  background: var(--accent);
  color: #21180a;
  font-weight: 700;
}
button:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}
</style>
