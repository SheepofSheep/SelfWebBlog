<script setup>
import { nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { X } from 'lucide-vue-next'
import IconButton from './IconButton.vue'

const props = defineProps({
  open: { type: Boolean, default: false },
  title: { type: String, required: true },
  description: { type: String, default: '' },
  closeLabel: { type: String, default: '关闭对话框' }
})

const emit = defineEmits(['close'])
const dialog = ref(null)
let previousFocus = null

function close() {
  emit('close')
}

function onKeydown(event) {
  if (event.key === 'Escape') close()
  if (event.key !== 'Tab' || !dialog.value) return

  const focusable = [
    ...dialog.value.querySelectorAll('button, a, input, textarea, select, [tabindex]')
  ].filter((element) => !element.disabled && element.tabIndex >= 0)
  if (!focusable.length) return
  const first = focusable[0]
  const last = focusable.at(-1)
  if (event.shiftKey && document.activeElement === first) {
    event.preventDefault()
    last.focus()
  } else if (!event.shiftKey && document.activeElement === last) {
    event.preventDefault()
    first.focus()
  }
}

watch(
  () => props.open,
  async (open) => {
    if (open) {
      previousFocus = document.activeElement
      document.body.style.overflow = 'hidden'
      await nextTick()
      dialog.value
        ?.querySelector('[autofocus], input, textarea, select, button, a, [tabindex]')
        ?.focus()
    } else {
      document.body.style.removeProperty('overflow')
      previousFocus?.focus?.()
    }
  }
)

onBeforeUnmount(() => document.body.style.removeProperty('overflow'))
</script>

<template>
  <Teleport to="body">
    <Transition name="dialog">
      <div v-if="open" class="dialog-backdrop" @mousedown.self="close">
        <section
          ref="dialog"
          class="dialog-panel"
          role="dialog"
          aria-modal="true"
          :aria-labelledby="`${title}-dialog-title`"
          @keydown="onKeydown"
        >
          <div class="dialog-header">
            <div>
              <h2 :id="`${title}-dialog-title`">{{ title }}</h2>
              <p v-if="description">{{ description }}</p>
            </div>
            <IconButton :label="closeLabel" @click="close"><X :size="18" /></IconButton>
          </div>
          <div class="dialog-content"><slot /></div>
          <div v-if="$slots.footer" class="dialog-footer"><slot name="footer" /></div>
        </section>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.dialog-backdrop {
  position: fixed;
  inset: 0;
  z-index: 800;
  display: grid;
  place-items: center;
  padding: 20px;
  background: rgb(20 16 10 / 44%);
  backdrop-filter: blur(5px);
}
.dialog-panel {
  width: min(560px, 100%);
  max-height: min(760px, calc(100vh - 40px));
  overflow: auto;
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-card);
  background: var(--surface-solid);
  box-shadow: var(--shadow-float);
}
.dialog-header,
.dialog-footer {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  padding: 18px 20px;
}
.dialog-header {
  border-bottom: 1px solid var(--border-subtle);
}
.dialog-footer {
  justify-content: flex-end;
  border-top: 1px solid var(--border-subtle);
}
h2,
p {
  margin: 0;
}
h2 {
  font-size: 1.08rem;
}
p {
  margin-top: 4px;
  color: var(--text-secondary);
  font-size: 0.82rem;
}
.dialog-content {
  padding: 20px;
}
.dialog-enter-active,
.dialog-leave-active {
  transition: opacity var(--motion-normal-duration);
}
.dialog-enter-active .dialog-panel,
.dialog-leave-active .dialog-panel {
  transition: transform var(--motion-normal-duration) var(--motion-ease);
}
.dialog-enter-from,
.dialog-leave-to {
  opacity: 0;
}
.dialog-enter-from .dialog-panel,
.dialog-leave-to .dialog-panel {
  transform: translateY(10px) scale(0.985);
}
</style>
