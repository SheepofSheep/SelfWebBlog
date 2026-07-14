<script setup>
import { Clipboard, Download, Link, LoaderCircle, Share2 } from 'lucide-vue-next'
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import AppDialog from '../../../components/ui/AppDialog.vue'
import SegmentedControl from '../../../components/ui/SegmentedControl.vue'
import { showToast } from '../../../composables/toast'
import { renderShareCard } from '../services/shareCardRenderer'

const props = defineProps({
  open: { type: Boolean, default: false },
  post: { type: Object, default: null },
  url: { type: String, default: '' }
})
defineEmits(['close'])
const orientation = ref('horizontal')
const rendering = ref(false)
const card = ref(null)
const imageUrl = ref('')
const orientations = [
  { value: 'horizontal', label: '横版' },
  { value: 'vertical', label: '竖版' }
]
const canSystemShare = computed(() => typeof navigator !== 'undefined' && Boolean(navigator.share))

function clearPreview() {
  if (imageUrl.value) URL.revokeObjectURL(imageUrl.value)
  imageUrl.value = ''
  card.value = null
}

async function generate() {
  if (!props.open || !props.post || !props.url) return
  rendering.value = true
  clearPreview()
  try {
    card.value = await renderShareCard({
      post: props.post,
      url: props.url,
      orientation: orientation.value
    })
    imageUrl.value = URL.createObjectURL(card.value.blob)
  } catch (error) {
    showToast(error.message || '分享卡片生成失败。', 'error')
  } finally {
    rendering.value = false
  }
}

async function copyImage() {
  if (!card.value || !navigator.clipboard || typeof ClipboardItem === 'undefined') return
  await navigator.clipboard.write([new ClipboardItem({ 'image/png': card.value.blob })])
  showToast('分享卡片已复制。', 'success')
}

async function copyLink() {
  await navigator.clipboard.writeText(props.url)
  showToast('文章链接已复制。', 'success')
}

function saveImage() {
  if (!imageUrl.value) return
  const link = document.createElement('a')
  link.href = imageUrl.value
  link.download = `gabriel-${props.post.id}.png`
  link.click()
}

async function systemShare() {
  if (!card.value || !canSystemShare.value) return
  const file = new File([card.value.blob], `gabriel-${props.post.id}.png`, { type: 'image/png' })
  await navigator.share({ title: props.post.title, text: props.post.summary || '', url: props.url, files: [file] })
}

watch(() => [props.open, props.post?.id, orientation.value], generate)
onBeforeUnmount(clearPreview)
</script>

<template>
  <AppDialog
    :open="open"
    title="分享文章"
    description="卡片在当前浏览器中生成。"
    @close="$emit('close')"
  >
    <SegmentedControl v-model="orientation" :options="orientations" label="卡片方向" />
    <div class="share-preview" :class="orientation">
      <LoaderCircle v-if="rendering" :size="24" />
      <img v-else-if="imageUrl" :src="imageUrl" alt="文章分享卡片预览" />
    </div>
    <div class="share-actions">
      <button type="button" :disabled="!card" @click="copyImage"><Clipboard :size="16" />复制图片</button>
      <button type="button" :disabled="!card" @click="saveImage"><Download :size="16" />保存 PNG</button>
      <button type="button" @click="copyLink"><Link :size="16" />复制链接</button>
      <button v-if="canSystemShare" type="button" :disabled="!card" @click="systemShare"><Share2 :size="16" />系统分享</button>
    </div>
  </AppDialog>
</template>

<style scoped>
.share-preview {
  min-height: 280px;
  display: grid;
  place-items: center;
  margin-top: 16px;
  overflow: hidden;
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-control);
  background: var(--surface-soft);
}
.share-preview.vertical {
  min-height: 420px;
}
.share-preview svg {
  color: var(--text-secondary);
  animation: spin 0.8s linear infinite;
}
.share-preview img {
  display: block;
  max-width: 100%;
  max-height: 480px;
  object-fit: contain;
}
.share-actions {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  margin-top: 14px;
}
.share-actions button {
  min-height: 40px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-control);
  background: var(--surface-solid);
  color: var(--text-primary);
  font: inherit;
  font-size: 0.75rem;
  cursor: pointer;
}
.share-actions button:disabled {
  cursor: not-allowed;
  opacity: 0.45;
}
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
