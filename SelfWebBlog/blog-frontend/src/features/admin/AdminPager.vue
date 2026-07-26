<script setup>
import { ChevronLeft, ChevronRight } from 'lucide-vue-next'

defineProps({
  page: { type: Number, default: 1 },
  pages: { type: Number, default: 1 },
  total: { type: Number, default: 0 },
  busy: Boolean
})
defineEmits(['change'])
</script>

<template>
  <nav v-if="pages > 1" class="admin-pager" aria-label="分页">
    <span>共 {{ total }} 项</span>
    <div>
      <button
        class="icon-button"
        type="button"
        aria-label="上一页"
        :disabled="busy || page <= 1"
        @click="$emit('change', page - 1)"
      >
        <ChevronLeft :size="17" />
      </button>
      <strong>{{ page }} / {{ pages }}</strong>
      <button
        class="icon-button"
        type="button"
        aria-label="下一页"
        :disabled="busy || page >= pages"
        @click="$emit('change', page + 1)"
      >
        <ChevronRight :size="17" />
      </button>
    </div>
  </nav>
</template>

<style scoped>
.admin-pager {
  display: flex;
  gap: 16px;
  align-items: center;
  justify-content: space-between;
  padding-top: 18px;
  color: var(--muted);
  font-size: 12px;
}
.admin-pager > div {
  display: flex;
  gap: 8px;
  align-items: center;
}
.admin-pager strong {
  min-width: 70px;
  color: var(--ink);
  text-align: center;
}
</style>
