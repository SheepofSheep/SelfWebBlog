<script setup>
import { ref, computed, onMounted, onBeforeUnmount, inject, nextTick, watch } from 'vue'
import { navigate } from '../router'
import { savePost, uploadImage, listTags, listCategories } from '../utils/api'
import { useToast } from '../composables/toast'
import { renderMarkdown } from '../utils/marked'
import { ArrowLeft, Eye, Edit3, Image, Bold, Italic, Code, Quote, List, Heading, Send, Loader, Settings2, Check, Upload, Link, Minus, ListChecks } from 'lucide-vue-next'

const { push } = useToast()
const refreshHome = inject('refreshHome', () => {})

// ── Form state ──
const title = ref('')
const content = ref('')
const summary = ref('')
const coverUrl = ref('')
const category = ref('')
const tags = ref('')
const loading = ref(false)
const uploading = ref(false)
const previewMode = ref(false)
const showSettings = ref(false)
const fileInput = ref(null)
const coverInput = ref(null)
const editingId = ref(null)
const postStatus = ref('PUBLISHED') // DRAFT | PUBLISHED

// ── Draft state ──
const draftStatus = ref('') // '' | 'saving' | 'saved'
let draftTimer = null
const LEGACY_DRAFT_KEY = 'postDraft'
const NEW_DRAFT_KEY = 'postDraft:new'

const isEditing = computed(() => !!editingId.value)
const canPublish = computed(() => title.value.trim() && content.value.trim())
const canSubmit = computed(() => postStatus.value === 'DRAFT' ? title.value.trim() && content.value.trim() : title.value.trim() && content.value.trim() && category.value.trim() && summary.value.trim())
const renderedContent = computed(() => renderMarkdown(content.value))
const tagList = computed(() => tags.value.split(/[,，\s]+/).filter(Boolean))
const draftKey = computed(() => getDraftKey(editingId.value))

// 标签建议
const allTags = ref([])
const tagSuggestions = computed(() => {
  const current = new Set(tagList.value)
  return allTags.value.filter(t => !current.has(t.name))
})

async function loadAllTags() {
  try { allTags.value = await listTags() } catch { allTags.value = [] }
}

const allCategories = ref([])
async function loadAllCategories() {
  try { allCategories.value = await listCategories() } catch { allCategories.value = [] }
}

function selectCategory(name) { category.value = name }

function addTag(name) {
  const current = tagList.value
  if (current.includes(name)) return
  tags.value = [...current, name].join(', ')
}

function removeTag(name) {
  tags.value = tagList.value.filter(t => t !== name).join(', ')
}

onMounted(() => {
  migrateLegacyDraft()
  const editing = sessionStorage.getItem('editingPost')
  if (editing) {
    try {
      const e = JSON.parse(editing)
      applyDraftData(e)
      sessionStorage.removeItem('editingPost')
      loadDraft()
    } catch {}
  } else {
    loadDraft()
  }
  loadAllTags()
  loadAllCategories()
  window.addEventListener('beforeunload', handleBeforeUnload)
})

onBeforeUnmount(() => {
  window.removeEventListener('beforeunload', handleBeforeUnload)
  clearTimeout(draftTimer)
})

function getDraftKey(id) {
  return id ? `postDraft:${id}` : NEW_DRAFT_KEY
}

function migrateLegacyDraft() {
  const legacy = localStorage.getItem(LEGACY_DRAFT_KEY)
  if (!legacy) return
  try {
    const d = JSON.parse(legacy)
    localStorage.setItem(getDraftKey(d.editingId || null), legacy)
  } catch {}
  localStorage.removeItem(LEGACY_DRAFT_KEY)
}

function getDraftData() {
  return {
    title: title.value, content: content.value, summary: summary.value,
    coverUrl: coverUrl.value, category: category.value, tags: tags.value,
    postStatus: postStatus.value,
    editingId: editingId.value, savedAt: new Date().toISOString()
  }
}

function saveDraft() {
  draftStatus.value = 'saving'
  localStorage.setItem(draftKey.value, JSON.stringify(getDraftData()))
  clearTimeout(draftTimer)
  draftTimer = setTimeout(() => { draftStatus.value = 'saved' }, 300)
}

function loadDraft() {
  const draft = localStorage.getItem(draftKey.value)
  if (!draft) return
  try {
    const d = JSON.parse(draft)
    applyDraftData(d)
  } catch {}
}

function applyDraftData(d) {
  editingId.value = d.id || d.editingId || null
  title.value = d.title || ''
  content.value = d.content || ''
  summary.value = d.summary || ''
  coverUrl.value = d.coverUrl || ''
  category.value = d.category || ''
  tags.value = Array.isArray(d.tags) ? d.tags.join(', ') : (d.tags || '')
  postStatus.value = d.postStatus || d.status || 'PUBLISHED'
}

function clearCurrentDraft() {
  localStorage.removeItem(draftKey.value)
  localStorage.removeItem(LEGACY_DRAFT_KEY)
}

// ── Image upload ──
function triggerUpload() { fileInput.value?.click() }
function triggerCoverUpload() { coverInput.value?.click() }

async function onPickImage(e) {
  const file = e.target.files?.[0]
  if (!file) return
  uploading.value = true
  try {
    const url = await uploadImage(file)
    insertMarkdown(`![图片](${url})`)
    push('图片上传成功')
  } catch (err) { push(err?.message || '上传失败', 'error') }
  finally { uploading.value = false; e.target.value = '' }
}

async function onPickCover(e) {
  const file = e.target.files?.[0]
  if (!file) return
  uploading.value = true
  try {
    const url = await uploadImage(file)
    coverUrl.value = url
    saveDraft()
    push('封面上传成功')
  } catch (err) { push(err?.message || '上传失败', 'error') }
  finally { uploading.value = false; e.target.value = '' }
}

// ── Markdown helpers ──
function insertMarkdown(prefix, suffix = '') {
  const ta = document.querySelector('.write-area')
  if (!ta) return
  const start = ta.selectionStart, end = ta.selectionEnd
  const selected = content.value.substring(start, end)
  const newText = prefix + (selected || '') + suffix
  content.value = content.value.slice(0, start) + newText + content.value.slice(end)
  nextTick(() => { ta.focus(); ta.setSelectionRange(start + newText.length, start + newText.length) })
  saveDraft()
}

function insertLink() { insertMarkdown('[链接文字](', 'https://)') }
function insertCodeBlock() { insertMarkdown('```\n', '\n```') }
function insertDivider() { insertMarkdown('\n---\n') }
function insertTodo() { insertMarkdown('- [ ] ') }

function hasDraftContent() {
  return Boolean(
    title.value.trim() ||
    content.value.trim() ||
    summary.value.trim() ||
    coverUrl.value.trim() ||
    category.value.trim() ||
    tags.value.trim()
  )
}

function handleBeforeUnload(e) {
  if (!hasDraftContent() || loading.value) return
  e.preventDefault()
  e.returnValue = ''
}

// ── Submit ──
function validateBeforePublish() {
  if (postStatus.value === 'DRAFT') return true
  if (!title.value.trim()) { push('请填写标题', 'warning'); return false }
  if (!content.value.trim()) { push('请填写内容', 'warning'); return false }
  if (!category.value.trim()) { push('建议填写分类再发布', 'warning'); return false }
  if (!summary.value.trim()) { push('建议填写摘要再发布', 'warning'); return false }
  return true
}

async function submit() {
  if (!canPublish.value || loading.value) return
  if (!validateBeforePublish()) return
  const t = title.value.trim(), c = content.value.trim()
  loading.value = true
  try {
    const body = { title: t, content: c, summary: summary.value.trim(), coverUrl: coverUrl.value.trim(), category: category.value.trim(), tags: tagList.value.join(','), status: postStatus.value }
    if (editingId.value) body.id = editingId.value
    await savePost(body)
    push(postStatus.value === 'DRAFT' ? '草稿已保存' : (isEditing.value ? '更新成功' : '发布成功'))
    clearCurrentDraft()
    refreshHome()
    navigate('/')
  } catch (err) { push(err?.message || '操作失败', 'error') }
  finally { loading.value = false }
}

function goBack() {
  saveDraft()
  if (hasDraftContent() && !confirm('本地草稿已保存，确定离开写作页吗？')) return
  navigate('/')
}

// Auto-save on content change
watch([title, content, summary, coverUrl, category, tags], () => { saveDraft() }, { deep: true })
</script>

<template>
  <div class="write-page">
    <!-- ═══ 顶部栏 ═══ -->
    <div class="write-topbar">
      <button class="back-link" @click="goBack">
        <ArrowLeft :size="18" /> 返回
      </button>

      <div class="topbar-center">
        <span v-if="draftStatus === 'saving'" class="draft-status saving">保存中...</span>
        <span v-else-if="draftStatus === 'saved'" class="draft-status saved"><Check :size="14" /> 已保存</span>
        <span v-if="isEditing" class="editing-badge">编辑模式</span>
      </div>

      <div class="topbar-actions">
        <span class="word-count">{{ content.length }} 字</span>
        <button class="tool-pill" :class="{ active: showSettings }" @click="showSettings = !showSettings" title="文章设置">
          <Settings2 :size="16" />
          设置
        </button>
        <button class="tool-pill" :class="{ active: previewMode }" @click="previewMode = !previewMode">
          <Eye v-if="!previewMode" :size="16" />
          <Edit3 v-else :size="16" />
          {{ previewMode ? '编辑' : '预览' }}
        </button>
        <button class="pill-btn pill-btn-primary" :disabled="!canPublish || loading" @click="submit">
          <Loader v-if="loading" :size="16" class="spin" />
          <Send v-else :size="16" />
          {{ loading ? '保存中...' : (postStatus === 'DRAFT' ? '保存草稿' : (isEditing ? '更新' : '发布')) }}
        </button>
      </div>
    </div>

    <!-- ═══ 主体区 ═══ -->
    <div class="write-body">
      <!-- 编辑区 -->
      <div class="write-main" :class="{ 'with-settings': showSettings }">
        <div class="write-glass glass-card">
        <template v-if="!previewMode">
          <input v-model="title" type="text" placeholder="文章标题..." class="title-input" />

          <div class="toolbar">
            <button class="tb-btn" title="标题" aria-label="插入标题" @click="insertMarkdown('## ')"><Heading :size="16" /></button>
            <button class="tb-btn" title="粗体" aria-label="插入粗体" @click="insertMarkdown('**', '**')"><Bold :size="16" /></button>
            <button class="tb-btn" title="斜体" aria-label="插入斜体" @click="insertMarkdown('*', '*')"><Italic :size="16" /></button>
            <button class="tb-btn" title="行内代码" aria-label="插入行内代码" @click="insertMarkdown('`', '`')"><Code :size="16" /></button>
            <button class="tb-btn" title="代码块" aria-label="插入代码块" @click="insertCodeBlock"><Code :size="16" /></button>
            <button class="tb-btn" title="链接" aria-label="插入链接" @click="insertLink"><Link :size="16" /></button>
            <button class="tb-btn" title="引用" aria-label="插入引用" @click="insertMarkdown('> ')"><Quote :size="16" /></button>
            <button class="tb-btn" title="无序列表" aria-label="插入无序列表" @click="insertMarkdown('- ')"><List :size="16" /></button>
            <button class="tb-btn" title="待办" aria-label="插入待办" @click="insertTodo"><ListChecks :size="16" /></button>
            <button class="tb-btn" title="分割线" aria-label="插入分割线" @click="insertDivider"><Minus :size="16" /></button>
            <span class="tb-divider"></span>
            <button class="tb-btn" title="插入图片" aria-label="插入图片" @click="triggerUpload">
              <Image :size="16" />
              <span v-if="uploading" class="uploading-dot"></span>
            </button>
            <input ref="fileInput" type="file" accept="image/*" class="hidden-input" @change="onPickImage" />
          </div>

          <textarea
            v-model="content"
            placeholder="开始写作...&#10;&#10;支持 Markdown 语法"
            class="write-area"
            @keydown.tab.prevent="insertMarkdown('  ')"
          ></textarea>
        </template>

        <!-- 预览 -->
        <template v-else>
          <h1 class="preview-title">{{ title || '未命名文章' }}</h1>
          <div class="preview-content" v-html="renderedContent"></div>
        </template>
        </div>
      </div>

      <!-- 设置侧边栏 -->
      <Transition name="settings-slide">
        <aside v-if="showSettings" class="write-settings">
          <h4 class="settings-head">文章设置</h4>

          <div class="settings-field">
            <label class="settings-label">摘要</label>
            <textarea v-model="summary" class="settings-textarea" placeholder="简要描述文章内容，会展示在文章列表..." rows="3" maxlength="200"></textarea>
            <span class="settings-hint">{{ summary.length }}/200</span>
          </div>

          <div class="settings-field">
            <label class="settings-label">封面图</label>
            <div class="cover-area" @click="triggerCoverUpload">
              <img v-if="coverUrl" :src="coverUrl" class="cover-preview" />
              <div v-else class="cover-placeholder">
                <Upload :size="20" />
                <span>点击上传封面</span>
              </div>
            </div>
            <input v-if="coverUrl" v-model="coverUrl" class="settings-input" placeholder="或粘贴图片URL..." />
            <input v-else v-model="coverUrl" class="settings-input" placeholder="或粘贴图片URL..." />
            <input ref="coverInput" type="file" accept="image/*" class="hidden-input" @change="onPickCover" />
          </div>

          <div class="settings-field">
            <label class="settings-label">分类</label>
            <input v-model="category" class="settings-input" placeholder="如：技术、随笔、日记..." />
            <div v-if="allCategories.length" class="tag-suggestions">
              <span class="suggest-label">已有分类：</span>
              <span
                v-for="c in allCategories.filter(c => c !== category)"
                :key="c"
                class="tag-chip suggest"
                @click="selectCategory(c)"
              >{{ c }}</span>
            </div>
          </div>

          <div class="settings-field">
            <label class="settings-label">标签</label>
            <input v-model="tags" class="settings-input" placeholder="输入标签，用逗号分隔..." @keyup.enter="tags += ', '" />
            <!-- 已选标签 -->
            <div v-if="tagList.length" class="tag-list">
              <span v-for="t in tagList" :key="t" class="tag-chip" @click="removeTag(t)">{{ t }} &times;</span>
            </div>
            <!-- 已有标签建议 -->
            <div v-if="tagSuggestions.length" class="tag-suggestions">
              <span class="suggest-label">复用已有：</span>
              <span v-for="t in tagSuggestions.slice(0, 8)" :key="t.id" class="tag-chip suggest" @click="addTag(t.name)">+ {{ t.name }}</span>
            </div>
          </div>

          <div class="settings-field">
            <label class="settings-label">状态</label>
            <div class="status-toggle">
              <button :class="['status-opt', { active: postStatus === 'PUBLISHED' }]" @click="postStatus = 'PUBLISHED'">发布</button>
              <button :class="['status-opt', { active: postStatus === 'DRAFT' }]" @click="postStatus = 'DRAFT'">草稿</button>
            </div>
          </div>
        </aside>
      </Transition>
    </div>
  </div>
</template>

<style scoped>
.write-page {
  max-width: 1060px;
  margin: 0 auto;
  width: 100%;
  min-height: 80vh;
  display: flex;
  flex-direction: column;
}

/* ═══ 顶栏 ═══ */
.write-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 20px;
  margin-bottom: 16px;
  border-radius: var(--radius-lg);
  background: rgba(255,255,255,0.85);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255,255,255,0.5);
  box-shadow: var(--shadow-soft);
  gap: 12px;
  flex-wrap: wrap;
}
[data-theme='dark'] .write-topbar {
  background: rgba(30,28,30,0.80);
  border-color: rgba(255,255,255,0.08);
}

.back-link {
  display: inline-flex; align-items: center; gap: 6px;
  background: none; border: none;
  color: var(--text-secondary); font-size: 0.88rem; font-weight: 500;
  cursor: pointer; padding: 6px 0;
  font-family: var(--font-body);
  transition: color var(--duration-fast);
  flex-shrink: 0;
}
.back-link:hover { color: var(--primary-hover); }

.topbar-center {
  display: flex; align-items: center; gap: 8px;
}

.draft-status {
  font-size: 0.72rem; font-weight: 500;
  display: flex; align-items: center; gap: 4px;
  padding: 3px 8px; border-radius: var(--radius-pill);
  white-space: nowrap;
}
.draft-status.saving { color: var(--amber); background: rgba(212, 154, 90, 0.1); }
.draft-status.saved { color: var(--success); background: rgba(109, 168, 138, 0.1); }

.editing-badge {
  font-size: 0.7rem; color: var(--primary-hover);
  background: var(--primary-soft); padding: 3px 10px;
  font-weight: 600; border-radius: var(--radius-pill);
}

.topbar-actions {
  display: flex; align-items: center; gap: 10px;
}

.word-count {
  font-size: 0.82rem; color: var(--text-secondary); font-weight: 500;
  min-width: 50px; text-align: right; white-space: nowrap;
  background: var(--surface-muted); padding: 4px 12px; border-radius: var(--radius-pill);
}

.tool-pill {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 8px 16px;
  border: 1px solid var(--border); border-radius: var(--radius-pill);
  background: var(--surface-muted); color: var(--text-secondary);
  font-size: 0.82rem; font-family: var(--font-body); font-weight: 500;
  cursor: pointer;
  transition: border-color var(--duration-fast), color var(--duration-fast), background var(--duration-fast);
}
.tool-pill:hover { border-color: var(--primary); color: var(--primary-hover); background: var(--primary-soft); }
.tool-pill.active { background: var(--primary); border-color: var(--primary); color: var(--on-primary); font-weight: 600; }

/* ═══ 主体 ═══ */
.write-body {
  flex: 1; display: flex; gap: 28px;
}

.write-main { flex: 1; min-width: 0; }
.write-main.with-settings { flex: 1; }

.write-glass {
  padding: 24px;
  min-height: 500px;
}
.write-glass:hover { transform: none; }

/* ═══ 标题输入 ═══ */
.title-input {
  width: 100%; box-sizing: border-box;
  padding: 12px 0; border: none; outline: none;
  font-family: var(--font-display); font-size: 1.8rem; font-weight: 700;
  color: var(--text); background: transparent; margin-bottom: 8px;
}
.title-input::placeholder { color: var(--text-muted); opacity: 0.5; }

/* ═══ 工具栏 ═══ */
.toolbar {
  display: flex; align-items: center; gap: 2px;
  padding: 8px 0; margin-bottom: 12px;
  border-bottom: 1px solid var(--border);
  flex-wrap: wrap;
}

.tb-btn {
  display: inline-flex; align-items: center; gap: 4px;
  padding: 7px 10px; border: none; border-radius: var(--radius-sm);
  background: transparent; color: var(--text-muted);
  cursor: pointer; font-size: 0.8rem; font-family: var(--font-body);
  transition: color var(--duration-fast), background var(--duration-fast);
}
.tb-btn:hover { color: var(--primary-hover); background: var(--primary-soft); }

.tb-divider { width: 1px; height: 20px; background: var(--border); margin: 0 6px; }

.uploading-dot { width: 6px; height: 6px; background: var(--primary); border-radius: 50%; animation: pulse 1s ease-in-out infinite; }
@keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.3; } }

.hidden-input { display: none; }

/* ═══ 编辑器 ═══ */
.write-area {
  flex: 1; width: 100%; box-sizing: border-box;
  min-height: 400px; border: none; outline: none;
  resize: vertical; padding: 8px 0;
  font-family: var(--font-body); font-size: 1rem;
  line-height: 1.85; color: var(--text); background: transparent;
}
.write-area::placeholder { color: var(--text-muted); opacity: 0.4; }

/* ═══ 设置侧边栏 ═══ */
.write-settings {
  width: 260px; flex-shrink: 0;
  padding: 20px;
  border-radius: var(--radius-lg);
  background: rgba(255,255,255,0.85);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255,255,255,0.5);
  box-shadow: var(--shadow-soft);
  display: flex; flex-direction: column; gap: 16px;
  overflow-y: auto;
}
[data-theme='dark'] .write-settings {
  background: rgba(30,28,30,0.80);
  border-color: rgba(255,255,255,0.08);
}

.settings-head {
  margin: 0; font-size: 0.9rem; font-weight: 700;
  color: var(--text); letter-spacing: 0.02em;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border-light);
}

.settings-field { display: flex; flex-direction: column; gap: 6px; }

.settings-label {
  font-size: 0.72rem; font-weight: 600;
  color: var(--text-secondary); text-transform: uppercase; letter-spacing: 0.04em;
}

.settings-input {
  width: 100%; box-sizing: border-box;
  padding: 8px 10px;
  border: 1px solid var(--border); border-radius: var(--radius-sm);
  background: rgba(255,255,255,0.65); color: var(--text);
  font-size: 0.8rem; font-family: var(--font-body); outline: none;
  transition: border-color var(--duration-fast);
}
[data-theme='dark'] .settings-input { background: rgba(40,36,40,0.60); }
.settings-input:focus { border-color: var(--primary); }
.settings-input::placeholder { color: var(--text-faint); }

.settings-textarea {
  width: 100%; box-sizing: border-box;
  padding: 8px 10px; resize: vertical;
  border: 1px solid var(--border); border-radius: var(--radius-sm);
  background: rgba(255,255,255,0.65); color: var(--text);
  font-size: 0.8rem; font-family: var(--font-body); outline: none;
  line-height: 1.6;
  transition: border-color var(--duration-fast);
}
[data-theme='dark'] .settings-textarea { background: rgba(40,36,40,0.60); }
.settings-textarea:focus { border-color: var(--primary); }
.settings-textarea::placeholder { color: var(--text-faint); }

.settings-hint { font-size: 0.65rem; color: var(--text-faint); text-align: right; }

/* ═══ 封面 ═══ */
.cover-area {
  border: 1px dashed var(--border); border-radius: var(--radius-sm);
  overflow: hidden; cursor: pointer; min-height: 100px;
  display: flex; align-items: center; justify-content: center;
  transition: border-color var(--duration-fast);
}
.cover-area:hover { border-color: var(--primary); }

.cover-preview { width: 100%; display: block; object-fit: cover; }

.cover-placeholder {
  display: flex; flex-direction: column; align-items: center; gap: 8px;
  color: var(--text-muted); font-size: 0.75rem; padding: 24px;
}

/* ═══ 状态切换 ═══ */
.status-toggle { display: flex; gap: 0; border-radius: var(--radius-pill); overflow: hidden; border: 1px solid var(--border); }
.status-opt {
  flex: 1; padding: 8px 0; border: none; background: transparent;
  font-size: 0.78rem; font-family: var(--font-body); cursor: pointer;
  color: var(--text-muted); transition: background var(--duration-fast), color var(--duration-fast);
}
.status-opt.active { background: var(--primary); color: var(--on-primary); }

/* ═══ 标签 ═══ */
.tag-list { display: flex; flex-wrap: wrap; gap: 6px; }
.tag-chip {
  display: inline-flex; align-items: center; gap: 2px;
  padding: 3px 10px; font-size: 0.7rem; font-weight: 500;
  border-radius: var(--radius-pill); cursor: pointer;
  background: var(--primary-soft); color: var(--primary-hover);
  border: 1px solid rgba(244, 164, 184, 0.3);
  transition: background var(--duration-fast);
}
.tag-chip:hover { background: rgba(244, 164, 184, 0.25); }
.tag-chip.suggest {
  background: var(--surface-muted); color: var(--text-muted);
  border: 1px dashed var(--border);
}
.tag-chip.suggest:hover { border-color: var(--primary); color: var(--primary-hover); }

.tag-suggestions {
  display: flex; flex-wrap: wrap; align-items: center; gap: 4px;
  margin-top: 8px;
}
.suggest-label {
  font-size: 0.65rem; color: var(--text-faint); margin-right: 2px;
}

/* ═══ 预览 ═══ */
.preview-title {
  font-family: var(--font-display); font-size: 1.8rem; font-weight: 700;
  color: var(--text); margin: 0 0 24px;
  padding-bottom: 16px; border-bottom: 1px solid var(--border);
}
.preview-content { font-size: 1rem; line-height: 1.85; color: var(--text); }
.preview-content h2 { font-family: var(--font-display); font-size: 1.3rem; margin: 2rem 0 1rem; }
.preview-content p { margin-bottom: 1rem; }
.preview-content blockquote {
  border-left: 3px solid var(--primary); margin: 1.5rem 0; padding: 0.8rem 1.2rem;
  background: var(--primary-soft); color: var(--text-muted);
}
.preview-content pre {
  background: var(--surface-muted); padding: 16px;
  overflow-x: auto; font-size: 0.88rem; border-radius: var(--radius-md);
}
.preview-content code { font-family: 'SF Mono', 'Fira Code', monospace; font-size: 0.88em; }

.spin { animation: spin 0.8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

/* ═══ Settings panel transition ═══ */
.settings-slide-enter-active { transition: opacity 0.3s var(--ease-out), transform 0.3s var(--ease-out), width 0.3s var(--ease-out); }
.settings-slide-leave-active { transition: opacity 0.2s var(--ease-out), transform 0.2s var(--ease-out), width 0.2s var(--ease-out); }
.settings-slide-enter-from { opacity: 0; transform: translateX(20px); }
.settings-slide-leave-to { opacity: 0; transform: translateX(20px); }

/* ═══ 响应式 ═══ */
@media (max-width: 860px) {
  .write-body { flex-direction: column; }
  .write-settings {
    width: 100%; border: 1px solid var(--border);
    padding: 16px; border-radius: var(--radius-lg);
  }
}

@media (max-width: 640px) {
  .title-input { font-size: 1.4rem; }
  .topbar-actions { gap: 6px; }
  .tool-pill { padding: 6px 12px; font-size: 0.76rem; }
  .write-topbar { gap: 8px; }
}
</style>
