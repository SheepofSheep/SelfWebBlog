<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Eye, ImagePlus, PanelRight, RefreshCw, RotateCcw, Save, Send, X } from 'lucide-vue-next'
import { api } from '@/api/client.js'
import { canPublish } from '@/core/publishPolicy.js'
import { postTags } from '@/core/format.js'
import { backgroundValueForMode, normalizePostBackground } from '@/core/postBackground.js'
import {
  draftKey,
  hasUsefulDraft,
  migrateLegacyDraft,
  readDraft,
  removeLocalMedia,
  removeDraft,
  stripLocalMedia,
  writeDraft
} from '@/core/draftStorage.js'
import { appStore, notify } from '@/stores/app.js'
import MarkdownBody from '@/components/MarkdownBody.vue'
import TagInput from '@/components/TagInput.vue'
const route = useRoute()
const router = useRouter()
const preview = ref(true)
const settings = ref(true)
const textarea = ref()
const uploads = ref([])
const uploadControllers = new Set()
const busy = ref(false)
const initialLoading = ref(true)
const loadError = ref('')
const draftReady = ref(false)
const draftDirty = ref(false)
const editingArchived = ref(false)
const editingPublished = ref(false)
let draftTimer
let loadTicket = 0
let beforeUnloadAttached = false
const post = reactive({
  id: null,
  title: '',
  content: '',
  summary: '',
  coverUrl: '',
  category: '',
  tags: [],
  status: 'DRAFT',
  backgroundMode: 'COVER',
  backgroundValue: '',
  backgroundFocusX: 50,
  backgroundFocusY: 50
})
const publishable = computed(
  () => canPublish(post.content) && !uploads.value.some((item) => item.status === 'uploading')
)
const pendingUploads = computed(() =>
  uploads.value.some((item) => ['uploading', 'failed'].includes(item.status))
)
const storageKey = computed(() => draftKey(route.params.id))
function persistLocalDraft() {
  if (!draftReady.value || !draftDirty.value) return
  const draft = {
    ...post,
    content: stripLocalMedia(post.content),
    tags: [...post.tags],
    savedAt: Date.now()
  }
  if (hasUsefulDraft(draft)) writeDraft(localStorage, storageKey.value, draft)
  else removeDraft(localStorage, storageKey.value)
  draftDirty.value = false
}
watch(
  post,
  () => {
    if (!draftReady.value) return
    draftDirty.value = true
    clearTimeout(draftTimer)
    draftTimer = setTimeout(persistLocalDraft, 450)
  },
  { deep: true }
)
watch(
  () => appStore.ready,
  (ready) => {
    if (ready && appStore.user?.role !== 'ADMIN')
      router.replace({ path: '/login', query: { redirect: route.fullPath } })
  },
  { immediate: true }
)
watch(
  () => post.backgroundMode,
  (mode, previous) => {
    if (!previous || mode === previous) return
    post.backgroundValue = backgroundValueForMode(mode, post.backgroundValue)
  }
)
function insert(text) {
  const el = textarea.value
  const start = el?.selectionStart ?? post.content.length
  const end = el?.selectionEnd ?? start
  post.content = post.content.slice(0, start) + text + post.content.slice(end)
  requestAnimationFrame(() => {
    el?.focus()
    el?.setSelectionRange(start + text.length, start + text.length)
  })
}
async function uploadEntry(entry) {
  const controller = new AbortController()
  entry.controller = controller
  entry.status = 'uploading'
  uploadControllers.add(controller)
  try {
    const asset = await api.upload(entry.file, { signal: controller.signal })
    post.content = post.content.replaceAll(entry.localUrl, asset.originalUrl || asset.largeWebpUrl)
    entry.status = 'done'
    entry.url = asset.originalUrl
    URL.revokeObjectURL(entry.localUrl)
  } catch (error) {
    if (error.name !== 'AbortError') {
      entry.status = 'failed'
      notify(error.message)
    }
  } finally {
    uploadControllers.delete(controller)
  }
}

async function addFiles(files) {
  const entries = [...files]
    .filter((item) => item.type.startsWith('image/'))
    .map((file) => {
      const entry = reactive({
        name: file.name,
        file,
        localUrl: URL.createObjectURL(file),
        status: 'uploading'
      })
      uploads.value.push(entry)
      insert(`\n![图片](${entry.localUrl})\n`)
      return entry
    })
  await Promise.allSettled(entries.map(uploadEntry))
}

function selectFiles(event) {
  const files = event.target.files
  event.target.value = ''
  addFiles(files)
}
async function retryUpload(entry) {
  await uploadEntry(entry)
}
function removeUpload(entry) {
  entry.controller?.abort()
  post.content = removeLocalMedia(post.content, entry.localUrl)
  URL.revokeObjectURL(entry.localUrl)
  uploads.value = uploads.value.filter((item) => item !== entry)
}
function onPaste(event) {
  const files = [...event.clipboardData.files]
  if (files.length) {
    event.preventDefault()
    addFiles(files)
  }
}
function onDrop(event) {
  event.preventDefault()
  addFiles(event.dataTransfer.files)
}
async function save(status) {
  if (busy.value || initialLoading.value || loadError.value || (route.params.id && !post.id)) return
  if (status !== 'DRAFT' && !publishable.value) return notify('文章需要包含正文或已上传的图片')
  if (pendingUploads.value) return notify('请等待图片上传完成，或移除失败的图片')
  const wasArchived = editingArchived.value
  const wasPublished = editingPublished.value
  busy.value = true
  try {
    await api.savePost({
      ...post,
      content: stripLocalMedia(post.content),
      tags: post.tags.join(', '),
      status
    })
    clearTimeout(draftTimer)
    draftDirty.value = false
    removeDraft(localStorage, storageKey.value)
    post.status = status
    editingArchived.value = status === 'ARCHIVED'
    editingPublished.value = status === 'PUBLISHED'
    notify(
      status === 'ARCHIVED'
        ? '归档内容已保存'
        : status === 'PUBLISHED'
          ? wasArchived
            ? '文章已恢复发布'
            : wasPublished
              ? '修改已保存'
              : '已发布'
          : '草稿已保存'
    )
    if (!post.id) router.push('/admin?tab=content')
  } catch (e) {
    notify(e.message)
  } finally {
    busy.value = false
  }
}
async function loadEditor() {
  const ticket = ++loadTicket
  initialLoading.value = true
  loadError.value = ''
  draftReady.value = false
  migrateLegacyDraft(localStorage)
  if (appStore.ready && appStore.user?.role !== 'ADMIN')
    return router.replace({ path: '/login', query: { redirect: route.fullPath } })
  try {
    if (route.params.id) {
      const value = await api.post(route.params.id)
      if (ticket !== loadTicket) return
      if (!value?.id || String(value.id) !== String(route.params.id)) {
        throw new Error('文章加载结果无效')
      }
      editingArchived.value = value.status === 'ARCHIVED'
      editingPublished.value = value.status === 'PUBLISHED'
      Object.assign(post, value, {
        tags: postTags(value),
        ...normalizePostBackground(value)
      })
    }
    const draft = readDraft(localStorage, storageKey.value)
    const serverTime = post.updateTime ? new Date(post.updateTime).getTime() : 0
    if (hasUsefulDraft(draft) && (!draft.savedAt || draft.savedAt > serverTime)) {
      Object.assign(post, draft, {
        id: post.id,
        tags: postTags(draft),
        ...normalizePostBackground(draft)
      })
      notify('已恢复本地草稿')
    }
    if (ticket !== loadTicket) return
    draftReady.value = true
    if (!beforeUnloadAttached) {
      window.addEventListener('beforeunload', persistLocalDraft)
      beforeUnloadAttached = true
    }
  } catch (error) {
    if (ticket === loadTicket) loadError.value = error?.message || '文章加载失败'
  } finally {
    if (ticket === loadTicket) initialLoading.value = false
  }
}

onMounted(() => {
  loadEditor()
})
onBeforeUnmount(() => {
  loadTicket += 1
  if (beforeUnloadAttached) window.removeEventListener('beforeunload', persistLocalDraft)
  clearTimeout(draftTimer)
  persistLocalDraft()
  uploadControllers.forEach((controller) => controller.abort())
  uploads.value.forEach((item) => URL.revokeObjectURL(item.localUrl))
})
</script>
<template>
  <div class="editor-page">
    <div v-if="initialLoading" class="editor-load-state glass" aria-live="polite">加载中</div>
    <div v-else-if="loadError" class="editor-load-state glass" role="alert">
      <strong>无法打开这篇文章</strong>
      <span>{{ loadError }}</span>
      <div>
        <button class="secondary-button" type="button" @click="loadEditor">
          <RefreshCw :size="16" />重试
        </button>
        <RouterLink class="secondary-button" to="/admin?tab=content">返回文章管理</RouterLink>
      </div>
    </div>
    <template v-else>
      <header class="editor-header glass">
        <RouterLink to="/admin" class="icon-button" aria-label="关闭"><X /></RouterLink
        ><input v-model="post.title" class="editor-title" placeholder="标题（可选）" />
        <div class="editor-actions">
          <button
            class="icon-button"
            :class="{ active: preview }"
            aria-label="预览"
            @click="preview = !preview"
          >
            <Eye /></button
          ><button
            class="icon-button"
            :class="{ active: settings }"
            aria-label="文章设置"
            @click="settings = !settings"
          >
            <PanelRight /></button
          ><button
            class="secondary-button editor-save-button"
            :aria-label="editingArchived ? '保存归档' : editingPublished ? '保存修改' : '保存草稿'"
            :title="editingArchived ? '保存归档' : editingPublished ? '保存修改' : '保存草稿'"
            :disabled="busy || pendingUploads"
            @click="save(editingArchived ? 'ARCHIVED' : editingPublished ? 'PUBLISHED' : 'DRAFT')"
          >
            <Save :size="17" /><span class="editor-save-label">{{
              editingArchived ? '保存归档' : editingPublished ? '保存修改' : '草稿'
            }}</span></button
          ><button
            v-if="!editingPublished"
            class="primary-button"
            :disabled="busy || !publishable"
            @click="save('PUBLISHED')"
          >
            <RotateCcw v-if="editingArchived" :size="17" /><Send v-else :size="17" />{{
              editingArchived ? '恢复发布' : '发布'
            }}
          </button>
        </div>
      </header>
      <div
        class="editor-grid"
        :class="{ 'without-preview': !preview, 'without-settings': !settings }"
      >
        <section class="editor-pane">
          <div class="editor-toolbar">
            <label class="secondary-button"
              ><ImagePlus :size="17" />图片<input
                type="file"
                accept="image/*"
                multiple
                hidden
                @change="selectFiles" /></label
            ><span v-if="uploads.some((item) => item.status === 'uploading')">图片上传中</span
            ><span
              v-for="item in uploads.filter((value) => value.status === 'failed')"
              :key="item.localUrl"
              class="error-text"
              >{{ item.name }} 上传失败
              <button type="button" @click="retryUpload(item)">重试</button>
              <button type="button" @click="removeUpload(item)">移除</button></span
            >
          </div>
          <textarea
            ref="textarea"
            v-model="post.content"
            aria-label="文章正文"
            placeholder="正文（可粘贴或拖入图片）"
            @paste="onPaste"
            @drop="onDrop"
            @dragover.prevent
          />
        </section>
        <section v-if="preview" class="preview-pane">
          <MarkdownBody :content="post.content" allow-local-media />
        </section>
        <aside v-if="settings" class="settings-pane glass">
          <h2>文章设置</h2>
          <label>摘要<textarea v-model="post.summary" rows="3" /></label
          ><label>封面地址<input v-model="post.coverUrl" /></label
          ><label>分类<input v-model="post.category" /></label
          ><label>标签<TagInput v-model="post.tags" /></label
          ><label
            >卡片背景<select v-model="post.backgroundMode">
              <option value="COVER">跟随封面</option>
              <option value="CUSTOM_IMAGE">自定义图片</option>
              <option value="GRADIENT">渐变预设</option>
              <option value="SOLID">纯色</option>
              <option value="NONE">无背景</option>
            </select></label
          ><label v-if="post.backgroundMode === 'CUSTOM_IMAGE'"
            >图片地址<input v-model="post.backgroundValue" /></label
          ><label v-if="post.backgroundMode === 'GRADIENT'"
            >渐变<select v-model="post.backgroundValue">
              <option value="tide">潮汐</option>
              <option value="mist">雾蓝</option>
              <option value="dusk">暮色</option>
              <option value="leaf">浅绿</option>
              <option value="ember">暖橙</option>
            </select></label
          ><label v-if="post.backgroundMode === 'SOLID'"
            >颜色<input v-model="post.backgroundValue" type="color" /></label
          ><template v-if="['COVER', 'CUSTOM_IMAGE'].includes(post.backgroundMode)"
            ><label
              >横向焦点<input
                v-model.number="post.backgroundFocusX"
                type="range"
                min="0"
                max="100" /></label
            ><label
              >纵向焦点<input
                v-model.number="post.backgroundFocusY"
                type="range"
                min="0"
                max="100" /></label
          ></template>
        </aside>
      </div>
    </template>
  </div>
</template>
