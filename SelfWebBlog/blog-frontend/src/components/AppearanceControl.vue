<script setup>
import { onBeforeUnmount, ref } from 'vue'
import { Monitor, Moon, Sparkles, Sun } from 'lucide-vue-next'
import { useAppearance } from '../composables/useAppearance'
import IconButton from './ui/IconButton.vue'
import SegmentedControl from './ui/SegmentedControl.vue'

const open = ref(false)
const root = ref(null)
const { theme, motionMode, effectiveTheme, setTheme, setMotionMode } = useAppearance()

const themeOptions = [
  { value: 'light', label: '浅色', icon: Sun },
  { value: 'dark', label: '深色', icon: Moon },
  { value: 'system', label: '跟随系统', icon: Monitor }
]
const motionOptions = [
  { value: 'full', label: '完整' },
  { value: 'reduced', label: '精简' },
  { value: 'off', label: '关闭' }
]

function chooseTheme(value, event) {
  setTheme(value, { x: event.clientX, y: event.clientY })
}

function closeOutside(event) {
  if (!root.value?.contains(event.target)) open.value = false
}

document.addEventListener('pointerdown', closeOutside)
onBeforeUnmount(() => document.removeEventListener('pointerdown', closeOutside))
</script>

<template>
  <div ref="root" class="appearance-control">
    <IconButton
      class="appearance-trigger"
      :aria-expanded="open"
      label="外观设置"
      @click="open = !open"
    >
      <Sun v-if="effectiveTheme === 'light'" :size="18" />
      <Moon v-else :size="18" />
    </IconButton>

    <Transition name="popover">
      <div v-if="open" class="appearance-menu" role="menu">
        <section>
          <p>主题</p>
          <SegmentedControl
            :model-value="theme"
            :options="themeOptions"
            label="主题模式"
            @update:model-value="chooseTheme($event, { clientX: innerWidth - 36, clientY: 36 })"
          />
        </section>
        <section>
          <p><Sparkles :size="13" /> 动效</p>
          <SegmentedControl
            :model-value="motionMode"
            :options="motionOptions"
            label="动效级别"
            @update:model-value="setMotionMode"
          />
        </section>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.appearance-control {
  position: relative;
}
.appearance-menu {
  position: absolute;
  top: calc(100% + 12px);
  right: 0;
  z-index: 260;
  width: min(310px, calc(100vw - 32px));
  padding: 14px;
  border: 1px solid var(--border-medium);
  border-radius: var(--radius-card);
  background: var(--surface-solid);
  box-shadow: var(--shadow-float);
}
.appearance-menu section + section {
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px solid var(--border-light);
}
.appearance-menu p {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 0 0 8px;
  color: var(--text-muted);
  font-size: 0.75rem;
  font-weight: 800;
}
.popover-enter-active,
.popover-leave-active {
  transition:
    opacity var(--motion-fast),
    transform var(--motion-fast);
}
.popover-enter-from,
.popover-leave-to {
  opacity: 0;
  transform: translateY(-6px) scale(0.98);
}
@media (max-width: 560px) {
  .appearance-menu {
    position: fixed;
    top: 58px;
    right: 10px;
    width: min(310px, calc(100vw - 20px));
  }
}
</style>
