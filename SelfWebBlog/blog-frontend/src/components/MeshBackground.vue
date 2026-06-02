<script setup>
// Honey Paper Studio 背景柔光，不使用厚重玻璃感。
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
  filter: blur(150px);
}

.glow-a {
  width: 700px;
  height: 500px;
  background: radial-gradient(ellipse, rgba(214, 185, 119, 0.1) 0%, transparent 70%);
  top: -20%;
  left: -15%;
}

.glow-b {
  width: 600px;
  height: 400px;
  background: radial-gradient(ellipse, rgba(210, 216, 194, 0.1) 0%, transparent 70%);
  bottom: -15%;
  right: -10%;
}

[data-theme='dark'] .glow-a {
  background: radial-gradient(ellipse, rgba(212, 160, 68, 0.06) 0%, transparent 70%);
}
[data-theme='dark'] .glow-b {
  background: radial-gradient(ellipse, rgba(111, 127, 69, 0.045) 0%, transparent 70%);
}
</style>
