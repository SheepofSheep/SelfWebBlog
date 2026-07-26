<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
const dots = ref([])
let nextId = 0
const timers = new Set()
function clear() {
  dots.value = []
  timers.forEach((timer) => clearTimeout(timer))
  timers.clear()
}
function move(event) {
  if (
    document.documentElement.dataset.motion === 'reduced' ||
    document.hidden ||
    !document.hasFocus() ||
    matchMedia('(pointer: coarse)').matches ||
    matchMedia('(prefers-reduced-motion: reduce)').matches
  )
    return
  const dot = { id: nextId++, x: event.clientX, y: event.clientY }
  dots.value = [...dots.value.slice(-11), dot]
  const timer = setTimeout(() => {
    dots.value = dots.value.filter((item) => item.id !== dot.id)
    timers.delete(timer)
  }, 650)
  timers.add(timer)
}
onMounted(() => {
  window.addEventListener('pointermove', move, { passive: true })
  document.addEventListener('visibilitychange', clear)
  window.addEventListener('blur', clear)
})
onBeforeUnmount(() => {
  window.removeEventListener('pointermove', move)
  document.removeEventListener('visibilitychange', clear)
  window.removeEventListener('blur', clear)
  clear()
})
</script>
<template>
  <div class="pointer-trail" aria-hidden="true">
    <i
      v-for="(dot, index) in dots"
      :key="dot.id"
      :style="{
        transform: `translate3d(${dot.x}px,${dot.y}px,0) scale(${0.35 + index / 18})`,
        opacity: index / dots.length
      }"
    />
  </div>
</template>
