<script setup>
import { useRoute, useRouter } from 'vue-router'
import { ref, watch } from 'vue'
import { Camera, LogOut, Save } from 'lucide-vue-next'
import { api } from '@/api/client.js'
import { appStore, notify } from '@/stores/app.js'
const router = useRouter()
const route = useRoute()
const nickname = ref('')
const saving = ref(false)
watch(
  () => appStore.ready,
  (ready) => {
    if (ready && !appStore.user)
      router.replace({ path: '/login', query: { redirect: route.fullPath } })
    if (ready && appStore.user) nickname.value = appStore.user.nickname || ''
  },
  { immediate: true }
)
async function saveNickname() {
  const value = nickname.value.trim()
  if (!value) return notify('昵称不能为空')
  saving.value = true
  try {
    const updated = await api.updateProfile({ nickname: value })
    Object.assign(appStore.user, updated)
    notify('昵称已更新')
  } catch (error) {
    notify(error.message)
  } finally {
    saving.value = false
  }
}
async function changeAvatar(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  if (!file.type.startsWith('image/')) return notify('请选择图片文件')
  try {
    appStore.user.avatarUrl = await api.uploadAvatar(file)
    notify('头像已更新')
  } catch (error) {
    notify(error.message)
  }
}
async function logout() {
  try {
    await api.logout()
    appStore.user = null
    router.push('/')
  } catch (error) {
    notify(error.message)
  }
}
</script>
<template>
  <div class="page narrow-page">
    <section v-if="appStore.user" class="profile-card glass">
      <label class="profile-avatar" aria-label="更换头像">
        <img v-if="appStore.user.avatarUrl" :src="appStore.user.avatarUrl" alt="" />
        <span v-else>{{ (appStore.user.nickname || appStore.user.username).slice(0, 1) }}</span>
        <Camera :size="20" />
        <input type="file" accept="image/*" hidden @change="changeAvatar" />
      </label>
      <div class="profile-details">
        <span class="eyebrow">ACCOUNT</span>
        <h1>{{ appStore.user.nickname || appStore.user.username }}</h1>
        <span v-if="appStore.user.titleName" class="title-badge">{{
          appStore.user.titleName
        }}</span>
        <form class="profile-form" @submit.prevent="saveNickname">
          <label>昵称<input v-model="nickname" maxlength="30" /></label>
          <button class="primary-button" :disabled="saving"><Save :size="16" />保存</button>
        </form>
      </div>
      <button class="secondary-button" @click="logout"><LogOut :size="17" />退出</button>
    </section>
  </div>
</template>
