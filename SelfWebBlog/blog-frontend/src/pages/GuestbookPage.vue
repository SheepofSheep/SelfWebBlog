<script setup>
import InteractionThread from '@/components/InteractionThread.vue'
</script>
<template>
  <div class="page narrow-page">
    <header class="page-title">
      <div>
        <h1>留言</h1>
      </div>
    </header>
    <InteractionThread target-type="GUESTBOOK" :target-id="1" guestbook />
  </div>
</template>
