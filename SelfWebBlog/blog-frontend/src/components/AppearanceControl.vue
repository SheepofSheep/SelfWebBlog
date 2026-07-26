<script setup>
import { ref } from 'vue'
import { Monitor, Moon, Sparkles, Sun } from 'lucide-vue-next'
import { installAppearance } from '@/core/appearance.js'

defineEmits(['close'])
const appearance = installAppearance()
const selected = ref(appearance.current())
const motion = ref(appearance.motion())
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
</script>

<template>
  <div class="appearance-popover glass" role="dialog" aria-label="外观设置">
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
  </div>
</template>
