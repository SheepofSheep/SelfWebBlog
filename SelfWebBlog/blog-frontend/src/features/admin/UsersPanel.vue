<script setup>
import { onMounted, ref } from 'vue'
import { AlertTriangle, Pencil, Trash2 } from 'lucide-vue-next'
import { api } from '@/api/client.js'
import {
  createAdminRequestGate,
  normalizeAdminUserPage,
  pageAfterMutation
} from '@/core/adminModel.js'
import { formatDate } from '@/core/format.js'
import { appStore, notify } from '@/stores/app.js'
import AppAvatar from '@/components/AppAvatar.vue'
import FormDialog from '@/components/FormDialog.vue'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import AdminPager from './AdminPager.vue'
import AdminToolbar from './AdminToolbar.vue'

const users = ref([])
const summary = ref({ total: 0, current: 1, pages: 1, size: 20, duplicateIpCount: 0 })
const page = ref(1)
const query = ref('')
const role = ref('')
const title = ref('')
const loading = ref(false)
const busy = ref(false)
const editing = ref(null)
const titleName = ref('')
const titleStyle = ref('default')
const removing = ref(null)
const requestGate = createAdminRequestGate()
const titleStyles = [
  ['default', '默认'],
  ['blue', '蓝色'],
  ['green', '绿色'],
  ['gold', '金色'],
  ['rose', '玫红']
]

async function load(nextPage = page.value) {
  const ticket = requestGate.begin()
  loading.value = true
  try {
    const value = await api.users({
      keyword: query.value.trim(),
      role: role.value,
      title: title.value.trim(),
      page: nextPage,
      size: 20
    })
    if (!requestGate.isCurrent(ticket)) return false
    const normalized = normalizeAdminUserPage(value, nextPage, 20)
    users.value = normalized.records
    summary.value = normalized
    page.value = normalized.current
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

function changeFilter() {
  page.value = 1
  load(1)
}

async function reloadAfterMutation() {
  const previousPage = page.value
  if (!(await load(previousPage))) return
  const nextPage = pageAfterMutation(previousPage, users.value)
  if (nextPage !== previousPage) await load(nextPage)
}

function openTitle(user) {
  editing.value = user
  titleName.value = user.titleName || ''
  titleStyle.value = user.titleStyle || 'default'
}

async function saveTitle() {
  if (!editing.value) return
  busy.value = true
  try {
    const editingId = editing.value.id
    const updated = await api.updateUserTitle(editingId, {
      titleName: titleName.value.trim(),
      titleStyle: titleName.value.trim() ? titleStyle.value : 'default'
    })
    Object.assign(editing.value, updated)
    if (String(appStore.user?.id) === String(editingId))
      appStore.user = { ...appStore.user, ...updated }
    editing.value = null
    notify('称号已更新，历史评论会同步显示')
    await reloadAfterMutation()
  } catch (error) {
    notify(error.message)
  } finally {
    busy.value = false
  }
}

async function remove() {
  if (!removing.value) return
  busy.value = true
  try {
    await api.deleteUser(removing.value.id)
    removing.value = null
    notify('用户已删除')
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
  <section class="users-panel">
    <AdminToolbar
      v-model="query"
      placeholder="搜索昵称、账号、邮箱或称号"
      :busy="loading"
      @submit="changeFilter"
      @refresh="load()"
    >
      <select v-model="role" aria-label="用户角色" @change="changeFilter">
        <option value="">全部角色</option>
        <option value="ADMIN">管理员</option>
        <option value="USER">用户</option>
      </select>
      <label class="title-filter"
        ><span class="visually-hidden">称号筛选</span
        ><input v-model="title" aria-label="称号筛选" maxlength="20" placeholder="称号"
      /></label>
      <span class="user-summary">{{ summary.total ?? users.length }} 位用户</span>
      <span v-if="summary.duplicateIpCount" class="risk-summary"
        ><AlertTriangle :size="14" />{{ summary.duplicateIpCount }} 组注册来源重复</span
      >
    </AdminToolbar>

    <div class="user-list glass" :aria-busy="loading">
      <article v-for="user in users" :key="user.id" class="user-row">
        <AppAvatar
          :src="user.avatarUrl"
          :name="user.nickname || user.username"
          :size="48"
          rounded
        />
        <div class="user-identity">
          <div>
            <strong>{{ user.nickname || user.username }}</strong
            ><span
              v-if="user.titleName"
              :class="['title-badge', `title-${user.titleStyle || 'default'}`]"
              >{{ user.titleName }}</span
            >
          </div>
          <span
            >@{{ user.username }}<template v-if="user.email"> · {{ user.email }}</template></span
          >
        </div>
        <div class="user-meta">
          <span :class="['role-chip', user.role?.toLowerCase()]">{{ user.role }}</span>
          <time>{{ formatDate(user.createTime) }}</time>
          <span
            v-if="user.duplicateIp"
            class="risk-chip"
            :title="`同来源账号：${(user.sameIpUsers || []).join('、')}`"
            ><AlertTriangle :size="13" />注册来源重复</span
          >
        </div>
        <div class="user-actions">
          <button class="icon-button" type="button" aria-label="编辑称号" @click="openTitle(user)">
            <Pencil :size="16" />
          </button>
          <button
            v-if="user.role !== 'ADMIN'"
            class="icon-button danger"
            type="button"
            aria-label="删除用户"
            @click="removing = user"
          >
            <Trash2 :size="16" />
          </button>
        </div>
      </article>
      <p v-if="!loading && !users.length" class="users-empty">没有符合条件的用户</p>
    </div>

    <AdminPager
      :page="page"
      :pages="Number(summary.pages || 1)"
      :total="Number(summary.total || 0)"
      :busy="loading"
      @change="load"
    />

    <FormDialog
      :open="Boolean(editing)"
      title="用户称号"
      description="称号会同步到该用户已有和新发布的评论。"
      :busy="busy"
      :valid="titleName.length <= 20"
      @close="!busy && (editing = null)"
      @submit="saveTitle"
    >
      <div class="title-preview">
        <AppAvatar
          :src="editing?.avatarUrl"
          :name="editing?.nickname || editing?.username"
          :size="42"
          rounded
        /><strong>{{ editing?.nickname || editing?.username }}</strong
        ><span v-if="titleName" :class="['title-badge', `title-${titleStyle}`]">{{
          titleName
        }}</span>
      </div>
      <label class="dialog-field"
        >称号<input
          v-model="titleName"
          autofocus
          maxlength="20"
          placeholder="留空可清除称号"
        /><small>{{ titleName.length }}/20</small></label
      >
      <fieldset class="style-options">
        <legend>样式</legend>
        <label
          v-for="item in titleStyles"
          :key="item[0]"
          :class="{ active: titleStyle === item[0] }"
          ><input v-model="titleStyle" type="radio" :value="item[0]" /><span
            :class="['title-badge', `title-${item[0]}`]"
            >{{ item[1] }}</span
          ></label
        >
      </fieldset>
    </FormDialog>

    <ConfirmDialog
      :open="Boolean(removing)"
      title="删除用户"
      :message="`确定删除账号“${removing?.username || ''}”？该操作无法恢复。`"
      confirm-label="删除"
      danger
      :busy="busy"
      @close="!busy && (removing = null)"
      @confirm="remove"
    />
  </section>
</template>

<style scoped>
.users-panel select,
.title-filter input {
  padding: 8px 10px;
  border: 1px solid var(--line);
  border-radius: 10px;
  color: var(--ink);
  background: var(--glass-strong);
  outline: 0;
}
.title-filter input {
  width: 108px;
}
.user-summary {
  color: var(--muted);
  font-size: 12px;
  white-space: nowrap;
}
.risk-summary,
.risk-chip {
  display: inline-flex;
  gap: 5px;
  align-items: center;
  color: #a66c12;
  font-size: 11px;
  white-space: nowrap;
}
.user-list {
  padding: 8px 16px;
  border-radius: 24px;
}
.user-row {
  display: grid;
  grid-template-columns: auto minmax(190px, 1fr) minmax(180px, 0.6fr) auto;
  gap: 14px;
  align-items: center;
  padding: 15px 4px;
  border-bottom: 1px solid var(--line);
}
.user-row:last-child {
  border-bottom: 0;
}
.user-identity,
.user-meta {
  display: grid;
  min-width: 0;
  gap: 4px;
}
.user-identity > div {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
  align-items: center;
}
.user-identity > span,
.user-meta time {
  overflow: hidden;
  color: var(--muted);
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.user-meta {
  grid-template-columns: max-content 1fr;
  align-items: center;
}
.risk-chip {
  grid-column: 1 / -1;
}
.user-actions {
  display: flex;
}
.role-chip {
  width: max-content;
  padding: 4px 7px;
  border-radius: 8px;
  color: #267c59;
  background: rgb(70 189 132 / 14%);
  font-size: 10px;
}
.role-chip.admin {
  color: var(--accent);
  background: var(--accent-soft);
}
.title-badge {
  display: inline-flex;
  width: max-content;
  padding: 3px 8px;
  border: 1px solid var(--line);
  border-radius: 999px;
  font-size: 10px;
}
.title-blue {
  color: #2979de;
  background: rgb(41 121 222 / 11%);
}
.title-green {
  color: #25815c;
  background: rgb(37 129 92 / 11%);
}
.title-gold {
  color: #a66c12;
  background: rgb(188 127 31 / 12%);
}
.title-rose {
  color: #bb4770;
  background: rgb(187 71 112 / 11%);
}
.title-default {
  color: var(--muted);
  background: var(--accent-soft);
}
.users-empty {
  padding: 50px;
  color: var(--muted);
  text-align: center;
}
.title-preview {
  display: flex;
  gap: 9px;
  align-items: center;
  padding: 12px;
  border-radius: 14px;
  background: var(--accent-soft);
}
.dialog-field {
  position: relative;
  display: grid;
  gap: 7px;
  color: var(--muted);
  font-size: 12px;
}
.dialog-field input {
  padding: 11px 45px 11px 12px;
  border: 1px solid var(--line);
  border-radius: 12px;
  color: var(--ink);
  background: var(--glass-strong);
  outline: 0;
}
.dialog-field small {
  position: absolute;
  right: 10px;
  bottom: 11px;
}
.style-options {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
  padding: 0;
  border: 0;
}
.style-options legend {
  width: 100%;
  margin-bottom: 7px;
  color: var(--muted);
  font-size: 12px;
}
.style-options label {
  padding: 3px;
  cursor: pointer;
  border: 1px solid transparent;
  border-radius: 999px;
}
.style-options label.active {
  border-color: var(--accent);
}
.style-options input {
  position: absolute;
  opacity: 0;
  pointer-events: none;
}
@media (max-width: 760px) {
  .user-row {
    grid-template-columns: auto minmax(0, 1fr) auto;
  }
  .user-meta {
    grid-column: 2;
  }
  .user-actions {
    grid-row: 1 / span 2;
    grid-column: 3;
  }
}
</style>
