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
  background: radial-gradient(ellipse, rgba(244, 164, 184, 0.06) 0%, transparent 70%);
  top: -20%; left: -15%;
}

.glow-b {
  width: 600px; height: 400px;
  background: radial-gradient(ellipse, rgba(224, 242, 241, 0.05) 0%, transparent 70%);
  bottom: -15%; right: -10%;
}

[data-theme='dark'] .glow-a {
  background: radial-gradient(ellipse, rgba(224, 135, 154, 0.07) 0%, transparent 70%);
}
[data-theme='dark'] .glow-b {
  background: radial-gradient(ellipse, rgba(180, 210, 200, 0.04) 0%, transparent 70%);
}
</style>
