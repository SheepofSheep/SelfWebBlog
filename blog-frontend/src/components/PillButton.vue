<script setup>
defineProps({
  variant: { type: String, default: 'primary' },
  disabled: { type: Boolean, default: false }
})
defineEmits(['click'])
</script>

<template>
  <button
    class="pill-btn"
    :class="[`pill-btn-${variant}`]"
    :disabled="disabled"
    @click="$emit('click', $event)"
  >
    <slot></slot>
  </button>
</template>
