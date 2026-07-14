<script setup>
import { ImagePlus, LoaderCircle, X } from 'lucide-vue-next'
import { computed, ref } from 'vue'
import { uploadImage } from '../../../api/uploads'
import { showToast } from '../../../composables/toast'
import { toAbsoluteUrl } from '../../../utils/url'

const props = defineProps({
  modelValue: { type: Object, required: true },
  tagOptions: { type: Array, default: () => [] },
  categories: { type: Array, default: () => [] }
})
const emit = defineEmits(['update:modelValue'])
const coverInput = ref(null)
const uploading = ref(false)
const tagList = computed(() =>
  String(props.modelValue.tags || '')
    .split(/[,，\n]+/)
    .map((item) => item.trim())
    .filter(Boolean)
)

function patch(field, value) {
  emit('update:modelValue', { ...props.modelValue, [field]: value })
}

function addTag(name) {
  if (tagList.value.some((tag) => tag.toLocaleLowerCase() === name.toLocaleLowerCase())) return
  patch('tags', [...tagList.value, name].join(', '))
}

function removeTag(name) {
  patch('tags', tagList.value.filter((tag) => tag !== name).join(', '))
}

async function uploadCover(event) {
  const file = event.target.files?.[0]
  if (!file) return
  uploading.value = true
  try {
    const asset = await uploadImage(file)
    patch('coverUrl', asset.originalUrl || String(asset))
  } catch (error) {
    showToast(error.message || '封面上传失败。', 'error')
  } finally {
    uploading.value = false
    event.target.value = ''
  }
}
</script>

<template>
  <aside class="settings-panel">
    <section>
      <h2>发布设置</h2>
      <label>
        <span>状态</span>
        <select :value="modelValue.status" @change="patch('status', $event.target.value)">
          <option value="PUBLISHED">公开发布</option>
          <option value="DRAFT">保存草稿</option>
        </select>
      </label>
      <label>
        <span>摘要</span>
        <textarea
          :value="modelValue.summary"
          rows="4"
          maxlength="240"
          @input="patch('summary', $event.target.value)"
        />
      </label>
      <label>
        <span>分类</span>
        <input
          :value="modelValue.category"
          list="post-categories"
          maxlength="30"
          @input="patch('category', $event.target.value)"
        />
        <datalist id="post-categories">
          <option v-for="item in categories" :key="item" :value="item" />
        </datalist>
      </label>
      <label>
        <span>标签</span>
        <input
          :value="modelValue.tags"
          placeholder="使用逗号分隔"
          @input="patch('tags', $event.target.value)"
        />
      </label>
      <div v-if="tagList.length" class="selected-tags">
        <button v-for="tag in tagList" :key="tag" type="button" @click="removeTag(tag)">
          {{ tag }} <X :size="12" />
        </button>
      </div>
      <div v-if="tagOptions.length" class="tag-suggestions">
        <button
          v-for="tag in tagOptions.slice(0, 12)"
          :key="tag.id || tag.name"
          type="button"
          @click="addTag(tag.name)"
        >
          {{ tag.name }}
        </button>
      </div>
    </section>

    <section>
      <h2>封面</h2>
      <img
        v-if="modelValue.coverUrl"
        class="cover-preview"
        :src="toAbsoluteUrl(modelValue.coverUrl)"
        alt="文章封面预览"
      />
      <button class="cover-action" type="button" :disabled="uploading" @click="coverInput?.click()">
        <LoaderCircle v-if="uploading" :size="16" class="spin" />
        <ImagePlus v-else :size="16" />
        {{ uploading ? '上传中' : '选择封面' }}
      </button>
      <input ref="coverInput" class="file-input" type="file" accept="image/*" @change="uploadCover" />
      <label>
        <span>或使用图片地址</span>
        <input
          :value="modelValue.coverUrl"
          placeholder="/uploads/cover.webp"
          @input="patch('coverUrl', $event.target.value)"
        />
      </label>
    </section>
  </aside>
</template>

<style scoped>
.settings-panel {
  display: grid;
  align-content: start;
  gap: 20px;
}
section {
  display: grid;
  gap: 13px;
  padding: 16px;
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-card);
  background: var(--surface-solid);
}
h2 {
  margin: 0 0 2px;
  font-size: 0.9rem;
}
label {
  display: grid;
  gap: 6px;
  color: var(--text-secondary);
  font-size: 0.73rem;
}
input,
textarea,
select {
  width: 100%;
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-control);
  background: var(--surface-raised);
  color: var(--text-primary);
  font: inherit;
}
input,
select {
  height: 39px;
  padding: 0 10px;
}
textarea {
  padding: 10px;
  resize: vertical;
  line-height: 1.6;
}
input:focus,
textarea:focus,
select:focus {
  border-color: var(--accent);
  box-shadow: 0 0 0 3px var(--focus-ring);
  outline: none;
}
.selected-tags,
.tag-suggestions {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.selected-tags button,
.tag-suggestions button {
  min-height: 28px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 0 8px;
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-control);
  background: var(--surface-soft);
  color: var(--text-secondary);
  font: inherit;
  font-size: 0.68rem;
  cursor: pointer;
}
.tag-suggestions button {
  background: transparent;
}
.cover-preview {
  width: 100%;
  aspect-ratio: 16 / 9;
  border-radius: var(--radius-control);
  object-fit: cover;
}
.cover-action {
  min-height: 38px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-control);
  background: var(--surface-soft);
  color: var(--text-primary);
  font: inherit;
  cursor: pointer;
}
.file-input {
  display: none;
}
.spin {
  animation: spin 0.8s linear infinite;
}
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
