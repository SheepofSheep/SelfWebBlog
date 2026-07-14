<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useAppearance } from '../../composables/useAppearance'
import { shouldEnablePointerTrail } from '../../composables/motion/pointerTrailModel'

const { effectiveMotion } = useAppearance()
const finePointer = ref(false)
const documentVisible = ref(!document.hidden)
const dots = reactive(
  Array.from({ length: 10 }, (_, index) => ({ id: index, x: -20, y: -20, active: false }))
)
const enabled = computed(() =>
  shouldEnablePointerTrail({
    motion: effectiveMotion.value,
    finePointer: finePointer.value,
    documentVisible: documentVisible.value
  })
)
let pointerQuery
let cursor = 0
let lastPaint = 0
const timers = new Set()

function onPointerMove(event) {
  if (!enabled.value || performance.now() - lastPaint < 33) return
  lastPaint = performance.now()
  const dot = dots[cursor]
  cursor = (cursor + 1) % dots.length
  dot.x = event.clientX
  dot.y = event.clientY
  dot.active = false
  requestAnimationFrame(() => {
    dot.active = true
    const timer = window.setTimeout(() => {
      dot.active = false
      timers.delete(timer)
    }, 360)
    timers.add(timer)
  })
}

function onPointerChange(event) {
  finePointer.value = event.matches
}

function onVisibilityChange() {
  documentVisible.value = !document.hidden
}

onMounted(() => {
  pointerQuery = window.matchMedia('(hover: hover) and (pointer: fine)')
  finePointer.value = pointerQuery.matches
  pointerQuery.addEventListener('change', onPointerChange)
  window.addEventListener('pointermove', onPointerMove, { passive: true })
  document.addEventListener('visibilitychange', onVisibilityChange)
})

onBeforeUnmount(() => {
  pointerQuery?.removeEventListener('change', onPointerChange)
  window.removeEventListener('pointermove', onPointerMove)
  document.removeEventListener('visibilitychange', onVisibilityChange)
  timers.forEach((timer) => window.clearTimeout(timer))
})
</script>

<template>
  <div v-if="enabled" class="pointer-trail" aria-hidden="true">
    <i
      v-for="dot in dots"
      :key="dot.id"
      :class="{ active: dot.active }"
      :style="{ transform: `translate3d(${dot.x}px, ${dot.y}px, 0)` }"
    />
  </div>
</template>

<style scoped>
.pointer-trail {
  position: fixed;
  inset: 0;
  z-index: 700;
  overflow: hidden;
  pointer-events: none;
}
i {
  position: absolute;
  top: -3px;
  left: -3px;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--accent);
  opacity: 0;
  transition:
    opacity 360ms ease-out,
    scale 360ms ease-out;
}
i.active {
  opacity: 0.48;
  scale: 1;
}
i:not(.active) {
  scale: 0.25;
}
</style>
