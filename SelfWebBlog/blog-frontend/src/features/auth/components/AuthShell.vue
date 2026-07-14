<script setup>
import { ArrowLeft } from 'lucide-vue-next'
import SegmentedControl from '../../../components/ui/SegmentedControl.vue'

defineProps({
  mode: { type: String, required: true },
  modes: { type: Array, required: true },
  imageSrc: { type: String, required: true }
})
defineEmits(['update:mode', 'home'])
</script>

<template>
  <div class="auth-page">
    <section class="auth-shell">
      <div class="auth-visual">
        <Transition name="auth-image" mode="out-in">
          <img :key="imageSrc" :src="imageSrc" alt="" />
        </Transition>
        <button type="button" @click="$emit('home')"><ArrowLeft :size="16" />返回博客</button>
      </div>
      <div class="auth-panel">
        <header>
          <p>Gabriel's Blog</p>
          <h1>{{ mode === 'login' ? '欢迎回来' : '创建账号' }}</h1>
        </header>
        <SegmentedControl
          :model-value="mode"
          :options="modes"
          label="登录或注册"
          @update:model-value="$emit('update:mode', $event)"
        />
        <div class="auth-form-stack"><slot /></div>
        <footer><slot name="footer" /></footer>
      </div>
    </section>
  </div>
</template>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
  background: var(--page-bg);
}
.auth-shell {
  width: min(960px, 100%);
  min-height: 620px;
  display: grid;
  grid-template-columns: minmax(0, 1.08fr) minmax(380px, 0.92fr);
  overflow: hidden;
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-card);
  background: var(--surface-solid);
  box-shadow: var(--shadow-float);
  animation: content-rise var(--motion-slow-duration) var(--motion-ease) both;
}
.auth-visual {
  position: relative;
  min-height: 620px;
  overflow: hidden;
  background: var(--surface-soft);
}
.auth-visual img {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
}
.auth-visual::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, transparent 65%, rgb(25 18 8 / 44%));
  pointer-events: none;
}
.auth-visual button {
  position: absolute;
  bottom: 20px;
  left: 20px;
  z-index: 1;
  min-height: 40px;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 0 13px;
  border: 1px solid rgb(255 255 255 / 38%);
  border-radius: var(--radius-control);
  background: rgb(24 18 10 / 42%);
  color: white;
  font: inherit;
  font-size: 0.76rem;
  backdrop-filter: blur(10px);
  cursor: pointer;
}
.auth-panel {
  min-width: 0;
  display: flex;
  flex-direction: column;
  padding: clamp(30px, 5vw, 52px);
}
header {
  margin-bottom: 24px;
}
header p,
header h1 {
  margin: 0;
}
header p {
  color: var(--accent-strong);
  font-size: 0.7rem;
  font-weight: 900;
}
header h1 {
  margin-top: 7px;
  font-family: var(--font-reading);
  font-size: 2.25rem;
  letter-spacing: 0;
}
.auth-form-stack {
  display: grid;
  flex: 1;
  margin-top: 24px;
}
.auth-form-stack :deep(> *) {
  grid-area: 1 / 1;
}
footer {
  margin-top: 18px;
}
.auth-image-enter-active,
.auth-image-leave-active {
  transition:
    opacity var(--motion-normal-duration),
    transform var(--motion-normal-duration) var(--motion-ease);
}
.auth-image-enter-from {
  opacity: 0;
  transform: scale(1.015);
}
.auth-image-leave-to {
  opacity: 0;
}
@media (max-width: 760px) {
  .auth-page {
    padding: 0;
  }
  .auth-shell {
    min-height: 100vh;
    grid-template-columns: 1fr;
    border: 0;
  }
  .auth-visual {
    min-height: 190px;
    max-height: 230px;
  }
  .auth-panel {
    padding: 28px 22px 34px;
  }
}
</style>
