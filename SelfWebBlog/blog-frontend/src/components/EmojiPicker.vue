<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { Smile, X } from 'lucide-vue-next'
import { EMOJI_CATALOG } from '@/core/emojiCatalog.js'

defineProps({ label: { type: String, default: '选择表情' } })
const emit = defineEmits(['select'])
const open = ref(false)
const root = ref(null)

function choose(item) {
  emit('select', item)
  open.value = false
}

function outside(event) {
  if (!root.value?.contains(event.target)) open.value = false
}

function keydown(event) {
  if (event.key === 'Escape') open.value = false
}

onMounted(() => {
  document.addEventListener('pointerdown', outside)
  window.addEventListener('keydown', keydown)
})
onBeforeUnmount(() => {
  document.removeEventListener('pointerdown', outside)
  window.removeEventListener('keydown', keydown)
})
</script>

<template>
  <div ref="root" class="emoji-picker">
    <button
      class="emoji-trigger"
      type="button"
      :aria-label="label"
      :aria-expanded="open"
      @click="open = !open"
    >
      <Smile :size="17" />
    </button>
    <Transition name="emoji-pop">
      <div v-if="open" class="emoji-popover glass" role="dialog" :aria-label="label">
        <header>
          <strong>表情</strong
          ><button type="button" aria-label="关闭表情" @click="open = false">
            <X :size="15" />
          </button>
        </header>
        <div class="emoji-grid">
          <button
            v-for="item in EMOJI_CATALOG"
            :key="item.name"
            type="button"
            :title="item.name"
            :aria-label="item.name"
            @click="choose(item)"
          >
            <img :src="item.src" :alt="item.name" loading="lazy" />
          </button>
        </div>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.emoji-picker {
  position: relative;
}
.emoji-trigger {
  display: grid;
  width: 36px;
  height: 36px;
  padding: 0;
  place-items: center;
  border: 0;
  border-radius: 11px;
  color: var(--muted);
  background: transparent;
}
.emoji-trigger:hover,
.emoji-trigger[aria-expanded='true'] {
  color: var(--accent);
  background: var(--accent-soft);
}
.emoji-popover {
  position: absolute;
  z-index: 90;
  bottom: calc(100% + 9px);
  left: 0;
  width: min(340px, calc(100vw - 42px));
  padding: 10px;
  border-radius: 20px;
}
.emoji-popover header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 2px 4px 8px;
  font-size: 12px;
}
.emoji-popover header button {
  display: grid;
  width: 28px;
  height: 28px;
  padding: 0;
  place-items: center;
  border: 0;
  border-radius: 9px;
  background: transparent;
}
.emoji-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 6px;
  max-height: 260px;
  overflow: auto;
}
.emoji-grid button {
  aspect-ratio: 1;
  overflow: hidden;
  padding: 3px;
  border: 1px solid transparent;
  border-radius: 12px;
  background: var(--glass-strong);
}
.emoji-grid button:hover {
  border-color: var(--accent);
  transform: translateY(-1px);
}
.emoji-grid img {
  width: 100%;
  height: 100%;
  border-radius: 8px;
  object-fit: cover;
}
.emoji-pop-enter-active,
.emoji-pop-leave-active {
  transition: 160ms ease;
}
.emoji-pop-enter-from,
.emoji-pop-leave-to {
  opacity: 0;
  transform: translateY(5px) scale(0.98);
}
@media (max-width: 460px) {
  .emoji-grid {
    grid-template-columns: repeat(5, 1fr);
  }
}
</style>
