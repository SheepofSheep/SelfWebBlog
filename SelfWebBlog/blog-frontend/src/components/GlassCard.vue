<script setup>
defineProps({
  hover: { type: Boolean, default: true },
  padding: { type: String, default: '24px' }
})
</script>

<template>
  <div class="card" :class="{ 'no-hover': !hover }" :style="{ padding }">
    <slot></slot>
  </div>
</template>

<style scoped>
.card {
  position: relative;
  background: linear-gradient(180deg, rgba(255, 253, 247, 0.96), rgba(255, 246, 225, 0.9));
  border: 1px solid var(--rule);
  overflow: hidden;
  box-shadow: var(--shadow-paper);
  transition:
    border-color var(--duration-fast) var(--ease-out),
    box-shadow var(--duration-normal) var(--ease-soft),
    transform var(--duration-normal) var(--ease-spring);
}
.card:not(.no-hover):hover {
  border-color: var(--border-warm);
  box-shadow: var(--shadow-lift);
  transform: translateY(-2px);
}
</style>
