<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { CloudFog, Monitor, Moon, Sparkles, Sun } from 'lucide-vue-next'
import { installAppearance } from '@/core/appearance.js'
import { appStore, setBackgroundBlur } from '@/stores/app.js'

const props = defineProps({ trigger: { type: Object, default: null } })
const emit = defineEmits(['close'])
const appearance = installAppearance()
const selected = ref(appearance.current())
const motion = ref(appearance.motion())
const blur = computed({
  get: () => appStore.backgroundBlur,
  set: (value) => setBackgroundBlur(value)
})
const root = ref(null)
let previousFocus = null
const options = [
  ['light', '亮色', Sun],
  ['dark', '暗色', Moon],
  ['system', '跟随系统', Monitor]
]
function choose(mode, event) {
  selected.value = mode
  appearance.apply(mode, event)
}
function toggleMotion() {
  motion.value = motion.value === 'full' ? 'reduced' : 'full'
  appearance.setMotion(motion.value)
}

function close({ restoreFocus = false } = {}) {
  const target = props.trigger || previousFocus
  emit('close')
  if (restoreFocus) nextTick(() => target?.isConnected && target.focus())
}

function onDocumentPointerDown(event) {
  if (!(event.target instanceof Node)) return
  if (root.value?.contains(event.target) || props.trigger?.contains?.(event.target)) return
  close()
}

function onDocumentKeyDown(event) {
  if (event.key !== 'Escape') return
  event.preventDefault()
  close({ restoreFocus: true })
}

onMounted(async () => {
  previousFocus = props.trigger || document.activeElement
  document.addEventListener('pointerdown', onDocumentPointerDown)
  document.addEventListener('keydown', onDocumentKeyDown)
  await nextTick()
  root.value?.querySelector('button')?.focus()
})
onBeforeUnmount(() => {
  document.removeEventListener('pointerdown', onDocumentPointerDown)
  document.removeEventListener('keydown', onDocumentKeyDown)
})
</script>

<template>
  <div ref="root" class="appearance-popover glass" role="dialog" aria-label="外观设置">
    <button
      v-for="item in options"
      :key="item[0]"
      :class="{ active: selected === item[0] }"
      @click="choose(item[0], $event)"
    >
      <component :is="item[2]" :size="17" />{{ item[1] }}
    </button>
    <button @click="toggleMotion">
      <Sparkles :size="17" />{{ motion === 'full' ? '完整动效' : '减少动效' }}
    </button>
    <label class="appearance-blur">
      <span
        ><CloudFog :size="17" />背景模糊 <output>{{ blur }}%</output></span
      >
      <input
        v-model.number="blur"
        type="range"
        min="0"
        max="100"
        step="1"
        aria-label="背景模糊度"
      />
    </label>
  </div>
</template>
