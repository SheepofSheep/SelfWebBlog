<script setup>
import { computed, ref, watch, onMounted, nextTick } from 'vue'
import { savePost, uploadImage } from '../utils/api'
import { useToast } from '../composables/toast'
import { Image, Bold, Italic, Code, Quote, List, Heading, X, Check } from 'lucide-vue-next'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  initial: { type: Object, default: null }
})
const emit = defineEmits(['update:modelValue', 'success'])
const { push } = useToast()

const open = computed(() => props.modelValue)
const title = ref('')
const content = ref('')
const images = ref([])
const loading = ref(false)
const uploading = ref(false)
const editingId = ref(null)
const fileInput = ref(null)
const draftStatus = ref('')
let draftTimer = null

const canPublish = computed(() => title.value.trim() && content.value.trim())

watch(() => props.initial, (val) => {
  editingId.value = val?.id ?? null
  title.value = val?.title ?? ''
  content.value = val?.content ?? ''
}, { immediate: true })

watch(() => props.modelValue, (v) => { if (!v) { images.value = []; saveDraft() } })

function close() { saveDraft(); emit('update:modelValue', false) }

function triggerImageUpload() { fileInput.value?.click() }

async function onPickImage(e) {
  const files = Array.from(e.target.files || [])
  if (files.length === 0) return
  await uploadFiles(files)
  e.target.value = ''
}

async function uploadFiles(files) {
  const remaining = 9 - images.value.length
  const toProcess = files.slice(0, remaining)
  for (const file of toProcess) {
    if (!file.type.startsWith('image/')) continue
    uploading.value = true
    try {
      const url = await uploadImage(file)
      const fullUrl = url.startsWith('/')
        ? `${import.meta.env.VITE_API_ORIGIN}${url}`
        : url
      insertAtCursor(`![图片](${fullUrl})`)
      images.value.push(fullUrl)
      push('图片上传成功')
      saveDraft()
    } catch (err) {
      push(err?.message || '上传失败', 'error')
    } finally { uploading.value = false }
  }
}

function insertAtCursor(text) {
  const textarea = document.querySelector('.write-textarea')
  if (!textarea) return
  const start = textarea.selectionStart, end = textarea.selectionEnd
  content.value = content.value.slice(0, start) + text + content.value.slice(end)
  nextTick(() => { textarea.focus(); textarea.setSelectionRange(start + text.length, start + text.length) })
  saveDraft()
}

function insertMarkdown(prefix, suffix = '') {
  const textarea = document.querySelector('.write-textarea')
  if (!textarea) return
  const start = textarea.selectionStart, end = textarea.selectionEnd
  const selected = content.value.substring(start, end)
  const newText = prefix + (selected || '') + suffix
  content.value = content.value.slice(0, start) + newText + content.value.slice(end)
  nextTick(() => { textarea.focus(); textarea.setSelectionRange(start + newText.length, start + newText.length) })
  saveDraft()
}

function saveDraft() {
  draftStatus.value = 'saving'
  localStorage.setItem('postDraft', JSON.stringify({
    title: title.value, content: content.value, images: images.value,
    editingId: editingId.value, savedAt: new Date().toISOString()
  }))
  clearTimeout(draftTimer)
  draftTimer = setTimeout(() => { draftStatus.value = 'saved' }, 300)
}

function loadDraft() {
  const raw = localStorage.getItem('postDraft')
  if (!raw) return
  try {
    const d = JSON.parse(raw)
    title.value = d.title || ''
    content.value = d.content || ''
    images.value = d.images || []
    editingId.value = d.editingId || null
  } catch {}
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
    push(editingId.value ? '修改成功' : '发布成功')
    editingId.value = null
    title.value = ''; content.value = ''; images.value = []
    localStorage.removeItem('postDraft')
    emit('success'); close()
  } catch (err) { push(err?.message || '操作失败', 'error') }
  finally { loading.value = false }
}

watch([title, content], saveDraft, { deep: true })

onMounted(() => {
  loadDraft()
  nextTick(() => {
    const ta = document.querySelector('.write-textarea')
    if (ta) { ta.style.height = 'auto'; ta.style.height = Math.min(ta.scrollHeight, 380) + 'px' }
  })
})
</script>

<template>
  <Transition name="modal">
    <div v-if="open" class="write-overlay" @click="close">
      <div class="write-panel glass-card" @click.stop>
        <div class="write-head">
          <h3 class="write-title">{{ editingId ? '编辑文章' : '写新文章' }}</h3>
          <div class="write-head-right">
            <span v-if="draftStatus === 'saving'" class="draft-indicator saving">保存中...</span>
            <span v-else-if="draftStatus === 'saved'" class="draft-indicator saved"><Check :size="14" /> 已保存</span>
            <button class="close-btn" @click="close" aria-label="关闭"><X :size="18" /></button>
          </div>
        </div>

        <input v-model="title" type="text" placeholder="标题..." class="write-input" autocomplete="off" />

        <div class="write-editor">
          <textarea
            v-model="content" ref="contentRef"
            placeholder="写下你的想法，支持 Markdown 语法"
            class="write-textarea"
            maxlength="2000"
            @input="(e) => { e.target.style.height = 'auto'; e.target.style.height = Math.min(e.target.scrollHeight, 380) + 'px' }"
          ></textarea>
          <div class="write-counter">{{ content.length }}/2000</div>
        </div>

        <div class="write-toolbar">
          <button class="tool-btn" title="图片" aria-label="插入图片" @click="triggerImageUpload"><Image :size="16" /></button>
          <span class="tool-divider"></span>
          <button class="tool-btn" title="标题" aria-label="插入标题" @click="insertMarkdown('## ')"><Heading :size="16" /></button>
          <button class="tool-btn" title="粗体" aria-label="插入粗体" @click="insertMarkdown('**', '**')"><Bold :size="16" /></button>
          <button class="tool-btn" title="斜体" aria-label="插入斜体" @click="insertMarkdown('*', '*')"><Italic :size="16" /></button>
          <button class="tool-btn" title="代码" aria-label="插入代码" @click="insertMarkdown('`', '`')"><Code :size="16" /></button>
          <button class="tool-btn" title="引用" aria-label="插入引用" @click="insertMarkdown('> ')"><Quote :size="16" /></button>
          <button class="tool-btn" title="列表" aria-label="插入列表" @click="insertMarkdown('- ')"><List :size="16" /></button>
        </div>

        <input ref="fileInput" type="file" accept="image/*" multiple class="hidden-input" @change="onPickImage" />

        <div v-if="images.length" class="write-previews">
          <div v-for="(img, i) in images" :key="i" class="preview-item">
            <img :src="img" alt="preview" />
            <button class="preview-remove" @click="images.splice(i, 1)">&times;</button>
          </div>
        </div>

        <div v-if="uploading" class="write-uploading">上传中...</div>

        <div class="write-actions">
          <button class="pill-btn pill-btn-ghost" @click="close">取消</button>
          <button class="pill-btn pill-btn-primary" :disabled="!canPublish || loading" @click="submit">
            {{ loading ? '发布中...' : '发布' }}
          </button>
        </div>
      </div>
    </div>
  </Transition>
</template>

<style scoped>
.write-overlay {
  position: fixed; inset: 0; z-index: 9998;
  display: flex; align-items: center; justify-content: center;
  background: rgba(80, 60, 60, 0.18);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  padding: 24px;
}

.write-panel {
  width: min(680px, calc(100vw - 32px));
  max-height: 90vh;
  overflow-y: auto;
  padding: 28px;
  display: flex; flex-direction: column; gap: 16px;
  background: var(--canvas) !important;
}

.write-head { display: flex; justify-content: space-between; align-items: center; }
.write-head-right { display: flex; align-items: center; gap: 10px; }
.write-title {
  margin: 0;
  font-family: var(--font-display);
  font-size: 1.15rem; font-weight: 700;
  background: linear-gradient(135deg, var(--primary), var(--secondary-light));
  background-clip: text;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.draft-indicator {
  font-size: 0.7rem; font-weight: 500;
  display: flex; align-items: center; gap: 4px;
  padding: 2px 8px; border-radius: var(--radius-pill);
  white-space: nowrap;
}
.draft-indicator.saving { color: var(--amber); background: rgba(212, 154, 90, 0.1); }
.draft-indicator.saved { color: var(--success); background: rgba(109, 168, 138, 0.1); }
.close-btn {
  background: none; border: none; color: var(--ink-muted);
  cursor: pointer; padding: 4px; border-radius: 8px;
}
.close-btn:hover { background: var(--primary-soft); color: var(--primary); }

.write-input {
  width: 100%; box-sizing: border-box;
  font-size: 1.05rem; font-weight: 600;
  font-family: var(--font-body);
  border: none; outline: none;
  padding: 12px 0;
  background: transparent;
  color: var(--ink);
  border-bottom: 2px solid var(--border-light);
  transition: border-color var(--duration-fast);
}
.write-input::placeholder { color: var(--ink-faint); }
.write-input:focus { border-bottom-color: var(--primary); }

.write-editor { position: relative; }
.write-textarea {
  width: 100%; box-sizing: border-box;
  min-height: 140px; max-height: 380px;
  resize: none;
  border: none; outline: none;
  padding: 12px 0;
  font-family: var(--font-body);
  font-size: 0.9rem;
  line-height: 1.7;
  background: transparent;
  color: var(--ink);
  border-bottom: 2px solid var(--border-light);
  transition: border-color var(--duration-fast);
}
.write-textarea::placeholder { color: var(--ink-faint); }
.write-textarea:focus { border-bottom-color: var(--primary); }
.write-counter {
  position: absolute; bottom: -22px; right: 0;
  font-size: 0.7rem; color: var(--ink-faint);
}

.write-toolbar {
  display: flex; align-items: center; gap: 2px;
  padding: 8px 0;
  border-bottom: 1px solid var(--border-light);
  flex-wrap: wrap;
}
.tool-btn {
  display: flex; align-items: center; justify-content: center;
  width: 34px; height: 34px;
  border: none; border-radius: 8px;
  background: transparent; color: var(--ink-muted);
  cursor: pointer;
  transition: color var(--duration-fast), background var(--duration-fast);
}
.tool-btn:hover { color: var(--primary); background: var(--primary-soft); }
.tool-divider { width: 1px; height: 20px; background: var(--border-light); margin: 0 4px; }

.hidden-input { display: none; }

.write-previews { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; }
.preview-item {
  position: relative; aspect-ratio: 1;
  border-radius: var(--radius-sm); overflow: hidden;
  border: 1px solid var(--border-light);
}
.preview-item img { width: 100%; height: 100%; object-fit: cover; }
.preview-remove {
  position: absolute; top: 4px; right: 4px; width: 20px; height: 20px;
  border: none; border-radius: 50%;
  background: rgba(80, 55, 55, 0.6); color: #fff; font-size: 14px; cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  opacity: 0; transition: opacity var(--duration-fast);
}
.preview-item:hover .preview-remove { opacity: 1; }

.write-uploading { font-size: 0.8rem; color: var(--primary); font-weight: 600; }

.write-actions { display: flex; justify-content: flex-end; gap: 10px; padding-top: 4px; }

.modal-enter-active { transition: opacity 0.3s var(--ease-out), transform 0.3s var(--ease-out); }
.modal-leave-active { transition: opacity 0.2s var(--ease-out), transform 0.2s var(--ease-out); }
.modal-enter-from, .modal-leave-to { opacity: 0; }
.modal-enter-from .write-panel, .modal-leave-to .write-panel { transform: scale(0.95); }

@media (max-width: 640px) {
  .write-panel { padding: 20px; gap: 12px; }
}
</style>
