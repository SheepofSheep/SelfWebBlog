<script setup>
defineProps({
  title: { type: String, required: true },
  description: { type: String, default: '' }
})
</script>

<template>
  <div class="empty-state">
    <span v-if="$slots.icon" class="empty-icon"><slot name="icon" /></span>
    <h3>{{ title }}</h3>
    <p v-if="description">{{ description }}</p>
    <div v-if="$slots.default" class="empty-actions"><slot /></div>
  </div>
</template>

<style scoped>
.empty-state {
  min-height: 220px;
  display: grid;
  place-items: center;
  align-content: center;
  gap: 8px;
  padding: 28px;
  color: var(--text-secondary);
  text-align: center;
}
.empty-icon {
  display: grid;
  width: 44px;
  height: 44px;
  place-items: center;
  margin-bottom: 4px;
  border-radius: var(--radius-card);
  background: var(--accent-soft);
  color: var(--accent-strong);
}
h3,
p {
  margin: 0;
}
h3 {
  color: var(--text-primary);
  font-size: 1rem;
}
p {
  max-width: 38ch;
  font-size: 0.84rem;
}
.empty-actions {
  margin-top: 10px;
}
</style>
