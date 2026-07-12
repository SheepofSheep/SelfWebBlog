<script setup>
import { onBeforeUnmount, ref } from 'vue'
import { Check, Monitor, Moon, Sparkles, Sun } from 'lucide-vue-next'
import { useAppearance } from '../composables/useAppearance'

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
  { value: 'system', label: '跟随系统' }
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
    <button
      class="appearance-trigger"
      type="button"
      :aria-expanded="open"
      aria-haspopup="menu"
      aria-label="外观设置"
      title="外观设置"
      @click="open = !open"
    >
      <Sun v-if="effectiveTheme === 'light'" :size="18" />
      <Moon v-else :size="18" />
    </button>

    <Transition name="popover">
      <div v-if="open" class="appearance-menu" role="menu">
        <section>
          <p>主题</p>
          <div class="appearance-segments">
            <button
              v-for="option in themeOptions"
              :key="option.value"
              type="button"
              :class="{ active: theme === option.value }"
              @click="chooseTheme(option.value, $event)"
            >
              <component :is="option.icon" :size="14" />
              {{ option.label }}
              <Check v-if="theme === option.value" :size="12" />
            </button>
          </div>
        </section>
        <section>
          <p><Sparkles :size="13" /> 动效</p>
          <div class="appearance-segments compact">
            <button
              v-for="option in motionOptions"
              :key="option.value"
              type="button"
              :class="{ active: motionMode === option.value }"
              @click="setMotionMode(option.value)"
            >
              {{ option.label }}
            </button>
          </div>
        </section>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.appearance-control {
  position: relative;
}
.appearance-trigger {
  display: grid;
  width: 40px;
  height: 40px;
  place-items: center;
  border: 0;
  border-radius: 50%;
  background: transparent;
  color: var(--text-main);
  cursor: pointer;
}
.appearance-trigger:hover {
  background: var(--surface-muted);
  color: var(--primary-hover);
}
.appearance-menu {
  position: absolute;
  top: calc(100% + 12px);
  right: 0;
  z-index: 260;
  width: min(310px, calc(100vw - 32px));
  padding: 14px;
  border: 1px solid var(--border-medium);
  border-radius: 10px;
  background: var(--surface-strong);
  box-shadow: var(--shadow-lift);
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
.appearance-segments {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 4px;
  padding: 4px;
  border-radius: 8px;
  background: var(--surface-muted);
}
.appearance-segments button {
  display: flex;
  min-width: 0;
  min-height: 38px;
  align-items: center;
  justify-content: center;
  gap: 5px;
  padding: 7px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: var(--text-secondary);
  font: inherit;
  font-size: 0.72rem;
  cursor: pointer;
}
.appearance-segments button.active {
  background: var(--surface-strong);
  color: var(--text-main);
  box-shadow: 0 1px 4px rgba(20, 16, 10, 0.12);
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
</style>
