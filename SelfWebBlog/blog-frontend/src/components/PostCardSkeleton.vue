<template>
  <transition name="fade">
    <div v-if="isLoading" class="post-card-skeleton">
      <div class="skeleton-image"></div>
      <div class="skeleton-content">
        <div class="skeleton-title"></div>
        <div class="skeleton-description"></div>
        <div class="skeleton-description"></div>
      </div>
    </div>
  </transition>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';

const props = defineProps({
  isLoading: {
    type: Boolean,
    default: true
  }
});
</script>

<style scoped>
.post-card-skeleton {
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(255, 255, 255, 0.4);
  border-radius: 20px;
  padding: 1.5rem;
  box-shadow: 0 8px 24px rgba(244, 164, 184, 0.12);
  transition: opacity 0.3s ease;
  overflow: hidden;
}

.skeleton-image {
  width: 100%;
  height: 200px;
  background: #eeeeee;
  border-radius: 12px;
  margin-bottom: 1rem;
  animation: pulse 1.5s ease-in-out infinite;
}

.skeleton-content {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.skeleton-title {
  width: 60%;
  height: 24px;
  background: #eeeeee;
  border-radius: 8px;
  animation: pulse 1.5s ease-in-out infinite;
  animation-delay: 0.2s;
}

.skeleton-description {
  width: 100%;
  height: 16px;
  background: #eeeeee;
  border-radius: 6px;
  animation: pulse 1.5s ease-in-out infinite;
}

.skeleton-description:nth-child(3) {
  width: 80%;
  animation-delay: 0.4s;
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.6;
  }
}

/* Vue 3 过渡样式 */
.fade-enter-active, .fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from, .fade-leave-to {
  opacity: 0;
}

@media (max-width: 768px) {
  .post-card-skeleton {
    padding: 1rem;
  }
  
  .skeleton-image {
    height: 150px;
  }
}
</style>