<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Github, RefreshCw } from 'lucide-vue-next'
import { api } from '@/api/client.js'
import { oauthErrorMessage, safeRedirect } from '@/core/authNavigation.js'
import { appStore, notify } from '@/stores/app.js'
const route = useRoute()
const router = useRouter()
const mode = ref('login')
const form = ref({ username: '', password: '', email: '', captchaAnswer: '' })
const captcha = ref(null)
const busy = ref(false)
function finishLogin(user) {
  appStore.user = user
  const fallback = user?.role === 'ADMIN' ? '/admin' : '/me'
  router.replace(safeRedirect(route.query.redirect, fallback))
}
function switchMode(next) {
  mode.value = next
  if (next === 'register') loadCaptcha()
}
async function loadCaptcha() {
  if (mode.value === 'register') captcha.value = await api.captcha('register')
}
async function submit() {
  busy.value = true
  try {
    const payload = { ...form.value, captchaId: captcha.value?.challengeId }
    const data = mode.value === 'login' ? await api.login(payload) : await api.register(payload)
    finishLogin(data.user)
  } catch (e) {
    notify(e.message)
    loadCaptcha()
  } finally {
    busy.value = false
  }
}
async function startGithub() {
  busy.value = true
  try {
    location.assign(await api.githubLogin())
  } catch (error) {
    notify(error.message)
    busy.value = false
  }
}
watch(
  () => appStore.ready,
  (ready) => {
    if (ready && appStore.user && !route.query.ticket) finishLogin(appStore.user)
  }
)
onMounted(async () => {
  if (route.query.error) {
    notify(oauthErrorMessage(String(route.query.error)))
    return router.replace({ path: '/login', query: { redirect: route.query.redirect } })
  }
  if (route.query.ticket) {
    busy.value = true
    try {
      const data = await api.exchangeOAuth(String(route.query.ticket))
      finishLogin(data.user)
    } catch (error) {
      notify(error.message)
      router.replace('/login')
    } finally {
      busy.value = false
    }
  } else if (appStore.user) finishLogin(appStore.user)
})
</script>
<template>
  <div class="auth-page page">
    <section class="auth-card glass">
      <div class="auth-tabs">
        <button :class="{ active: mode === 'login' }" @click="switchMode('login')">登录</button
        ><button :class="{ active: mode === 'register' }" @click="switchMode('register')">
          注册
        </button>
      </div>
      <form @submit.prevent="submit">
        <label>用户名<input v-model="form.username" autocomplete="username" required /></label
        ><label v-if="mode === 'register'"
          >邮箱<input v-model="form.email" type="email" autocomplete="email" /></label
        ><label
          >密码<input
            v-model="form.password"
            type="password"
            :autocomplete="mode === 'login' ? 'current-password' : 'new-password'"
            required /></label
        ><label v-if="mode === 'register'" class="captcha-field"
          >验证码
          <div>
            <img v-if="captcha" :src="captcha.imageDataUrl" alt="验证码" /><button
              type="button"
              class="icon-button"
              @click="loadCaptcha"
            >
              <RefreshCw :size="17" />
            </button>
          </div>
          <input v-model="form.captchaAnswer" required /></label
        ><button class="primary-button wide" :disabled="busy">
          {{ mode === 'login' ? '登录' : '注册' }}
        </button>
      </form>
      <div class="auth-divider"><span>或</span></div>
      <button class="secondary-button wide" :disabled="busy" @click="startGithub">
        <Github :size="18" />GitHub
      </button>
    </section>
  </div>
</template>
