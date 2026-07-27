<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { Pencil, Trash2 } from 'lucide-vue-next'
import { api } from '@/api/client.js'
import { createAdminRequestGate } from '@/core/adminModel.js'
import { prepareTagCreation } from '@/core/tagModel.js'
import { notify } from '@/stores/app.js'
import FormDialog from '@/components/FormDialog.vue'
import ConfirmDialog from '@/components/ConfirmDialog.vue'

const tags = ref([])
const draftName = ref('')
const loading = ref(false)
const busy = ref(false)
const editing = ref(null)
const editName = ref('')
const removing = ref(null)
const requestGate = createAdminRequestGate()
const controlsLocked = computed(() => loading.value || busy.value)

async function load() {
  const ticket = requestGate.begin()
  loading.value = true
  try {
    const rows = await api.tags()
    if (requestGate.isCurrent(ticket)) tags.value = Array.isArray(rows) ? rows : []
  } catch (error) {
    if (requestGate.isCurrent(ticket)) notify(error.message)
  } finally {
    requestGate.runIfCurrent(ticket, () => {
      loading.value = false
    })
  }
}

async function add(event) {
  if (event?.isComposing || controlsLocked.value) return
  const prepared = prepareTagCreation(tags.value, draftName.value)
  if (!prepared.name) {
    if (draftName.value.trim()) notify('标签名称需要 1 到 30 个字符')
    return
  }
  draftName.value = ''
  if (!prepared.shouldCreate) {
    notify(`“${prepared.name}”已存在，可直接复用`)
    return
  }
  busy.value = true
  try {
    await api.createTag(prepared.name)
    notify('标签已添加')
    await load()
  } catch (error) {
    notify(error.message)
  } finally {
    busy.value = false
  }
}

function openEdit(tag) {
  if (controlsLocked.value) return
  editing.value = tag
  editName.value = tag.name
}

async function saveEdit() {
  const nextName = editName.value.trim()
  if (!editing.value || controlsLocked.value || !nextName || nextName.length > 30) return
  busy.value = true
  try {
    await api.renameTag(editing.value.id, nextName)
    editing.value = null
    notify('标签已更新')
    await load()
  } catch (error) {
    notify(error.message)
  } finally {
    busy.value = false
  }
}

async function remove() {
  if (!removing.value || controlsLocked.value) return
  busy.value = true
  try {
    await api.deleteTag(removing.value.id)
    removing.value = null
    notify('标签已删除')
    await load()
  } catch (error) {
    notify(error.message)
  } finally {
    busy.value = false
  }
}

function openRemove(tag) {
  if (!controlsLocked.value) removing.value = tag
}

onMounted(load)
onBeforeUnmount(() => requestGate.begin())
</script>

<template>
  <section class="tags-panel glass" :aria-busy="loading">
    <div class="tag-create">
      <div>
        <h2>标签</h2>
        <p>输入任意文字，按回车生成；已有标签可直接复用。</p>
      </div>
      <div class="tag-create-control">
        <input
          v-model="draftName"
          type="text"
          maxlength="30"
          placeholder="输入标签后按回车"
          aria-label="新标签名称"
          :disabled="controlsLocked"
          @keydown.enter.prevent="add"
        />
      </div>
    </div>
    <div class="tag-rows">
      <article v-for="tag in tags" :key="tag.id">
        <div>
          <strong>{{ tag.name }}</strong
          ><span>{{ tag.postCount || 0 }} 篇文章</span>
        </div>
        <div>
          <button
            class="icon-button"
            type="button"
            :disabled="controlsLocked"
            aria-label="重命名标签"
            @click="openEdit(tag)"
          >
            <Pencil :size="16" />
          </button>
          <button
            class="icon-button danger"
            type="button"
            :disabled="controlsLocked"
            aria-label="删除标签"
            @click="openRemove(tag)"
          >
            <Trash2 :size="16" />
          </button>
        </div>
      </article>
      <p v-if="!loading && !tags.length" class="admin-empty">还没有标签</p>
    </div>

    <FormDialog
      :open="Boolean(editing)"
      title="修改标签"
      :busy="controlsLocked"
      :valid="Boolean(editName.trim()) && editName.trim().length <= 30"
      @close="!controlsLocked && (editing = null)"
      @submit="saveEdit"
    >
      <label class="dialog-field"
        >标签名称<input v-model="editName" autofocus maxlength="30"
      /></label>
    </FormDialog>
    <ConfirmDialog
      :open="Boolean(removing)"
      title="删除标签"
      :message="`确定删除“${removing?.name || ''}”？正在使用的标签需先从文章中移除。`"
      confirm-label="删除"
      danger
      :busy="controlsLocked"
      @close="!controlsLocked && (removing = null)"
      @confirm="remove"
    />
  </section>
</template>

<style scoped>
.tags-panel {
  padding: 22px;
  border-radius: 26px;
}
.tag-create {
  display: grid;
  grid-template-columns: minmax(180px, 0.55fr) 1fr;
  gap: 24px;
  padding-bottom: 22px;
  border-bottom: 1px solid var(--line);
}
.tag-create h2 {
  margin: 0;
}
.tag-create p {
  margin: 7px 0 0;
  color: var(--muted);
  font-size: 12px;
  line-height: 1.6;
}
.tag-create-control {
  display: block;
}
.tag-create-control input {
  width: 100%;
  padding: 13px 15px;
  border: 1px solid var(--line);
  border-radius: 15px;
  color: var(--ink);
  background: var(--glass-strong);
  outline: 0;
  transition:
    border-color 180ms ease,
    box-shadow 180ms ease;
}
.tag-create-control input:focus {
  border-color: color-mix(in srgb, var(--accent) 55%, var(--line));
  box-shadow: 0 0 0 4px var(--accent-soft);
}
.tag-rows {
  display: grid;
}
.tag-rows article {
  display: flex;
  gap: 14px;
  align-items: center;
  justify-content: space-between;
  padding: 14px 3px;
  border-bottom: 1px solid var(--line);
}
.tag-rows article > div:first-child {
  display: grid;
  gap: 3px;
}
.tag-rows article > div:last-child {
  display: flex;
}
.tag-rows span {
  color: var(--muted);
  font-size: 11px;
}
.admin-empty {
  padding: 42px 0;
  color: var(--muted);
  text-align: center;
}
.dialog-field {
  display: grid;
  gap: 7px;
  color: var(--muted);
  font-size: 12px;
}
.dialog-field input {
  padding: 11px 12px;
  border: 1px solid var(--line);
  border-radius: 12px;
  color: var(--ink);
  background: var(--glass-strong);
  outline: 0;
}
@media (max-width: 720px) {
  .tag-create {
    grid-template-columns: 1fr;
  }
}
</style>
