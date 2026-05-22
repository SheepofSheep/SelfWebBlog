<script setup>
// 柔和暖色渐变 — Lovable 风格 hero glow
</script>

<template>
  <div class="bg-glow">
    <div class="glow-blob glow-a"></div>
    <div class="glow-blob glow-b"></div>
  </div>
</template>

<style scoped>
.bg-glow {
  position: fixed;
  inset: 0;
  z-index: -2;
  pointer-events: none;
  overflow: hidden;
}

.glow-blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(180px);
}

.glow-a {
  width: 700px; height: 500px;
  background: radial-gradient(ellipse, rgba(184, 51, 80, 0.04) 0%, transparent 70%);
  top: -20%; left: -15%;
}

.glow-b {
  width: 600px; height: 400px;
  background: radial-gradient(ellipse, rgba(224, 136, 92, 0.03) 0%, transparent 70%);
  bottom: -15%; right: -10%;
}

[data-theme='dark'] .glow-a {
  background: radial-gradient(ellipse, rgba(212, 103, 124, 0.05) 0%, transparent 70%);
}
[data-theme='dark'] .glow-b {
  background: radial-gradient(ellipse, rgba(212, 148, 92, 0.03) 0%, transparent 70%);
}
</style>
