<script setup>
import { computed } from 'vue'
import { RouterView, useRoute } from 'vue-router'
import SiteHeader from '@/components/SiteHeader.vue'
import PointerTrail from '@/components/PointerTrail.vue'
import { appStore } from '@/stores/app.js'
import { siteBackgroundImage } from '@/core/siteAppearance.js'
const route = useRoute()
const siteName = computed(() => appStore.site?.nickname || '我的博客')
const immersive = computed(() => route.meta.immersive)
const background = computed(() => siteBackgroundImage(appStore.site?.bgUrl))
const shellStyle = computed(() =>
  background.value ? { '--site-background-image': background.value } : undefined
)
</script>
<template>
  <div
    class="app-shell aurora-app"
    :class="{
      'workspace-shell': immersive,
      'has-site-background': background,
      'navigation-expanded': appStore.navigationExpanded
    }"
    :style="shellStyle"
  >
    <div class="aurora-backdrop" aria-hidden="true" />
    <a class="skip-link" href="#main">跳到正文</a>
    <SiteHeader v-if="!immersive" :site-name="siteName" />
    <main id="main" :class="{ 'public-canvas': !immersive }">
      <RouterView v-slot="{ Component }"
        ><component :is="Component" :key="route.path"
      /></RouterView>
    </main>
    <footer v-if="!immersive" class="site-footer">
      <RouterLink to="/">{{ siteName }}</RouterLink>
      <p>{{ appStore.site?.bio || '记录所学，也记录生活。' }}</p>
      <nav aria-label="页脚导航">
        <RouterLink to="/articles">所有文章</RouterLink
        ><RouterLink to="/guestbook">留一句话</RouterLink><RouterLink to="/about">关于</RouterLink>
      </nav>
    </footer>
    <PointerTrail />
    <Transition name="toast"
      ><div v-if="appStore.toast" class="toast glass" role="status">
        {{ appStore.toast }}
      </div></Transition
    >
  </div>
</template>
