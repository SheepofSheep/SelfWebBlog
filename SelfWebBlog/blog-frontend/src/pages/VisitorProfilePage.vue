<script setup>
import { ref, inject, onMounted, computed } from 'vue'
import { updateUserProfile, uploadUserAvatar } from '../utils/api'
import { useToast } from '../composables/toast'
import { toAbsoluteUrl } from '../utils/url'
import { Camera } from 'lucide-vue-next'

const { push } = useToast()
const user = inject('user', ref(null))
const refreshUser = inject('refreshUser', () => {})

const nickname = ref('')
const saving = ref(false)

onMounted(() => {
  nickname.value = user.value?.nickname || ''
})

const displayName = computed(() => user.value?.nickname || user.value?.username || '访客')
const avatarSrc = computed(
  () =>
    toAbsoluteUrl(user.value?.avatarUrl) ||
    `https://api.dicebear.com/7.x/avataaars/svg?seed=${user.value?.username || 'guest'}`
)

async function saveNickname() {
  const n = nickname.value.trim()
  if (!n) {
    push('先给自己取个昵称吧。', 'warning')
    return
  }
  if (n.length > 30) {
    push('昵称控制在 30 个字符以内会更好看。', 'warning')
    return
  }
  saving.value = true
  try {
    await updateUserProfile({ nickname: n })
    push('昵称已更新，会显示在评论区。')
    await refreshUser()
  } catch (e) {
    push(e?.message || '这次没有保存成功，稍后再试一次。', 'error')
  } finally {
    saving.value = false
  }
}

async function onPickAvatar(e) {
  const file = e?.target?.files?.[0]
  if (!file) return
  if (!file.type.startsWith('image/')) {
    push('请选择一张图片作为头像。', 'error')
    e.target.value = ''
    return
  }
  try {
    const url = await uploadUserAvatar(file)
    await updateUserProfile({ avatarUrl: url })
    push('头像已更新，评论区也会同步显示。')
    await refreshUser()
  } catch (err) {
    push(err?.message || '头像没有传上去，换张图片再试试。', 'error')
  } finally {
    e.target.value = ''
  }
}
</script>

<template>
  <main class="vp-main">
    <div class="vp-card glass-card">
      <h2 class="vp-title">我的小名片</h2>

      <!-- 头像区 -->
      <label class="vp-avatar-wrap">
        <img
          :src="avatarSrc"
          :alt="displayName"
          class="vp-avatar"
          :class="{ loaded: user?.avatarUrl }"
          @load="(e) => e.target.classList.add('loaded')"
        />
        <div class="vp-avatar-overlay"><Camera :size="20" /></div>
        <input type="file" accept="image/*" class="hidden-input" @change="onPickAvatar" />
      </label>

      <p class="vp-hint">点击头像上传新图片，会同步到评论区</p>

      <!-- 昵称区 -->
      <div class="vp-field">
        <label class="vp-label">昵称</label>
        <div class="vp-input-row">
          <input
            v-model="nickname"
            class="vp-input"
            :placeholder="user?.username || '输入你的昵称...'"
            maxlength="30"
            @keyup.enter="saveNickname"
          />
          <button class="pill-btn pill-btn-primary" :disabled="saving" @click="saveNickname">
            {{ saving ? '保存中...' : '保存' }}
          </button>
        </div>
        <p class="vp-field-hint">设置后将在评论等位置展示，留空则使用用户名</p>
      </div>

      <!-- 信息区 -->
      <div class="vp-info">
        <div class="vp-info-row">
          <span class="vp-info-label">用户名</span>
          <span class="vp-info-value">{{ user?.username || '-' }}</span>
        </div>
        <div class="vp-info-row">
          <span class="vp-info-label">角色</span>
          <span class="vp-info-value">{{ user?.role === 'ADMIN' ? '管理员' : '访客' }}</span>
        </div>
        <div v-if="user?.titleName" class="vp-info-row">
          <span class="vp-info-label">称号</span>
          <span :class="['title-badge', 'title-' + (user?.titleStyle || 'default')]">{{
            user.titleName
          }}</span>
        </div>
      </div>
    </div>
  </main>
</template>

<style scoped>
.vp-main {
  max-width: 480px;
  margin: 0 auto;
  padding: 40px 20px;
}

.vp-card {
  padding: 36px 28px;
  text-align: center;
}

.vp-title {
  margin: 0 0 24px;
  font-family: var(--font-serif);
  font-size: 1.35rem;
  font-weight: 700;
  color: var(--ink);
}

.vp-avatar-wrap {
  display: inline-block;
  position: relative;
  cursor: pointer;
  margin-bottom: 8px;
}
.vp-avatar-wrap:hover .vp-avatar-overlay {
  opacity: 1;
}

.vp-avatar {
  width: 88px;
  height: 88px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid var(--border);
  opacity: 0;
  transition: opacity var(--duration-slow) var(--ease-out);
}
.vp-avatar.loaded {
  opacity: 1;
}

.vp-avatar-overlay {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  background: rgba(63, 52, 41, 0.42);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  opacity: 0;
  transition: opacity var(--duration-fast);
}

.vp-hint {
  font-size: 0.7rem;
  color: var(--ink-muted);
  margin: 0 0 28px;
}

.vp-field {
  text-align: left;
  margin-bottom: 24px;
}

.vp-label {
  display: block;
  font-size: 0.78rem;
  font-weight: 600;
  color: var(--ink);
  margin-bottom: 8px;
}

.vp-input-row {
  display: flex;
  gap: 8px;
}

.vp-input {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: var(--surface);
  color: var(--ink);
  font-size: 0.88rem;
  outline: none;
  font-family: var(--font-body);
}
.vp-input:focus {
  border-color: var(--primary);
  box-shadow: 0 0 0 4px rgba(217, 154, 29, 0.14);
}

.vp-field-hint {
  margin: 6px 0 0;
  font-size: 0.68rem;
  color: var(--ink-faint);
}

.vp-info {
  text-align: left;
  border-top: 1px solid var(--border);
  padding-top: 16px;
}

.vp-info-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 0;
}
.vp-info-label {
  font-size: 0.78rem;
  color: var(--ink-muted);
}
.vp-info-value {
  font-size: 0.82rem;
  color: var(--ink);
  font-weight: 500;
}

.hidden-input {
  display: none;
}
</style>
