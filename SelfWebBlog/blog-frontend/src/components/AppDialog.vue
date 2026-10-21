<script>
let openDialogCount = 0
</script>

<script setup>
import { nextTick, onBeforeUnmount, ref, useId, watch } from 'vue'
import { X } from 'lucide-vue-next'
import { nextDialogFocusIndex } from '@/core/dialogModel.js'

const props = defineProps({
  open: Boolean,
  title: { type: String, default: '' },
  description: { type: String, default: '' },
  label: { type: String, default: '对话框' },
  closeLabel: { type: String, default: '关闭' },
  closeOnBackdrop: { type: Boolean, default: true },
  closeDisabled: Boolean,
  size: { type: String, default: 'medium' }
})
const emit = defineEmits(['close'])
const panel = ref(null)
const titleId = `dialog-title-${useId()}`
const descriptionId = `dialog-description-${useId()}`
let previousFocus = null
let locked = false

const focusableSelector = [
  'a[href]',
  'button:not([disabled])',
  'input:not([disabled])',
  'select:not([disabled])',
  'textarea:not([disabled])',
  '[tabindex]:not([tabindex="-1"])'
].join(',')

function lockPage() {
  if (locked) return
  locked = true
  openDialogCount += 1
  document.documentElement.classList.add('dialog-open')
}

function unlockPage() {
  if (!locked) return
  locked = false
  openDialogCount = Math.max(0, openDialogCount - 1)
  if (!openDialogCount) document.documentElement.classList.remove('dialog-open')
}

function restoreFocus() {
  if (previousFocus?.isConnected) previousFocus.focus()
  previousFocus = null
}

function close() {
  if (!props.closeDisabled) emit('close')
}

function backdrop(event) {
  if (props.closeOnBackdrop && event.target === event.currentTarget) close()
}

function onKeydown(event) {
  if (event.key === 'Escape') {
    event.preventDefault()
    close()
    return
  }
  if (event.key !== 'Tab') return
  const focusable = [...(panel.value?.querySelectorAll(focusableSelector) || [])]
  if (!focusable.length) {
    event.preventDefault()
    panel.value?.focus()
    return
  }
  const current = focusable.indexOf(document.activeElement)
  const next = nextDialogFocusIndex(current, focusable.length, event.shiftKey ? -1 : 1)
  if (
    current < 0 ||
    (!event.shiftKey && current === focusable.length - 1) ||
    (event.shiftKey && current === 0)
  ) {
    event.preventDefault()
    focusable[next]?.focus()
  }
}

watch(
  () => props.open,
  async (open) => {
    if (open) {
      previousFocus = document.activeElement
      lockPage()
      await nextTick()
      const first =
        panel.value?.querySelector('[autofocus]') || panel.value?.querySelector(focusableSelector)
      ;(first || panel.value)?.focus()
      return
    }
    unlockPage()
    restoreFocus()
  },
  { immediate: true }
)

onBeforeUnmount(() => {
  unlockPage()
  restoreFocus()
})
</script>

<template>
  <Teleport to="body">
    <Transition name="dialog-fade">
      <div v-if="open" class="app-dialog-backdrop" @mousedown="backdrop">
        <section
          ref="panel"
          :class="['app-dialog-panel', 'glass', `size-${size}`]"
          role="dialog"
          aria-modal="true"
          :aria-label="title ? undefined : label"
          :aria-labelledby="title ? titleId : undefined"
          :aria-describedby="description ? descriptionId : undefined"
          tabindex="-1"
          @keydown="onKeydown"
        >
          <header class="app-dialog-head">
            <div>
              <h2 v-if="title" :id="titleId">{{ title }}</h2>
              <p v-if="description" :id="descriptionId">{{ description }}</p>
            </div>
            <button
              class="icon-button"
              type="button"
              :aria-label="closeLabel"
              :disabled="closeDisabled"
              @click="close"
            >
              <X :size="19" />
            </button>
          </header>
          <div class="app-dialog-body"><slot /></div>
          <footer v-if="$slots.footer" class="app-dialog-footer"><slot name="footer" /></footer>
        </section>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.app-dialog-backdrop {
  position: fixed;
  z-index: 1000;
  inset: 0;
  display: grid;
  overflow: auto;
  padding: clamp(16px, 5vw, 54px);
  place-items: center;
  background: rgb(5 17 31 / 42%);
  backdrop-filter: blur(10px) saturate(80%);
}
.app-dialog-panel {
  display: flex;
  flex-direction: column;
  width: min(100%, 620px);
  max-height: min(820px, calc(100vh - 32px));
  overflow: hidden;
  border-radius: 28px;
}
.app-dialog-panel.size-small {
  width: min(100%, 440px);
}
.app-dialog-panel.size-large {
  width: min(100%, 980px);
}
.app-dialog-head {
  flex-shrink: 0;
  display: flex;
  gap: 20px;
  align-items: flex-start;
  justify-content: space-between;
  padding: 22px 22px 16px;
  border-bottom: 1px solid var(--line);
}
.app-dialog-head h2,
.app-dialog-head p {
  margin: 0;
}
.app-dialog-head h2 {
  font-size: clamp(22px, 4vw, 32px);
  letter-spacing: -0.04em;
}
.app-dialog-head p {
  margin-top: 5px;
  color: var(--muted);
  font-size: 13px;
}
.app-dialog-body {
  min-height: 0;
  max-height: calc(100vh - 230px);
  overflow: auto;
  padding: 22px;
}
.app-dialog-footer {
  flex-shrink: 0;
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  padding: 15px 22px 20px;
  border-top: 1px solid var(--line);
}
.dialog-fade-enter-active,
.dialog-fade-leave-active {
  transition: opacity 180ms ease;
}
.dialog-fade-enter-active .app-dialog-panel,
.dialog-fade-leave-active .app-dialog-panel {
  transition: transform 260ms cubic-bezier(0.2, 0.8, 0.2, 1);
}
.dialog-fade-enter-from,
.dialog-fade-leave-to {
  opacity: 0;
}
.dialog-fade-enter-from .app-dialog-panel,
.dialog-fade-leave-to .app-dialog-panel {
  transform: translateY(12px) scale(0.985);
}
@media (max-width: 720px) {
  .app-dialog-backdrop {
    align-items: end;
    padding: 10px;
  }
  .app-dialog-panel,
  .app-dialog-panel.size-small,
  .app-dialog-panel.size-large {
    width: 100%;
    max-height: calc(100dvh - 10px);
    border-radius: 27px 27px 18px 18px;
  }
  .app-dialog-body {
    max-height: calc(100dvh - 190px);
    padding: 18px;
  }
  .dialog-fade-enter-from .app-dialog-panel,
  .dialog-fade-leave-to .app-dialog-panel {
    transform: translateY(28px) scale(0.99);
  }
}
</style>

<style>
html.dialog-open {
  overflow: hidden;
}
</style>
