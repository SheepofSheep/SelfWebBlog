<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { ImagePlus, LoaderCircle, RefreshCw, Trash2, X } from 'lucide-vue-next'
import { api } from '@/api/client.js'

const props = defineProps({
  title: { type: String, default: '' },
  summary: { type: String, default: '' },
  coverUrl: { type: String, default: '' },
  disabled: Boolean,
  compact: Boolean
})
const emit = defineEmits(['update:title', 'update:summary', 'update:coverUrl', 'uploading-change'])
const fileInput = ref()
const uploading = ref(false)
const localPreview = ref('')
const error = ref('')
const dragging = ref(false)
const imageBroken = ref(false)
const previewUrl = computed(() => localPreview.value || props.coverUrl)
let controller
let ticket = 0
let retryFile

watch(previewUrl, () => {
  imageBroken.value = false
})
function releasePreview() {
  if (localPreview.value) URL.revokeObjectURL(localPreview.value)
  localPreview.value = ''
}
function cancelUpload() {
  ticket += 1
  controller?.abort()
  controller = null
  uploading.value = false
  emit('uploading-change', false)
  releasePreview()
}
async function uploadCover(file) {
  if (!file || props.disabled) return
  const supported = file.type
    ? /^image\/(png|jpeg|webp|gif)$/i.test(file.type)
    : /\.(png|jpe?g|webp|gif)$/i.test(file.name)
  if (!supported) {
    error.value = '请选择 JPG、PNG、WebP 或 GIF 图片'
    return
  }
  cancelUpload()
  retryFile = file
  error.value = ''
  localPreview.value = URL.createObjectURL(file)
  const current = ++ticket
  controller = new AbortController()
  uploading.value = true
  emit('uploading-change', true)
  try {
    const asset = await api.upload(file, { signal: controller.signal })
    if (current !== ticket) return
    const url = asset.originalUrl || asset.largeWebpUrl
    if (!url) throw new Error('上传结果缺少图片地址，请重试')
    emit('update:coverUrl', url)
    retryFile = null
  } catch (failure) {
    if (current === ticket && failure.name !== 'AbortError')
      error.value = failure.message || '封面上传失败，请重试'
  } finally {
    if (current === ticket) {
      controller = null
      uploading.value = false
      emit('uploading-change', false)
      releasePreview()
    }
  }
}
function selectCover(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  uploadCover(file)
}
function dropCover(event) {
  dragging.value = false
  uploadCover(event.dataTransfer.files?.[0])
}
function removeCover() {
  cancelUpload()
  retryFile = null
  error.value = ''
  emit('update:coverUrl', '')
}
function changeAddress(event) {
  error.value = ''
  retryFile = null
  emit('update:coverUrl', event.target.value.trim())
}
onBeforeUnmount(cancelUpload)
</script>

<template>
  <section class="article-details glass" :class="{ compact }" aria-label="文章信息">
    <div class="article-details-fields">
      <div class="article-details-heading">
        <span>文章信息</span><small>先记录想法，再慢慢完善</small>
      </div>
      <label class="article-title-field">
        <span>文章标题 <small>选填</small></span>
        <input
          :value="title"
          :disabled="disabled"
          placeholder="标题（可选）"
          @input="emit('update:title', $event.target.value)"
        />
      </label>
      <label class="article-summary-field">
        <span>摘要 <small>用一两句话介绍这篇文章</small></span>
        <textarea
          :value="summary"
          :disabled="disabled"
          rows="3"
          placeholder="写下文章的核心内容，方便读者快速了解…"
          @input="emit('update:summary', $event.target.value)"
        />
      </label>
    </div>
    <div class="article-cover-field">
      <div class="article-cover-heading">
        <span>文章封面 <small>选填</small></span
        ><small>建议横向图片</small>
      </div>
      <div
        class="article-cover-dropzone"
        :class="{ dragging, 'has-cover': previewUrl && !imageBroken }"
        :aria-busy="uploading"
        @dragover.prevent="dragging = !disabled"
        @dragleave.prevent="dragging = false"
        @drop.prevent="dropCover"
      >
        <img
          v-if="previewUrl && !imageBroken"
          :src="previewUrl"
          alt="文章封面预览"
          @error="imageBroken = true"
        />
        <div v-else class="article-cover-placeholder">
          <ImagePlus :size="28" :stroke-width="1.4" />
          <strong>{{ imageBroken ? '封面暂时无法显示' : '为文章添加一张封面' }}</strong>
          <span>{{
            imageBroken ? '可以更换图片或检查封面地址' : '拖拽图片到这里，或点击下方上传'
          }}</span>
          <small>JPG / PNG / WebP / GIF</small>
        </div>
        <div v-if="uploading" class="article-cover-progress" aria-live="polite">
          <LoaderCircle :size="22" />封面上传中…
        </div>
      </div>
      <input
        ref="fileInput"
        type="file"
        accept="image/jpeg,image/png,image/webp,image/gif"
        aria-label="选择封面图片"
        :disabled="disabled"
        hidden
        @change="selectCover"
      />
      <div class="article-cover-actions">
        <button
          type="button"
          class="secondary-button"
          :disabled="disabled"
          @click="fileInput?.click()"
        >
          <ImagePlus :size="16" />{{ previewUrl ? '更换封面' : '上传封面' }}
        </button>
        <button v-if="uploading" type="button" class="secondary-button" @click="cancelUpload">
          <X :size="16" />取消上传
        </button>
        <button
          v-else-if="coverUrl"
          type="button"
          class="secondary-button"
          :disabled="disabled"
          @click="removeCover"
        >
          <Trash2 :size="16" />移除封面
        </button>
      </div>
      <div v-if="error" class="article-cover-error" role="alert">
        <span>{{ error }}</span>
        <button
          v-if="retryFile && !uploading"
          type="button"
          :disabled="disabled"
          @click="uploadCover(retryFile)"
        >
          <RefreshCw :size="14" />重试上传
        </button>
      </div>
      <details class="article-cover-address">
        <summary>使用图片地址</summary>
        <label
          >封面地址<input
            :value="coverUrl"
            :disabled="disabled || uploading"
            placeholder="https://… 或 /uploads/…"
            @input="changeAddress"
        /></label>
      </details>
    </div>
  </section>
</template>

<style scoped>
.article-details.compact {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 20px;
  padding: 0;
  margin: 18px 0 0;
  border: 0;
  border-radius: 0;
  background: transparent;
  backdrop-filter: none;
  box-shadow: none;
}
.compact .article-details-heading {
  display: none;
}
.compact .article-cover-dropzone {
  aspect-ratio: auto;
  height: 145px;
  background: var(--field);
}
.compact .article-title-field input {
  padding: 10px 0;
  font-size: 22px;
  font-weight: 550;
}
.compact .article-summary-field textarea,
.compact .article-cover-address input {
  background: var(--field);
}
.compact .article-cover-heading small {
  display: none;
}
@media (max-width: 720px) {
  .article-details.compact {
    grid-template-columns: minmax(0, 1fr) 120px;
    gap: 16px;
  }
  .compact .article-cover-dropzone {
    height: 120px;
  }
  .compact .article-cover-placeholder > span,
  .compact .article-cover-placeholder > small,
  .compact .article-cover-placeholder > strong {
    display: none;
  }
  .compact .article-summary-field {
    margin-top: 12px;
  }
  .compact .article-summary-field textarea {
    min-height: 64px;
    padding: 8px;
    font-size: 12px;
  }
}
.article-details {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(280px, 360px);
  gap: 36px;
  margin-top: 12px;
  padding: 28px 32px;
  border-radius: 20px;
}
.article-details-fields,
.article-cover-field {
  min-width: 0;
}
.article-details-heading,
.article-cover-heading,
.article-cover-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.article-details-heading {
  margin-bottom: 24px;
}
.article-details-heading > span {
  font-size: 15px;
  font-weight: 700;
}
small {
  color: var(--muted);
  font-size: 12px;
  font-weight: 400;
}
label {
  display: grid;
  gap: 10px;
}
label > span,
.article-cover-heading > span {
  font-size: 13px;
  font-weight: 600;
}
label small,
.article-cover-heading small {
  margin-left: 6px;
}
input,
textarea {
  width: 100%;
  min-width: 0;
  color: var(--ink);
}
.article-title-field input {
  padding: 0 0 16px;
  border: 0;
  border-bottom: 1px solid var(--line);
  border-radius: 0;
  background: transparent;
  font-size: clamp(24px, 2.4vw, 34px);
  font-weight: 700;
  line-height: 1.5;
}
.article-summary-field {
  margin-top: 22px;
}
.article-summary-field textarea,
.article-cover-address input {
  padding: 12px 14px;
  border: 1px solid var(--line);
  border-radius: 12px;
  background: var(--glass-strong);
  font: inherit;
  font-size: 14px;
  line-height: 1.7;
  resize: vertical;
}
.article-summary-field textarea {
  min-height: 90px;
}
.article-cover-heading {
  margin-bottom: 10px;
}
.article-cover-dropzone {
  position: relative;
  display: grid;
  aspect-ratio: 16 / 9;
  overflow: hidden;
  place-items: center;
  border: 1px dashed var(--line);
  border-radius: 14px;
  background: var(--glass-strong);
}
.article-cover-dropzone.dragging {
  outline: 2px solid var(--ink);
  outline-offset: 3px;
}
.article-cover-dropzone.has-cover {
  border-style: solid;
}
.article-cover-dropzone > img {
  width: 100%;
  height: 100%;
  position: absolute;
  object-fit: cover;
}
.article-cover-placeholder {
  display: grid;
  gap: 7px;
  justify-items: center;
  padding: 16px;
  color: var(--muted);
  text-align: center;
}
.article-cover-placeholder strong {
  color: var(--ink);
  font-size: 14px;
}
.article-cover-placeholder span {
  font-size: 12px;
}
.article-cover-progress {
  position: absolute;
  inset: 0;
  display: flex;
  gap: 8px;
  align-items: center;
  justify-content: center;
  color: #fff;
  background: #1119;
  font-size: 14px;
}
.article-cover-progress svg {
  animation: cover-spin 1.2s linear infinite;
}
.article-cover-actions {
  justify-content: flex-start;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}
.article-cover-actions button {
  min-height: 36px;
  padding: 7px 12px;
  font-size: 12px;
}
.article-cover-error {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-top: 10px;
  color: var(--danger, #b73b35);
  font-size: 12px;
}
.article-cover-error button {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: inherit;
  text-decoration: underline;
}
.article-cover-address {
  margin-top: 12px;
  color: var(--muted);
  font-size: 12px;
}
.article-cover-address summary {
  width: fit-content;
  cursor: pointer;
}
.article-cover-address label {
  margin-top: 10px;
}
@keyframes cover-spin {
  to {
    transform: rotate(360deg);
  }
}
@media (prefers-reduced-motion: reduce) {
  .article-cover-progress svg {
    animation: none;
  }
}
@media (max-width: 760px) {
  .article-details {
    grid-template-columns: 1fr;
    gap: 24px;
    padding: 22px 20px;
  }
  .article-details-heading {
    align-items: flex-start;
    flex-direction: column;
    gap: 5px;
    margin-bottom: 20px;
  }
  .article-cover-field {
    padding-top: 20px;
    border-top: 1px solid var(--line);
  }
}
</style>
