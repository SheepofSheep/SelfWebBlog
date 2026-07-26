<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  BarChart3,
  FileText,
  MessageSquare,
  Tags,
  Image,
  Users,
  Settings,
  ShieldCheck,
  Plus,
  MoreHorizontal,
  Pin,
  Trash2,
  Pencil,
  LogOut,
  ChevronDown
} from 'lucide-vue-next'
import { api } from '@/api/client.js'
import { appStore, notify } from '@/stores/app.js'
import { formatDate, postTitle } from '@/core/format.js'
import TagInput from '@/components/TagInput.vue'
const route = useRoute()
const router = useRouter()
const active = ref(route.query.tab || 'overview')
const loading = ref(false)
const tabs = [
  ['overview', '概览', BarChart3],
  ['content', '文章', FileText],
  ['interactions', '评论', MessageSquare],
  ['tags', '标签', Tags],
  ['assets', '媒体', Image],
  ['users', '用户', Users],
  ['site', '站点', Settings],
  ['security', '安全', ShieldCheck]
]
const overview = ref({})
const posts = ref([])
const drafts = ref([])
const interactions = ref([])
const tags = ref([])
const assets = ref([])
const users = ref([])
const events = ref([])
const openThreads = ref(new Set())
const tagDraft = ref([])
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
async function loadAllInteractions() {
  const collected = []
  let page = 1
  let total = 0
  do {
    const value = await api.adminInteractions('', page, 100)
    collected.push(...(value?.records || []))
    total = value?.total || collected.length
    page += 1
  } while (collected.length < total)
  interactions.value = collected
}
const roots = computed(() =>
  interactions.value
    .filter((item) => !item.rootId)
    .map((item) => ({
      ...item,
      replies: interactions.value.filter((reply) => reply.rootId === item.id)
    }))
)
async function load(tab = active.value) {
  loading.value = true
  try {
    if (tab === 'overview') overview.value = await api.overview()
    if (tab === 'content') {
      const [p, d] = await Promise.all([api.posts(1, 100), api.drafts()])
      posts.value = p?.records || []
      drafts.value = d?.records || []
    }
    if (tab === 'interactions') await loadAllInteractions()
    if (tab === 'tags') tags.value = await api.tags()
    if (tab === 'assets') assets.value = await api.assets()
    if (tab === 'users') {
      const value = await api.users()
      users.value = value?.users || value?.records || value || []
    }
    if (tab === 'site') Object.assign(site, await api.siteAdmin())
    if (tab === 'security') events.value = await api.security()
  } catch (e) {
    if (e.status === 401 || e.status === 403) router.replace('/login')
    else notify(e.message)
  } finally {
    loading.value = false
  }
}
function select(tab) {
  active.value = tab
  router.replace({ query: { tab } })
  load(tab)
}
async function removePost(id) {
  if (!confirm('确定删除这篇文章？')) return
  await api.deletePost(id)
  notify('已删除')
  load('content')
}
async function addTag() {
  if (!tagDraft.value.length) return
  for (const name of tagDraft.value) await api.createTag(name)
  tagDraft.value = []
  load('tags')
}
async function renameTag(tag) {
  const name = prompt('标签名称', tag.name)
  if (name && name !== tag.name) {
    await api.renameTag(tag.id, name)
    load('tags')
  }
}
async function removeTag(tag) {
  try {
    await api.deleteTag(tag.id)
    load('tags')
  } catch (e) {
    notify(e.message)
  }
}
async function editTitle(user) {
  const titleName = prompt('用户称号', user.titleName || '')
  if (titleName === null) return
  const titleStyle = prompt('称号样式', user.titleStyle || 'default') || 'default'
  await api.updateUserTitle(user.id, { titleName, titleStyle })
  load('users')
}
async function removeUser(user) {
  if (!confirm(`确定删除 ${user.username}？`)) return
  try {
    await api.deleteUser(user.id)
    load('users')
  } catch (e) {
    notify(e.message)
  }
}
async function interactionAction(item, action) {
  if (action === 'pin') await api.pin(item.id, !item.pinned)
  else if (action === 'delete' && confirm('确定删除这条互动？'))
    await api.deleteInteraction(item.id)
  load('interactions')
}
async function moderate(item, status) {
  await api.moderate(item.id, status)
  load('interactions')
}
function toggleThread(id) {
  const next = new Set(openThreads.value)
  next.has(id) ? next.delete(id) : next.add(id)
  openThreads.value = next
}
async function saveSite() {
  try {
    Object.assign(site, await api.saveSite(site))
    appStore.site = { ...site }
    notify('站点设置已保存')
  } catch (e) {
    notify(e.message)
  }
}
async function logout() {
  await api.logout()
  appStore.user = null
  router.push('/')
}
async function copyAsset(url) {
  await navigator.clipboard.writeText(url)
  notify('地址已复制')
}
watch(
  () => route.query.tab,
  (tab) => {
    if (tab && tab !== active.value) {
      active.value = tab
      load(tab)
    }
  }
)
watch(
  () => appStore.ready,
  (ready) => {
    if (!ready) return
    if (appStore.user?.role !== 'ADMIN') {
      router.replace({ path: '/login', query: { redirect: route.fullPath } })
      return
    }
    load()
  },
  { immediate: true }
)
</script>
<template>
  <div class="admin-page">
    <aside class="admin-sidebar glass">
      <RouterLink class="brand" to="/"
        ><span class="brand-mark">◌</span
        ><span>{{ appStore.site?.nickname || '博客' }}</span></RouterLink
      >
      <nav>
        <button
          v-for="tab in tabs"
          :key="tab[0]"
          :class="{ active: active === tab[0] }"
          @click="select(tab[0])"
        >
          <component :is="tab[2]" :size="18" />{{ tab[1] }}
        </button>
      </nav>
      <div class="admin-user">
        <div>
          <strong>{{ appStore.user?.nickname || appStore.user?.username }}</strong
          ><small>管理员</small>
        </div>
        <button class="icon-button" aria-label="退出" @click="logout"><LogOut :size="18" /></button>
      </div>
    </aside>
    <main class="admin-main">
      <header class="admin-top">
        <div>
          <span class="eyebrow">WORKBENCH</span>
          <h1>{{ tabs.find((item) => item[0] === active)?.[1] }}</h1>
        </div>
        <RouterLink v-if="active === 'content'" class="primary-button" to="/write"
          ><Plus :size="17" />新文章</RouterLink
        >
      </header>
      <div v-if="loading" class="admin-loading glass">加载中</div>
      <section v-else-if="active === 'overview'" class="stat-grid">
        <article
          v-for="item in [
            ['已发布', overview.publishedCount],
            ['草稿', overview.draftCount],
            ['待处理', overview.pendingCount],
            ['用户', overview.userCount],
            ['媒体', overview.assetCount]
          ]"
          :key="item[0]"
          class="stat-card glass"
        >
          <span>{{ item[0] }}</span
          ><strong>{{ item[1] ?? 0 }}</strong>
        </article>
        <article class="stat-card glass wide">
          <span>最近活动</span
          ><strong class="date-value">{{ formatDate(overview.latestActivity) || '—' }}</strong>
        </article>
      </section>
      <section v-else-if="active === 'content'" class="admin-panel glass">
        <div class="panel-heading">
          <h2>已发布</h2>
          <span>{{ posts.length }}</span>
        </div>
        <div class="data-list">
          <article v-for="post in posts" :key="post.id">
            <div>
              <strong>{{ postTitle(post) }}</strong
              ><span>{{ formatDate(post.updateTime || post.createTime) }}</span>
            </div>
            <div>
              <RouterLink class="icon-button" :to="`/write/${post.id}`"
                ><Pencil :size="17" /></RouterLink
              ><button class="icon-button danger" @click="removePost(post.id)">
                <Trash2 :size="17" />
              </button>
            </div>
          </article>
        </div>
        <div class="panel-heading sub">
          <h2>草稿</h2>
          <span>{{ drafts.length }}</span>
        </div>
        <div class="data-list">
          <article v-for="post in drafts" :key="post.id">
            <div>
              <strong>{{ postTitle(post) }}</strong
              ><span>{{ formatDate(post.updateTime || post.createTime) }}</span>
            </div>
            <RouterLink class="secondary-button" :to="`/write/${post.id}`">继续写</RouterLink>
          </article>
        </div>
      </section>
      <section v-else-if="active === 'interactions'" class="admin-panel">
        <article v-for="item in roots" :key="item.id" class="moderation-card glass">
          <div class="interaction-head">
            <span class="avatar-fallback">{{ (item.nickname || '访').slice(0, 1) }}</span>
            <div>
              <strong>{{ item.nickname || '访客' }}</strong
              ><span v-if="item.titleName" class="title-badge">{{ item.titleName }}</span
              ><time>{{ formatDate(item.createTime) }}</time>
            </div>
            <details class="more-menu">
              <summary><MoreHorizontal :size="19" /></summary>
              <div class="glass">
                <button @click="interactionAction(item, 'pin')">
                  <Pin :size="15" />{{ item.pinned ? '取消置顶' : '置顶' }}</button
                ><button class="danger" @click="interactionAction(item, 'delete')">
                  <Trash2 :size="15" />删除
                </button>
              </div>
            </details>
          </div>
          <p>{{ item.content }}</p>
          <footer>
            <div>
              <button v-if="item.status !== 'PUBLISHED'" @click="moderate(item, 'PUBLISHED')">
                通过</button
              ><button v-if="item.status === 'PUBLISHED'" @click="moderate(item, 'HIDDEN')">
                隐藏</button
              ><span :class="`status-${item.status?.toLowerCase()}`">{{ item.status }}</span>
            </div>
            <button v-if="item.replies.length" @click="toggleThread(item.id)">
              <ChevronDown :class="{ rotated: openThreads.has(item.id) }" :size="16" />{{
                item.replies.length
              }}
              条回复
            </button>
          </footer>
          <div v-if="openThreads.has(item.id)" class="reply-list">
            <article v-for="reply in item.replies" :key="reply.id">
              <strong>{{ reply.nickname || '用户' }}</strong>
              <p>{{ reply.content }}</p>
            </article>
          </div>
        </article>
      </section>
      <section v-else-if="active === 'tags'" class="admin-panel glass">
        <div class="tag-create">
          <TagInput v-model="tagDraft" /><button class="primary-button" @click="addTag">
            添加
          </button>
        </div>
        <div class="tag-admin-list">
          <article v-for="tag in tags" :key="tag.id">
            <div>
              <strong>{{ tag.name }}</strong
              ><span>{{ tag.postCount }} 篇文章</span>
            </div>
            <div>
              <button class="icon-button" @click="renameTag(tag)"><Pencil :size="16" /></button
              ><button class="icon-button danger" @click="removeTag(tag)">
                <Trash2 :size="16" />
              </button>
            </div>
          </article>
        </div>
      </section>
      <section v-else-if="active === 'assets'" class="asset-grid">
        <article v-for="asset in assets" :key="asset.id" class="asset-card glass">
          <img :src="asset.smallWebpUrl || asset.originalUrl" :alt="asset.id" />
          <div>
            <span>{{ asset.width }} × {{ asset.height }}</span
            ><button @click="copyAsset(asset.originalUrl)">复制地址</button>
          </div>
        </article>
        <p v-if="!assets.length" class="muted">还没有媒体</p>
      </section>
      <section v-else-if="active === 'users'" class="admin-panel glass">
        <div class="data-list">
          <article v-for="user in users" :key="user.id">
            <div>
              <strong>{{ user.nickname || user.username }}</strong
              ><span>{{ user.titleName || '无称号' }}</span>
            </div>
            <div>
              <span class="status-published">{{ user.role }}</span
              ><button class="icon-button" aria-label="编辑称号" @click="editTitle(user)">
                <Pencil :size="16" /></button
              ><button
                v-if="user.role !== 'ADMIN'"
                class="icon-button danger"
                aria-label="删除用户"
                @click="removeUser(user)"
              >
                <Trash2 :size="16" />
              </button>
            </div>
          </article>
        </div>
      </section>
      <form v-else-if="active === 'site'" class="settings-form glass" @submit.prevent="saveSite">
        <div class="form-grid">
          <label>昵称<input v-model="site.nickname" required /></label
          ><label>建站日期<input v-model="site.siteStartDate" type="date" /></label
          ><label class="full">简介<textarea v-model="site.bio" rows="3" /></label
          ><label class="full">当前近况<textarea v-model="site.currentStatus" rows="3" /></label
          ><label>头像地址<input v-model="site.avatarUrl" /></label
          ><label>背景地址<input v-model="site.bgUrl" /></label
          ><label class="full">关于正文<textarea v-model="site.aboutMarkdown" rows="12" /></label
          ><label>GitHub<input v-model="site.socialLinks.github" /></label
          ><label>邮箱<input v-model="site.socialLinks.email" /></label>
        </div>
        <button class="primary-button">保存</button>
      </form>
      <section v-else-if="active === 'security'" class="admin-panel glass">
        <div class="data-list">
          <article v-for="event in events" :key="event.id">
            <div>
              <strong>{{ event.action }}</strong
              ><span>{{ event.targetType }} · {{ event.detail }}</span>
            </div>
            <time>{{ formatDate(event.createTime) }}</time>
          </article>
          <p v-if="!events.length" class="muted">没有安全事件</p>
        </div>
      </section>
    </main>
  </div>
</template>
