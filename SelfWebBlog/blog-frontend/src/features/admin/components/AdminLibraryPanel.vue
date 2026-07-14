<script setup>
import { computed, inject, onMounted, ref } from 'vue'
import { Image, Merge, RefreshCw, Save, Trash2 } from 'lucide-vue-next'
import { createTag, deleteTagFromAdmin, listTags, mergeTags, renameTag } from '../../../api/tags'
import { deleteAdminAsset, listAdminAssets, scanAdminAssets } from '../../../api/admin'
import { toAbsoluteUrl } from '../../../utils/url'
import AdminSectionHeader from './AdminSectionHeader.vue'

const confirmAction = inject('adminConfirm')
const tab = ref('tags')
const tags = ref([])
const assets = ref([])
const newTag = ref('')
const sourceId = ref('')
const targetId = ref('')
const loading = ref(true)
const orphanCount = computed(
  () => assets.value.filter((asset) => asset.referenceStatus === 'ORPHANED').length
)

async function loadTags() {
  tags.value = (await listTags()).map((tag) => ({ ...tag, editName: tag.name }))
}
async function loadAssets() {
  assets.value = await listAdminAssets()
}
async function load() {
  loading.value = true
  try {
    await Promise.all([loadTags(), loadAssets()])
  } finally {
    loading.value = false
  }
}
async function addTag() {
  if (!newTag.value.trim()) return
  await createTag(newTag.value.trim())
  newTag.value = ''
  await loadTags()
}
async function saveTag(tag) {
  await renameTag(tag.id, tag.editName)
  await loadTags()
}
async function removeTag(tag) {
  if (
    !(await confirmAction({
      title: '删除标签',
      message: `删除「${tag.name}」后，文章不会被删除。`,
      confirmText: '删除'
    }))
  )
    return
  await deleteTagFromAdmin(tag.id)
  await loadTags()
}
async function merge() {
  if (!sourceId.value || !targetId.value || sourceId.value === targetId.value) return
  if (
    !(await confirmAction({
      title: '合并标签',
      message: '来源标签会被删除，关联文章将统一转到目标标签。',
      confirmText: '合并'
    }))
  )
    return
  await mergeTags(sourceId.value, targetId.value)
  sourceId.value = ''
  targetId.value = ''
  await loadTags()
}
async function scan() {
  await scanAdminAssets()
  await loadAssets()
}
async function removeAsset(asset) {
  if (
    !(await confirmAction({
      title: '删除孤立图片',
      message: '原图和响应式副本会从本机存储中永久删除。',
      confirmText: '删除图片'
    }))
  )
    return
  await deleteAdminAsset(asset.id)
  await loadAssets()
}
onMounted(load)
</script>

<template>
  <div>
    <AdminSectionHeader title="标签与图片" description="整理内容词汇，并确认未引用图片后再清理。">
      <div class="admin-segments">
        <button :class="{ active: tab === 'tags' }" @click="tab = 'tags'">标签</button
        ><button :class="{ active: tab === 'assets' }" @click="tab = 'assets'">图片</button>
      </div>
    </AdminSectionHeader>
    <div v-if="loading" class="admin-loading">正在读取内容库</div>
    <template v-else-if="tab === 'tags'">
      <div class="admin-toolbar admin-tag-tools">
        <input
          v-model="newTag"
          class="admin-input"
          placeholder="新标签名称"
          @keyup.enter="addTag"
        />
        <button class="admin-button primary" type="button" @click="addTag">创建标签</button>
        <select v-model="sourceId" class="admin-select">
          <option value="">来源标签</option>
          <option v-for="tag in tags" :key="tag.id" :value="tag.id">{{ tag.name }}</option>
        </select>
        <select v-model="targetId" class="admin-select">
          <option value="">目标标签</option>
          <option v-for="tag in tags" :key="tag.id" :value="tag.id">{{ tag.name }}</option>
        </select>
        <button class="admin-button secondary" type="button" @click="merge">
          <Merge :size="16" />合并
        </button>
      </div>
      <div class="admin-table-list">
        <div v-for="tag in tags" :key="tag.id" class="admin-tag-row">
          <input v-model="tag.editName" class="admin-input" />
          <span>{{ tag.postCount }} 篇文章</span>
          <small>{{ tag.slug }}</small>
          <div class="admin-row-actions">
            <button title="保存" aria-label="保存" @click="saveTag(tag)"><Save :size="15" /></button
            ><button class="danger" title="删除" aria-label="删除" @click="removeTag(tag)">
              <Trash2 :size="15" />
            </button>
          </div>
        </div>
      </div>
    </template>
    <template v-else>
      <div class="admin-toolbar">
        <span class="admin-toolbar-note"
          >{{ assets.length }} 个资产，{{ orphanCount }} 个未引用</span
        ><button class="admin-button secondary" type="button" @click="scan">
          <RefreshCw :size="16" />扫描引用
        </button>
      </div>
      <div class="admin-asset-grid">
        <article v-for="asset in assets" :key="asset.id">
          <div class="admin-asset-preview">
            <img
              v-if="asset.smallWebpUrl || asset.originalUrl"
              :src="toAbsoluteUrl(asset.smallWebpUrl || asset.originalUrl)"
              alt=""
              loading="lazy"
            /><Image v-else :size="24" />
          </div>
          <div>
            <strong>{{ asset.width }} × {{ asset.height }}</strong
            ><span>{{ Math.round(asset.byteSize / 1024) }} KB</span>
          </div>
          <span :class="['admin-status', asset.referenceStatus.toLowerCase()]">{{
            asset.referenceStatus === 'ORPHANED' ? '未引用' : '使用中'
          }}</span>
          <button
            v-if="asset.referenceStatus === 'ORPHANED'"
            class="admin-icon-danger"
            type="button"
            title="删除图片"
            aria-label="删除图片"
            @click="removeAsset(asset)"
          >
            <Trash2 :size="15" />
          </button>
        </article>
      </div>
    </template>
  </div>
</template>
