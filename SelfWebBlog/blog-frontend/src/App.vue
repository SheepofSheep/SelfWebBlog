<script setup>
import { computed, ref, watch } from 'vue'
import { RouterView, useRoute } from 'vue-router'
import SiteHeader from '@/components/SiteHeader.vue'
import PointerTrail from '@/components/PointerTrail.vue'
import { appStore } from '@/stores/app.js'
import { backgroundBlurRadius } from '@/core/backgroundBlur.js'
import { softenBackgroundImage } from '@/core/softenBackground.js'
import { siteBackgroundImage, siteBackgroundUrl } from '@/core/siteAppearance.js'
const route = useRoute()
const siteName = computed(() => appStore.site?.nickname || '我的博客')
const immersive = computed(() => route.meta.immersive)
const backgroundUrl = computed(() => siteBackgroundUrl(appStore.site?.bgUrl))
const background = ref(siteBackgroundImage(appStore.site?.bgUrl))
const shellStyle = computed(() =>
  background.value ? { '--site-background-image': background.value } : undefined
)
let backgroundTicket = 0
let backgroundBlurTimer = 0
let lastBackgroundUrl = ''
watch(
  [backgroundUrl, () => appStore.backgroundBlur],
  ([url, blur]) => {
    const ticket = ++backgroundTicket
    window.clearTimeout(backgroundBlurTimer)
    if (url !== lastBackgroundUrl) {
      lastBackgroundUrl = url
      background.value = siteBackgroundImage(url)
    }
    if (!url) return
    if (Number(blur) <= 0) {
      background.value = siteBackgroundImage(url)
      return
    }
    backgroundBlurTimer = window.setTimeout(async () => {
      const softened = await softenBackgroundImage(url, { blur: backgroundBlurRadius(blur) })
      if (ticket !== backgroundTicket || !softened) return
      background.value = `url("${softened}")`
    }, 90)
  },
  { immediate: true }
)
</script>
<template>
  <div
    class="app-shell aurora-app"
    :class="{
      'workspace-shell': immersive,
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
