<script setup>
import { Search, RotateCw } from 'lucide-vue-next'

defineProps({
  modelValue: { type: String, default: '' },
  placeholder: { type: String, default: '搜索' },
  busy: Boolean,
  hideSearch: Boolean
})
defineEmits(['update:modelValue', 'submit', 'refresh'])
</script>

<template>
  <form class="admin-toolbar glass" @submit.prevent="$emit('submit')">
    <label v-if="!hideSearch" class="admin-search">
      <Search :size="17" />
      <input
        :value="modelValue"
        :placeholder="placeholder"
        aria-label="搜索"
        @input="$emit('update:modelValue', $event.target.value)"
      />
    </label>
    <div class="toolbar-filters"><slot /></div>
    <div class="toolbar-actions">
      <slot name="actions" />
      <button
        class="icon-button"
        type="button"
        :disabled="busy"
        aria-label="刷新"
        @click="$emit('refresh')"
      >
        <RotateCw :class="{ spinning: busy }" :size="17" />
      </button>
    </div>
  </form>
</template>

<style scoped>
.admin-toolbar {
  position: sticky;
  z-index: 25;
  top: 12px;
  display: grid;
  grid-template-columns: minmax(180px, 1fr) auto auto;
  gap: 10px;
  align-items: center;
  padding: 9px;
  margin-bottom: 14px;
  border-radius: 18px;
}
.admin-search {
  display: flex;
  gap: 8px;
  align-items: center;
  min-width: 0;
  padding: 0 10px;
  color: var(--muted);
}
.admin-search input {
  width: 100%;
  min-width: 0;
  padding: 8px 0;
  border: 0;
  outline: 0;
  color: var(--ink);
  background: transparent;
}
.toolbar-filters,
.toolbar-actions {
  display: flex;
  gap: 7px;
  align-items: center;
}
.spinning {
  animation: admin-spin 0.8s linear infinite;
}
@keyframes admin-spin {
  to {
    transform: rotate(360deg);
  }
}
@media (max-width: 720px) {
  .admin-toolbar {
    position: static;
    grid-template-columns: minmax(0, 1fr);
  }
  .admin-search,
  .toolbar-filters,
  .toolbar-actions {
    grid-column: 1;
    width: 100%;
  }
  .toolbar-filters {
    overflow-x: auto;
  }
  .toolbar-actions {
    flex-wrap: wrap;
  }
}
</style>
