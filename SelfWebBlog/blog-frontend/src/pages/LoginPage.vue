<script setup>
import { ref, computed, onMounted } from 'vue'
import { navigate, useRoute } from '../router'
import { login, register, getGithubAuthUrl } from '../utils/api'
import { showToast } from '../composables/toast'
import { gsap } from 'gsap'
import { Github } from 'lucide-vue-next'

const tab = ref('login')
const loading = ref(false)
const imageKey = ref(0)
const { query } = useRoute()

const loginUsername = ref('')
const loginPassword = ref('')

const regUsername = ref('')
const regPassword = ref('')
const regEmail = ref('')
const regAvatar = ref('')

onMounted(() => {
  gsap.to('.login-layout', { opacity: 1, duration: 0.4, ease: 'power1.out' })
})

function switchTab(t) {
  if (t === tab.value || loading.value) return
  const illust = document.querySelector('.login-illust')
  const panel = document.querySelector('.login-panel-wrap')
  const dir = tab.value === 'login' ? 1 : -1

  if (illust && panel) {
    // 快速滑出
    gsap.to([illust, panel], {
      autoAlpha: 0,
      x: dir * -40,
      duration: 0.2,
      ease: 'power2.in',
      onComplete: () => {
        tab.value = t
        imageKey.value++
        const newIllust = document.querySelector('.login-illust')
        const newPanel = document.querySelector('.login-panel-wrap')
        if (newIllust && newPanel) {
          gsap.fromTo(
            [newIllust, newPanel],
            { autoAlpha: 0, x: dir * 40 },
            { autoAlpha: 1, x: 0, duration: 0.3, ease: 'power2.out' }
          )
        }
      }
    })
  }
}

const illustSrc = computed(
  () => `/images/${tab.value === 'login' ? 'login-illust' : 'register-illust'}.jpg`
)

function saveAuth(data) {
  localStorage.setItem('token', data.token)
  localStorage.setItem('user', JSON.stringify(data.user))
}

function goAfterLogin() {
  navigate(query.value.get('redirect') || '/')
}

async function handleLogin() {
  if (!loginUsername.value.trim() || !loginPassword.value.trim()) {
    showToast('先填一下用户名和密码。')
    return
  }
  loading.value = true
  try {
    const data = await login(loginUsername.value.trim(), loginPassword.value)
    saveAuth(data)
    showToast('登录好了，欢迎回来。')
    goAfterLogin()
  } catch (e) {
    showToast(e?.message || '登录没有成功，检查一下账号或密码。', 'error')
  } finally {
    loading.value = false
  }
}

async function handleRegister() {
  if (!regUsername.value.trim() || !regPassword.value.trim()) {
    showToast('先填一下用户名和密码。')
    return
  }
  if (regUsername.value.trim().length < 2 || regUsername.value.trim().length > 20) {
    showToast('用户名需要 2 到 20 个字符。')
    return
  }
  if (regPassword.value.length < 6) {
    showToast('密码至少 6 个字符。')
    return
  }
  loading.value = true
  try {
    const data = await register({
      username: regUsername.value.trim(),
      password: regPassword.value,
      email: regEmail.value.trim() || undefined,
      avatarUrl: regAvatar.value.trim() || undefined
    })
    saveAuth(data)
    showToast('注册好了，已经帮你登录。')
    goAfterLogin()
  } catch (e) {
    showToast(e?.message || '注册没有成功，稍后再试一次。', 'error')
  } finally {
    loading.value = false
  }
}

async function handleGithubLogin() {
  try {
    const url = await getGithubAuthUrl()
    if (url) window.location.href = url
  } catch (e) {
    showToast('GitHub 登录暂不可用，先用账号密码登录吧。', 'warning')
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-layout" :class="{ 'register-mode': tab === 'register' }">
      <!-- 左侧：插画 -->
      <div class="login-illust">
        <div class="illust-wrap">
          <img
            :key="imageKey"
            :src="illustSrc"
            :alt="tab === 'login' ? '登录' : '注册'"
            class="illust-img"
            @error="
              (e) => {
                e.target.style.display = 'none'
                e.target.nextElementSibling.style.display = 'flex'
              }
            "
          />
          <!-- 图片未放置时的占位 -->
          <div class="illust-placeholder">
            <div class="placeholder-inner">
              <p class="placeholder-icon">{{ tab === 'login' ? 'Honey' : 'Hello' }}</p>
              <p class="placeholder-text">可放置插画</p>
              <code class="placeholder-path"
                >public/images/{{ tab === 'login' ? 'login-illust' : 'register-illust' }}.png</code
              >
            </div>
          </div>
        </div>
      </div>

      <!-- 表单卡片 -->
      <div class="login-panel-wrap">
        <div class="login-card glass-panel">
          <!-- Tabs -->
          <div class="tab-row">
            <button :class="['tab', { active: tab === 'login' }]" @click="switchTab('login')">
              登录
            </button>
            <button :class="['tab', { active: tab === 'register' }]" @click="switchTab('register')">
              注册
            </button>
          </div>

          <!-- Forms -->
          <div class="form-area">
            <Transition name="form-switch" mode="out-in">
              <form v-if="tab === 'login'" key="login" class="form" @submit.prevent="handleLogin">
                <div class="field">
                  <label class="field-label">用户名</label>
                  <input
                    v-model="loginUsername"
                    type="text"
                    class="field-control"
                    placeholder="请输入用户名"
                    autocomplete="username"
                  />
                </div>
                <div class="field">
                  <label class="field-label">密码</label>
                  <input
                    v-model="loginPassword"
                    type="password"
                    class="field-control"
                    placeholder="请输入密码"
                    autocomplete="current-password"
                  />
                </div>
                <button type="submit" class="btn btn-primary btn-full" :disabled="loading">
                  {{ loading ? '登录中...' : '登录' }}
                </button>
              </form>

              <!-- Register -->
              <form v-else key="register" class="form" @submit.prevent="handleRegister">
                <div class="field">
                  <label class="field-label">用户名</label>
                  <input
                    v-model="regUsername"
                    type="text"
                    class="field-control"
                    placeholder="2-20个字符"
                  />
                </div>
                <div class="field">
                  <label class="field-label">邮箱</label>
                  <input v-model="regEmail" type="email" class="field-control" placeholder="可选" />
                </div>
                <div class="field">
                  <label class="field-label">密码</label>
                  <input
                    v-model="regPassword"
                    type="password"
                    class="field-control"
                    placeholder="至少6个字符"
                  />
                </div>
                <div class="field">
                  <label class="field-label">头像链接</label>
                  <input v-model="regAvatar" type="url" class="field-control" placeholder="可选" />
                </div>
                <button type="submit" class="btn btn-primary btn-full" :disabled="loading">
                  {{ loading ? '注册中...' : '注册' }}
                </button>
              </form>
            </Transition>
          </div>

          <!-- Switch -->
          <p class="switch-text">
            <button v-if="tab === 'login'" class="link-btn" @click="switchTab('register')">
              没有账号？<span>去注册</span>
            </button>
            <button v-if="tab === 'register'" class="link-btn" @click="switchTab('login')">
              已有账号？<span>去登录</span>
            </button>
          </p>

          <div class="sep"><span>或</span></div>

          <button class="btn btn-ghost btn-full" :disabled="loading" @click="handleGithubLogin">
            <Github :size="14" />
            使用 GitHub 登录
          </button>

          <button class="btn btn-ghost btn-full skip-btn" @click="navigate('/')">
            跳过登录，直接浏览
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-xl);
  background:
    radial-gradient(circle at 20% 10%, rgba(246, 215, 131, 0.28), transparent 24rem),
    radial-gradient(circle at 90% 90%, rgba(229, 234, 212, 0.28), transparent 24rem);
}

/* ═══ 左右分栏 ═══ */
.login-layout {
  display: flex;
  flex-direction: row;
  width: min(1000px, 100%);
  min-height: 620px;
  overflow: hidden;
  opacity: 0;
  border: 1px solid var(--border);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-paper);
}
.login-layout.register-mode {
  flex-direction: row-reverse;
}

/* ═══ 插画面板 ═══ */
.login-illust {
  flex: 1.25;
  position: relative;
  overflow: hidden;
  background:
    linear-gradient(135deg, var(--brand-primary-wash), var(--accent-olive-soft)),
    radial-gradient(circle at 30% 10%, rgba(255, 253, 247, 0.7), transparent 18rem);
  display: none; /* 移动端隐藏 */
  will-change: transform, opacity;
}

.illust-wrap {
  width: 100%;
  height: 100%;
  position: relative;
}
.illust-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.illust-placeholder {
  display: none;
  width: 100%;
  height: 100%;
  align-items: center;
  justify-content: center;
  background:
    linear-gradient(135deg, var(--brand-primary-wash), var(--accent-olive-soft)),
    radial-gradient(circle at 30% 10%, rgba(255, 253, 247, 0.7), transparent 18rem);
}
.placeholder-inner {
  text-align: center;
  color: var(--text-muted);
}
.placeholder-icon {
  font-family: var(--font-serif);
  font-size: 1.4rem;
  font-weight: 700;
  margin: 0 0 8px;
  color: var(--primary-hover);
}
.placeholder-text {
  font-size: 0.8rem;
  margin: 0 0 4px;
}
.placeholder-path {
  font-size: 0.65rem;
  background: rgba(255, 253, 247, 0.56);
  padding: 2px 8px;
  border-radius: 4px;
  color: var(--primary-hover);
}

/* ═══ 表单面板 ═══ */
.login-panel-wrap {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--surface-page);
  padding: var(--space-xl);
  will-change: transform, opacity;
}

.login-card {
  width: 100%;
  min-height: 420px;
  padding: var(--space-xl);
  display: flex;
  flex-direction: column;
}

/* ═══ Tabs ═══ */
.tab-row {
  display: flex;
  gap: 0;
  margin-bottom: var(--space-md);
  border-bottom: 2px solid var(--border-light);
}
.tab {
  flex: 1;
  padding: 10px 0;
  border: none;
  background: none;
  font-family: var(--font-sans);
  font-size: 0.95rem;
  font-weight: 500;
  color: var(--text-muted);
  cursor: pointer;
  position: relative;
  transition: color var(--duration-fast);
}
.tab.active {
  color: var(--primary-hover);
  font-weight: 600;
}
.tab.active::after {
  content: '';
  position: absolute;
  bottom: -2px;
  left: 20%;
  right: 20%;
  height: 2px;
  background: var(--primary);
  border-radius: 1px;
}

/* ═══ Forms ═══ */
.form {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
  flex: 1;
  margin-bottom: var(--space-sm);
}
.field {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.field-label {
  font-size: var(--font-size-xs);
  font-weight: 600;
  color: var(--text-secondary);
  letter-spacing: 0.04em;
}
.field-control {
  width: 100%;
  padding: 10px 14px;
  box-sizing: border-box;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: rgba(255, 253, 247, 0.86);
  color: var(--text);
  font-size: var(--font-size-sm);
  font-family: var(--font-body);
  outline: none;
  transition: border-color var(--duration-fast);
}
.field-control::placeholder {
  color: var(--text-faint);
}
.field-control:focus {
  border-color: var(--primary);
  box-shadow: 0 0 0 4px rgba(217, 154, 29, 0.14);
}

.btn-full {
  width: 100%;
}

/* ═══ Switch ═══ */
.switch-text {
  text-align: center;
  margin-bottom: var(--space-sm);
}
.link-btn {
  background: none;
  border: none;
  font-size: 0.82rem;
  color: var(--text-muted);
  cursor: pointer;
  font-family: var(--font-body);
  transition: color var(--duration-fast);
}
.link-btn span {
  color: var(--primary-hover);
  font-weight: 500;
}
.link-btn:hover span {
  color: var(--primary);
}

/* ═══ Sep ═══ */
.sep {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  margin-bottom: var(--space-sm);
  color: var(--text-faint);
  font-size: 0.75rem;
}
.sep::before,
.sep::after {
  content: '';
  flex: 1;
  height: 1px;
  background: var(--border-light);
}

.skip-btn {
  margin-top: var(--space-sm);
}

/* ═══ Form transition ═══ */
.form-switch-enter-active {
  transition:
    opacity 0.25s var(--ease-out),
    transform 0.25s var(--ease-out);
  position: absolute;
}
.form-switch-leave-active {
  transition:
    opacity 0.15s var(--ease-out),
    transform 0.15s var(--ease-out);
  position: absolute;
}
.form-switch-enter-from {
  opacity: 0;
  transform: translateX(12px);
}
.form-switch-leave-to {
  opacity: 0;
  transform: translateX(-12px);
}

.form-area {
  flex: 1;
  position: relative;
}

/* ═══ Responsive ═══ */
@media (min-width: 700px) {
  .login-illust {
    display: block;
  }
}

@media (max-width: 699px) {
  .login-layout {
    min-height: auto;
    box-shadow: none;
  }
  .login-panel-wrap {
    padding: 0;
    background: transparent;
  }
  .login-card {
    max-width: 100%;
  }
}
</style>
