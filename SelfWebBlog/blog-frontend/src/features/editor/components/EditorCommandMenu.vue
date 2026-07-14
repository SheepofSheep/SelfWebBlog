<script setup>
defineProps({ open: { type: Boolean, default: false } })
defineEmits(['select'])

const options = [
  { value: 'heading', label: '小标题' },
  { value: 'quote', label: '引用' },
  { value: 'codeBlock', label: '代码块' },
  { value: 'task', label: '任务列表' },
  { value: 'table', label: '表格' }
]
</script>

<template>
  <Transition name="menu">
    <div v-if="open" class="command-menu" role="menu" aria-label="插入内容">
      <button
        v-for="item in options"
        :key="item.value"
        type="button"
        role="menuitem"
        @click="$emit('select', item.value)"
      >
        {{ item.label }}
      </button>
    </div>
  </Transition>
</template>

<style scoped>
.command-menu {
  position: absolute;
  top: 46px;
  right: 8px;
  z-index: 20;
  width: 150px;
  display: grid;
  padding: 6px;
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-control);
  background: var(--surface-solid);
  box-shadow: var(--shadow-panel);
}
button {
  min-height: 34px;
  padding: 0 9px;
  border: 0;
  border-radius: 4px;
  background: transparent;
  color: var(--text-secondary);
  font: inherit;
  font-size: 0.75rem;
  text-align: left;
  cursor: pointer;
}
button:hover {
  background: var(--surface-soft);
  color: var(--text-primary);
}
.menu-enter-active,
.menu-leave-active {
  transition:
    opacity var(--motion-fast-duration),
    transform var(--motion-fast-duration);
}
.menu-enter-from,
.menu-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}
</style>
