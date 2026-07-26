<script setup>
import { computed, ref, watch } from 'vue'
import { avatarInitial } from '@/core/format.js'

const props = defineProps({
  src: { type: String, default: '' },
  name: { type: String, default: '' },
  size: { type: [Number, String], default: 40 },
  rounded: { type: Boolean, default: false }
})

const failed = ref(false)
const dimension = computed(() =>
  typeof props.size === 'number' ? `${props.size}px` : String(props.size)
)
const style = computed(() => ({ width: dimension.value, height: dimension.value }))

watch(
  () => props.src,
  () => {
    failed.value = false
  }
)
</script>

<template>
  <span
    class="app-avatar"
    :class="{ rounded }"
    :style="style"
    :role="!src || failed ? 'img' : undefined"
    :aria-label="!src || failed ? name || '头像' : undefined"
  >
    <img v-if="src && !failed" :src="src" :alt="name || '头像'" @error="failed = true" />
    <span v-else aria-hidden="true">{{ avatarInitial(name) }}</span>
  </span>
</template>

<style scoped>
.app-avatar {
  display: inline-grid;
  flex: 0 0 auto;
  overflow: hidden;
  place-items: center;
  border: 1px solid color-mix(in srgb, var(--line) 76%, white);
  border-radius: 50%;
  color: var(--accent-deep);
  background:
    radial-gradient(circle at 32% 22%, rgb(255 255 255 / 76%), transparent 42%), var(--accent-soft);
  box-shadow: inset 0 1px 0 rgb(255 255 255 / 55%);
  font-weight: 700;
  line-height: 1;
}
.app-avatar.rounded {
  border-radius: 30%;
}
.app-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
</style>
