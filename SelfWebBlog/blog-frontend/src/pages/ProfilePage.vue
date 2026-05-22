<script setup>
import { computed, inject, onMounted, ref } from 'vue'
import { deletePost, getProfile, updateProfile, uploadAvatar, uploadImage, listPosters, savePoster, deletePoster as delPoster, listUsers, grantTitle } from '../utils/api'
import { navigate } from '../router'
import { useToast } from '../composables/toast'
import { toAbsoluteUrl } from '../utils/url'
import { formatTime, toPlainText, getFirstImageUrl } from '../utils/format'
import { Image, Settings, Edit3, Trash2, Camera, Plus, ChevronUp, ChevronDown, X } from 'lucide-vue-next'

const { push } = useToast()
const user = inject('user', ref(null))
const isAdmin = computed(() => user.value?.role === 'ADMIN')

const loading = ref(false)
const blogInfo = ref(null)
const posts = ref([])
const nicknameModal = ref(false)
const newNickname = ref('')
const newBio = ref('')
const tab = ref('posts') // posts | posters | users

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
  { value: 'candy', label: '糖果', color: '#f9a8c2' },
]

// 评论管理
const comments = ref([])
const commentsLoading = ref(false)
const commentsTotal = ref(0)

// 海报相关
const posters = ref([])
const posterModal = ref(false)
const posterImageUrl = ref('')
const posterUploading = ref(false)
const posterInput = ref(null)

const allPosts = computed(() => [...posts.value].reverse())

async function refresh() {
  loading.value = true
  try {
    const data = await getProfile()
    blogInfo.value = data.blogInfo || null
    posts.value = Array.isArray(data.posts) ? data.posts : []
  } catch (e) { push(e?.message || '加载失败', 'error') }
  finally { loading.value = false }
}

async function loadPosters() {
  try {
    const data = await listPosters()
    posters.value = Array.isArray(data) ? data : []
  } catch (e) {
    console.warn('加载海报失败', e)
    posters.value = []
  }
}

function goHome() { navigate('/') }
function openPost(id) { navigate(`/post?id=${encodeURIComponent(String(id))}`) }
function openNicknameModal() { newNickname.value = blogInfo.value?.nickname || ''; newBio.value = blogInfo.value?.bio || ''; nicknameModal.value = true }
function closeNicknameModal() { nicknameModal.value = false }

async function saveNickname() {
  const n = newNickname.value.trim()
  if (!n) { push('请输入昵称', 'warning'); return }
  try { await updateProfile({ nickname: n, bio: newBio.value.trim() }); push('资料修改成功'); closeNicknameModal(); await refresh() }
  catch (e) { push(e?.message || '修改失败', 'error') }
}

async function onPickAvatar(e) {
  const file = e?.target?.files?.[0]
  if (!file) return
  if (!file.type.startsWith('image/')) { push('请选择图片文件', 'error'); e.target.value = ''; return }
  try { await uploadAvatar(file); push('头像更新成功'); await refresh() }
  catch (err) { push(err?.message || '上传失败', 'error') }
  finally { e.target.value = '' }
}

async function onPickBg(e) {
  const file = e?.target?.files?.[0]
  if (!file) return
  try { const url = await uploadImage(file); await updateProfile({ bgUrl: url }); push('背景图更新成功'); await refresh() }
  catch (err) { push(err?.message || '上传失败', 'error') }
  finally { e.target.value = '' }
}

async function clearBg() { try { await updateProfile({ bgUrl: '' }); push('背景图已清除'); await refresh() } catch (e) { push(e?.message || '操作失败', 'error') } }

function editPost(p) {
  sessionStorage.setItem('editingPost', JSON.stringify({ id: p.id, title: p.title, content: p.content }))
  navigate('/write')
}

async function remove(id) {
  if (!confirm('确定要删除这篇文章吗？')) return
  try { await deletePost(id); push('删除成功'); await refresh() }
  catch (e) { push(e?.message || '删除失败', 'error') }
}

// ====== 海报管理 ======

function openPosterModal() {
  posterImageUrl.value = ''
  posterModal.value = true
}

function closePosterModal() { posterModal.value = false }

async function onPickPosterImg(e) {
  const file = e?.target?.files?.[0]
  if (!file) return
  posterUploading.value = true
  try {
    const url = await uploadImage(file)
    posterImageUrl.value = url
    push('图片上传成功')
  } catch (err) { push(err?.message || '上传失败', 'error') }
  finally { posterUploading.value = false; e.target.value = '' }
}

async function handleSavePoster() {
  if (!posterImageUrl.value) { push('请上传海报图片', 'warning'); return }
  try {
    await savePoster({ imageUrl: posterImageUrl.value })
    push('海报添加成功')
    closePosterModal()
    await loadPosters()
  } catch (e) { push(e?.message || '操作失败', 'error') }
}

async function handleDeletePoster(id) {
  if (!confirm('确定要删除这张海报吗？')) return
  try {
    await delPoster(id)
    posters.value = posters.value.filter(p => p.id !== id)
    push('删除成功')
  } catch (e) { push(e?.message || '删除失败', 'error') }
}

async function movePoster(id, direction) {
  const idx = posters.value.findIndex(p => p.id === id)
  if (idx === -1) return
  const target = idx + direction
  if (target < 0 || target >= posters.value.length) return
  // swap
  const a = posters.value[idx], b = posters.value[target]
  const { updatePosterSort } = await import('../utils/api')
  try {
    await updatePosterSort({ id: a.id, sortOrder: b.sortOrder })
    await updatePosterSort({ id: b.id, sortOrder: a.sortOrder })
    await loadPosters()
  } catch (e) { push(e?.message || '排序失败', 'error') }
}

// ====== 用户管理 ======

async function loadUsers() {
  usersLoading.value = true
  try {
    const data = await listUsers()
    users.value = data.users || []
  } catch (e) { push(e?.message || '加载用户失败', 'error') }
  finally { usersLoading.value = false }
}

function openTitleModal(user) {
  titleUserId.value = user.id
  titleName.value = user.titleName || ''
  titleStyle.value = user.titleStyle || 'default'
  titleModal.value = true
}

function closeTitleModal() { titleModal.value = false }

async function handleGrantTitle() {
  if (!titleName.value.trim()) { push('请输入称号名称', 'warning'); return }
  try {
    await grantTitle(titleUserId.value, titleName.value.trim(), titleStyle.value)
    push('称号授予成功')
    closeTitleModal()
    await loadUsers()
  } catch (e) { push(e?.message || '操作失败', 'error') }
}

function tabClass(t) {
  return ['tab-btn', { active: tab.value === t }]
}

onMounted(async () => { await refresh(); await loadPosters() })
</script>

<template>
  <main class="profile-main">
    <!-- 左侧 -->
    <aside class="profile-aside">
      <div class="glass-card profile-card">
        <label class="avatar-wrap">
          <img
            :src="toAbsoluteUrl(user?.avatarUrl || blogInfo?.avatarUrl || 'https://api.dicebear.com/7.x/avataaars/svg?seed=Felix')"
            :alt="blogInfo?.nickname || '博主头像'" class="avatar-img"
            :class="{ loaded: user?.avatarUrl || blogInfo?.avatarUrl }"
            @load="(e) => e.target.classList.add('loaded')"
          />
          <div class="avatar-overlay"><Camera :size="18" /></div>
          <input type="file" accept="image/*" @change="onPickAvatar" class="hidden-input" />
        </label>
        <h2 class="profile-name">{{ blogInfo?.nickname || '加载中...' }}</h2>
        <p class="profile-bio">{{ blogInfo?.bio || '...' }}</p>
        <div class="profile-stat"><span class="stat-num">{{ posts.length }}</span><span class="stat-label">篇文章</span></div>
      </div>

      <div class="glass-card settings-card">
        <div class="settings-head"><Settings :size="14" /><span>设置</span></div>
        <div class="setting-row" @click="openNicknameModal"><span>昵称</span><span class="setting-val">{{ blogInfo?.nickname || '未设置' }}</span></div>
        <div class="setting-row" @click="openNicknameModal"><span>简介</span><span class="setting-val">{{ blogInfo?.bio || '未设置' }}</span></div>
        <label class="setting-row"><span>头像</span><span class="setting-val link">点击头像上传</span><input type="file" accept="image/*" class="hidden-input" @change="onPickAvatar" /></label>
        <div class="setting-row"><span>背景图</span>
          <div class="bg-actions">
            <label class="bg-btn">上传<input type="file" accept="image/*" class="hidden-input" @change="onPickBg" /></label>
            <button class="bg-clear" @click="clearBg">清除</button>
          </div>
        </div>
      </div>
    </aside>

    <!-- 右侧 -->
    <section class="profile-feed">
      <div class="tab-bar">
        <button :class="tabClass('posts')" @click="tab = 'posts'">文章</button>
        <button :class="tabClass('posters')" @click="tab = 'posters'">海报</button>
        <button :class="tabClass('users')" @click="tab = 'users'; loadUsers()">用户</button>
      </div>

      <!-- 文章列表 -->
      <div v-if="tab === 'posts'">
        <div class="feed-head">
          <h2 class="feed-title">我的文章</h2>
          <span class="badge-count">{{ posts.length }} 篇</span>
        </div>
        <div v-if="allPosts.length === 0" class="empty-state">
          <div class="empty-icon"><Edit3 :size="32" /></div>
          <div class="empty-title">还没有文章</div>
          <button class="pill-btn pill-btn-primary" @click="goHome">回首页写文章</button>
        </div>
        <div v-else class="post-list">
          <article v-for="p in allPosts" :key="p.id" class="glass-card post-item" @click="openPost(p.id)">
            <div class="post-thumb">
              <img v-if="getFirstImageUrl(p.content)" :src="toAbsoluteUrl(getFirstImageUrl(p.content))" alt="" />
              <Image v-else :size="22" class="thumb-icon" />
            </div>
            <div class="post-body">
              <h3 class="post-title">{{ p.title }}</h3>
              <span class="post-date">{{ formatTime(p.createTime) }}</span>
              <p class="post-excerpt">{{ toPlainText(p.content).slice(0, 80) }}{{ toPlainText(p.content).length > 80 ? '...' : '' }}</p>
            </div>
            <div class="post-actions" @click.stop>
              <button class="act-btn edit" @click="editPost(p)" aria-label="编辑文章"><Edit3 :size="14" /></button>
              <button class="act-btn del" @click="remove(p.id)" aria-label="删除文章"><Trash2 :size="14" /></button>
            </div>
          </article>
        </div>
      </div>

      <!-- 海报管理 -->
      <div v-if="tab === 'posters'">
        <div class="feed-head">
          <h2 class="feed-title">海报管理</h2>
          <button class="pill-btn pill-btn-primary" @click="openPosterModal"><Plus :size="16" /> 添加海报</button>
        </div>
        <div v-if="posters.length === 0" class="empty-state">
          <div class="empty-icon"><Image :size="32" /></div>
          <div class="empty-title">还没有海报</div>
          <div class="empty-desc">添加海报将在首页轮播展示</div>
        </div>
        <div v-else class="poster-list">
          <div v-for="(p, i) in posters" :key="p.id" class="glass-card poster-item">
            <img :src="p.imageUrl" class="poster-thumb" />
            <div class="poster-info">
              <span class="poster-title">{{ p.title || '无标题' }}</span>
              <span class="poster-link" v-if="p.linkUrl">{{ p.linkUrl }}</span>
            </div>
            <div class="poster-sort">
              <button :disabled="i === 0" @click="movePoster(p.id, -1)" aria-label="上移海报"><ChevronUp :size="16" /></button>
              <button :disabled="i === posters.length - 1" @click="movePoster(p.id, 1)" aria-label="下移海报"><ChevronDown :size="16" /></button>
            </div>
            <div class="poster-actions">
              <button class="act-btn del" @click="handleDeletePoster(p.id)" aria-label="删除海报"><Trash2 :size="14" /></button>
            </div>
          </div>
        </div>
      </div>
    </section>

      <!-- 用户管理 -->
      <div v-if="tab === 'users'">
        <div class="feed-head">
          <h2 class="feed-title">用户管理</h2>
          <span class="badge-count">{{ users.length }} 人</span>
        </div>
        <div v-if="usersLoading" class="loading-state">加载中...</div>
        <div v-else-if="users.length === 0" class="empty-state">
          <div class="empty-title">暂无用户</div>
        </div>
        <div v-else class="user-list">
          <div v-for="u in users" :key="u.id" class="glass-card user-item">
            <img :src="u.avatarUrl || 'https://api.dicebear.com/7.x/avataaars/svg?seed=' + u.username" class="user-avatar" :alt="u.username" />
            <div class="user-body">
              <div class="user-head">
                <span class="user-name">{{ u.username }}</span>
                <span v-if="u.titleName" :class="['title-badge', 'title-' + (u.titleStyle || 'default')]">{{ u.titleName }}</span>
                <span v-if="u.role === 'ADMIN'" class="user-role-badge">管理员</span>
              </div>
              <div class="user-meta">
                <span class="user-email">{{ u.email || '未填邮箱' }}</span>
                <span class="user-ip" v-if="u.ipAddress">IP: {{ u.ipAddress }}</span>
                <span v-if="u.duplicateIp" class="dup-ip-warn" :title="'同IP账号: ' + (u.sameIpUsers || []).join(', ')">⚠ 同IP多账号</span>
              </div>
              <div class="user-time">注册于 {{ formatTime(u.createTime) }}</div>
            </div>
            <button class="pill-btn pill-btn-ghost user-title-btn" @click="openTitleModal(u)">
              {{ u.titleName ? '修改称号' : '授予称号' }}
            </button>
          </div>
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
            <input v-model="titleName" class="modal-input" placeholder="如：荣誉会员、星标访客..." @keyup.enter="handleGrantTitle" />
          </div>
          <div class="modal-field">
            <label class="field-label">称号样式</label>
            <div class="theme-grid">
              <button
                v-for="t in titleThemes" :key="t.value"
                :class="['theme-chip', { active: titleStyle === t.value }]"
                :style="{ '--chip-color': t.color }"
                @click="titleStyle = t.value"
              >
                <span class="chip-dot"></span>
                {{ t.label }}
              </button>
            </div>
          </div>
          <div class="modal-field" v-if="titleName">
            <label class="field-label">预览</label>
            <span :class="['title-badge', 'title-' + titleStyle]">{{ titleName }}</span>
          </div>
          <div class="modal-btns">
            <button class="pill-btn pill-btn-ghost" @click="closeTitleModal">取消</button>
            <button v-if="titleName" class="pill-btn pill-btn-ghost" style="color:var(--danger);border-color:var(--danger)" @click="titleName='';handleGrantTitle()">移除称号</button>
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
            <input v-model="newNickname" class="modal-input" placeholder="输入新昵称..." @keyup.enter="saveNickname" />
          </div>
          <div class="modal-field">
            <label class="field-label">简介</label>
            <input v-model="newBio" class="modal-input" placeholder="写一句话介绍自己..." @keyup.enter="saveNickname" />
          </div>
          <div class="modal-btns">
            <button class="pill-btn pill-btn-ghost" @click="closeNicknameModal">取消</button>
            <button class="pill-btn pill-btn-primary" @click="saveNickname">保存</button>
          </div>
        </div>
      </div>
    </Transition>

    <!-- 海报编辑弹窗 -->
    <Transition name="modal">
      <div v-if="posterModal" class="modal-mask" @click.self="closePosterModal">
        <div class="glass-card modal-card">
          <h3 class="modal-title">添加海报</h3>
          <div class="poster-upload-area" @click="posterInput?.click()">
            <img v-if="posterImageUrl" :src="posterImageUrl" class="poster-preview" />
            <div v-else class="poster-placeholder"><Plus :size="28" /><span>点击上传图片</span></div>
            <div v-if="posterUploading" class="poster-uploading">上传中...</div>
          </div>
          <input ref="posterInput" type="file" accept="image/*" class="hidden-input" @change="onPickPosterImg" />
          <div class="modal-btns">
            <button class="pill-btn pill-btn-ghost" @click="closePosterModal">取消</button>
            <button class="pill-btn pill-btn-primary" :disabled="!posterImageUrl" @click="handleSavePoster">保存</button>
          </div>
        </div>
      </div>
    </Transition>
  </main>
</template>

<style scoped>
.profile-main { display: flex; gap: 28px; max-width: 1140px; margin: 0 auto; width: 100%; }

.profile-aside { width: 260px; flex-shrink: 0; display: flex; flex-direction: column; gap: 16px; position: sticky; top: 88px; align-self: flex-start; }

.profile-card { padding: 24px 18px; text-align: center; }
.avatar-wrap { display: inline-block; position: relative; cursor: pointer; margin-bottom: 14px; }
.avatar-wrap:hover .avatar-overlay { opacity: 1; }
.avatar-wrap:hover .avatar-img { border-color: var(--red); }

.profile-name { margin: 0 0 4px; font-family: var(--font-display); font-size: 1.1rem; font-weight: 700; color: var(--red); }
.profile-bio { margin: 0 0 12px; font-size: 0.76rem; color: var(--ink-muted); }
.profile-stat { display: flex; flex-direction: column; align-items: center; }
.stat-num { font-family: var(--font-display); font-size: 1.4rem; color: var(--red); font-weight: 700; }
.stat-label { font-size: 0.65rem; color: var(--ink-muted); }

.settings-card { padding: 0; overflow: hidden; }
.settings-head { display: flex; align-items: center; gap: 8px; padding: 12px 16px; font-size: 0.82rem; font-weight: 600; color: var(--ink); border-bottom: 1px solid var(--rule); }
.setting-row { display: flex; align-items: center; justify-content: space-between; padding: 10px 16px; border-bottom: 1px solid var(--rule); font-size: 0.78rem; color: var(--ink-muted); cursor: pointer; transition: background var(--duration-fast); }
.setting-row:last-child { border-bottom: none; }
.setting-row:hover { background: var(--red-soft); }
.setting-val { font-size: 0.7rem; color: var(--ink-muted); }
.setting-val.link { color: var(--red); }
.bg-actions { display: flex; gap: 6px; }
.bg-btn:hover { background: var(--red); color: #fff; }
.bg-clear { font-size: 0.68rem; padding: 4px 8px; border: none; background: none; color: var(--ink-muted); cursor: pointer; }
.bg-clear:hover { color: var(--red); }
.hidden-input { display: none; }

/* ====== Feed ====== */
.profile-feed { flex: 1; min-width: 0; }
.tab-btn { padding: 8px 24px; border: none; background: transparent; color: var(--ink-muted); font-size: 0.82rem; font-family: var(--font-body); cursor: pointer; font-weight: 500; transition: background var(--duration-fast), color var(--duration-fast); }
.tab-btn.active { background: var(--red); color: #fff; font-weight: 600; }

.feed-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.feed-title { margin: 0; font-family: var(--font-display); font-size: 1.1rem; font-weight: 700; }


.post-list { display: flex; flex-direction: column; gap: 10px; }
.post-item { padding: 16px; display: flex; gap: 16px; align-items: center; cursor: pointer; transition: border-color var(--duration-normal) var(--ease-out), background var(--duration-normal) var(--ease-out); }
.post-item:hover { transform: none; }
.post-thumb img { width: 100%; height: 100%; object-fit: cover; }
.thumb-icon { color: var(--red); opacity: 0.4; }
.post-body { flex: 1; min-width: 0; }
.post-title { margin: 0 0 4px; font-family: var(--font-display); font-size: 0.9rem; font-weight: 700; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.post-date { font-size: 0.65rem; color: var(--ink-muted); }
.post-excerpt { margin: 4px 0 0; font-size: 0.76rem; color: var(--ink-muted); display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.post-actions { display: flex; gap: 6px; opacity: 0; transition: opacity var(--duration-fast); flex-shrink: 0; }
.post-item:hover .post-actions { opacity: 1; }
.act-btn { width: 30px; height: 30px; border: 1px solid var(--rule); display: flex; align-items: center; justify-content: center; cursor: pointer; background: transparent; transition: background var(--duration-fast), color var(--duration-fast), border-color var(--duration-fast); }
.act-btn.edit { color: var(--red); }
.act-btn.edit:hover { background: var(--red); color: #fff; }
.act-btn.del { color: var(--ink-muted); }
.act-btn.del:hover { background: var(--red); color: #fff; }

/* ====== 海报管理 ====== */
.poster-list { display: flex; flex-direction: column; gap: 8px; }
.poster-item { padding: 8px 12px; display: flex; gap: 12px; align-items: center; }
.poster-item:hover { transform: none; }
.poster-thumb { width: 56px; height: 36px; object-fit: cover; flex-shrink: 0; border: 1px solid var(--rule-thin); }
.poster-info { flex: 1; min-width: 0; }
.poster-title { font-weight: var(--fw-medium); font-size: 0.8rem; color: var(--ink); display: block; }
.poster-link { font-size: 0.65rem; color: var(--ink-muted); display: flex; align-items: center; gap: 4px; margin-top: 2px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.poster-sort { display: flex; gap: 4px; flex-shrink: 0; }
.poster-sort button { width: 22px; height: 22px; display: flex; align-items: center; justify-content: center; border: 1px solid var(--rule); background: transparent; color: var(--ink-muted); cursor: pointer; transition: color var(--duration-fast), border-color var(--duration-fast); }
.poster-sort button:hover:not(:disabled) { color: var(--red); border-color: var(--red); }
.poster-sort button:disabled { opacity: 0.2; cursor: default; }
.poster-actions { display: flex; gap: 4px; flex-shrink: 0; }
.poster-actions .act-btn.del { color: var(--ink-muted); }
.poster-actions .act-btn.del:hover { background: var(--red); color: #fff; }

.poster-preview { width: 100%; height: 100%; object-fit: cover; }
.poster-placeholder { display: flex; flex-direction: column; align-items: center; gap: 8px; color: var(--ink-muted); font-size: 0.82rem; }
.poster-uploading { font-size: 0.78rem; color: var(--red); font-weight: 500; }

/* ====== 弹窗 ====== */
.modal-mask { position: fixed; inset: 0; z-index: 9998; display: flex; align-items: center; justify-content: center; background: rgba(0,0,0,0.3); backdrop-filter: blur(8px); }
.modal-card { width: min(400px, calc(100vw - 32px)); padding: 24px; }
.modal-title { margin: 0 0 1rem; font-family: var(--font-display); font-size: 1.1rem; font-weight: 700; }
.modal-field { margin-bottom: 0.75rem; }
.modal-field .field-label { display: block; font-size: 0.72rem; font-weight: 600; color: var(--ink-muted); margin-bottom: 4px; }
.modal-input { width: 100%; box-sizing: border-box; padding: 8px 12px; border: 1px solid var(--rule); background: var(--paper); color: var(--ink); font-size: 0.88rem; outline: none; }
.modal-input:focus { border-color: var(--red); }
.modal-btns { display: flex; justify-content: flex-end; gap: 8px; }

.modal-enter-active { transition: opacity 0.25s var(--ease-out), transform 0.25s var(--ease-out); }
.modal-leave-active { transition: opacity 0.15s var(--ease-out), transform 0.15s var(--ease-out); }
.modal-enter-from, .modal-leave-to { opacity: 0; }

.empty-state { text-align: center; padding: 48px 0; color: var(--ink-muted); }
.empty-icon { margin-bottom: 12px; color: var(--ink-muted); }
.empty-title { font-family: var(--font-display); font-size: 1.05rem; font-weight: 700; color: var(--ink); margin-bottom: 6px; }
.empty-desc { font-size: 0.82rem; color: var(--ink-muted); margin-bottom: 16px; }

/* ====== 评论列表 ====== */
.comment-list { display: flex; flex-direction: column; gap: 10px; }
.comment-item { padding: 14px 16px; display: flex; gap: 12px; align-items: flex-start; }
.comment-item:hover { transform: none; }
.comment-body { flex: 1; min-width: 0; }
.comment-head { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; }
.comment-name { font-weight: 600; font-size: 0.85rem; color: var(--ink); }
.comment-time { font-size: 0.72rem; color: var(--ink-muted); margin-left: auto; }
.comment-text { margin: 0 0 6px; font-size: 0.88rem; color: var(--ink-muted); line-height: 1.55; }
.comment-post-id { font-size: 0.68rem; color: var(--ink-muted); }
.comment-item:hover .comment-del { opacity: 1; }
.comment-del:hover { color: var(--red); background: var(--red-soft); }

@keyframes spin { to { transform: rotate(360deg); } }
.loading-state { text-align: center; padding: 24px 0; }

/* ====== 用户管理 ====== */
.user-list { display: flex; flex-direction: column; gap: 8px; }
.user-item { padding: 14px 16px; display: flex; gap: 14px; align-items: center; }
.user-item:hover { transform: none; }
.user-avatar { width: 40px; height: 40px; border-radius: var(--radius-sm); object-fit: cover; flex-shrink: 0; }
.user-body { flex: 1; min-width: 0; }
.user-head { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; margin-bottom: 4px; }
.user-name { font-weight: 600; font-size: 0.88rem; color: var(--ink); }
.user-role-badge { font-size: 0.6rem; padding: 1px 8px; border-radius: var(--radius-sm); background: var(--primary-soft); color: var(--primary); font-weight: 600; }
.user-meta { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; margin-bottom: 2px; }
.user-email { font-size: 0.72rem; color: var(--ink-muted); }
.user-ip { font-size: 0.68rem; color: var(--ink-faint); font-family: var(--font-mono); }
.dup-ip-warn { font-size: 0.68rem; color: var(--amber); font-weight: 600; cursor: help; }
.user-time { font-size: 0.68rem; color: var(--ink-faint); }
.user-title-btn { flex-shrink: 0; font-size: 0.72rem; padding: 5px 12px; white-space: nowrap; }

/* ====== 称号主题选择器 ====== */
.theme-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 6px; }
.theme-chip {
  display: flex; align-items: center; gap: 5px;
  padding: 6px 10px; border: 1px solid var(--border); border-radius: var(--radius-sm);
  background: var(--surface); cursor: pointer;
  font-size: 0.7rem; color: var(--ink-muted);
  transition: border-color var(--duration-fast), background var(--duration-fast);
}
.theme-chip:hover { border-color: var(--ink-muted); }
.theme-chip.active { border-color: var(--chip-color, var(--primary)); background: var(--primary-soft); color: var(--ink); }
.chip-dot { width: 10px; height: 10px; border-radius: 50%; background: var(--chip-color, #888); flex-shrink: 0; }

@media (max-width: 900px) {
  .profile-main { flex-direction: column; }
  .profile-aside { width: 100%; position: static; }
  .theme-grid { grid-template-columns: repeat(2, 1fr); }
}
</style>
