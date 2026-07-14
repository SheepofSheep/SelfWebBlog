<script setup>
defineProps({
  modelValue: { type: [String, Number], required: true },
  options: { type: Array, required: true },
  label: { type: String, required: true }
})

defineEmits(['update:modelValue'])
</script>

<template>
  <div class="segmented-control" role="group" :aria-label="label">
    <button
      v-for="option in options"
      :key="option.value"
      type="button"
      :class="{ active: modelValue === option.value }"
      :aria-pressed="modelValue === option.value"
      @click="$emit('update:modelValue', option.value)"
    >
      <component :is="option.icon" v-if="option.icon" :size="15" />
      <span>{{ option.label }}</span>
    </button>
  </div>
</template>

<style scoped>
.segmented-control {
  display: grid;
  grid-auto-flow: column;
  grid-auto-columns: minmax(0, 1fr);
  gap: 3px;
  padding: 3px;
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-card);
  background: var(--surface-soft);
}
.segmented-control button {
  min-width: 0;
  min-height: 36px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 6px 10px;
  border: 0;
  border-radius: var(--radius-control);
  background: transparent;
  color: var(--text-secondary);
  font: inherit;
  font-size: 0.78rem;
  cursor: pointer;
}
.segmented-control button.active {
  background: var(--surface-solid);
  color: var(--text-primary);
  box-shadow: 0 2px 8px rgb(54 41 20 / 9%);
}
</style>
