<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { EditorState, Transaction } from '@codemirror/state'
import { EditorView, keymap, placeholder } from '@codemirror/view'
import { basicSetup } from 'codemirror'
import { markdown } from '@codemirror/lang-markdown'
import { undo, redo, isolateHistory } from '@codemirror/commands'
import {
  Bold,
  Italic,
  Strikethrough,
  Code,
  CodeXml,
  Link,
  Quote,
  List,
  ListOrdered,
  ListTodo,
  Table,
  Minus,
  AlignLeft,
  AlignCenter,
  AlignRight,
  Undo2,
  Redo2,
  ImagePlus
} from 'lucide-vue-next'
import { formatMarkdown } from '@/core/markdownFormatting.js'

const props = defineProps({ modelValue: { type: String, default: '' }, disabled: Boolean })
const emit = defineEmits(['update:modelValue', 'files'])
const host = ref(null)
const heading = ref('0')
const fontSize = ref('normal')
const announcement = ref('')
let view
const tools = [
  { action: 'bold', label: '粗体', shortcut: 'Ctrl / ⌘ + B', icon: Bold },
  { action: 'italic', label: '斜体', shortcut: 'Ctrl / ⌘ + I', icon: Italic },
  { action: 'strike', label: '删除线', icon: Strikethrough },
  { action: 'code', label: '行内代码', icon: Code },
  { action: 'link', label: '链接', icon: Link },
  { action: 'quote', label: '引用', icon: Quote },
  { action: 'bullet', label: '无序列表', icon: List },
  { action: 'ordered', label: '有序列表', icon: ListOrdered },
  { action: 'task', label: '任务列表', icon: ListTodo },
  { action: 'codeblock', label: '代码块', icon: CodeXml },
  { action: 'table', label: '表格', icon: Table },
  { action: 'divider', label: '分隔线', icon: Minus }
]
const alignments = [
  { value: 'left', label: '左对齐', icon: AlignLeft },
  { value: 'center', label: '居中', icon: AlignCenter },
  { value: 'right', label: '右对齐', icon: AlignRight }
]
const count = computed(() => props.modelValue.replace(/\s/g, '').length)

function format(action, value) {
  if (!view || props.disabled) return
  const { from, to } = view.state.selection.main
  const change = formatMarkdown(view.state.doc.toString(), from, to, action, value)
  if (!change) return
  view.dispatch({
    changes: { from: change.from, to: change.to, insert: change.insert },
    selection: { anchor: change.anchor, head: change.head },
    scrollIntoView: true,
    userEvent: 'input.format',
    annotations: isolateHistory.of('full')
  })
  view.focus()
  announcement.value = '格式已应用，可撤销'
}
function insert(text) {
  if (!view || props.disabled) return
  const { from, to } = view.state.selection.main
  view.dispatch({
    changes: { from, to, insert: text },
    selection: { anchor: from + text.length },
    scrollIntoView: true,
    userEvent: 'input'
  })
  view.focus()
}
function history(direction) {
  if (!view || props.disabled) return
  direction === 'undo' ? undo(view) : redo(view)
  view.focus()
}
function selectedFiles(event) {
  emit('files', Array.from(event.target.files))
  event.target.value = ''
}
onMounted(() => {
  view = new EditorView({
    parent: host.value,
    state: EditorState.create({
      doc: props.modelValue,
      extensions: [
        basicSetup,
        markdown(),
        EditorView.lineWrapping,
        placeholder('写下你的想法… 支持 Markdown，可粘贴或拖入图片'),
        EditorView.contentAttributes.of({ 'aria-label': '文章正文', spellcheck: 'false' }),
        keymap.of([
          {
            key: 'Mod-b',
            run: () => {
              format('bold')
              return true
            }
          },
          {
            key: 'Mod-i',
            run: () => {
              format('italic')
              return true
            }
          },
          {
            key: 'Mod-k',
            run: () => {
              format('link')
              return true
            }
          }
        ]),
        EditorView.updateListener.of((update) => {
          if (update.docChanged) emit('update:modelValue', update.state.doc.toString())
        }),
        EditorView.domEventHandlers({
          paste(event) {
            const files = [...(event.clipboardData?.files || [])].filter((file) =>
              file.type.startsWith('image/')
            )
            if (!files.length) return false
            if (!props.disabled) emit('files', files)
            event.preventDefault()
            return true
          },
          drop(event, editor) {
            const files = [...(event.dataTransfer?.files || [])].filter((file) =>
              file.type.startsWith('image/')
            )
            if (!files.length) return false
            event.preventDefault()
            if (!props.disabled) {
              const position = editor.posAtCoords({ x: event.clientX, y: event.clientY })
              if (position !== null) editor.dispatch({ selection: { anchor: position } })
              emit('files', files)
            }
            return true
          }
        })
      ]
    })
  })
})
watch(
  () => props.modelValue,
  (value) => {
    if (view && value !== view.state.doc.toString())
      view.dispatch({
        changes: { from: 0, to: view.state.doc.length, insert: value },
        annotations: Transaction.addToHistory.of(false)
      })
  }
)
onBeforeUnmount(() => view?.destroy())
defineExpose({ insert })
</script>

<template>
  <section class="markdown-editor">
    <div class="format-toolbar" role="toolbar" aria-label="正文格式工具">
      <div class="format-group">
        <select
          v-model="heading"
          aria-label="段落样式"
          :disabled="disabled"
          @change="format('heading', heading)"
        >
          <option value="0">正文</option>
          <option value="1">标题 1</option>
          <option value="2">标题 2</option>
          <option value="3">标题 3</option>
        </select>
        <select
          v-model="fontSize"
          aria-label="字号"
          :disabled="disabled"
          @change="format('size', fontSize)"
        >
          <option value="small">小号</option>
          <option value="normal">标准字号</option>
          <option value="large">大号</option>
          <option value="xlarge">特大号</option>
        </select>
      </div>
      <div class="format-group">
        <button
          v-for="tool in tools"
          :key="tool.action"
          type="button"
          :aria-label="tool.label"
          :title="tool.label + (tool.shortcut ? `（${tool.shortcut}）` : '')"
          :disabled="disabled"
          @mousedown.prevent
          @click="format(tool.action)"
        >
          <component :is="tool.icon" :size="17" />
        </button>
      </div>
      <div class="format-group">
        <button
          v-for="alignment in alignments"
          :key="alignment.value"
          type="button"
          :aria-label="alignment.label"
          :title="alignment.label"
          :disabled="disabled"
          @mousedown.prevent
          @click="format('align', alignment.value)"
        >
          <component :is="alignment.icon" :size="17" />
        </button>
      </div>
      <div class="format-group">
        <label class="format-upload" title="上传图片"
          ><ImagePlus :size="17" /><span>图片</span
          ><input
            type="file"
            accept="image/*"
            multiple
            aria-label="插入正文图片"
            :disabled="disabled"
            @change="selectedFiles"
        /></label>
        <button
          type="button"
          aria-label="撤销"
          title="撤销（Ctrl / ⌘ + Z）"
          :disabled="disabled"
          @mousedown.prevent
          @click="history('undo')"
        >
          <Undo2 :size="17" />
        </button>
        <button
          type="button"
          aria-label="重做"
          title="重做（Ctrl / ⌘ + Shift + Z）"
          :disabled="disabled"
          @mousedown.prevent
          @click="history('redo')"
        >
          <Redo2 :size="17" />
        </button>
      </div>
    </div>
    <div ref="host" class="markdown-editor-host" />
    <footer class="editor-count">
      <span
        >{{ count.toLocaleString() }} 字 · 约
        {{ Math.max(1, Math.ceil(count / 400)) }} 分钟阅读</span
      ><span>Markdown · 选中文字应用格式</span>
    </footer>
    <span class="sr-only" aria-live="polite">{{ announcement }}</span>
  </section>
</template>
