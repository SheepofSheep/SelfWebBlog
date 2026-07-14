<script setup>
import { ArrowLeft, Check, Cloud, LoaderCircle, PanelRight, Save } from 'lucide-vue-next'
import { computed, inject, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { listCategories, listTags, savePost } from '../api'
import AppDialog from '../components/ui/AppDialog.vue'
import IconButton from '../components/ui/IconButton.vue'
import SegmentedControl from '../components/ui/SegmentedControl.vue'
import { showToast } from '../composables/toast'
import {
  getDraftKey,
  hasUsefulDraft,
  migrateLegacyDraft,
  readDraft,
  removeDraft,
  writeDraft
} from '../composables/draftStorage'
import ArticleAssetPicker from '../features/editor/components/ArticleAssetPicker.vue'
import EditorWorkspace from '../features/editor/components/EditorWorkspace.vue'
import { buildImageMarkdown } from '../features/editor/model/imageMarkdown'
import MarkdownEditor from '../features/editor/components/MarkdownEditor.vue'
import MarkdownPreview from '../features/editor/components/MarkdownPreview.vue'
import PostSettingsPanel from '../features/editor/components/PostSettingsPanel.vue'
import PublishChecklist from '../features/editor/components/PublishChecklist.vue'
import { validatePost } from '../features/editor/model/publishValidation'

const router = useRouter()
const refreshHome = inject('refreshHome', async () => {})
const form = reactive({
  title: '',
  content: '',
  summary: '',
  coverUrl: '',
  category: '',
  tags: '',
  status: 'PUBLISHED'
})
const editingId = ref(null)
const mode = ref('edit')
const settingsOpen = ref(false)
const assetPickerOpen = ref(false)
const restoreDialogOpen = ref(false)
const pendingDraft = ref(null)
const localState = ref('saved')
const serverState = ref('idle')
const hasActiveUploads = ref(false)
const editor = ref(null)
const titleInput = ref(null)
const tags = ref([])
const categories = ref([])
let draftTimer = null

const modes = [
  { value: 'edit', label: '编辑' },
  { value: 'split', label: '分屏' },
  { value: 'preview', label: '预览' }
]
const validation = computed(() => {
  const result = validatePost(form, form.status)
  if (hasActiveUploads.value && !result.issues.some((item) => item.message.includes('上传'))) {
    result.issues.push({ field: 'content', message: '仍有图片任务未处理完成。' })
    result.valid = false
  }
  return result
})
const draftKey = computed(() => getDraftKey(editingId.value))
const saveLabel = computed(() => {
  if (serverState.value === 'saving') return '保存中'
  if (form.status === 'DRAFT') return '保存草稿'
  return editingId.value ? '更新文章' : '发布文章'
})
const stateLabel = computed(() => {
  if (serverState.value === 'saved') return '服务器已保存'
  if (serverState.value === 'failed') return '服务器保存失败'
  if (hasActiveUploads.value) return '图片处理中'
  if (localState.value === 'saving') return '本地保存中'
  return '本地已保存'
})

function applyData(data) {
  editingId.value = data.id || data.editingId || null
  Object.assign(form, {
    title: data.title || '',
    content: data.content || '',
    summary: data.summary || '',
    coverUrl: data.coverUrl || '',
    category: data.category || '',
    tags: Array.isArray(data.tags) ? data.tags.join(', ') : data.tags || '',
    status: data.postStatus || data.status || 'PUBLISHED'
  })
}

function draftData() {
  return {
    ...form,
    editingId: editingId.value,
    savedAt: new Date().toISOString()
  }
}

function queueLocalSave() {
  localState.value = 'saving'
  clearTimeout(draftTimer)
  draftTimer = setTimeout(() => {
    writeDraft(localStorage, draftKey.value, draftData())
    localState.value = 'saved'
  }, 500)
}

function restoreDraft() {
  if (pendingDraft.value) applyData(pendingDraft.value)
  pendingDraft.value = null
  restoreDialogOpen.value = false
  localState.value = 'saved'
}

function discardDraft() {
  removeDraft(localStorage, draftKey.value)
  pendingDraft.value = null
  restoreDialogOpen.value = false
}

async function loadOptions() {
  const [tagResult, categoryResult] = await Promise.allSettled([listTags(), listCategories()])
  tags.value = tagResult.status === 'fulfilled' ? tagResult.value : []
  categories.value = categoryResult.status === 'fulfilled' ? categoryResult.value : []
}

async function focusIssue(field) {
  if (field === 'title') {
    titleInput.value?.focus()
    return
  }
  if (field === 'content') {
    mode.value = 'edit'
    await nextTick()
    editor.value?.focus()
    return
  }
  settingsOpen.value = true
}

async function submit() {
  if (serverState.value === 'saving') return
  if (!validation.value.valid) {
    showToast(validation.value.issues[0]?.message || '请完成发布检查。', 'warning')
    await focusIssue(validation.value.issues[0]?.field)
    return
  }
  serverState.value = 'saving'
  try {
    await savePost({ id: editingId.value, ...form })
    serverState.value = 'saved'
    removeDraft(localStorage, draftKey.value)
    await refreshHome()
    showToast(form.status === 'DRAFT' ? '草稿已保存。' : '文章已发布。', 'success')
    await router.push(form.status === 'DRAFT' ? { name: 'admin', query: { tab: 'drafts' } } : '/')
  } catch (error) {
    serverState.value = 'failed'
    showToast(error.message || '保存失败，请稍后重试。', 'error')
  }
}

function insertAsset(asset) {
  editor.value?.insert(`${buildImageMarkdown(asset)}\n`)
  assetPickerOpen.value = false
}

function handleBeforeUnload(event) {
  if (localState.value === 'saving' || serverState.value === 'saving' || hasActiveUploads.value) {
    event.preventDefault()
    event.returnValue = ''
  }
}

watch(form, queueLocalSave, { deep: true })

onMounted(() => {
  migrateLegacyDraft(localStorage)
  const editing = sessionStorage.getItem('editingPost')
  if (editing) {
    try {
      applyData(JSON.parse(editing))
    } catch {
      sessionStorage.removeItem('editingPost')
    }
    sessionStorage.removeItem('editingPost')
  }
  const savedDraft = readDraft(localStorage, draftKey.value)
  if (savedDraft && hasUsefulDraft(savedDraft)) {
    pendingDraft.value = savedDraft
    restoreDialogOpen.value = true
  }
  loadOptions()
  window.addEventListener('beforeunload', handleBeforeUnload)
})

onBeforeUnmount(() => {
  clearTimeout(draftTimer)
  window.removeEventListener('beforeunload', handleBeforeUnload)
})
</script>

<template>
  <div class="write-page">
    <header class="write-toolbar">
      <div class="toolbar-inner">
        <IconButton label="返回" @click="router.back()"><ArrowLeft :size="18" /></IconButton>
        <div class="document-state">
          <strong>{{ editingId ? '编辑文章' : '新文章' }}</strong>
          <span :class="serverState"><Cloud :size="13" />{{ stateLabel }}</span>
        </div>
        <SegmentedControl v-model="mode" :options="modes" label="编辑视图" />
        <IconButton label="发布设置" @click="settingsOpen = !settingsOpen">
          <PanelRight :size="18" />
        </IconButton>
        <button class="save-button" type="button" :disabled="serverState === 'saving'" @click="submit">
          <LoaderCircle v-if="serverState === 'saving'" :size="16" class="spin" />
          <Save v-else-if="form.status === 'DRAFT'" :size="16" />
          <Check v-else :size="16" />
          {{ saveLabel }}
        </button>
      </div>
    </header>

    <main class="write-main page-width">
      <label class="title-field">
        <span class="sr-only">文章标题</span>
        <input ref="titleInput" v-model="form.title" maxlength="120" placeholder="文章标题" />
      </label>

      <EditorWorkspace
        :mode="mode"
        :settings-open="settingsOpen"
        @close-settings="settingsOpen = false"
      >
        <template #editor>
          <MarkdownEditor
            ref="editor"
            v-model="form.content"
            @assets="assetPickerOpen = true"
            @upload-state="hasActiveUploads = $event"
          />
        </template>
        <template #preview><MarkdownPreview :content="form.content" /></template>
        <template #settings>
          <PostSettingsPanel
            :model-value="form"
            :tag-options="tags"
            :categories="categories"
            @update:model-value="Object.assign(form, $event)"
          />
          <PublishChecklist :issues="validation.issues" @focus="focusIssue" />
        </template>
      </EditorWorkspace>
    </main>

    <ArticleAssetPicker
      :open="assetPickerOpen"
      @close="assetPickerOpen = false"
      @select="insertAsset"
    />

    <AppDialog
      :open="restoreDialogOpen"
      title="恢复本地草稿"
      description="检测到这个文章槽位中有未清理的本地内容。"
      @close="discardDraft"
    >
      <p class="restore-copy">草稿会覆盖当前编辑区内容，服务器中的文章不会被修改。</p>
      <template #footer>
        <button class="quiet-button" type="button" @click="discardDraft">忽略草稿</button>
        <button class="save-button" type="button" @click="restoreDraft">恢复草稿</button>
      </template>
    </AppDialog>
  </div>
</template>

<style scoped>
.write-page {
  min-height: 100vh;
  background: var(--page-bg);
}
.write-toolbar {
  position: sticky;
  top: 0;
  z-index: 300;
  border-bottom: 1px solid var(--border-subtle);
  background: color-mix(in srgb, var(--surface-glass) 94%, transparent);
  backdrop-filter: blur(12px);
}
.toolbar-inner {
  width: min(1440px, calc(100% - 24px));
  min-height: 66px;
  display: grid;
  grid-template-columns: auto minmax(150px, 1fr) minmax(250px, 340px) auto auto;
  align-items: center;
  gap: 12px;
  margin: 0 auto;
}
.document-state {
  min-width: 0;
  display: grid;
  gap: 2px;
}
.document-state strong {
  font-size: 0.82rem;
}
.document-state span {
  display: flex;
  align-items: center;
  gap: 5px;
  color: var(--text-tertiary);
  font-size: 0.67rem;
}
.document-state span.failed {
  color: var(--danger-color);
}
.document-state span.saved {
  color: var(--success-color);
}
.save-button,
.quiet-button {
  min-height: 40px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  padding: 0 14px;
  border-radius: var(--radius-control);
  font: inherit;
  font-size: 0.78rem;
  font-weight: 750;
  cursor: pointer;
}
.save-button {
  border: 1px solid var(--accent);
  background: var(--accent);
  color: #21180a;
}
.quiet-button {
  border: 1px solid var(--border-subtle);
  background: transparent;
  color: var(--text-secondary);
}
.save-button:disabled {
  cursor: wait;
  opacity: 0.6;
}
.write-main {
  padding-block: 28px 64px;
}
.title-field {
  display: block;
  margin-bottom: 20px;
}
.title-field input {
  width: 100%;
  padding: 8px 0 14px;
  border: 0;
  border-bottom: 1px solid var(--border-strong);
  background: transparent;
  color: var(--text-primary);
  font-family: var(--font-reading);
  font-size: clamp(2rem, 5vw, 4.4rem);
  line-height: 1.08;
}
.title-field input:focus {
  border-bottom-color: var(--accent);
  outline: none;
}
.restore-copy {
  margin: 0;
  color: var(--text-secondary);
  font-size: 0.84rem;
  line-height: 1.7;
}
.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  overflow: hidden;
  clip-path: inset(50%);
}
.spin {
  animation: spin 0.8s linear infinite;
}
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
@media (max-width: 760px) {
  .toolbar-inner {
    grid-template-columns: auto minmax(0, 1fr) auto auto;
  }
  .toolbar-inner :deep(.segmented-control) {
    grid-column: 1 / -1;
    grid-row: 2;
    margin-bottom: 8px;
  }
  .save-button {
    padding: 0 10px;
  }
  .write-main {
    padding-top: 22px;
  }
}
</style>
