<script setup>
import { CheckCircle2, CircleAlert } from 'lucide-vue-next'

defineProps({ issues: { type: Array, default: () => [] } })
defineEmits(['focus'])
</script>

<template>
  <section class="publish-checklist" :class="{ ready: !issues.length }">
    <div v-if="!issues.length"><CheckCircle2 :size="17" /> 发布检查已通过</div>
    <template v-else>
      <p><CircleAlert :size="17" /> 发布前还需处理 {{ issues.length }} 项</p>
      <button v-for="item in issues" :key="`${item.field}-${item.message}`" type="button" @click="$emit('focus', item.field)">
        {{ item.message }}
      </button>
    </template>
  </section>
</template>

<style scoped>
.publish-checklist {
  display: grid;
  gap: 5px;
  padding: 11px 13px;
  border: 1px solid color-mix(in srgb, var(--danger-color) 35%, var(--border-subtle));
  border-radius: var(--radius-control);
  background: var(--surface-solid);
  color: var(--danger-color);
  font-size: 0.75rem;
}
.publish-checklist.ready {
  border-color: color-mix(in srgb, var(--success-color) 35%, var(--border-subtle));
  color: var(--success-color);
}
div,
p {
  min-height: 24px;
  display: flex;
  align-items: center;
  gap: 7px;
  margin: 0;
  font-weight: 750;
}
button {
  padding: 4px 0 4px 24px;
  border: 0;
  background: transparent;
  color: var(--text-secondary);
  font: inherit;
  text-align: left;
  cursor: pointer;
}
button:hover {
  color: var(--text-primary);
}
</style>
