<script setup>
import { reactive } from 'vue'

defineProps({ loading: { type: Boolean, default: false } })
const emit = defineEmits(['submit'])
const form = reactive({ username: '', email: '', password: '', avatarUrl: '' })

function submit() {
  emit('submit', {
    username: form.username.trim(),
    email: form.email.trim() || undefined,
    password: form.password,
    avatarUrl: form.avatarUrl.trim() || undefined
  })
}
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
    <button type="submit" :disabled="loading">{{ loading ? '正在注册' : '注册并登录' }}</button>
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
button {
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
@media (max-width: 430px) {
  .field-row {
    grid-template-columns: 1fr;
  }
}
</style>
