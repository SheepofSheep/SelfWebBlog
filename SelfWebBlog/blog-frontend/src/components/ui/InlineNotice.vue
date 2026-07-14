<script setup>
import { CircleAlert, CircleCheck, Info } from 'lucide-vue-next'
import { computed } from 'vue'

const props = defineProps({
  tone: {
    type: String,
    default: 'info',
    validator: (value) => ['info', 'success', 'danger'].includes(value)
  }
})

const icon = computed(() => {
  if (props.tone === 'success') return CircleCheck
  if (props.tone === 'danger') return CircleAlert
  return Info
})
</script>

<template>
  <div class="inline-notice" :class="`inline-notice--${tone}`" role="status">
    <component :is="icon" :size="17" />
    <div><slot /></div>
  </div>
</template>

<style scoped>
.inline-notice {
  display: grid;
  grid-template-columns: auto 1fr;
  align-items: start;
  gap: 10px;
  padding: 11px 13px;
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-control);
  background: var(--surface-soft);
  color: var(--text-secondary);
  font-size: 0.82rem;
}
.inline-notice--success {
  color: var(--success-color);
}
.inline-notice--danger {
  color: var(--danger-color);
}
</style>
