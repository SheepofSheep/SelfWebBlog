<script setup>
defineProps({
  mode: { type: String, default: 'edit' },
  settingsOpen: { type: Boolean, default: false }
})
defineEmits(['close-settings'])
</script>

<template>
  <div class="editor-workspace" :class="[`mode-${mode}`, { 'settings-open': settingsOpen }]">
    <div v-show="mode !== 'preview'" class="workspace-editor"><slot name="editor" /></div>
    <div v-show="mode !== 'edit'" class="workspace-preview"><slot name="preview" /></div>
    <div class="workspace-settings"><slot name="settings" /></div>
    <button
      v-if="settingsOpen"
      class="settings-backdrop"
      type="button"
      aria-label="关闭发布设置"
      @click="$emit('close-settings')"
    ></button>
  </div>
</template>

<style scoped>
.editor-workspace {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  align-items: start;
  gap: 18px;
}
.workspace-editor,
.workspace-preview {
  min-width: 0;
}
.workspace-preview {
  overflow: hidden;
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-card);
  background: var(--surface-solid);
}
.mode-split {
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr) 280px;
}
.workspace-settings {
  position: sticky;
  top: 86px;
}
.settings-backdrop {
  display: none;
}
@media (max-width: 1080px) {
  .mode-split {
    grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  }
  .editor-workspace:not(.mode-split) {
    grid-template-columns: minmax(0, 1fr);
  }
  .workspace-settings {
    position: fixed;
    top: 0;
    right: 0;
    bottom: 0;
    z-index: 650;
    width: min(360px, calc(100vw - 34px));
    padding: 20px;
    overflow-y: auto;
    background: var(--page-bg);
    transform: translateX(105%);
    transition: transform var(--motion-normal-duration) var(--motion-ease);
  }
  .settings-open .workspace-settings {
    transform: translateX(0);
  }
  .settings-backdrop {
    position: fixed;
    inset: 0;
    z-index: 640;
    display: block;
    border: 0;
    background: rgb(20 16 10 / 38%);
  }
}
@media (max-width: 760px) {
  .mode-split {
    grid-template-columns: 1fr;
  }
}
</style>
