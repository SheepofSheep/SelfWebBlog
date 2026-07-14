<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'

const props = defineProps({ minHeight: { type: String, default: '120px' } })
const ready = ref(false)
let handle

onMounted(() => {
  if ('requestIdleCallback' in window) {
    handle = window.requestIdleCallback(
      () => {
        ready.value = true
      },
      { timeout: 900 }
    )
  } else {
    handle = window.setTimeout(() => {
      ready.value = true
    }, 180)
  }
})

onBeforeUnmount(() => {
  if ('cancelIdleCallback' in window) window.cancelIdleCallback(handle)
  else clearTimeout(handle)
})
</script>

<template>
  <slot v-if="ready" />
  <div
    v-else
    class="deferred-placeholder"
    :style="{ minHeight: props.minHeight }"
    aria-hidden="true"
  ></div>
</template>

<style scoped>
.deferred-placeholder {
  border-block: 1px solid var(--border-subtle);
  background: var(--surface-soft);
}
</style>
