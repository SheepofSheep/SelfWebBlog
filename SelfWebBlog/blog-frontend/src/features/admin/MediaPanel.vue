<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { Copy, Grid2X2, List, ScanSearch, Trash2, Upload } from 'lucide-vue-next'
import { api } from '@/api/client.js'
import {
  createAdminRequestGate,
  formatBytes,
  mergeAssetReferenceDetails
} from '@/core/adminModel.js'
import { formatDate } from '@/core/format.js'
import { notify } from '@/stores/app.js'
import AppDialog from '@/components/AppDialog.vue'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import AdminPager from './AdminPager.vue'
import AdminToolbar from './AdminToolbar.vue'

const result = ref({ records: [], current: 1, pages: 1, total: 0 })
const page = ref(1)
const keyword = ref('')
const route = useRoute()
const status = ref(
  ['REFERENCED', 'ORPHANED'].includes(route.query.status) ? route.query.status : ''
)
const purpose = ref('')
const view = ref('grid')
const loading = ref(false)
const busy = ref(false)
const preview = ref(null)
const reference = ref(null)
const removing = ref(null)
const selected = ref(new Set())
const uploadInput = ref(null)
const requestGate = createAdminRequestGate()
let referenceRequest = 0
const records = computed(() => result.value.records || [])
const selectableIds = computed(() =>
  records.value
    .filter((asset) => asset.referenceStatus === 'ORPHANED')
    .map((asset) => String(asset.id))
)
const allSelectableSelected = computed(
  () => selectableIds.value.length > 0 && selectableIds.value.every((id) => selected.value.has(id))
)
const removalCount = computed(() => removing.value?.ids?.length || (removing.value ? 1 : 0))

async function load(nextPage = page.value) {
  const ticket = requestGate.begin()
  loading.value = true
  try {
    const value = await api.assets({
      page: nextPage,
      size: 24,
      status: status.value,
      purpose: purpose.value,
      keyword: keyword.value.trim()
    })
    if (!requestGate.isCurrent(ticket)) return false
    result.value = value
    page.value = Number(value.current || nextPage)
    selected.value = new Set()
    return true
  } catch (error) {
    if (requestGate.isCurrent(ticket)) notify(error.message)
    return false
  } finally {
    requestGate.runIfCurrent(ticket, () => {
      loading.value = false
    })
  }
}

async function reloadAfterMutation() {
  const applied = await load(page.value)
  if (!applied) return
  if (!(result.value.records || []).length && page.value > 1) {
    await load(Math.max(1, Math.min(page.value - 1, Number(result.value.pages || 1))))
  }
}

async function scan() {
  busy.value = true
  try {
    const summary = await api.scanAssets()
    notify(`扫描完成：${summary.active || 0} 个使用中，${summary.orphaned || 0} 个未引用`)
    await load(1)
  } catch (error) {
    notify(error.message)
  } finally {
    busy.value = false
  }
}

async function openPreview(asset) {
  const request = ++referenceRequest
  preview.value = asset
  reference.value = null
  try {
    const value = await api.assetReferences(asset.id)
    if (request === referenceRequest && String(preview.value?.id) === String(asset.id)) {
      preview.value = mergeAssetReferenceDetails(preview.value, value)
      reference.value = mergeAssetReferenceDetails(value, preview.value)
      result.value = {
        ...result.value,
        records: (result.value.records || []).map((item) =>
          String(item.id) === String(asset.id) ? mergeAssetReferenceDetails(item, value) : item
        )
      }
      if (preview.value.referenceStatus !== 'ORPHANED') {
        selected.value = new Set(
          [...selected.value].filter((id) => String(id) !== String(asset.id))
        )
      }
    }
  } catch (error) {
    if (request === referenceRequest) notify(error.message)
  }
}

function closePreview() {
  referenceRequest += 1
  preview.value = null
  reference.value = null
}

async function copy(url) {
  try {
    await navigator.clipboard.writeText(url)
    notify('地址已复制')
  } catch {
    notify('复制失败，请手动复制')
  }
}

async function upload(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  busy.value = true
  try {
    await api.upload(file)
    notify('图片已上传')
    await load(1)
  } catch (error) {
    notify(error.message)
  } finally {
    busy.value = false
  }
}

function toggle(assetId) {
  const next = new Set(selected.value)
  const id = String(assetId)
  next.has(id) ? next.delete(id) : next.add(id)
  selected.value = next
}

function toggleAll() {
  selected.value = allSelectableSelected.value ? new Set() : new Set(selectableIds.value)
}

function requestBatchRemove() {
  if (!selected.value.size) return
  removing.value = { ids: [...selected.value] }
}

async function remove() {
  if (!removing.value) return
  busy.value = true
  try {
    const ids = removing.value.ids || [String(removing.value.id)]
    if (ids.length === 1) await api.deleteAsset(ids[0])
    else await api.deleteAssets(ids)
    closePreview()
    removing.value = null
    selected.value = new Set()
    notify(ids.length > 1 ? '已删除 ' + ids.length + ' 个未引用媒体' : '未引用媒体已删除')
    await reloadAfterMutation()
  } catch (error) {
    notify(error.message)
  } finally {
    busy.value = false
  }
}

onMounted(load)
</script>

<template>
  <section class="media-panel">
    <AdminToolbar
      v-model="keyword"
      placeholder="搜索文件地址或内容摘要"
      :busy="loading || busy"
      @submit="load(1)"
      @refresh="load()"
    >
      <select v-model="status" aria-label="引用状态" @change="load(1)">
        <option value="">全部状态</option>
        <option value="ACTIVE">使用中</option>
        <option value="ORPHANED">未引用</option>
      </select>
      <select v-model="purpose" aria-label="媒体用途" @change="load(1)">
        <option value="">全部用途</option>
        <option value="ARTICLE">文章图片</option>
      </select>
      <template #actions>
        <button class="secondary-button compact" type="button" :disabled="busy" @click="scan">
          <ScanSearch :size="16" />扫描引用
        </button>
        <button
          class="primary-button compact"
          type="button"
          :disabled="busy"
          @click="uploadInput.click()"
        >
          <Upload :size="16" />上传
        </button>
        <input
          ref="uploadInput"
          class="visually-hidden"
          type="file"
          accept="image/*"
          tabindex="-1"
          aria-hidden="true"
          @change="upload"
        />
        <div class="view-switch">
          <button
            type="button"
            :class="{ active: view === 'grid' }"
            aria-label="网格显示"
            :aria-pressed="view === 'grid'"
            @click="view = 'grid'"
          >
            <Grid2X2 :size="16" /></button
          ><button
            type="button"
            :class="{ active: view === 'list' }"
            aria-label="列表显示"
            :aria-pressed="view === 'list'"
            @click="view = 'list'"
          >
            <List :size="16" />
          </button>
        </div>
      </template>
    </AdminToolbar>

    <div v-if="selectableIds.length" class="media-batch glass">
      <label>
        <input
          type="checkbox"
          :checked="allSelectableSelected"
          aria-label="选择本页全部未引用媒体"
          @change="toggleAll"
        />
        本页未引用 {{ selectableIds.length }} 个
      </label>
      <button
        v-if="selected.size"
        class="danger-text-button"
        type="button"
        @click="requestBatchRemove"
      >
        <Trash2 :size="16" />删除所选 {{ selected.size }} 个
      </button>
    </div>

    <div :class="['media-collection', view]" :aria-busy="loading">
      <article v-for="asset in result.records || []" :key="asset.id" class="media-card glass">
        <label v-if="asset.referenceStatus === 'ORPHANED'" class="asset-select" @click.stop>
          <input
            type="checkbox"
            :checked="selected.has(String(asset.id))"
            :aria-label="'选择媒体 ' + asset.id"
            @change="toggle(asset.id)"
          />
        </label>
        <button
          class="media-preview"
          type="button"
          :aria-label="`查看媒体 ${asset.id}`"
          @click="openPreview(asset)"
        >
          <img
            :src="asset.smallWebpUrl || asset.originalUrl"
            :alt="`媒体 ${asset.id}`"
            loading="lazy"
          />
        </button>
        <div class="media-meta">
          <div>
            <strong>{{ asset.width }} × {{ asset.height }}</strong
            ><span>{{ formatBytes(asset.byteSize) }} · {{ asset.purpose }}</span
            ><span
              >上传者 {{ asset.uploaderId ? '#' + asset.uploaderId : '未知' }} · 引用
              {{ asset.referenceCount ?? '待扫描' }}</span
            ><time>{{ formatDate(asset.createTime) }}</time>
          </div>
          <span :class="['reference-chip', asset.referenceStatus?.toLowerCase()]">{{
            asset.referenceStatus === 'ORPHANED' ? '未引用' : '使用中'
          }}</span>
          <button
            class="icon-button"
            type="button"
            aria-label="复制地址"
            @click="copy(asset.originalUrl)"
          >
            <Copy :size="15" />
          </button>
        </div>
      </article>
      <p v-if="!loading && !(result.records || []).length" class="media-empty">
        没有符合条件的媒体
      </p>
    </div>
    <AdminPager
      :page="page"
      :pages="Number(result.pages || 1)"
      :total="Number(result.total || 0)"
      :busy="loading"
      @change="load"
    />

    <AppDialog :open="Boolean(preview)" title="媒体详情" size="large" @close="closePreview">
      <div v-if="preview" class="asset-detail">
        <div class="asset-image">
          <img :src="preview.largeWebpUrl || preview.originalUrl" :alt="`媒体 ${preview.id}`" />
        </div>
        <div class="asset-info">
          <dl>
            <div>
              <dt>尺寸</dt>
              <dd>{{ preview.width }} × {{ preview.height }}</dd>
            </div>
            <div>
              <dt>大小</dt>
              <dd>{{ formatBytes(preview.byteSize) }}</dd>
            </div>
            <div>
              <dt>用途</dt>
              <dd>{{ preview.purpose }}</dd>
            </div>
            <div>
              <dt>上传者</dt>
              <dd>{{ preview.uploaderId ? '#' + preview.uploaderId : '未知' }}</dd>
            </div>
            <div>
              <dt>状态</dt>
              <dd>{{ preview.referenceStatus === 'ORPHANED' ? '未引用' : '使用中' }}</dd>
            </div>
          </dl>
          <label
            >原图地址
            <div class="copy-field">
              <input :value="preview.originalUrl" readonly /><button
                type="button"
                aria-label="复制原图地址"
                @click="copy(preview.originalUrl)"
              >
                <Copy :size="15" />
              </button></div
          ></label>
          <div class="reference-list">
            <strong>引用位置 {{ reference?.referenceCount ?? '…' }}</strong>
            <article
              v-for="item in reference?.references || []"
              :key="`${item.sourceType}-${item.targetId}-${item.field}`"
            >
              <RouterLink
                v-if="item.sourceType === 'POST' && item.targetId"
                class="reference-link"
                :to="`/write/${item.targetId}`"
              >
                <span>{{ item.label || item.sourceType }}</span>
                <small>{{ item.sourceType }} · {{ item.field }}</small>
              </RouterLink>
              <div v-else>
                <span>{{ item.label || item.sourceType }}</span>
                <small>{{ item.sourceType }} · {{ item.field }}</small>
              </div>
            </article>
            <p v-if="reference && !reference.references.length">当前没有引用</p>
          </div>
          <button
            v-if="preview.referenceStatus === 'ORPHANED'"
            class="danger-action"
            type="button"
            @click="removing = preview"
          >
            <Trash2 :size="16" />删除媒体
          </button>
        </div>
      </div>
    </AppDialog>
    <ConfirmDialog
      :open="Boolean(removing)"
      :title="removalCount > 1 ? '批量删除未引用媒体' : '删除未引用媒体'"
      :message="
        removalCount > 1
          ? '将删除所选 ' + removalCount + ' 个文件与媒体记录，此操作无法恢复。'
          : '文件与媒体记录都会删除，此操作无法恢复。'
      "
      confirm-label="删除"
      danger
      :busy="busy"
      @close="!busy && (removing = null)"
      @confirm="remove"
    />
  </section>
</template>

<style scoped>
.media-panel select {
  padding: 8px 10px;
  border: 1px solid var(--line);
  border-radius: 10px;
  color: var(--ink);
  background: var(--glass-strong);
}
.compact {
  display: inline-flex;
  gap: 6px;
  align-items: center;
  padding: 8px 11px;
  white-space: nowrap;
}
.media-batch {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
  padding: 10px 13px;
  margin-bottom: 10px;
  border-radius: 15px;
}
.media-batch label {
  display: inline-flex;
  gap: 8px;
  align-items: center;
  color: var(--muted);
  font-size: 12px;
}
.media-batch button {
  display: inline-flex;
  gap: 6px;
  align-items: center;
}
.view-switch {
  display: flex;
  padding: 3px;
  border-radius: 10px;
  background: var(--accent-soft);
}
.view-switch button {
  display: grid;
  padding: 7px;
  place-items: center;
  border: 0;
  border-radius: 7px;
  color: var(--muted);
  background: transparent;
}
.view-switch button.active {
  color: var(--accent);
  background: var(--glass-strong);
}
.media-collection.grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}
.media-card {
  position: relative;
  min-width: 0;
  overflow: hidden;
  border-radius: 20px;
}
.asset-select {
  position: absolute;
  z-index: 2;
  top: 9px;
  left: 9px;
  display: grid;
  width: 26px;
  height: 26px;
  border: 1px solid rgb(255 255 255 / 75%);
  border-radius: 9px;
  place-items: center;
  background: rgb(247 251 255 / 88%);
  box-shadow: 0 5px 14px rgb(20 62 105 / 16%);
  backdrop-filter: blur(12px);
}
.asset-select input {
  margin: 0;
}
.media-preview {
  display: block;
  width: 100%;
  padding: 0;
  border: 0;
  background: var(--accent-soft);
}
.media-preview img {
  display: block;
  width: 100%;
  aspect-ratio: 1;
  object-fit: cover;
}
.media-meta {
  display: flex;
  gap: 8px;
  align-items: center;
  padding: 11px;
}
.media-meta > div {
  display: grid;
  flex: 1;
  min-width: 0;
  gap: 2px;
}
.media-meta strong,
.media-meta span,
.media-meta time {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.media-meta span,
.media-meta time {
  color: var(--muted);
  font-size: 11px;
}
.media-meta .reference-chip {
  flex: 0 0 auto;
  padding: 4px 6px;
  border-radius: 7px;
  color: #25815c;
  background: rgb(40 168 121 / 12%);
}
.media-meta .reference-chip.orphaned {
  color: #ad7318;
  background: rgb(214 151 50 / 14%);
}
.media-collection.list {
  display: grid;
  gap: 8px;
}
.media-collection.list .media-card {
  display: grid;
  grid-template-columns: 92px minmax(0, 1fr);
}
.media-collection.list .media-preview img {
  height: 84px;
  aspect-ratio: auto;
}
.media-empty {
  grid-column: 1 / -1;
  padding: 50px;
  color: var(--muted);
  text-align: center;
}
.asset-detail {
  display: grid;
  grid-template-columns: minmax(0, 1.25fr) minmax(260px, 0.75fr);
  gap: 20px;
}
.asset-image {
  display: grid;
  min-height: 360px;
  overflow: hidden;
  place-items: center;
  border-radius: 20px;
  background: var(--accent-soft);
}
.asset-image img {
  max-width: 100%;
  max-height: 60vh;
  object-fit: contain;
}
.asset-info {
  display: grid;
  align-content: start;
  gap: 16px;
}
.asset-info dl {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  margin: 0;
}
.asset-info dl div {
  display: grid;
  gap: 2px;
  padding: 11px;
  border-radius: 12px;
  background: var(--accent-soft);
}
.asset-info dt,
.asset-info label,
.reference-list small {
  color: var(--muted);
  font-size: 11px;
}
.asset-info dd {
  margin: 0;
  font-weight: 700;
}
.copy-field {
  display: flex;
  margin-top: 6px;
  overflow: hidden;
  border: 1px solid var(--line);
  border-radius: 11px;
}
.copy-field input {
  flex: 1;
  min-width: 0;
  padding: 9px;
  border: 0;
  color: var(--ink);
  background: transparent;
}
.copy-field button {
  display: grid;
  border: 0;
  place-items: center;
  color: var(--accent);
  background: var(--accent-soft);
}
.reference-list {
  display: grid;
  gap: 7px;
}
.reference-list article {
  padding: 9px;
  border-radius: 10px;
  background: var(--accent-soft);
}
.reference-list article > div,
.reference-link {
  display: grid;
  gap: 2px;
}
.reference-link {
  color: var(--ink);
  text-decoration: none;
}
.reference-link:hover span,
.reference-link:focus-visible span {
  color: var(--accent);
}
.reference-list p {
  color: var(--muted);
  font-size: 12px;
}
.danger-action {
  display: inline-flex;
  gap: 7px;
  align-items: center;
  width: max-content;
  padding: 10px 13px;
  border: 0;
  border-radius: 11px;
  color: var(--danger);
  background: color-mix(in srgb, var(--danger) 9%, transparent);
}
.visually-hidden {
  position: absolute;
  width: 1px;
  height: 1px;
  overflow: hidden;
  clip: rect(0 0 0 0);
}
@media (max-width: 1000px) {
  .media-collection.grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}
@media (max-width: 720px) {
  .media-collection.grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .asset-detail {
    grid-template-columns: 1fr;
  }
  .asset-image {
    min-height: 240px;
  }
}
</style>
