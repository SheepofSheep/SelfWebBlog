<script setup>
import { computed, inject, onMounted, ref } from 'vue'
import {
  deletePost,
  getProfile,
  updateProfile,
  updateBackground,
  uploadAvatar,
  uploadImage,
  listUsers,
  grantTitle,
  deleteUser,
  listDrafts
} from '../utils/api'
import { navigate } from '../router'
import { useToast } from '../composables/toast'
import { toAbsoluteUrl } from '../utils/url'
import { formatTime, toPlainText, getFirstImageUrl } from '../utils/format'
import ConfirmDialog from '../components/ConfirmDialog.vue'
import { Image, Settings, Edit3, Trash2, Camera, FileText, Users, FileClock } from 'lucide-vue-next'

const { push } = useToast()
const user = inject('user', ref(null))
const refreshHome = inject('refreshHome', null)
const loading = ref(false)
const blogInfo = ref(null)
const posts = ref([])
const nicknameModal = ref(false)
const newNickname = ref('')
const newBio = ref('')
const tab = ref('posts') // posts | users | drafts

// 草稿管理
const drafts = ref([])
const draftsLoading = ref(false)

// 用户管理
const users = ref([])
const usersLoading = ref(false)
const titleModal = ref(false)
const titleUserId = ref(null)
const titleName = ref('')
const titleStyle = ref('default')

const titleThemes = [
  { value: 'default', label: '素雅灰', color: '#6b6660' },
  { value: 'gold', label: '流金', color: '#fbbf24' },
  { value: 'fire', label: '烈焰', color: '#f87171' },
  { value: 'ice', label: '冰霜', color: '#7dd3fc' },
  { value: 'nature', label: '自然', color: '#86efac' },
  { value: 'star', label: '星空', color: '#c084fc' },
  { value: 'night', label: '暗夜', color: '#5a5580' },
  { value: 'candy', label: '糖果', color: '#f9a8c2' }
]

const showIpDiagnostics = ref(false)
const confirmDialog = ref({
  open: false,
  title: '',
  message: '',
  confirmText: '确认',
  onConfirm: null
})

const allPosts = computed(() => [...posts.value].reverse())
const publishedCount = computed(() => posts.value.filter((p) => p.status !== 'DRAFT').length)
const draftCount = computed(() => drafts.value.length)

const workbenchItems = computed(() => [
  { key: 'posts', label: '文章', value: posts.value.length, icon: FileText },
  { key: 'drafts', label: '草稿', value: draftCount.value || '查看', icon: FileClock },
  { key: 'users', label: '用户', value: users.value.length || '查看', icon: Users }
])

function openTab(nextTab) {
  tab.value = nextTab
  if (nextTab === 'users' && users.value.length === 0) loadUsers()
  if (nextTab === 'drafts' && drafts.value.length === 0) loadDrafts()
}

function askConfirm({ title, message, confirmText = '确认删除', onConfirm }) {
  confirmDialog.value = { open: true, title, message, confirmText, onConfirm }
}

async function handleConfirmAction() {
  const action = confirmDialog.value.onConfirm
  confirmDialog.value.open = false
  confirmDialog.value.onConfirm = null
  if (action) await action()
}

async function refresh() {
  loading.value = true
  try {
    const data = await getProfile()
    blogInfo.value = data.blogInfo || null
    posts.value = Array.isArray(data.posts) ? data.posts : []
  } catch (e) {
    push(e?.message || '加载失败', 'error')
  } finally {
    loading.value = false
  }
}

function goHome() {
  navigate('/')
}
function openPost(id) {
  navigate(`/post?id=${encodeURIComponent(String(id))}`)
}
function openNicknameModal() {
  newNickname.value = blogInfo.value?.nickname || ''
  newBio.value = blogInfo.value?.bio || ''
  nicknameModal.value = true
}
function closeNicknameModal() {
  nicknameModal.value = false
}

async function saveNickname() {
  const n = newNickname.value.trim()
  if (!n) {
    push('请输入昵称', 'warning')
    return
  }
  try {
    await updateProfile({ nickname: n, bio: newBio.value.trim() })
    push('资料修改成功')
    closeNicknameModal()
    await refresh()
    if (refreshHome) await refreshHome()
  } catch (e) {
    push(e?.message || '修改失败', 'error')
  }
}

async function onPickAvatar(e) {
  const file = e?.target?.files?.[0]
  if (!file) return
  if (!file.type.startsWith('image/')) {
    push('请选择图片文件', 'error')
    e.target.value = ''
    return
  }
  try {
    await uploadAvatar(file)
    push('头像更新成功')
    await refresh()
    if (refreshHome) await refreshHome()
  } catch (err) {
    push(err?.message || '上传失败', 'error')
  } finally {
    e.target.value = ''
  }
}

async function onPickBg(e) {
  const file = e?.target?.files?.[0]
  if (!file) return
  try {
    const url = await uploadImage(file)
    await updateBackground(url)
    push('背景图更新成功')
    await refresh()
    if (refreshHome) await refreshHome()
  } catch (err) {
    push(err?.message || '上传失败', 'error')
  } finally {
    e.target.value = ''
  }
}

async function clearBg() {
  try {
    await updateBackground('')
    push('背景图已清除')
    await refresh()
    if (refreshHome) await refreshHome()
  } catch (e) {
    push(e?.message || '背景暂时没有清除成功，稍后再试一次。', 'error')
  }
}

function editPost(p) {
  sessionStorage.setItem(
    'editingPost',
    JSON.stringify({
      id: p.id,
      title: p.title,
      content: p.content,
      summary: p.summary || '',
      coverUrl: p.coverUrl || '',
      category: p.category || '',
      tags: p.tags || ''
    })
  )
  navigate('/write')
}

async function remove(id) {
  askConfirm({
    title: tab.value === 'drafts' ? '删除草稿' : '删除文章',
    message: '删除后无法恢复，请确认这不是误操作。',
    onConfirm: async () => {
      try {
        await deletePost(id)
        push('已删除')
        await refresh()
        if (tab.value === 'drafts') await loadDrafts()
      } catch (e) {
        push(e?.message || '暂时没有删除成功，稍后再试一次。', 'error')
      }
    }
  })
}

// ====== 用户管理 ======

async function loadUsers() {
  usersLoading.value = true
  try {
    const data = await listUsers()
    users.value = data.users || []
  } catch (e) {
    push(e?.message || '加载用户失败', 'error')
  } finally {
    usersLoading.value = false
  }
}

async function loadDrafts() {
  draftsLoading.value = true
  try {
    const data = await listDrafts()
    drafts.value = Array.isArray(data?.records) ? data.records : Array.isArray(data) ? data : []
  } catch (e) {
    push(e?.message || '加载草稿失败', 'error')
  } finally {
    draftsLoading.value = false
  }
}

function editDraft(p) {
  sessionStorage.setItem(
    'editingPost',
    JSON.stringify({
      id: p.id,
      title: p.title,
      content: p.content,
      summary: p.summary || '',
      coverUrl: p.coverUrl || '',
      category: p.category || '',
      tags: p.tags || '',
      postStatus: p.status || 'DRAFT'
    })
  )
  navigate('/write')
}

function openTitleModal(user) {
  titleUserId.value = user.id
  titleName.value = user.titleName || ''
  titleStyle.value = user.titleStyle || 'default'
  titleModal.value = true
}

function closeTitleModal() {
  titleModal.value = false
}

async function handleGrantTitle() {
  if (!titleName.value.trim()) {
    push('请输入称号名称', 'warning')
    return
  }
  try {
    await grantTitle(titleUserId.value, titleName.value.trim(), titleStyle.value)
    push('称号授予成功')
    closeTitleModal()
    await loadUsers()
  } catch (e) {
    push(e?.message || '称号暂时没有设置成功，稍后再试一次。', 'error')
  }
}

async function handleClearTitle() {
  try {
    await grantTitle(titleUserId.value, '', 'default')
    push('称号已移除')
    closeTitleModal()
    await loadUsers()
  } catch (e) {
    push(e?.message || '称号暂时没有移除成功，稍后再试一次。', 'error')
  }
}

async function handleDeleteUser(u) {
  askConfirm({
    title: '删除用户',
    message: `确定要删除用户「${u.username}」吗？此操作不可恢复，也不会批量删除任何内容。`,
    onConfirm: async () => {
      try {
        await deleteUser(u.id)
        push('用户已删除')
        await loadUsers()
      } catch (e) {
        push(e?.message || '删除失败', 'error')
      }
    }
  })
}

onMounted(async () => {
  await refresh()
})
</script>

<template>
  <main class="profile-main">
    <!-- 左侧 -->
    <aside class="profile-aside">
      <div class="glass-card profile-card">
        <label class="avatar-wrap">
          <img
            :src="
              toAbsoluteUrl(
                user?.avatarUrl ||
                  blogInfo?.avatarUrl ||
                  'https://api.dicebear.com/7.x/avataaars/svg?seed=Felix'
              )
            "
            :alt="blogInfo?.nickname || '博主头像'"
            class="avatar-img"
            :class="{ loaded: user?.avatarUrl || blogInfo?.avatarUrl }"
            @load="(e) => e.target.classList.add('loaded')"
          />
          <div class="avatar-overlay"><Camera :size="18" /></div>
          <input type="file" accept="image/*" class="hidden-input" @change="onPickAvatar" />
        </label>
        <h2 class="profile-name">{{ blogInfo?.nickname || '加载中...' }}</h2>
        <p class="profile-bio">{{ blogInfo?.bio || '...' }}</p>
        <div class="profile-stat">
          <span class="stat-num">{{ posts.length }}</span
          ><span class="stat-label">篇文章</span>
        </div>
      </div>

      <div class="glass-card settings-card">
        <div class="settings-head"><Settings :size="14" /><span>设置</span></div>
        <div class="setting-row" @click="openNicknameModal">
          <span>昵称</span><span class="setting-val">{{ blogInfo?.nickname || '未设置' }}</span>
        </div>
        <div class="setting-row" @click="openNicknameModal">
          <span>简介</span><span class="setting-val">{{ blogInfo?.bio || '未设置' }}</span>
        </div>
        <label class="setting-row"
          ><span>头像</span><span class="setting-val link">点击头像上传</span
          ><input type="file" accept="image/*" class="hidden-input" @change="onPickAvatar"
        /></label>
        <div class="setting-row">
          <span>背景图</span>
          <div class="bg-actions">
            <label class="bg-btn"
              >上传<input type="file" accept="image/*" class="hidden-input" @change="onPickBg"
            /></label>
            <button class="bg-clear" @click="clearBg">清除</button>
          </div>
        </div>
      </div>
    </aside>

    <!-- 右侧 -->
    <section class="profile-feed">
      <div class="workbench-head glass-card">
        <div>
          <p class="workbench-kicker">Gabriel Workbench</p>
          <h1 class="workbench-title">博客轻工作台</h1>
          <p class="workbench-sub">写作、资料和用户管理都从这里进入。</p>
        </div>
        <button class="pill-btn pill-btn-primary" @click="navigate('/write')">
          <Edit3 :size="16" /> 写文章
        </button>
      </div>

      <div class="workbench-grid">
        <button
          v-for="item in workbenchItems"
          :key="item.key"
          :class="['glass-card', 'workbench-card', { active: tab === item.key }]"
          :aria-pressed="tab === item.key"
          @click="openTab(item.key)"
        >
          <component :is="item.icon" :size="18" class="workbench-icon" />
          <span class="workbench-value">{{ item.value }}</span>
          <span class="workbench-label">{{ item.label }}</span>
        </button>
      </div>

      <!-- 文章列表 -->
      <div v-if="tab === 'posts'">
        <div class="feed-head">
          <h2 class="feed-title">我的文章</h2>
          <span class="badge-count">{{ publishedCount }} 已发布 / {{ posts.length }} 全部</span>
        </div>
        <div v-if="allPosts.length === 0" class="empty-state">
          <div class="empty-icon"><Edit3 :size="32" /></div>
          <div class="empty-title">还没有文章</div>
          <button class="pill-btn pill-btn-primary" @click="goHome">回首页写文章</button>
        </div>
        <div v-else class="post-list">
          <article
            v-for="p in allPosts"
            :key="p.id"
            class="glass-card post-item"
            @click="openPost(p.id)"
          >
            <div class="post-thumb">
              <img
                v-if="p.coverUrl || getFirstImageUrl(p.content)"
                :src="p.coverUrl || toAbsoluteUrl(getFirstImageUrl(p.content))"
                alt=""
              />
              <Image v-else :size="22" class="thumb-icon" />
            </div>
            <div class="post-body">
              <div class="post-head-row">
                <h3 class="post-title">{{ p.title }}</h3>
                <span v-if="p.status === 'DRAFT'" class="status-dot draft">草稿</span>
                <span v-else class="status-dot published">已发布</span>
              </div>
              <span class="post-date">{{ formatTime(p.createTime) }}</span>
              <p class="post-excerpt">
                {{ p.summary || toPlainText(p.content).slice(0, 80)
                }}{{ (p.summary || toPlainText(p.content)).length > 80 ? '...' : '' }}
              </p>
            </div>
            <div class="post-actions" @click.stop>
              <button class="act-btn edit" aria-label="编辑文章" @click="editPost(p)">
                <Edit3 :size="14" />
              </button>
              <button class="act-btn del" aria-label="删除文章" @click="remove(p.id)">
                <Trash2 :size="14" />
              </button>
            </div>
          </article>
        </div>
      </div>

      <!-- 用户管理 -->
      <div v-if="tab === 'users'">
        <div class="feed-head">
          <h2 class="feed-title">用户管理</h2>
          <div class="feed-actions">
            <span class="badge-count">{{ users.length }} 人</span>
            <button class="diagnostic-toggle" @click="showIpDiagnostics = !showIpDiagnostics">
              <EyeOff v-if="showIpDiagnostics" :size="14" />
              <Eye v-else :size="14" />
              {{ showIpDiagnostics ? '隐藏 IP' : '显示 IP' }}
            </button>
          </div>
        </div>
        <div v-if="usersLoading" class="loading-state">加载中...</div>
        <div v-else-if="users.length === 0" class="empty-state">
          <div class="empty-title">现在还没有访客用户</div>
        </div>
        <div v-else class="user-list">
          <div v-for="u in users" :key="u.id" class="glass-card user-item">
            <img
              :src="u.avatarUrl || 'https://api.dicebear.com/7.x/avataaars/svg?seed=' + u.username"
              class="user-avatar"
              :alt="u.username"
            />
            <div class="user-body">
              <div class="user-head">
                <span class="user-name">{{ u.username }}</span>
                <span
                  v-if="u.titleName"
                  :class="['title-badge', 'title-' + (u.titleStyle || 'default')]"
                  >{{ u.titleName }}</span
                >
                <span v-if="u.role === 'ADMIN'" class="user-role-badge">管理员</span>
              </div>
              <div class="user-meta">
                <span class="user-email">{{ u.email || '未填邮箱' }}</span>
                <span v-if="showIpDiagnostics && u.ipAddress" class="user-ip"
                  >IP: {{ u.ipAddress }}</span
                >
                <span
                  v-if="u.duplicateIp"
                  class="dup-ip-warn"
                  :title="'同IP账号: ' + (u.sameIpUsers || []).join(', ')"
                  >⚠ 同IP多账号</span
                >
              </div>
              <div class="user-time">注册于 {{ formatTime(u.createTime) }}</div>
            </div>
            <button class="pill-btn pill-btn-ghost user-title-btn" @click="openTitleModal(u)">
              {{ u.titleName ? '修改称号' : '授予称号' }}
            </button>
            <button
              v-if="u.role !== 'ADMIN'"
              class="user-del-btn"
              :aria-label="'删除用户' + u.username"
              title="删除用户"
              @click="handleDeleteUser(u)"
            >
              <Trash2 :size="14" />
            </button>
          </div>
        </div>
      </div>

      <!-- 草稿列表 -->
      <div v-if="tab === 'drafts'">
        <div class="feed-head">
          <h2 class="feed-title">草稿箱</h2>
          <span class="badge-count">{{ drafts.length }} 篇</span>
        </div>
        <div v-if="draftsLoading" class="loading-state">加载中...</div>
        <div v-else-if="drafts.length === 0" class="empty-state">
          <div class="empty-title">暂无草稿</div>
          <div class="empty-desc">在写作页保存为草稿的文章会显示在这里</div>
        </div>
        <div v-else class="post-list">
          <article
            v-for="p in drafts"
            :key="p.id"
            class="glass-card post-item"
            @click="editDraft(p)"
          >
            <div class="post-thumb">
              <img v-if="p.coverUrl" :src="p.coverUrl" alt="" />
              <Image v-else :size="22" class="thumb-icon" />
            </div>
            <div class="post-body">
              <h3 class="post-title">{{ p.title || '未命名草稿' }}</h3>
              <span class="post-date">{{ formatTime(p.updateTime || p.createTime) }}</span>
              <p class="post-excerpt">
                {{ p.summary || toPlainText(p.content).slice(0, 80)
                }}{{ (p.summary || toPlainText(p.content)).length > 80 ? '...' : '' }}
              </p>
            </div>
            <div class="post-actions" @click.stop>
              <span class="draft-badge">草稿</span>
              <button class="act-btn del" aria-label="删除草稿" @click="remove(p.id)">
                <Trash2 :size="14" />
              </button>
            </div>
          </article>
        </div>
      </div>
    </section>

    <!-- 称号弹窗 -->
    <Transition name="modal">
      <div v-if="titleModal" class="modal-mask" @click.self="closeTitleModal">
        <div class="glass-card modal-card">
          <h3 class="modal-title">授予称号</h3>
          <div class="modal-field">
            <label class="field-label">称号名称</label>
            <input
              v-model="titleName"
              class="modal-input"
              placeholder="如：荣誉会员、星标访客..."
              @keyup.enter="handleGrantTitle"
            />
          </div>
          <div class="modal-field">
            <label class="field-label">称号样式</label>
            <div class="theme-grid">
              <button
                v-for="t in titleThemes"
                :key="t.value"
                :class="['theme-chip', { active: titleStyle === t.value }]"
                :style="{ '--chip-color': t.color }"
                @click="titleStyle = t.value"
              >
                <span class="chip-dot"></span>
                {{ t.label }}
              </button>
            </div>
          </div>
          <div v-if="titleName" class="modal-field">
            <label class="field-label">预览</label>
            <span :class="['title-badge', 'title-' + titleStyle]">{{ titleName }}</span>
          </div>
          <div class="modal-btns">
            <button class="pill-btn pill-btn-ghost" @click="closeTitleModal">取消</button>
            <button
              v-if="titleName"
              class="pill-btn pill-btn-ghost"
              style="color: var(--danger); border-color: var(--danger)"
              @click="handleClearTitle"
            >
              移除称号
            </button>
            <button class="pill-btn pill-btn-primary" @click="handleGrantTitle">保存</button>
          </div>
        </div>
      </div>
    </Transition>

    <!-- 昵称弹窗 -->
    <Transition name="modal">
      <div v-if="nicknameModal" class="modal-mask" @click.self="closeNicknameModal">
        <div class="glass-card modal-card">
          <h3 class="modal-title">修改资料</h3>
          <div class="modal-field">
            <label class="field-label">昵称</label>
            <input
              v-model="newNickname"
              class="modal-input"
              placeholder="输入新昵称..."
              @keyup.enter="saveNickname"
            />
          </div>
          <div class="modal-field">
            <label class="field-label">简介</label>
            <input
              v-model="newBio"
              class="modal-input"
              placeholder="写一句话介绍自己..."
              @keyup.enter="saveNickname"
            />
          </div>
          <div class="modal-btns">
            <button class="pill-btn pill-btn-ghost" @click="closeNicknameModal">取消</button>
            <button class="pill-btn pill-btn-primary" @click="saveNickname">保存</button>
          </div>
        </div>
      </div>
    </Transition>

    <ConfirmDialog
      v-model="confirmDialog.open"
      :title="confirmDialog.title"
      :message="confirmDialog.message"
      :confirm-text="confirmDialog.confirmText"
      cancel-text="再想想"
      @confirm="handleConfirmAction"
    />
  </main>
</template>

<style scoped>
.profile-main {
  display: flex;
  gap: 28px;
  max-width: var(--magazine-max, 1180px);
  margin: 0 auto;
  width: 100%;
  padding: 0 var(--space-md) var(--space-xl);
}

.profile-aside {
  width: 260px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
  position: sticky;
  top: 104px;
  align-self: flex-start;
}

.profile-card {
  padding: 24px 18px;
  text-align: center;
}
.avatar-wrap {
  display: inline-block;
  position: relative;
  cursor: pointer;
  margin-bottom: 14px;
}
.avatar-wrap:hover .avatar-overlay {
  opacity: 1;
}
.avatar-wrap:hover .avatar-img {
  border-color: var(--primary);
}

.profile-name {
  margin: 0 0 4px;
  font-family: var(--font-display);
  font-size: 1.1rem;
  font-weight: 700;
  color: var(--primary);
}
.profile-bio {
  margin: 0 0 12px;
  font-size: 0.76rem;
  color: var(--text-muted);
}
.profile-stat {
  display: flex;
  flex-direction: column;
  align-items: center;
}
.stat-num {
  font-family: var(--font-display);
  font-size: 1.4rem;
  color: var(--primary);
  font-weight: 700;
}
.stat-label {
  font-size: 0.65rem;
  color: var(--text-muted);
}

.settings-card {
  padding: 0;
  overflow: hidden;
}
.settings-head {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  font-size: 0.82rem;
  font-weight: 600;
  color: var(--text-main);
  border-bottom: 1px solid var(--border);
}
.setting-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  border-bottom: 1px solid var(--border);
  font-size: 0.78rem;
  color: var(--text-muted);
  cursor: pointer;
  transition: background var(--duration-fast);
}
.setting-row:last-child {
  border-bottom: none;
}
.setting-row:hover {
  background: var(--primary-soft);
}
.setting-val {
  font-size: 0.7rem;
  color: var(--text-muted);
}
.setting-val.link {
  color: var(--primary);
}
.bg-actions {
  display: flex;
  gap: 6px;
}
.bg-btn:hover {
  background: var(--primary);
  color: #fff;
}
.bg-clear {
  font-size: 0.68rem;
  padding: 4px 8px;
  border: none;
  background: none;
  color: var(--text-muted);
  cursor: pointer;
}
.bg-clear:hover {
  color: var(--primary);
}
.hidden-input {
  display: none;
}

/* ====== Feed ====== */
.profile-feed {
  flex: 1;
  min-width: 0;
}
.workbench-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 24px;
  margin-bottom: 14px;
}
.workbench-head:hover {
  transform: none;
}
.workbench-kicker {
  margin: 0 0 4px;
  font-size: 0.72rem;
  font-weight: 700;
  color: var(--primary-hover);
  letter-spacing: 0;
}
.workbench-title {
  margin: 0;
  font-size: 1.35rem;
  color: var(--text-main);
}
.workbench-sub {
  margin: 8px 0 0;
  color: var(--text-muted);
  font-size: 0.84rem;
}
.workbench-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 18px;
}
.workbench-card {
  min-height: 92px;
  padding: 16px;
  border: 0;
  text-align: left;
  cursor: pointer;
  color: var(--text-main);
  display: grid;
  grid-template-columns: auto 1fr;
  grid-template-areas:
    'icon value'
    'label label';
  align-items: center;
  gap: 8px 10px;
}
.workbench-card:hover {
  transform: translateY(-2px);
}
.workbench-card.active {
  border-color: var(--border-warm);
  background-image:
    linear-gradient(180deg, rgba(255, 248, 230, 0.98), rgba(255, 239, 199, 0.9)),
    radial-gradient(circle at 18% 0%, rgba(217, 154, 29, 0.2), transparent 34%);
  box-shadow:
    0 16px 42px rgba(135, 88, 18, 0.16),
    inset 0 0 0 1px rgba(217, 154, 29, 0.16);
}
[data-theme='dark'] .workbench-card.active {
  background-image:
    linear-gradient(180deg, rgba(56, 43, 24, 0.94), rgba(38, 30, 20, 0.9)),
    radial-gradient(circle at 18% 0%, rgba(216, 162, 58, 0.18), transparent 34%);
  box-shadow:
    0 18px 48px rgba(0, 0, 0, 0.38),
    inset 0 0 0 1px rgba(240, 189, 84, 0.12);
}
.workbench-icon {
  grid-area: icon;
  color: var(--primary-hover);
}
.workbench-value {
  grid-area: value;
  justify-self: end;
  font-size: 1.2rem;
  font-weight: 700;
  color: var(--primary-hover);
}
.workbench-label {
  grid-area: label;
  color: var(--text-muted);
  font-size: 0.78rem;
}

.feed-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}
.feed-title {
  margin: 0;
  font-family: var(--font-display);
  font-size: 1.1rem;
  font-weight: 700;
}
.feed-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
}
.badge-count {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 4px 12px;
  border-radius: var(--radius-pill);
  background: var(--primary-soft);
  color: var(--primary-hover);
  font-size: 0.74rem;
  font-weight: 600;
  white-space: nowrap;
}
.diagnostic-toggle {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  min-height: 30px;
  padding: 4px 12px;
  border-radius: var(--radius-pill);
  border: 1px solid var(--border);
  background: rgba(255, 255, 255, 0.42);
  color: var(--text-muted);
  font-size: 0.74rem;
  cursor: pointer;
  transition:
    color var(--duration-fast),
    border-color var(--duration-fast),
    background var(--duration-fast);
}
.diagnostic-toggle:hover {
  color: var(--primary-hover);
  border-color: var(--primary);
  background: var(--primary-soft);
}

.post-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.post-item {
  padding: 16px;
  display: flex;
  gap: 16px;
  align-items: center;
  cursor: pointer;
  transition:
    border-color var(--duration-normal) var(--ease-out),
    background var(--duration-normal) var(--ease-out);
}
.post-item:hover {
  transform: none;
}
.post-thumb {
  width: 80px;
  height: 56px;
  flex-shrink: 0;
  border-radius: var(--radius-sm);
  overflow: hidden;
}
.post-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.post-head-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 2px;
}
.status-dot {
  font-size: 0.6rem;
  font-weight: 600;
  padding: 1px 8px;
  border-radius: var(--radius-pill);
  white-space: nowrap;
  flex-shrink: 0;
}
.status-dot.draft {
  color: var(--amber);
  background: rgba(212, 154, 90, 0.15);
}
.status-dot.published {
  color: var(--success);
  background: rgba(109, 168, 138, 0.12);
}
.thumb-icon {
  color: var(--primary);
  opacity: 0.4;
}
.post-body {
  flex: 1;
  min-width: 0;
}
.post-title {
  margin: 0 0 4px;
  font-family: var(--font-display);
  font-size: 0.9rem;
  font-weight: 700;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.post-date {
  font-size: 0.65rem;
  color: var(--text-muted);
}
.post-excerpt {
  margin: 4px 0 0;
  font-size: 0.76rem;
  color: var(--text-muted);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.post-actions {
  display: flex;
  gap: 6px;
  opacity: 0;
  transition: opacity var(--duration-fast);
  flex-shrink: 0;
}
.post-item:hover .post-actions {
  opacity: 1;
}
.act-btn {
  width: 34px;
  height: 34px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  background: rgba(255, 255, 255, 0.32);
  transition:
    background var(--duration-fast),
    color var(--duration-fast),
    border-color var(--duration-fast);
}
.act-btn.edit {
  color: var(--primary);
}
.act-btn.edit:hover {
  background: var(--primary);
  color: #fff;
}
.act-btn.del {
  color: var(--text-muted);
}
.act-btn.del:hover {
  background: var(--primary);
  color: #fff;
}

/* ====== 弹窗 ====== */
.modal-mask {
  position: fixed;
  inset: 0;
  z-index: 9998;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(60, 45, 45, 0.18);
  backdrop-filter: blur(12px);
}
.modal-card {
  width: min(400px, calc(100vw - 32px));
  padding: 24px;
}
.modal-title {
  margin: 0 0 1rem;
  font-family: var(--font-display);
  font-size: 1.1rem;
  font-weight: 700;
}
.modal-field {
  margin-bottom: 0.75rem;
}
.modal-field .field-label {
  display: block;
  font-size: 0.72rem;
  font-weight: 600;
  color: var(--text-muted);
  margin-bottom: 4px;
}
.modal-input {
  width: 100%;
  box-sizing: border-box;
  padding: 8px 12px;
  border: 1px solid var(--border);
  background: var(--surface);
  color: var(--text-main);
  font-size: 0.88rem;
  outline: none;
}
.modal-input:focus {
  border-color: var(--primary);
}
.modal-btns {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.modal-enter-active {
  transition:
    opacity 0.25s var(--ease-out),
    transform 0.25s var(--ease-out);
}
.modal-leave-active {
  transition:
    opacity 0.15s var(--ease-out),
    transform 0.15s var(--ease-out);
}
.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

.empty-state {
  text-align: center;
  padding: 48px 0;
  color: var(--text-muted);
}
.empty-icon {
  margin-bottom: 12px;
  color: var(--text-muted);
}
.empty-title {
  font-family: var(--font-display);
  font-size: 1.05rem;
  font-weight: 700;
  color: var(--text-main);
  margin-bottom: 6px;
}
.empty-desc {
  font-size: 0.82rem;
  color: var(--text-muted);
  margin-bottom: 16px;
}

/* ====== 评论列表 ====== */
.comment-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.comment-item {
  padding: 14px 16px;
  display: flex;
  gap: 12px;
  align-items: flex-start;
}
.comment-item:hover {
  transform: none;
}
.comment-body {
  flex: 1;
  min-width: 0;
}
.comment-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}
.comment-name {
  font-weight: 600;
  font-size: 0.85rem;
  color: var(--text-main);
}
.comment-time {
  font-size: 0.72rem;
  color: var(--text-muted);
  margin-left: auto;
}
.comment-text {
  margin: 0 0 6px;
  font-size: 0.88rem;
  color: var(--text-muted);
  line-height: 1.55;
}
.comment-post-id {
  font-size: 0.68rem;
  color: var(--text-muted);
}
.comment-item:hover .comment-del {
  opacity: 1;
}
.comment-del:hover {
  color: var(--primary);
  background: var(--primary-soft);
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
.loading-state {
  text-align: center;
  padding: 24px 0;
}

/* ====== 用户管理 ====== */
.user-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.user-item {
  padding: 14px 16px;
  display: flex;
  gap: 14px;
  align-items: center;
}
.user-item:hover {
  transform: none;
}
.user-avatar {
  width: 40px;
  height: 40px;
  border-radius: var(--radius-sm);
  object-fit: cover;
  flex-shrink: 0;
}
.user-body {
  flex: 1;
  min-width: 0;
}
.user-head {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 4px;
}
.user-name {
  font-weight: 600;
  font-size: 0.88rem;
  color: var(--text-main);
}
.user-role-badge {
  font-size: 0.6rem;
  padding: 1px 8px;
  border-radius: var(--radius-sm);
  background: var(--primary-soft);
  color: var(--primary);
  font-weight: 600;
}
.user-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 2px;
}
.user-email {
  font-size: 0.72rem;
  color: var(--text-muted);
}
.user-ip {
  font-size: 0.68rem;
  color: var(--text-faint);
  font-family: var(--font-mono);
  padding: 1px 7px;
  border-radius: var(--radius-pill);
  background: rgba(255, 255, 255, 0.3);
  border: 1px solid var(--border-light);
}
.dup-ip-warn {
  font-size: 0.68rem;
  color: var(--amber);
  font-weight: 600;
  cursor: help;
}
.user-time {
  font-size: 0.68rem;
  color: var(--text-faint);
}
.user-title-btn {
  flex-shrink: 0;
  font-size: 0.72rem;
  padding: 5px 12px;
  white-space: nowrap;
}
.user-del-btn {
  flex-shrink: 0;
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--text-muted);
  cursor: pointer;
  transition:
    background var(--duration-fast),
    color var(--duration-fast),
    border-color var(--duration-fast);
}
.user-del-btn:hover {
  background: var(--danger);
  color: #fff;
  border-color: var(--danger);
}

.draft-badge {
  font-size: 0.65rem;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: var(--radius-pill);
  background: rgba(212, 154, 90, 0.15);
  color: var(--amber);
  white-space: nowrap;
}

/* ====== 称号主题选择器 ====== */
.theme-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 6px;
}
.theme-chip {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 6px 10px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: var(--surface);
  cursor: pointer;
  font-size: 0.7rem;
  color: var(--text-muted);
  transition:
    border-color var(--duration-fast),
    background var(--duration-fast);
}
.theme-chip:hover {
  border-color: var(--text-muted);
}
.theme-chip.active {
  border-color: var(--chip-color, var(--primary));
  background: var(--primary-soft);
  color: var(--text-main);
}
.chip-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: var(--chip-color, #888);
  flex-shrink: 0;
}

@media (max-width: 900px) {
  .profile-main {
    flex-direction: column;
    padding-inline: var(--space-sm);
  }
  .profile-aside {
    width: 100%;
    position: static;
  }
  .workbench-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .theme-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 560px) {
  .profile-main {
    gap: 18px;
  }
  .workbench-head {
    align-items: stretch;
    flex-direction: column;
    padding: 20px;
  }
  .workbench-head .pill-btn {
    width: 100%;
  }
  .workbench-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 8px;
  }
  .workbench-card {
    min-height: 84px;
    padding: 14px;
  }
  .feed-head {
    align-items: flex-start;
    flex-direction: column;
  }
  .feed-actions {
    width: 100%;
    justify-content: flex-start;
  }
  .post-item,
  .user-item {
    align-items: flex-start;
  }
  .post-item {
    display: grid;
    grid-template-columns: 64px minmax(0, 1fr);
  }
  .post-thumb {
    width: 64px;
    height: 52px;
  }
  .post-actions {
    grid-column: 1 / -1;
    opacity: 1;
    justify-content: flex-end;
  }
  .user-item {
    flex-wrap: wrap;
  }
  .user-title-btn {
    flex: 1 1 auto;
  }
}
</style>
