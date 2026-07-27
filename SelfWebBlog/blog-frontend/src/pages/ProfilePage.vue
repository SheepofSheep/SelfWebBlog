<script setup>
import { useRoute, useRouter } from 'vue-router'
import { ref, watch } from 'vue'
import { Camera, LogOut, Save } from 'lucide-vue-next'
import { api } from '@/api/client.js'
import { appStore, notify } from '@/stores/app.js'
import AppAvatar from '@/components/AppAvatar.vue'
const router = useRouter()
const route = useRoute()
const nickname = ref('')
const saving = ref(false)
const avatarBusy = ref(false)
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
  if (!file || avatarBusy.value) return
  if (!file.type.startsWith('image/')) return notify('请选择图片文件')
  avatarBusy.value = true
  try {
    appStore.user.avatarUrl = await api.uploadAvatar(file)
    notify('头像已更新')
  } catch (error) {
    notify(error.message)
  } finally {
    avatarBusy.value = false
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
      <label
        :class="['profile-avatar-control', { busy: avatarBusy }]"
        aria-label="更换头像"
        :aria-busy="avatarBusy"
      >
        <AppAvatar
          :src="appStore.user.avatarUrl"
          :name="appStore.user.nickname || appStore.user.username"
          :size="112"
          rounded
        />
        <Camera :size="20" />
        <input type="file" accept="image/*" hidden :disabled="avatarBusy" @change="changeAvatar" />
      </label>
      <div class="profile-details">
        <span class="eyebrow">ACCOUNT</span>
        <h1>{{ appStore.user.nickname || appStore.user.username }}</h1>
        <span
          v-if="appStore.user.titleName"
          :class="['title-badge', `title-${appStore.user.titleStyle || 'default'}`]"
          >{{ appStore.user.titleName }}</span
        >
        <form class="profile-form" @submit.prevent="saveNickname">
          <label>昵称<input v-model="nickname" maxlength="30" /></label>
          <button class="primary-button" :disabled="saving"><Save :size="16" />保存</button>
        </form>
      </div>
      <button class="secondary-button" @click="logout"><LogOut :size="17" />退出</button>
    </section>
  </div>
</template>

<style scoped>
.profile-avatar-control {
  position: relative;
  display: grid;
  flex: 0 0 auto;
  width: 112px;
  height: 112px;
  cursor: pointer;
}
.profile-avatar-control > svg {
  position: absolute;
  right: 3px;
  bottom: 3px;
  box-sizing: content-box;
  width: 18px;
  height: 18px;
  padding: 7px;
  border: 1px solid var(--line);
  border-radius: 50%;
  color: var(--ink);
  background: var(--glass-strong);
  box-shadow: var(--shadow);
}
.profile-avatar-control.busy {
  cursor: wait;
  opacity: 0.65;
  pointer-events: none;
}
@media (max-width: 680px) {
  .profile-avatar-control {
    width: 92px;
    height: 92px;
  }
  .profile-avatar-control :deep(.app-avatar) {
    width: 92px !important;
    height: 92px !important;
  }
}
</style>
