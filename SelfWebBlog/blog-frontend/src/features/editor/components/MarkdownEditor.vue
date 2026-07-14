<script setup>
import { basicSetup } from 'codemirror'
import { markdown } from '@codemirror/lang-markdown'
import { EditorSelection, EditorState } from '@codemirror/state'
import { EditorView, keymap } from '@codemirror/view'
import { defaultKeymap, historyKeymap } from '@codemirror/commands'
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { uploadImage } from '../../../api/uploads'
import { showToast } from '../../../composables/toast'
import { applyEditorCommand, uploadPlaceholder } from '../model/editorCommands'
import { buildImageMarkdown } from '../model/imageMarkdown'
import { createUploadQueue } from '../model/uploadQueue'
import EditorToolbar from './EditorToolbar.vue'
import EditorCommandMenu from './EditorCommandMenu.vue'
import ImageUploadQueue from './ImageUploadQueue.vue'

const props = defineProps({ modelValue: { type: String, default: '' } })
const emit = defineEmits(['update:modelValue', 'assets', 'upload-state'])
const editorHost = ref(null)
const taskList = ref([])
const commandMenuOpen = ref(false)
const queue = createUploadQueue()
const dismissed = new Set()
const uploadControllers = new Map()
let view = null
let unmounting = false

function syncTasks() {
  taskList.value = queue.list().filter((task) => !dismissed.has(task.id))
  emit(
    'upload-state',
    taskList.value.some((task) => ['queued', 'uploading', 'failed'].includes(task.status))
  )
}

function emitDocument() {
  if (view) emit('update:modelValue', view.state.doc.toString())
}

function replaceLiteral(search, replacement) {
  if (!view) return
  const text = view.state.doc.toString()
  const from = text.indexOf(search)
  if (from < 0) return
  view.dispatch({ changes: { from, to: from + search.length, insert: replacement } })
}

function insert(text) {
  if (!view) return
  const selection = view.state.selection.main
  view.dispatch({
    changes: { from: selection.from, to: selection.to, insert: text },
    selection: EditorSelection.cursor(selection.from + text.length),
    scrollIntoView: true
  })
  view.focus()
}

function runCommand(name) {
  commandMenuOpen.value = false
  if (!view) return
  const selection = view.state.selection.main
  const result = applyEditorCommand(name, {
    text: view.state.doc.toString(),
    from: selection.from,
    to: selection.to
  })
  view.dispatch({
    changes: { from: 0, to: view.state.doc.length, insert: result.text },
    selection: EditorSelection.range(result.selection.from, result.selection.to),
    scrollIntoView: true
  })
  view.focus()
}

async function startUpload(taskId) {
  const task = queue.get(taskId)
  if (!task) return
  queue.transition(taskId, 'uploading')
  syncTasks()
  const controller = new AbortController()
  uploadControllers.set(taskId, controller)
  try {
    const asset = await uploadImage(task.file, {
      signal: controller.signal,
      onUploadProgress(event) {
        queue.updateProgress(taskId, event.total ? (event.loaded / event.total) * 100 : 0)
        syncTasks()
      }
    })
    const alt = task.file.name.replace(/\.[^.]+$/, '') || '图片'
    const markdownText = buildImageMarkdown(asset, alt)
    replaceLiteral(uploadPlaceholder(taskId), markdownText)
    queue.transition(taskId, 'completed', { progress: 100, markdown: markdownText, asset })
  } catch (error) {
    if (error?.code === 'ERR_CANCELED' || error?.name === 'CanceledError') {
      if (!unmounting) {
        replaceLiteral(uploadPlaceholder(taskId), `![上传已取消](upload-canceled:${taskId})`)
        queue.transition(taskId, 'failed', { error: '上传已取消' })
        syncTasks()
      }
      return
    }
    replaceLiteral(uploadPlaceholder(taskId), `![上传失败](upload-failed:${taskId})`)
    queue.transition(taskId, 'failed', { error: error.message || '上传失败' })
    showToast(error.message || '图片上传失败，可在队列中重试。', 'error')
  }
  uploadControllers.delete(taskId)
  syncTasks()
}

function enqueueFiles(files) {
  const images = [...files].filter((file) => file.type.startsWith('image/'))
  for (const file of images) {
    const task = queue.add(file)
    insert(`${uploadPlaceholder(task.id)}\n`)
    startUpload(task.id)
  }
}

function retry(taskId) {
  const task = queue.get(taskId)
  if (!task || task.status !== 'failed') return
  replaceLiteral(`![上传失败](upload-failed:${taskId})`, uploadPlaceholder(taskId))
  queue.retry(taskId)
  syncTasks()
  startUpload(taskId)
}

function dismiss(taskId) {
  dismissed.add(taskId)
  syncTasks()
}

function onPaste(event) {
  const images = [...(event.clipboardData?.files || [])].filter((file) =>
    file.type.startsWith('image/')
  )
  if (!images.length) return false
  event.preventDefault()
  enqueueFiles(images)
  return true
}

function onDrop(event) {
  const images = [...(event.dataTransfer?.files || [])].filter((file) =>
    file.type.startsWith('image/')
  )
  if (!images.length) return false
  event.preventDefault()
  enqueueFiles(images)
  return true
}

onMounted(() => {
  view = new EditorView({
    parent: editorHost.value,
    state: EditorState.create({
      doc: props.modelValue,
      extensions: [
        basicSetup,
        markdown(),
        keymap.of([...defaultKeymap, ...historyKeymap]),
        EditorView.lineWrapping,
        EditorView.updateListener.of((update) => {
          if (update.docChanged) emitDocument()
        }),
        EditorView.domEventHandlers({ paste: onPaste, drop: onDrop }),
        EditorView.theme({
          '&': { height: '100%', backgroundColor: 'transparent' },
          '.cm-content': { padding: '22px 24px', minHeight: '520px', caretColor: 'var(--accent)' },
          '.cm-gutters': { backgroundColor: 'transparent', border: 'none' },
          '.cm-activeLine, .cm-activeLineGutter': { backgroundColor: 'var(--surface-soft)' },
          '&.cm-focused': { outline: 'none' }
        })
      ]
    })
  })
})

watch(
  () => props.modelValue,
  (value) => {
    if (!view || view.state.doc.toString() === value) return
    view.dispatch({ changes: { from: 0, to: view.state.doc.length, insert: value } })
  }
)

function cancelUploads() {
  uploadControllers.forEach((controller) => controller.abort())
  uploadControllers.clear()
}

onBeforeUnmount(() => {
  unmounting = true
  cancelUploads()
  view?.destroy()
})

defineExpose({
  focus: () => view?.focus(),
  cancelUploads,
  insert,
  replaceRange(from, to, text) {
    view?.dispatch({ changes: { from, to, insert: text } })
  },
  scrollToHeading(text) {
    if (!view) return
    const position = view.state.doc.toString().indexOf(text)
    if (position >= 0) view.dispatch({ selection: { anchor: position }, scrollIntoView: true })
  }
})
</script>

<template>
  <section class="markdown-editor" aria-label="Markdown 编辑器">
    <EditorToolbar
      @command="runCommand"
      @assets="$emit('assets')"
      @menu="commandMenuOpen = !commandMenuOpen"
    />
    <EditorCommandMenu :open="commandMenuOpen" @select="runCommand" />
    <div ref="editorHost" class="editor-host"></div>
    <ImageUploadQueue :tasks="taskList" @retry="retry" @dismiss="dismiss" />
  </section>
</template>

<style scoped>
.markdown-editor {
  position: relative;
  min-width: 0;
  overflow: hidden;
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-card);
  background: var(--surface-solid);
}
.editor-host {
  min-height: 520px;
  color: var(--text-primary);
}
.editor-host :deep(.cm-editor) {
  font-family: ui-monospace, 'SFMono-Regular', Consolas, monospace;
  font-size: 0.88rem;
  line-height: 1.75;
}
.editor-host :deep(.cm-scroller) {
  overflow: auto;
}
.editor-host :deep(.cm-cursor) {
  border-left-color: var(--accent);
}
.editor-host :deep(.cm-selectionBackground) {
  background: var(--accent-soft) !important;
}
</style>
