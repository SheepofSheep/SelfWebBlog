<script setup>
import { MessageSquareText } from 'lucide-vue-next'
import InteractionThread from '../features/interaction/components/InteractionThread.vue'
</script>

<template>
  <div class="guestbook-page page-width">
    <header class="guestbook-intro">
      <span><MessageSquareText :size="17" /> Guestbook</span>
      <h1>留言板</h1>
      <p>随便聊聊。首次留言会进入审核，之后会更快显示。</p>
    </header>
    <InteractionThread target-type="GUESTBOOK" :target-id="1" title="最近留言" />
  </div>
</template>

<style scoped>
.guestbook-page {
  display: grid;
  grid-template-columns: minmax(220px, 0.34fr) minmax(0, 0.66fr);
  gap: clamp(36px, 7vw, 92px);
  padding-block: clamp(44px, 8vw, 92px);
}
.guestbook-intro {
  position: sticky;
  top: 100px;
  align-self: start;
}
.guestbook-intro > span {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  color: var(--accent-strong);
  font-size: 0.76rem;
  font-weight: 800;
}
h1 {
  margin: 14px 0 12px;
  font-family: var(--font-reading);
  font-size: clamp(2.5rem, 7vw, 5.2rem);
  font-weight: 600;
  line-height: 1;
}
p {
  max-width: 30ch;
  margin: 0;
  color: var(--text-secondary);
  font-size: 0.88rem;
  line-height: 1.8;
}
@media (max-width: 820px) {
  .guestbook-page {
    grid-template-columns: 1fr;
    gap: 36px;
  }
  .guestbook-intro {
    position: static;
  }
}
</style>
