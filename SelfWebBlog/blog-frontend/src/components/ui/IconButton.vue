<script setup>
defineProps({
  label: { type: String, required: true },
  active: { type: Boolean, default: false },
  disabled: { type: Boolean, default: false }
})

defineEmits(['click'])
</script>

<template>
  <button
    class="icon-button"
    type="button"
    :class="{ active }"
    :aria-label="label"
    :aria-pressed="active || undefined"
    :title="label"
    :disabled="disabled"
    @click="$emit('click', $event)"
  >
    <slot />
  </button>
</template>

<style scoped>
.icon-button {
  width: 40px;
  height: 40px;
  display: inline-grid;
  flex: 0 0 auto;
  place-items: center;
  padding: 0;
  border: 1px solid transparent;
  border-radius: var(--radius-control);
  background: transparent;
  color: var(--text-secondary);
  cursor: pointer;
  transition:
    color var(--motion-fast-duration),
    background var(--motion-fast-duration),
    border-color var(--motion-fast-duration),
    transform var(--motion-fast-duration);
}
.icon-button:hover,
.icon-button.active {
  border-color: var(--border-subtle);
  background: var(--surface-soft);
  color: var(--text-primary);
}
.icon-button:active {
  transform: translateY(1px);
}
.icon-button:disabled {
  opacity: 0.42;
  cursor: not-allowed;
}
</style>
