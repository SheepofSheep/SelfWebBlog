<script setup>
import { onMounted, reactive, ref } from 'vue'
import { RefreshCw } from 'lucide-vue-next'
import { getAuthCaptcha } from '../../../api/auth'

defineProps({ loading: { type: Boolean, default: false } })
const emit = defineEmits(['submit'])
const form = reactive({ username: '', email: '', password: '', avatarUrl: '', captchaAnswer: '' })
const challenge = ref(null)
const captchaLoading = ref(false)

async function refreshCaptcha() {
  captchaLoading.value = true
  form.captchaAnswer = ''
  try {
    challenge.value = await getAuthCaptcha()
  } finally {
    captchaLoading.value = false
  }
}

function submit() {
  emit('submit', {
    username: form.username.trim(),
    email: form.email.trim() || undefined,
    password: form.password,
    avatarUrl: form.avatarUrl.trim() || undefined,
    captchaId: challenge.value?.id,
    captchaAnswer: form.captchaAnswer.trim()
  })
}

defineExpose({ refreshCaptcha })
onMounted(refreshCaptcha)
</script>

<template>
  <form class="auth-form" @submit.prevent="submit">
    <div class="field-row">
      <label
        >用户名<input v-model="form.username" type="text" autocomplete="username" required
      /></label>
      <label>邮箱<input v-model="form.email" type="email" autocomplete="email" /></label>
    </div>
    <label
      >密码<input v-model="form.password" type="password" autocomplete="new-password" required
    /></label>
    <label>头像链接<input v-model="form.avatarUrl" type="url" inputmode="url" /></label>
    <div class="captcha-field">
      <label
        >验证码<input
          v-model="form.captchaAnswer"
          type="text"
          autocomplete="off"
          maxlength="5"
          required
      /></label>
      <button
        class="captcha-image"
        type="button"
        :disabled="captchaLoading"
        aria-label="刷新验证码"
        title="刷新验证码"
        @click="refreshCaptcha"
      >
        <img v-if="challenge?.imageDataUrl" :src="challenge.imageDataUrl" alt="图片验证码" />
        <RefreshCw v-else :size="18" :class="{ spin: captchaLoading }" />
      </button>
    </div>
    <button class="submit-button" type="submit" :disabled="loading || captchaLoading">
      {{ loading ? '正在注册' : '注册并登录' }}
    </button>
  </form>
</template>

<style scoped>
.auth-form {
  align-self: start;
  display: grid;
  gap: 13px;
}
.field-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}
label {
  min-width: 0;
  display: grid;
  gap: 6px;
  color: var(--text-secondary);
  font-size: 0.75rem;
  font-weight: 700;
}
input {
  width: 100%;
  min-height: 42px;
  padding: 0 11px;
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-control);
  background: var(--surface-raised);
  color: var(--text-primary);
  font: inherit;
}
input:focus {
  border-color: var(--accent);
  box-shadow: 0 0 0 4px var(--focus-ring);
  outline: 0;
}
.submit-button {
  min-height: 44px;
  margin-top: 3px;
  border: 1px solid var(--accent);
  border-radius: var(--radius-control);
  background: var(--accent);
  color: #2c1d06;
  font: inherit;
  font-weight: 800;
  cursor: pointer;
}
.captcha-field {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 150px;
  gap: 10px;
  align-items: end;
}
.captcha-image {
  height: 42px;
  padding: 0;
  overflow: hidden;
  display: grid;
  place-items: center;
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-control);
  background: var(--surface-raised);
  color: var(--text-secondary);
  cursor: pointer;
}
.captcha-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.spin {
  animation: spin 0.8s linear infinite;
}
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
@media (max-width: 430px) {
  .field-row {
    grid-template-columns: 1fr;
  }
  .captcha-field {
    grid-template-columns: minmax(0, 1fr) 128px;
  }
}
</style>
