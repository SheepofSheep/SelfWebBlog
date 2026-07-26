<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ChevronDown, ChevronUp, ExternalLink, Pencil, Plus, Trash2, Upload } from 'lucide-vue-next'
import { api } from '@/api/client.js'
import { createAdminRequestGate, moveOrderedItem, syncSiteAvatar } from '@/core/adminModel.js'
import { appStore, notify } from '@/stores/app.js'
import AppAvatar from '@/components/AppAvatar.vue'
import FormDialog from '@/components/FormDialog.vue'
import ConfirmDialog from '@/components/ConfirmDialog.vue'

const site = reactive({
  nickname: '',
  bio: '',
  avatarUrl: '',
  bgUrl: '',
  siteStartDate: '',
  currentStatus: '',
  aboutMarkdown: '',
  socialLinks: {}
})
const links = ref([])
const profileLoading = ref(false)
const linksLoading = ref(false)
const profileReady = ref(false)
const linksReady = ref(false)
const profileError = ref('')
const linksError = ref('')
const busy = ref(false)
const avatarInput = ref(null)
const editing = ref(null)
const removing = ref(null)
const linkDraft = reactive({ label: '', url: '', iconKey: 'link', enabled: true })
const today = new Date().toLocaleDateString('en-CA')
const profileGate = createAdminRequestGate()
const linksGate = createAdminRequestGate()
const loading = computed(() => profileLoading.value || linksLoading.value)
const profileLocked = computed(() => profileLoading.value || busy.value || !profileReady.value)
const linksLocked = computed(() => linksLoading.value || busy.value || !linksReady.value)

function syncPublicSite(profile = null, rows) {
  appStore.site = {
    ...(appStore.site || {}),
    ...(profile || {})
  }
  if (rows === undefined) return
  const normalizedLinks = Array.isArray(rows) ? rows.map((link) => ({ ...link })) : []
  links.value = normalizedLinks
  appStore.site.links = normalizedLinks.map((link) => ({ ...link }))
}

async function loadProfile() {
  if (busy.value) return
  const ticket = profileGate.begin()
  profileLoading.value = true
  profileError.value = ''
  try {
    const profile = await api.siteAdmin()
    if (!profileGate.isCurrent(ticket)) return
    Object.assign(site, profile, { socialLinks: profile.socialLinks || {} })
    profileReady.value = true
    syncPublicSite(profile)
  } catch (error) {
    if (profileGate.isCurrent(ticket)) {
      profileError.value = error?.message || '站点内容加载失败'
      notify(profileError.value)
    }
  } finally {
    profileGate.runIfCurrent(ticket, () => {
      profileLoading.value = false
    })
  }
}

async function loadLinks() {
  if (busy.value) return
  const ticket = linksGate.begin()
  linksLoading.value = true
  linksError.value = ''
  try {
    const rows = await api.siteLinks()
    if (!linksGate.isCurrent(ticket)) return
    linksReady.value = true
    syncPublicSite(null, rows || [])
  } catch (error) {
    if (linksGate.isCurrent(ticket)) {
      linksError.value = error?.message || '自定义链接加载失败'
      notify(linksError.value)
    }
  } finally {
    linksGate.runIfCurrent(ticket, () => {
      linksLoading.value = false
    })
  }
}

async function saveSite() {
  if (profileLocked.value) return
  busy.value = true
  try {
    const updated = await api.saveSite({
      nickname: site.nickname,
      bio: site.bio,
      avatarUrl: site.avatarUrl,
      bgUrl: site.bgUrl,
      siteStartDate: site.siteStartDate,
      currentStatus: site.currentStatus,
      aboutMarkdown: site.aboutMarkdown,
      socialLinks: site.socialLinks || {}
    })
    Object.assign(site, updated)
    syncPublicSite(updated)
    notify('站点内容已保存')
  } catch (error) {
    notify(error.message)
  } finally {
    busy.value = false
  }
}

async function uploadAvatar(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file || profileLocked.value) return
  busy.value = true
  try {
    site.avatarUrl = await api.uploadSiteAvatar(file)
    syncSiteAvatar(appStore, site.avatarUrl)
    notify('站点头像已更新')
  } catch (error) {
    notify(error.message)
  } finally {
    busy.value = false
  }
}

function openLink(link = null) {
  if (linksLocked.value) return
  editing.value = link || { id: null }
  Object.assign(linkDraft, {
    label: link?.label || '',
    url: link?.url || '',
    iconKey: link?.iconKey || 'link',
    enabled: link?.enabled ?? true
  })
}

async function saveLink() {
  if (!editing.value || linksLocked.value) return
  busy.value = true
  try {
    const payload = {
      label: linkDraft.label.trim(),
      url: linkDraft.url.trim(),
      iconKey: linkDraft.iconKey || 'link',
      enabled: linkDraft.enabled
    }
    const editingId = editing.value.id
    const saved = editingId
      ? await api.updateSiteLink(editingId, payload)
      : await api.createSiteLink(payload)
    const next = editingId
      ? links.value.map((link) => (String(link.id) === String(editingId) ? saved : link))
      : [...links.value, saved]
    syncPublicSite(null, next)
    editing.value = null
    notify('链接已保存')
  } catch (error) {
    notify(error.message)
  } finally {
    busy.value = false
  }
}

async function toggle(link) {
  if (linksLocked.value) return
  busy.value = true
  try {
    const updated = await api.updateSiteLink(link.id, {
      label: link.label,
      url: link.url,
      iconKey: link.iconKey,
      enabled: !link.enabled
    })
    syncPublicSite(
      null,
      links.value.map((item) => (String(item.id) === String(link.id) ? updated : item))
    )
  } catch (error) {
    notify(error.message)
  } finally {
    busy.value = false
  }
}

async function move(link, direction) {
  if (linksLocked.value) return
  const next = moveOrderedItem(links.value, link.id, direction)
  if (next.every((item, index) => item.id === links.value[index]?.id)) return
  const previous = links.value
  busy.value = true
  syncPublicSite(null, next)
  try {
    const reordered = await api.reorderSiteLinks(next.map((item) => String(item.id)))
    syncPublicSite(null, reordered || next)
  } catch (error) {
    syncPublicSite(null, previous)
    notify(error.message)
  } finally {
    busy.value = false
  }
}

async function removeLink() {
  if (!removing.value || linksLocked.value) return
  busy.value = true
  try {
    const removedId = removing.value.id
    await api.deleteSiteLink(removedId)
    syncPublicSite(
      null,
      links.value.filter((link) => String(link.id) !== String(removedId))
    )
    removing.value = null
    notify('链接已删除')
  } catch (error) {
    notify(error.message)
  } finally {
    busy.value = false
  }
}

onMounted(() => {
  loadProfile()
  loadLinks()
})
onBeforeUnmount(() => {
  profileGate.begin()
  linksGate.begin()
})
</script>

<template>
  <div class="site-panel" :aria-busy="loading">
    <form class="site-profile glass" @submit.prevent="saveSite">
      <header>
        <div>
          <h2>站点内容</h2>
          <p>修改后会同步到公开页面。</p>
        </div>
        <button class="primary-button" :disabled="profileLocked">
          {{ busy ? '保存中' : profileLoading ? '加载中' : '保存' }}
        </button>
      </header>
      <div v-if="profileError" class="panel-inline-error" role="alert">
        <span>{{ profileError }}</span>
        <button class="secondary-button" type="button" :disabled="busy" @click="loadProfile">
          重试
        </button>
      </div>
      <fieldset class="site-form-grid" :disabled="profileLocked">
        <div class="avatar-editor">
          <AppAvatar :src="site.avatarUrl" :name="site.nickname" :size="84" />
          <div>
            <strong>{{ site.nickname || '站点头像' }}</strong
            ><button
              class="secondary-button"
              type="button"
              :disabled="profileLocked"
              @click="avatarInput.click()"
            >
              <Upload :size="15" />上传头像</button
            ><input
              ref="avatarInput"
              class="visually-hidden"
              type="file"
              accept="image/*"
              @change="uploadAvatar"
            />
          </div>
        </div>
        <label>昵称<input v-model="site.nickname" required maxlength="40" /></label>
        <label>建站日期<input v-model="site.siteStartDate" type="date" :max="today" /></label>
        <label class="full">简介<textarea v-model="site.bio" rows="3" maxlength="240" /></label>
        <label class="full"
          >当前近况<textarea v-model="site.currentStatus" rows="3" maxlength="300" />
        </label>
        <label>头像地址<input v-model="site.avatarUrl" maxlength="1000" /></label>
        <label>背景地址<input v-model="site.bgUrl" maxlength="1000" /></label>
        <label class="full"
          >关于正文<textarea v-model="site.aboutMarkdown" rows="12" maxlength="20000" />
        </label>
      </fieldset>
    </form>

    <section class="site-links glass">
      <header>
        <div>
          <h2>自定义链接</h2>
          <p>公开页面只显示已启用的链接，并按此处顺序排列。</p>
        </div>
        <button class="primary-button" type="button" :disabled="linksLocked" @click="openLink()">
          <Plus :size="16" />添加链接
        </button>
      </header>
      <div v-if="linksError" class="panel-inline-error" role="alert">
        <span>{{ linksError }}</span>
        <button class="secondary-button" type="button" :disabled="busy" @click="loadLinks">
          重试
        </button>
      </div>
      <div class="link-list">
        <article v-for="(link, index) in links" :key="link.id">
          <span class="link-icon">{{ (link.label || '链').slice(0, 1) }}</span>
          <div>
            <strong>{{ link.label }}</strong
            ><a :href="link.url" target="_blank" rel="noopener noreferrer"
              >{{ link.url }}<ExternalLink :size="12"
            /></a>
          </div>
          <label class="switch"
            ><input
              type="checkbox"
              :checked="link.enabled"
              :disabled="linksLocked"
              :aria-label="`${link.label}是否显示`"
              @change="toggle(link)" /><span></span
          ></label>
          <div class="link-actions">
            <button
              class="icon-button"
              type="button"
              :disabled="linksLocked || index === 0"
              aria-label="上移"
              @click="move(link, 'UP')"
            >
              <ChevronUp :size="16" /></button
            ><button
              class="icon-button"
              type="button"
              :disabled="linksLocked || index === links.length - 1"
              aria-label="下移"
              @click="move(link, 'DOWN')"
            >
              <ChevronDown :size="16" /></button
            ><button
              class="icon-button"
              type="button"
              :disabled="linksLocked"
              aria-label="编辑链接"
              @click="openLink(link)"
            >
              <Pencil :size="16" /></button
            ><button
              class="icon-button danger"
              type="button"
              :disabled="linksLocked"
              aria-label="删除链接"
              @click="removing = link"
            >
              <Trash2 :size="16" />
            </button>
          </div>
        </article>
        <p v-if="linksReady && !links.length" class="links-empty">还没有自定义链接</p>
      </div>
    </section>

    <FormDialog
      :open="Boolean(editing)"
      :title="editing?.id ? '编辑链接' : '添加链接'"
      :busy="linksLocked"
      :valid="Boolean(linkDraft.label.trim() && linkDraft.url.trim())"
      @close="!linksLocked && (editing = null)"
      @submit="saveLink"
    >
      <label class="dialog-field"
        >名称<input v-model="linkDraft.label" autofocus maxlength="30" placeholder="例如：GitHub"
      /></label>
      <label class="dialog-field"
        >地址<input v-model="linkDraft.url" maxlength="500" placeholder="https://… 或 mailto:…"
      /></label>
      <label class="dialog-field"
        >图标<select v-model="linkDraft.iconKey">
          <option value="link">链接</option>
          <option value="github">GitHub</option>
          <option value="bilibili">哔哩哔哩</option>
          <option value="mail">邮箱</option>
          <option value="website">网站</option>
        </select></label
      >
      <label class="enabled-field"
        ><input v-model="linkDraft.enabled" type="checkbox" />在公开页面显示</label
      >
    </FormDialog>
    <ConfirmDialog
      :open="Boolean(removing)"
      title="删除链接"
      :message="`确定删除“${removing?.label || ''}”？`"
      confirm-label="删除"
      danger
      :busy="linksLocked"
      @close="!linksLocked && (removing = null)"
      @confirm="removeLink"
    />
  </div>
</template>

<style scoped>
.site-panel {
  display: grid;
  gap: 14px;
}
.site-profile,
.site-links {
  padding: 22px;
  border-radius: 26px;
}
.site-profile > header,
.site-links > header {
  display: flex;
  gap: 18px;
  align-items: flex-start;
  justify-content: space-between;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--line);
}
.site-panel h2 {
  margin: 0;
}
.site-panel header p {
  margin: 5px 0 0;
  color: var(--muted);
  font-size: 12px;
}
.panel-inline-error {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
  padding: 12px 0;
  color: var(--danger, #b84242);
  font-size: 12px;
}
.site-panel .primary-button,
.avatar-editor button {
  display: inline-flex;
  gap: 6px;
  align-items: center;
  white-space: nowrap;
}
.site-form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 15px;
  padding-top: 20px;
  min-inline-size: 0;
  margin: 0;
  border: 0;
}
.site-form-grid label,
.dialog-field {
  display: grid;
  gap: 7px;
  color: var(--muted);
  font-size: 12px;
}
.site-form-grid input,
.site-form-grid textarea,
.dialog-field input,
.dialog-field select {
  width: 100%;
  padding: 11px 12px;
  border: 1px solid var(--line);
  border-radius: 12px;
  outline: 0;
  color: var(--ink);
  background: var(--glass-strong);
}
.site-form-grid .full {
  grid-column: 1 / -1;
}
.avatar-editor {
  display: flex;
  grid-column: 1 / -1;
  gap: 15px;
  align-items: center;
  padding: 14px;
  border-radius: 18px;
  background: var(--accent-soft);
}
.avatar-editor > div {
  display: grid;
  gap: 8px;
}
.link-list {
  display: grid;
}
.link-list article {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto auto;
  gap: 12px;
  align-items: center;
  padding: 14px 2px;
  border-bottom: 1px solid var(--line);
}
.link-icon {
  display: grid;
  width: 40px;
  height: 40px;
  place-items: center;
  border-radius: 12px;
  color: var(--accent);
  background: var(--accent-soft);
  font-weight: 800;
}
.link-list article > div:nth-child(2) {
  display: grid;
  min-width: 0;
  gap: 3px;
}
.link-list a {
  display: flex;
  gap: 4px;
  align-items: center;
  overflow: hidden;
  color: var(--muted);
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.link-actions {
  display: flex;
}
.switch {
  position: relative;
  display: inline-flex;
}
.switch input {
  position: absolute;
  opacity: 0;
}
.switch span {
  width: 38px;
  height: 22px;
  border-radius: 999px;
  background: var(--line);
  transition: 0.2s ease;
}
.switch span::after {
  position: absolute;
  top: 3px;
  left: 3px;
  width: 16px;
  height: 16px;
  content: '';
  border-radius: 50%;
  background: white;
  box-shadow: 0 2px 8px rgb(0 0 0 / 14%);
  transition: 0.2s ease;
}
.switch input:checked + span {
  background: var(--accent);
}
.switch input:checked + span::after {
  transform: translateX(16px);
}
.links-empty {
  padding: 40px;
  color: var(--muted);
  text-align: center;
}
.enabled-field {
  display: flex;
  gap: 8px;
  align-items: center;
  color: var(--muted);
  font-size: 12px;
}
.visually-hidden {
  position: absolute;
  width: 1px;
  height: 1px;
  overflow: hidden;
  clip: rect(0 0 0 0);
}
.site-form-grid input.visually-hidden {
  width: 1px;
  height: 1px;
  padding: 0;
  border: 0;
}
@media (max-width: 720px) {
  .site-form-grid {
    grid-template-columns: 1fr;
  }
  .site-form-grid .full,
  .avatar-editor {
    grid-column: auto;
  }
  .link-list article {
    grid-template-columns: auto minmax(0, 1fr) auto;
  }
  .link-actions {
    grid-column: 2 / -1;
    justify-content: flex-end;
  }
}
</style>
