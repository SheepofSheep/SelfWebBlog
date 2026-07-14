<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Github } from 'lucide-vue-next'
import { adminLogin, getGithubAuthUrl, login, register } from '../api'
import { showToast } from '../composables/toast'
import { useAuthStore } from '../stores/authStore'
import AuthShell from '../features/auth/components/AuthShell.vue'
import LoginForm from '../features/auth/components/LoginForm.vue'
import RegisterForm from '../features/auth/components/RegisterForm.vue'
import AdminLoginDisclosure from '../features/auth/components/AdminLoginDisclosure.vue'

const mode = ref('login')
const loading = ref(false)
const route = useRoute()
const router = useRouter()
const { saveUser } = useAuthStore()
const modes = [
  { value: 'login', label: '登录' },
  { value: 'register', label: '注册' }
]
const imageSrc = computed(() =>
  mode.value === 'login' ? '/images/login-illust.jpg' : '/images/register-illust.jpg'
)

function saveAuth(data) {
  localStorage.setItem('token', data.token)
  saveUser(data.user)
}

async function goAfterLogin() {
  const redirect = String(route.query.redirect || '/')
  await router.replace(redirect.startsWith('/') ? redirect : '/')
}

async function handleLogin(credentials) {
  if (!credentials.username || !credentials.password) return
  loading.value = true
  try {
    const data = await login(credentials.username, credentials.password)
    saveAuth(data)
    showToast('登录成功，欢迎回来。')
    await goAfterLogin()
  } catch (error) {
    showToast(error?.message || '登录没有成功，请检查账号和密码。', 'error')
  } finally {
    loading.value = false
  }
}

async function handleRegister(payload) {
  if (payload.username.length < 2 || payload.username.length > 20) {
    showToast('用户名需要 2 到 20 个字符。', 'warning')
    return
  }
  if (payload.password.length < 6) {
    showToast('密码至少需要 6 个字符。', 'warning')
    return
  }
  loading.value = true
  try {
    const data = await register(payload)
    saveAuth(data)
    showToast('账号已创建。')
    await goAfterLogin()
  } catch (error) {
    showToast(error?.message || '注册没有成功，请稍后重试。', 'error')
  } finally {
    loading.value = false
  }
}

async function handleAdminLogin(credentials) {
  if (!credentials.username || !credentials.password) return
  loading.value = true
  try {
    const data = await adminLogin(credentials.username, credentials.password)
    saveAuth(data)
    showToast('工作台登录成功。')
    await router.replace('/admin')
  } catch (error) {
    showToast(error?.message || '管理员登录失败。', 'error')
  } finally {
    loading.value = false
  }
}

async function handleGithubLogin() {
  try {
    const url = await getGithubAuthUrl()
    if (url) window.location.assign(url)
  } catch {
    showToast('GitHub 登录暂不可用，请使用账号密码登录。', 'warning')
  }
}
</script>

<template>
  <AuthShell v-model:mode="mode" :modes="modes" :image-src="imageSrc" @home="router.push('/')">
    <Transition name="form-switch" mode="out-in">
      <LoginForm v-if="mode === 'login'" key="login" :loading="loading" @submit="handleLogin" />
      <RegisterForm v-else key="register" :loading="loading" @submit="handleRegister" />
    </Transition>

    <template #footer>
      <button class="github-login" type="button" :disabled="loading" @click="handleGithubLogin">
        <Github :size="16" />使用 GitHub 登录
      </button>
      <AdminLoginDisclosure v-if="mode === 'login'" :loading="loading" @submit="handleAdminLogin" />
    </template>
  </AuthShell>
</template>

<style scoped>
.github-login {
  width: 100%;
  min-height: 42px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-control);
  background: transparent;
  color: var(--text-primary);
  font: inherit;
  font-size: 0.78rem;
  font-weight: 700;
  cursor: pointer;
}
.form-switch-enter-active,
.form-switch-leave-active {
  transition:
    opacity var(--motion-normal-duration),
    transform var(--motion-normal-duration) var(--motion-ease);
}
.form-switch-enter-from {
  opacity: 0;
  transform: translateX(12px);
}
.form-switch-leave-to {
  opacity: 0;
  transform: translateX(-12px);
}
</style>
