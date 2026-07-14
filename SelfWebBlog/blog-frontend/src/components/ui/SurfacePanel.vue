<script setup>
defineProps({
  variant: {
    type: String,
    default: 'solid',
    validator: (value) => ['solid', 'glass'].includes(value)
  },
  as: { type: String, default: 'section' }
})
</script>

<template>
  <component :is="as" class="surface-panel" :class="`surface-panel--${variant}`">
    <slot />
  </component>
</template>

<style scoped>
.surface-panel {
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-card);
  box-shadow: var(--shadow-panel);
}
.surface-panel--solid {
  background: var(--surface-solid);
}
.surface-panel--glass {
  background: var(--surface-glass);
  backdrop-filter: blur(16px) saturate(1.06);
}
</style>
