<script setup>
import { RefreshCw, Send } from 'lucide-vue-next'
import { onMounted, reactive, ref } from 'vue'
import { showToast } from '../../../composables/toast'
import { createInteraction, getCaptcha } from '../api/interactions'

const props = defineProps({
  targetId: { type: [String, Number], default: 1 },
  replyToId: { type: [String, Number], default: null },
  submitLabel: { type: String, default: '发布留言' }
})

const emit = defineEmits(['created'])
const form = reactive({ guestName: '', content: '', answer: '', website: '' })
const challenge = reactive({ id: '', image: '' })
const loadingCaptcha = ref(false)
const submitting = ref(false)

async function refreshCaptcha() {
  loadingCaptcha.value = true
  try {
    const result = await getCaptcha('guestbook')
    challenge.id = result.challengeId
    challenge.image = result.imageDataUrl
    form.answer = ''
  } catch (error) {
    showToast(error.message || '验证码加载失败。', 'error')
  } finally {
    loadingCaptcha.value = false
  }
}

async function submit() {
  if (submitting.value) return
  submitting.value = true
  try {
    const created = await createInteraction({
      targetType: 'GUESTBOOK',
      targetId: Number(props.targetId),
      replyToId: props.replyToId ? Number(props.replyToId) : null,
      guestName: form.guestName.trim(),
      content: form.content.trim(),
      captchaId: challenge.id,
      captchaAnswer: form.answer.trim(),
      website: form.website
    })
    form.content = ''
    form.answer = ''
    emit('created', created)
    showToast(
      created.status === 'PENDING' ? '留言已收到，审核后会公开显示。' : '留言发布成功。',
      'success'
    )
    await refreshCaptcha()
  } catch (error) {
    showToast(error.message || '留言发送失败，请检查后重试。', 'error')
    await refreshCaptcha()
  } finally {
    submitting.value = false
  }
}

onMounted(refreshCaptcha)
</script>

<template>
  <form class="guest-composer" @submit.prevent="submit">
    <div class="guest-fields">
      <label>
        <span>昵称</span>
        <input v-model="form.guestName" minlength="2" maxlength="30" required />
      </label>
      <label class="honeypot" aria-hidden="true">
        <span>Website</span>
        <input v-model="form.website" tabindex="-1" autocomplete="off" />
      </label>
    </div>
    <label>
      <span class="sr-only">留言内容</span>
      <textarea
        v-model="form.content"
        rows="4"
        maxlength="2000"
        placeholder="写下想说的话..."
        required
      />
    </label>
    <div class="verification-row">
      <button
        class="captcha-image"
        type="button"
        :disabled="loadingCaptcha"
        aria-label="刷新验证码"
        title="刷新验证码"
        @click="refreshCaptcha"
      >
        <img v-if="challenge.image" :src="challenge.image" alt="验证码" />
        <RefreshCw v-else :size="18" :class="{ spinning: loadingCaptcha }" />
      </button>
      <label>
        <span>验证码</span>
        <input
          v-model="form.answer"
          maxlength="12"
          autocomplete="off"
          spellcheck="false"
          required
        />
      </label>
      <button class="submit-button" type="submit" :disabled="submitting || !challenge.id">
        <Send :size="16" /> {{ submitting ? '发送中' : submitLabel }}
      </button>
    </div>
  </form>
</template>

<style scoped>
.guest-composer {
  display: grid;
  gap: 12px;
}
.guest-fields {
  display: grid;
  grid-template-columns: minmax(0, 260px);
}
label {
  display: grid;
  gap: 6px;
  color: var(--text-secondary);
  font-size: 0.76rem;
}
input,
textarea {
  width: 100%;
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-control);
  background: var(--surface-solid);
  color: var(--text-primary);
  font: inherit;
}
input {
  height: 40px;
  padding: 0 11px;
}
textarea {
  min-height: 104px;
  padding: 12px 13px;
  resize: vertical;
  line-height: 1.7;
}
input:focus,
textarea:focus {
  border-color: var(--accent);
  box-shadow: 0 0 0 3px var(--focus-ring);
  outline: none;
}
.honeypot {
  position: absolute;
  width: 1px;
  height: 1px;
  overflow: hidden;
  clip-path: inset(50%);
}
.verification-row {
  display: grid;
  grid-template-columns: 150px minmax(120px, 180px) auto;
  align-items: end;
  gap: 10px;
}
.captcha-image {
  width: 150px;
  height: 50px;
  display: grid;
  place-items: center;
  padding: 0;
  overflow: hidden;
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-control);
  background: var(--surface-soft);
  color: var(--text-secondary);
  cursor: pointer;
}
.captcha-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.submit-button {
  min-height: 40px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  padding: 0 15px;
  border: 1px solid var(--accent);
  border-radius: var(--radius-control);
  background: var(--accent);
  color: #21180a;
  font: inherit;
  font-size: 0.8rem;
  font-weight: 750;
  cursor: pointer;
}
.submit-button:disabled {
  cursor: wait;
  opacity: 0.58;
}
.spinning {
  animation: spin 0.8s linear infinite;
}
.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  overflow: hidden;
  clip-path: inset(50%);
}
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
@media (max-width: 620px) {
  .verification-row {
    grid-template-columns: 150px minmax(0, 1fr);
  }
  .submit-button {
    grid-column: 1 / -1;
  }
}
</style>
