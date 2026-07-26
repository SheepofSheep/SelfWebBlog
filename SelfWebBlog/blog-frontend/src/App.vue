<script setup>
import { computed } from 'vue'
import { RouterView } from 'vue-router'
import { useRoute } from 'vue-router'
import SiteHeader from '@/components/SiteHeader.vue'
import PointerTrail from '@/components/PointerTrail.vue'
import { appStore } from '@/stores/app.js'

const siteName = computed(() => appStore.site?.nickname || '博客')
const route = useRoute()
const immersive = computed(() => route.meta.immersive)
</script>

<template>
  <div class="app-shell">
    <a class="skip-link" href="#main">跳到正文</a>
    <SiteHeader v-if="!immersive" :site-name="siteName" />
    <main id="main">
      <RouterView v-slot="{ Component }">
        <Transition name="page" mode="out-in"><component :is="Component" /></Transition>
      </RouterView>
    </main>
    <PointerTrail />
    <Transition name="toast"
      ><div v-if="appStore.toast" class="toast" role="status">{{ appStore.toast }}</div></Transition
    >
  </div>
</template>
