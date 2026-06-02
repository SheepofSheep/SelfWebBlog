<script setup>
import { ref, onMounted, onUnmounted, onActivated, computed } from 'vue'
import { listPosters } from '../utils/api'
import { ChevronLeft, ChevronRight, X } from 'lucide-vue-next'

const posters = ref([])
const current = ref(0)
const previewIdx = ref(-1)
let timer = null

const total = computed(() => posters.value.length)
const previewPoster = computed(() =>
  previewIdx.value >= 0 ? posters.value[previewIdx.value] : null
)

function openPreview(index) {
  previewIdx.value = index
  clearInterval(timer)
}
function closePreview() {
  previewIdx.value = -1
  if (total.value > 1) timer = setInterval(next, 5000)
}
function prevPreview() {
  previewIdx.value = previewIdx.value === 0 ? total.value - 1 : previewIdx.value - 1
}
function nextPreview() {
  previewIdx.value = previewIdx.value === total.value - 1 ? 0 : previewIdx.value + 1
}

function prev() {
  current.value = current.value === 0 ? total.value - 1 : current.value - 1
  resetTimer()
}

function next() {
  current.value = current.value === total.value - 1 ? 0 : current.value + 1
  resetTimer()
}

function goTo(index) {
  current.value = index
  resetTimer()
}

function resetTimer() {
  clearInterval(timer)
  if (total.value > 1) {
    timer = setInterval(next, 5000)
  }
}

async function fetchPosters() {
  try {
    posters.value = await listPosters()
    if (total.value > 1 && !timer) timer = setInterval(next, 5000)
  } catch {}
}

function onKeydown(e) {
  if (!previewPoster.value) return
  if (e.key === 'Escape') closePreview()
  else if (e.key === 'ArrowLeft') prevPreview()
  else if (e.key === 'ArrowRight') nextPreview()
}

onMounted(async () => {
  await fetchPosters()
  window.addEventListener('keydown', onKeydown)
})

onActivated(async () => {
  await fetchPosters()
})

onUnmounted(() => {
  clearInterval(timer)
  window.removeEventListener('keydown', onKeydown)
})
</script>

<template>
  <div v-if="total > 0" class="carousel glass-panel">
    <div class="carousel-track">
      <div
        v-for="(p, i) in posters"
        :key="p.id"
        class="carousel-slide"
        :class="{ active: i === current }"
        @click.prevent.stop="openPreview(i)"
      >
        <img :src="p.imageUrl" alt="博客海报" />
      </div>
    </div>

    <button v-if="total > 1" class="carousel-btn carousel-prev" @click="prev" aria-label="上一张">
      <ChevronLeft :size="20" />
    </button>
    <button v-if="total > 1" class="carousel-btn carousel-next" @click="next" aria-label="下一张">
      <ChevronRight :size="20" />
    </button>

    <div v-if="total > 1" class="carousel-dots">
      <button
        v-for="(_, i) in posters"
        :key="i"
        :class="['dot', { active: i === current }]"
        @click="goTo(i)"
        :aria-label="`跳转到第${i + 1}张`"
      ></button>
    </div>
  </div>

  <!-- 全屏预览（在 carousel 外面，不受 overflow:hidden 影响） -->
  <Teleport to="body">
    <Transition name="fade">
      <div v-if="previewPoster" class="preview-overlay" @click.self="closePreview">
        <div class="preview-modal">
          <button class="preview-close" @click="closePreview" aria-label="关闭预览">
            <X :size="20" />
          </button>
          <button
            v-if="total > 1"
            class="preview-arrow prev"
            @click="prevPreview"
            aria-label="上一张"
          >
            <ChevronLeft :size="24" />
          </button>
          <img :src="previewPoster.imageUrl" class="preview-img" alt="海报预览" />
          <button
            v-if="total > 1"
            class="preview-arrow next"
            @click="nextPreview"
            aria-label="下一张"
          >
            <ChevronRight :size="24" />
          </button>
          <div class="preview-info">
            <span class="preview-index">{{ previewIdx + 1 }} / {{ total }}</span>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.carousel {
  position: relative;
  width: 100%;
  max-width: 100%;
  margin: 0 auto var(--space-lg);
  padding: 12px;
  border-radius: 24px;
  overflow: hidden;
}

.carousel::after {
  content: '';
  position: absolute;
  inset: 12px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 18px;
  pointer-events: none;
}

[data-theme='dark'] .carousel::after {
  border-color: rgba(255, 232, 186, 0.08);
}

.carousel:hover {
  transform: none;
}

.carousel-track {
  position: relative;
  width: 100%;
  aspect-ratio: 16 / 7;
  min-height: 260px;
  max-height: 380px;
  border-radius: 18px;
  overflow: hidden;
  background: var(--surface-parchment);
}

[data-theme='dark'] .carousel-track {
  background: #14110c;
}

.carousel-slide {
  position: absolute;
  inset: 0;
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.6s var(--ease-out);
}

.carousel-slide.active {
  opacity: 1;
  pointer-events: auto;
}

.carousel-slide img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center 42%;
  display: block;
  filter: saturate(0.96) contrast(1.02);
  background: var(--surface-muted);
}

.carousel-slide::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, transparent 58%, rgba(17, 16, 13, 0.28));
  pointer-events: none;
}

.carousel-btn {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  z-index: 2;
  width: 36px;
  height: 36px;
  border: 1px solid var(--border-warm);
  border-radius: 50%;
  background: var(--surface-strong);
  backdrop-filter: none;
  color: var(--text-main);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition:
    opacity var(--duration-normal) var(--ease-bounce),
    background var(--duration-normal) var(--ease-bounce),
    color var(--duration-normal) var(--ease-bounce),
    transform var(--duration-normal) var(--ease-bounce);
}
.carousel:hover .carousel-btn {
  opacity: 1;
}
.carousel-prev {
  left: 24px;
}
.carousel-next {
  right: 24px;
}
.carousel-btn:hover {
  background: var(--surface-page);
  color: var(--primary-hover);
  transform: translateY(-50%) scale(1.05);
}

.carousel-dots {
  position: absolute;
  right: 28px;
  bottom: 24px;
  z-index: 2;
  left: auto;
  transform: none;
  display: flex;
  gap: 7px;
  padding: 7px 9px;
  border: 1px solid rgba(255, 226, 177, 0.2);
  border-radius: var(--radius-pill);
  background: rgba(17, 16, 13, 0.38);
}
.dot {
  width: 7px;
  height: 7px;
  border: none;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.45);
  cursor: pointer;
  padding: 0;
  transition:
    background var(--duration-normal) var(--ease-bounce),
    transform var(--duration-normal) var(--ease-bounce);
}
.dot.active {
  background: var(--primary);
  transform: scale(1.45);
  box-shadow: none;
}

@media (max-width: 768px) {
  .carousel {
    padding: 8px;
    border-radius: 20px;
  }

  .carousel::after {
    inset: 8px;
    border-radius: 15px;
  }

  .carousel-track {
    min-height: 210px;
    aspect-ratio: 4 / 3;
    border-radius: 15px;
  }

  .carousel-btn {
    opacity: 1;
    width: 34px;
    height: 34px;
  }

  .carousel-prev {
    left: 16px;
  }

  .carousel-next {
    right: 16px;
  }

  .carousel-dots {
    right: 18px;
    bottom: 16px;
  }
}

.carousel-slide {
  cursor: pointer;
}

/* ====== 全屏预览 ====== */
.preview-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  background: rgba(30, 20, 20, 0.94);
  display: flex;
  align-items: center;
  justify-content: center;
}
.preview-modal {
  position: relative;
  width: 100vw;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent !important;
  border: none !important;
  overflow: visible !important;
}
.preview-close {
  position: fixed;
  top: 20px;
  right: 20px;
  z-index: 10000;
  border: none;
  background: rgba(255, 255, 255, 0.12);
  color: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background var(--duration-fast);
}
.preview-close:hover {
  background: rgba(255, 255, 255, 0.25);
}
.preview-arrow {
  position: fixed;
  top: 50%;
  transform: translateY(-50%);
  z-index: 10000;
  border: none;
  background: rgba(255, 255, 255, 0.12);
  color: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background var(--duration-fast);
}
.preview-arrow:hover {
  background: rgba(255, 255, 255, 0.3);
}
.preview-arrow.prev {
  left: 20px;
}
.preview-arrow.next {
  right: 20px;
}
.preview-info {
  position: fixed;
  bottom: 24px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 10000;
  display: flex;
  align-items: center;
  gap: 16px;
  background: rgba(30, 20, 20, 0.65);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(12px);
}
.preview-title {
  font-size: 0.85rem;
  font-weight: 500;
  color: #fff;
}
.preview-index {
  font-size: 0.72rem;
  color: rgba(255, 255, 255, 0.6);
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.25s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
