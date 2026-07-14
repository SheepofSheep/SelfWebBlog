<script setup>
import { ChevronDown, LockKeyhole } from 'lucide-vue-next'
import { ref } from 'vue'

defineProps({ loading: { type: Boolean, default: false } })
defineEmits(['submit'])
const open = ref(false)
const username = ref('')
const password = ref('')
</script>

<template>
  <section class="admin-login">
    <button class="disclosure" type="button" :aria-expanded="open" @click="open = !open">
      <span><LockKeyhole :size="14" />博主工作台登录</span>
      <ChevronDown :size="15" :class="{ open }" />
    </button>
    <Transition name="admin-form">
      <form v-if="open" @submit.prevent="$emit('submit', { username: username.trim(), password })">
        <input
          v-model="username"
          type="text"
          autocomplete="username"
          placeholder="管理员用户名"
          required
        />
        <input
          v-model="password"
          type="password"
          autocomplete="current-password"
          placeholder="管理员密码"
          required
        />
        <button type="submit" :disabled="loading">进入工作台</button>
      </form>
    </Transition>
  </section>
</template>

<style scoped>
.admin-login {
  margin-top: 12px;
  border-top: 1px solid var(--border-subtle);
}
.disclosure {
  width: 100%;
  min-height: 42px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--text-tertiary);
  font: inherit;
  font-size: 0.72rem;
  cursor: pointer;
}
.disclosure span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.disclosure svg {
  transition: transform var(--motion-normal-duration) var(--motion-ease);
}
.disclosure svg.open {
  transform: rotate(180deg);
}
form {
  display: grid;
  gap: 8px;
  padding-bottom: 10px;
}
input,
form button {
  min-height: 38px;
  padding: 0 10px;
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-control);
  background: var(--surface-raised);
  color: var(--text-primary);
  font: inherit;
  font-size: 0.75rem;
}
form button {
  background: var(--text-primary);
  color: var(--surface-solid);
  font-weight: 800;
  cursor: pointer;
}
.admin-form-enter-active,
.admin-form-leave-active {
  transition:
    opacity var(--motion-normal-duration),
    transform var(--motion-normal-duration) var(--motion-ease);
}
.admin-form-enter-from,
.admin-form-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}
</style>
