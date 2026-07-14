<script setup>
import {
  Bold,
  Braces,
  Heading2,
  Image,
  Italic,
  Link,
  ListPlus,
  ListChecks,
  Quote,
  Table2
} from 'lucide-vue-next'
import IconButton from '../../../components/ui/IconButton.vue'

defineEmits(['command', 'assets', 'menu'])

const commands = [
  { id: 'bold', label: '粗体', icon: Bold },
  { id: 'italic', label: '斜体', icon: Italic },
  { id: 'heading', label: '二级标题', icon: Heading2 },
  { id: 'link', label: '链接', icon: Link },
  { id: 'quote', label: '引用', icon: Quote },
  { id: 'codeBlock', label: '代码块', icon: Braces },
  { id: 'task', label: '任务列表', icon: ListChecks },
  { id: 'table', label: '表格', icon: Table2 }
]
</script>

<template>
  <div class="editor-toolbar" role="toolbar" aria-label="Markdown 工具栏">
    <IconButton
      v-for="command in commands"
      :key="command.id"
      :label="command.label"
      @click="$emit('command', command.id)"
    >
      <component :is="command.icon" :size="17" />
    </IconButton>
    <span class="toolbar-separator" aria-hidden="true"></span>
    <IconButton label="插入已有图片" @click="$emit('assets')"><Image :size="17" /></IconButton>
    <IconButton label="更多插入项" @click="$emit('menu')"><ListPlus :size="17" /></IconButton>
  </div>
</template>

<style scoped>
.editor-toolbar {
  min-height: 48px;
  display: flex;
  align-items: center;
  gap: 2px;
  padding: 3px 6px;
  overflow-x: auto;
  border-bottom: 1px solid var(--border-subtle);
  scrollbar-width: thin;
}
.toolbar-separator {
  width: 1px;
  height: 22px;
  margin: 0 5px;
  background: var(--border-subtle);
}
</style>
