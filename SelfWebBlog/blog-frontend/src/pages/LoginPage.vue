<script setup>
import { ref, onMounted } from 'vue'
import { navigate } from '../router'
import { login, register, getGithubAuthUrl } from '../utils/api'
import { showToast } from '../composables/toast'
import { gsap } from 'gsap'
import { Github } from 'lucide-vue-next'

const tab = ref('login')
const loading = ref(false)

const loginUsername = ref('')
const loginPassword = ref('')

const regUsername = ref('')
const regPassword = ref('')
const regEmail = ref('')
const regAvatar = ref('')

onMounted(() => {
  gsap.fromTo('.login-card', { opacity: 0, y: 16 }, { opacity: 1, y: 0, duration: 0.5, ease: 'power2.out' })
  gsap.fromTo('.login-card > *', { opacity: 0, y: 6 }, { opacity: 1, y: 0, duration: 0.35, stagger: 0.05, delay: 0.15, ease: 'power2.out' })
})

function saveAuth(data) {
  localStorage.setItem('token', data.token)
  localStorage.setItem('user', JSON.stringify(data.user))
}

async function handleLogin() {
  if (!loginUsername.value.trim() || !loginPassword.value.trim()) { showToast('请填写用户名和密码', 'warning'); return }
  loading.value = true
  try { const data = await login(loginUsername.value, loginPassword.value); saveAuth(data); showToast('欢迎回来', 'success'); navigate('/') }
  catch (e) { showToast(e?.message || '登录失败', 'error') }
  finally { loading.value = false }
}

async function handleRegister() {
  if (!regUsername.value.trim() || !regPassword.value.trim()) { showToast('请填写用户名和密码', 'warning'); return }
  if (regPassword.value.length < 6) { showToast('密码至少6位', 'warning'); return }
  loading.value = true
  try { const data = await register({ username: regUsername.value, password: regPassword.value, email: regEmail.value, avatarUrl: regAvatar.value }); saveAuth(data); showToast('注册成功', 'success'); navigate('/') }
  catch (e) { showToast(e?.message || '注册失败', 'error') }
  finally { loading.value = false }
}

async function handleGithubLogin() {
  loading.value = true
  try { const url = await getGithubAuthUrl(); window.location.href = url }
  catch (e) { showToast(e?.message || 'GitHub 登录暂不可用', 'error'); loading.value = false }
}
</script>

<template>
  <div class="login-page">
    <!-- Masthead -->
    <div class="masthead">
      <h1 class="masthead-title">欢迎来我的博客参观OvO!</h1>
      <hr class="login-rule" />
    </div>

    <!-- Form card -->
    <div class="login-card">
      <!-- Tabs -->
      <div class="tab-row">
        <button :class="['tab', { active: tab === 'login' }]" @click="tab = 'login'">
          登录
        </button>
        <button :class="['tab', { active: tab === 'register' }]" @click="tab = 'register'">
          注册
        </button>
      </div>

      <!-- Login -->
      <form v-if="tab === 'login'" class="form" @submit.prevent="handleLogin">
        <div class="field">
          <label class="field-label">用户名</label>
          <input v-model="loginUsername" type="text" class="field-control" placeholder="请输入用户名" autocomplete="username" />
        </div>
        <div class="field">
          <label class="field-label">密码</label>
          <input v-model="loginPassword" type="password" class="field-control" placeholder="请输入密码" autocomplete="current-password" />
        </div>
        <button type="submit" class="btn btn-primary btn-full" :disabled="loading">
          {{ loading ? '登录中...' : '登录' }}
        </button>
      </form>

      <!-- Register -->
      <form v-if="tab === 'register'" class="form" @submit.prevent="handleRegister">
        <div class="field">
          <label class="field-label">用户名</label>
          <input v-model="regUsername" type="text" class="field-control" placeholder="2-20个字符" />
        </div>
        <div class="field">
          <label class="field-label">邮箱</label>
          <input v-model="regEmail" type="email" class="field-control" placeholder="可选" />
        </div>
        <div class="field">
          <label class="field-label">密码</label>
          <input v-model="regPassword" type="password" class="field-control" placeholder="至少6个字符" />
        </div>
        <div class="field">
          <label class="field-label">头像链接</label>
          <input v-model="regAvatar" type="url" class="field-control" placeholder="可选" />
        </div>
        <button type="submit" class="btn btn-primary btn-full" :disabled="loading">
          {{ loading ? '注册中...' : '注册' }}
        </button>
      </form>

      <!-- Switch -->
      <p class="switch-text">
        <button v-if="tab === 'login'" class="link-btn" @click="tab = 'register'">
          没有账号？<span>去注册</span>
        </button>
        <button v-if="tab === 'register'" class="link-btn" @click="tab = 'login'">
          已有账号？<span>去登录</span>
        </button>
      </p>

      <div class="sep">
        <span>或</span>
      </div>

      <button class="btn btn-ghost btn-full" :disabled="loading" @click="handleGithubLogin">
        <Github :size="14" />
        使用 GitHub 登录
      </button>

      <button class="btn btn-ghost btn-full skip-btn" @click="navigate('/')">
        跳过登录，直接浏览
      </button>
    </div>

    <p class="page-foot">Anon's Blog</p>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: var(--paper);
  padding: 2rem;
}

/* Masthead */
.masthead {
  text-align: center;
  margin-bottom: 2rem;
}
.masthead-title {
  font-family: var(--font-display);
  font-size: var(--fs-masthead);
  font-weight: var(--fw-regular);
  color: var(--ink);
  letter-spacing: 0.02em;
  margin: 0;
  line-height: 1;
}
.masthead-sub {
  font-family: var(--font-body);
  font-size: var(--fs-small);
  color: var(--ink-muted);
  font-style: italic;
  margin: 6px 0 0;
}
.login-rule {
  border: none;
  border-top: 3px double var(--rule);
  width: 120px;
  margin: 16px auto 0;
}

/* Card */
.login-card {
  width: min(400px, calc(100vw - 48px));
  padding: 2rem;
  background: var(--paper);
  border: 1px solid var(--rule);
}

/* Tabs */
.tab-row {
  display: flex;
  border-bottom: 1px solid var(--rule);
  margin-bottom: 1.5rem;
}
.tab {
  flex: 1;
  padding: 0 0 8px;
  border: none;
  background: none;
  font-family: var(--font-display);
  font-size: var(--fs-subhead);
  color: var(--ink-muted);
  cursor: pointer;
  position: relative;
  transition: color var(--duration-fast);
}
.tab.active {
  color: var(--ink);
}
.tab.active::after {
  content: '';
  position: absolute;
  bottom: -1px; left: 0; right: 0;
  height: 2px;
  background: var(--ink);
}

/* Form */
.form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  margin-bottom: 0.5rem;
}
.field {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.field-label {
  font-family: var(--font-ui);
  font-size: var(--fs-caption);
  font-weight: var(--fw-medium);
  color: var(--ink-muted);
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.btn-full {
  width: 100%;
  margin-top: 0.25rem;
}

.skip-btn {
  margin-top: 0.5rem;
}

/* Switch */
.switch-text {
  text-align: center;
  margin: 0.25rem 0;
}
.link-btn {
  background: none;
  border: none;
  font-family: var(--font-body);
  font-size: var(--fs-small);
  color: var(--ink-muted);
  font-style: italic;
  cursor: pointer;
  padding: 0;
}
.link-btn span {
  color: var(--ink);
  font-style: normal;
  text-decoration: underline;
  text-underline-offset: 2px;
}

/* Separator */
.sep {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 0.75rem 0;
  font-family: var(--font-ui);
  font-size: var(--fs-caption);
  color: var(--ink-faint);
  text-transform: uppercase;
  letter-spacing: 0.06em;
}
.sep::before, .sep::after {
  content: '';
  flex: 1;
  height: 1px;
  background: var(--rule-thin);
}

/* Footer */
.page-foot {
  margin-top: 1.5rem;
  font-family: var(--font-body);
  font-size: var(--fs-caption);
  font-style: italic;
  color: var(--ink-faint);
}

@media (max-width: 480px) {
  .login-card {
    padding: 1.5rem 1.25rem;
  }
}
</style>
