<script setup>
defineProps({
  count: { type: Number, default: 3 }
})
</script>

<template>
  <div v-for="i in count" :key="i" class="skeleton-card">
    <div class="sk-line sk-title"></div>
    <div class="sk-line sk-meta"></div>
    <div class="sk-line sk-text"></div>
    <div class="sk-line sk-text short"></div>
  </div>
</template>

<style scoped>
.skeleton-card {
  padding: 20px;
  border-radius: var(--radius-lg);
  background: rgba(0, 0, 0, 0.02);
  border: 1px solid var(--border-light);
  margin-bottom: 12px;
}

.sk-line {
  height: 14px;
  border-radius: 7px;
  background: linear-gradient(90deg, rgba(0,0,0,0.04) 25%, rgba(0,0,0,0.08) 50%, rgba(0,0,0,0.04) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s ease-in-out infinite;
  margin-bottom: 10px;
}

.sk-title { width: 60%; height: 18px; }
.sk-meta { width: 30%; }
.sk-text { width: 100%; }
.sk-text.short { width: 75%; }

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}
</style>
