<script setup>
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
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
const captchaLoading = ref(false)
const captchaError = ref('')
const formError = ref('')
let captchaTicket = 0
function finishLogin(user) {
  appStore.user = user
  const fallback = user?.role === 'ADMIN' ? '/admin' : '/me'
  router.replace(safeRedirect(route.query.redirect, fallback))
}
function switchMode(next) {
  if (busy.value || mode.value === next) return
  captchaTicket += 1
  mode.value = next
  formError.value = ''
  form.value.captchaAnswer = ''
  captcha.value = null
  captchaLoading.value = false
  if (next === 'register') loadCaptcha()
}
async function loadCaptcha() {
  if (mode.value !== 'register') return
  const ticket = ++captchaTicket
  captcha.value = null
  form.value.captchaAnswer = ''
  captchaError.value = ''
  captchaLoading.value = true
  try {
    const value = await api.captcha('register')
    if (ticket === captchaTicket) captcha.value = value
  } catch (error) {
    if (ticket === captchaTicket) captchaError.value = error.message || '验证码加载失败，请重试'
  } finally {
    if (ticket === captchaTicket) captchaLoading.value = false
  }
}
async function submit() {
  if (busy.value) return
  formError.value = ''
  const username = form.value.username.trim()
  if (!username || !form.value.password.trim()) {
    formError.value = '请输入用户名和密码'
    return
  }
  if (mode.value === 'register') {
    if (username.length < 2 || username.length > 20) formError.value = '用户名需要 2～20 个字符'
    else if (form.value.password.length < 6) formError.value = '密码至少需要 6 位'
    else if (!captcha.value || captchaLoading.value) formError.value = '请先加载验证码'
    else if (!form.value.captchaAnswer.trim()) formError.value = '请输入验证码'
    if (formError.value) return
  }
  busy.value = true
  try {
    const payload = { ...form.value, username, captchaId: captcha.value?.challengeId }
    const data = mode.value === 'login' ? await api.login(payload) : await api.register(payload)
    finishLogin(data.user)
  } catch (e) {
    formError.value = e.message || '提交失败，请重试'
    await loadCaptcha()
  } finally {
    busy.value = false
  }
}
async function startGithub() {
  if (busy.value) return
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
      router.replace({ path: '/login', query: { redirect: route.query.redirect } })
    } finally {
      busy.value = false
    }
  } else if (appStore.user) finishLogin(appStore.user)
})
onBeforeUnmount(() => {
  captchaTicket += 1
})
</script>
<template>
  <div class="auth-page page">
    <section class="auth-card glass">
      <header class="auth-intro">
        <h1>{{ mode === 'login' ? '登录博客' : '创建账户' }}</h1>
        <p>{{ mode === 'login' ? '继续阅读、交流与创作。' : '用一个账户，留下你的想法。' }}</p>
      </header>
      <div class="auth-tabs">
        <button :class="{ active: mode === 'login' }" :disabled="busy" @click="switchMode('login')">
          登录</button
        ><button
          :class="{ active: mode === 'register' }"
          :disabled="busy"
          @click="switchMode('register')"
        >
          注册
        </button>
      </div>
      <form @submit.prevent="submit">
        <fieldset class="auth-fields" :disabled="busy">
          <label>用户名<input v-model="form.username" autocomplete="username" required /></label
          ><label v-if="mode === 'register'"
            >邮箱<input v-model="form.email" type="email" autocomplete="email" /></label
          ><label
            >密码<input
              v-model="form.password"
              type="password"
              :autocomplete="mode === 'login' ? 'current-password' : 'new-password'"
              required
            /><small v-if="mode === 'register'" class="muted">至少 6 位</small></label
          ><label v-if="mode === 'register'" class="captcha-field"
            >验证码
            <div>
              <img v-if="captcha" :src="captcha.imageDataUrl" alt="验证码" /><button
                type="button"
                class="icon-button"
                aria-label="更换验证码"
                :disabled="captchaLoading"
                @click="loadCaptcha"
              >
                <RefreshCw :size="17" />
              </button>
            </div>
            <span v-if="captchaLoading" class="muted" aria-live="polite">正在加载验证码…</span>
            <span v-if="captchaError" class="error-text" role="alert"
              >{{ captchaError }}，请点击刷新按钮重试</span
            >
            <input
              v-model="form.captchaAnswer"
              :disabled="!captcha || captchaLoading"
              autocomplete="off"
              required
          /></label>
        </fieldset>
        <p v-if="formError" class="error-text" role="alert">{{ formError }}</p>
        <button
          class="primary-button wide"
          :disabled="busy || (mode === 'register' && (!captcha || captchaLoading))"
        >
          {{ busy ? '提交中…' : mode === 'login' ? '登录' : '注册' }}
        </button>
      </form>
      <div class="auth-divider"><span>或</span></div>
      <button class="secondary-button wide" :disabled="busy" @click="startGithub">
        <Github :size="18" />GitHub
      </button>
    </section>
  </div>
</template>

<style scoped>
.auth-fields {
  display: grid;
  gap: 16px;
  min-width: 0;
  margin: 0;
  padding: 0;
  border: 0;
}
</style>
