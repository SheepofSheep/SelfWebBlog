<script setup>
defineProps({
  count: { type: Number, default: 3 }
})
</script>

<template>
  <div v-for="i in count" :key="i" class="skeleton-card glass-panel">
    <div class="sk-line sk-title loading-shimmer"></div>
    <div class="sk-line sk-meta loading-shimmer"></div>
    <div class="sk-line sk-text loading-shimmer"></div>
    <div class="sk-line sk-text short loading-shimmer"></div>
  </div>
</template>

<style scoped>
.skeleton-card {
  padding: var(--space-md);
  margin-bottom: var(--space-sm);
}

.skeleton-card:hover {
  transform: none;
}

.sk-line {
  height: 14px;
  margin-bottom: 10px;
}

.sk-title { width: 60%; height: 18px; }
.sk-meta { width: 30%; }
.sk-text { width: 100%; }
.sk-text.short { width: 75%; }
</style>
