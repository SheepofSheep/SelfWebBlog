<script setup>
defineProps({ label: { type: String, default: '这里还没有内容' } })
</script>
<template>
  <div class="empty-state glass">
    <span>◌</span>
    <p>{{ label }}</p>
    <slot />
  </div>
</template>
