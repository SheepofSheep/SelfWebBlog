<script setup>
import { ref, computed, onMounted, inject, nextTick } from 'vue'
import { navigate } from '../router'
import { savePost, uploadImage } from '../utils/api'
import { useToast } from '../composables/toast'
import { renderMarkdown } from '../utils/marked'
import { ArrowLeft, Eye, Edit3, Image, Bold, Italic, Code, Quote, List, Heading, Send, Loader } from 'lucide-vue-next'

const { push } = useToast()
const refreshHome = inject('refreshHome', () => {})

const title = ref('')
const content = ref('')
const loading = ref(false)
const uploading = ref(false)
const previewMode = ref(false)
const fileInput = ref(null)
const editingId = ref(null)

const isEditing = computed(() => !!editingId.value)
const canPublish = computed(() => title.value.trim() && content.value.trim())
const renderedContent = computed(() => renderMarkdown(content.value))

onMounted(() => {
  // 优先加载编辑中的文章
  const editing = sessionStorage.getItem('editingPost')
  if (editing) {
    try {
      const e = JSON.parse(editing)
      editingId.value = e.id || null
      title.value = e.title || ''
      content.value = e.content || ''
      sessionStorage.removeItem('editingPost')
      return
    } catch {}
  }
  // 否则加载草稿
  const draft = localStorage.getItem('postDraft')
  if (draft) {
    try {
      const d = JSON.parse(draft)
      title.value = d.title || ''
      content.value = d.content || ''
      editingId.value = d.editingId || null
    } catch {}
  }
})

function saveDraft() {
  localStorage.setItem('postDraft', JSON.stringify({
    title: title.value, content: content.value,
    editingId: editingId.value, savedAt: new Date().toISOString()
  }))
}

function triggerUpload() { fileInput.value?.click() }

async function onPickImage(e) {
  const file = e.target.files?.[0]
  if (!file) return
  uploading.value = true
  try {
    const url = await uploadImage(file)
    const fullUrl = url.startsWith('/') ? `${import.meta.env.VITE_API_ORIGIN}${url}` : url
    insertMarkdown(`![图片](${fullUrl})`)
    push('图片上传成功')
  } catch (err) { push(err?.message || '上传失败', 'error') }
  finally { uploading.value = false; e.target.value = '' }
}

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

async function submit() {
  if (!canPublish.value || loading.value) return
  const t = title.value.trim(), c = content.value.trim()
  if (!t || !c) { push('请填写标题和内容', 'warning'); return }
  loading.value = true
  try {
    const body = { title: t, content: c }
    if (editingId.value) body.id = editingId.value
    await savePost(body)
    push(isEditing.value ? '更新成功' : '发布成功')
    localStorage.removeItem('postDraft')
    refreshHome()
    navigate('/')
  } catch (err) { push(err?.message || '操作失败', 'error') }
  finally { loading.value = false }
}

function goBack() { navigate('/') }
</script>

<template>
  <div class="write-page">
    <!-- 顶部栏 -->
    <div class="write-topbar">
      <button class="back-link" @click="goBack">
        <ArrowLeft :size="18" /> 返回
      </button>
      <div class="topbar-actions">
        <span v-if="isEditing" class="editing-badge">编辑模式</span>
        <span class="word-count">{{ content.length }} 字</span>
        <button class="tool-pill" :class="{ active: previewMode }" @click="previewMode = !previewMode">
          <Eye v-if="!previewMode" :size="16" />
          <Edit3 v-else :size="16" />
          {{ previewMode ? '编辑' : '预览' }}
        </button>
        <button class="pill-btn pill-btn-primary" :disabled="!canPublish || loading" @click="submit">
          <Loader v-if="loading" :size="16" class="spin" />
          <Send v-else :size="16" />
          {{ loading ? '保存中...' : (isEditing ? '更新' : '发布') }}
        </button>
      </div>
    </div>

    <!-- 编辑区 / 预览区 -->
    <div class="write-body">
      <template v-if="!previewMode">
        <input v-model="title" type="text" placeholder="文章标题..." class="title-input" @input="saveDraft" />
        <div class="toolbar">
          <button class="tb-btn" title="标题" aria-label="插入标题" @click="insertMarkdown('## ')"><Heading :size="16" /></button>
          <button class="tb-btn" title="粗体" aria-label="插入粗体" @click="insertMarkdown('**', '**')"><Bold :size="16" /></button>
          <button class="tb-btn" title="斜体" aria-label="插入斜体" @click="insertMarkdown('*', '*')"><Italic :size="16" /></button>
          <button class="tb-btn" title="行内代码" aria-label="插入行内代码" @click="insertMarkdown('`', '`')"><Code :size="16" /></button>
          <button class="tb-btn" title="引用" aria-label="插入引用" @click="insertMarkdown('> ')"><Quote :size="16" /></button>
          <button class="tb-btn" title="无序列表" aria-label="插入无序列表" @click="insertMarkdown('- ')"><List :size="16" /></button>
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
          @input="saveDraft"
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
</template>

<style scoped>
.write-page {
  max-width: 900px;
  margin: 0 auto;
  width: 100%;
  min-height: 80vh;
  display: flex;
  flex-direction: column;
}

/* ====== 顶栏 ====== */
.write-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 16px;
  margin-bottom: 16px;
  border-bottom: 1px solid var(--rule);
}

.back-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: none;
  border: none;
  color: var(--ink-muted);
  font-size: 0.88rem;
  cursor: pointer;
  padding: 6px 0;
  font-family: var(--font-body);
  transition: color var(--duration-fast);
}
.back-link:hover { color: var(--red); }

.topbar-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.editing-badge {
  font-size: 0.7rem;
  color: var(--red);
  background: var(--red-soft);
  padding: 3px 10px;
  font-weight: 600;
}

.word-count {
  font-size: 0.78rem;
  color: var(--ink-muted);
  min-width: 50px;
  text-align: right;
}

.tool-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border: 1px solid var(--rule);
  background: transparent;
  color: var(--ink-muted);
  font-size: 0.82rem;
  font-family: var(--font-body);
  cursor: pointer;
  transition: border-color var(--duration-fast), color var(--duration-fast), background var(--duration-fast);
}
.tool-pill:hover { border-color: var(--red); color: var(--red); }
.tool-pill.active { background: var(--red-soft); border-color: var(--red); color: var(--red); }

/* ====== 标题输入 ====== */
.title-input {
  width: 100%;
  box-sizing: border-box;
  padding: 12px 0;
  border: none;
  outline: none;
  font-family: var(--font-display);
  font-size: 2rem;
  font-weight: 700;
  color: var(--ink);
  background: transparent;
  margin-bottom: 12px;
}
.title-input::placeholder { color: var(--ink-muted); opacity: 0.5; }

/* ====== 工具栏 ====== */
.toolbar {
  display: flex;
  align-items: center;
  gap: 2px;
  padding: 8px 0;
  margin-bottom: 12px;
  border-bottom: 1px solid var(--rule);
  flex-wrap: wrap;
}

.tb-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 7px 10px;
  border: none;
  background: transparent;
  color: var(--ink-muted);
  cursor: pointer;
  font-size: 0.8rem;
  font-family: var(--font-body);
  transition: color var(--duration-fast), background var(--duration-fast);
}
.tb-btn:hover { color: var(--red); background: var(--red-soft); }

.tb-divider { width: 1px; height: 20px; background: var(--rule); margin: 0 6px; }

.uploading-dot {
  width: 6px; height: 6px;
  background: var(--red);
  animation: pulse 1s ease-in-out infinite;
}
@keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.3; } }

.hidden-input { display: none; }

/* ====== 写作区 ====== */
.write-area {
  flex: 1;
  width: 100%;
  box-sizing: border-box;
  min-height: 400px;
  border: none;
  outline: none;
  resize: vertical;
  padding: 8px 0;
  font-family: var(--font-body);
  font-size: 1rem;
  line-height: 1.85;
  color: var(--ink);
  background: transparent;
}
.write-area::placeholder { color: var(--ink-muted); opacity: 0.4; }

/* ====== 预览区 ====== */
.preview-title {
  font-family: var(--font-display);
  font-size: 2rem;
  font-weight: 700;
  color: var(--ink);
  margin: 0 0 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--rule);
}

.preview-content {
  font-size: 1rem;
  line-height: 1.85;
  color: var(--ink);
}
.preview-content h2 { font-family: var(--font-display); font-size: 1.3rem; margin: 2rem 0 1rem; }
.preview-content p { margin-bottom: 1rem; }
.preview-content blockquote {
  border-left: 3px solid var(--red);
  margin: 1.5rem 0;
  padding: 0.8rem 1.2rem;
  background: var(--red-soft);
  color: var(--ink-muted);
}
.preview-content pre {
  background: rgba(0,0,0,0.04);
  padding: 16px;
  overflow-x: auto;
  font-size: 0.88rem;
}
.preview-content code { font-family: 'SF Mono', 'Fira Code', monospace; font-size: 0.88em; }

.spin { animation: spin 0.8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

/* ====== 响应式 ====== */
@media (max-width: 640px) {
  .title-input { font-size: 1.4rem; }
  .topbar-actions { gap: 8px; }
  .tool-pill { padding: 6px 12px; font-size: 0.78rem; }
}
</style>
