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
  background: var(--paper);
  border: 1px solid var(--rule);
  overflow: hidden;
  transition: border-color var(--duration-fast) var(--ease-out);
}
.card:not(.no-hover):hover {
  border-color: var(--ink);
}
</style>
