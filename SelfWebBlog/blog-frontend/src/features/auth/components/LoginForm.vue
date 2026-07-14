<script setup>
import { ref } from 'vue'

defineProps({ loading: { type: Boolean, default: false } })
const emit = defineEmits(['submit'])
const username = ref('')
const password = ref('')

function submit() {
  emit('submit', { username: username.value.trim(), password: password.value })
}
</script>

<template>
  <form class="auth-form" @submit.prevent="submit">
    <label>用户名<input v-model="username" type="text" autocomplete="username" required /></label>
    <label
      >密码<input v-model="password" type="password" autocomplete="current-password" required
    /></label>
    <button type="submit" :disabled="loading">{{ loading ? '正在登录' : '登录' }}</button>
  </form>
</template>

<style scoped>
.auth-form {
  align-self: start;
  display: grid;
  gap: 15px;
}
label {
  display: grid;
  gap: 6px;
  color: var(--text-secondary);
  font-size: 0.75rem;
  font-weight: 700;
}
input {
  width: 100%;
  min-height: 44px;
  padding: 0 12px;
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
  margin-top: 4px;
  border: 1px solid var(--accent);
  border-radius: var(--radius-control);
  background: var(--accent);
  color: #2c1d06;
  font: inherit;
  font-weight: 800;
  cursor: pointer;
}
button:disabled {
  opacity: 0.55;
}
</style>
