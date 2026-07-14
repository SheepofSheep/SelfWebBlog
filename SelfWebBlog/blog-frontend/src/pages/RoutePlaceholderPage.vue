<script setup>
import { ArrowLeft } from 'lucide-vue-next'
import { useRouter } from 'vue-router'

defineProps({
  title: { type: String, required: true },
  description: { type: String, default: '' }
})

const router = useRouter()
</script>

<template>
  <section class="route-placeholder">
    <p class="route-kicker">Gabriel's Blog</p>
    <h1>{{ title }}</h1>
    <p v-if="description">{{ description }}</p>
    <button type="button" @click="router.push('/')">
      <ArrowLeft :size="17" />
      返回首页
    </button>
  </section>
</template>

<style scoped>
.route-placeholder {
  width: min(1120px, calc(100% - 32px));
  min-height: 62vh;
  margin: 0 auto;
  display: grid;
  align-content: center;
  justify-items: start;
  gap: 14px;
}
.route-kicker {
  color: var(--primary-hover);
  font-size: 0.78rem;
  font-weight: 800;
  text-transform: uppercase;
}
h1,
p {
  margin: 0;
}
h1 {
  font-family: var(--font-serif);
  font-size: clamp(2.4rem, 7vw, 5.4rem);
}
p {
  color: var(--text-secondary);
}
button {
  min-height: 42px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin-top: 14px;
  padding: 0 16px;
  border: 1px solid var(--border-medium);
  border-radius: 7px;
  background: var(--surface-base);
  color: var(--text-main);
  font: inherit;
  cursor: pointer;
}
</style>
