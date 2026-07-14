<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ImageUp, Save } from 'lucide-vue-next'
import { getAdminSite, updateAdminSite } from '../../../api/admin'
import { uploadAvatar, uploadImage } from '../../../api/uploads'
import { showToast } from '../../../composables/toast'
import AdminSectionHeader from './AdminSectionHeader.vue'

const loading = ref(true)
const saving = ref(false)
const form = reactive({
  nickname: '',
  bio: '',
  avatarUrl: '',
  bgUrl: '',
  siteStartDate: '',
  currentStatus: '',
  aboutMarkdown: '',
  socialLinks: { github: '', bilibili: '', email: '', website: '' }
})
function assign(data) {
  Object.assign(form, data || {})
  form.socialLinks = {
    github: '',
    bilibili: '',
    email: '',
    website: '',
    ...(data?.socialLinks || {})
  }
}
async function load() {
  loading.value = true
  try {
    assign(await getAdminSite())
  } finally {
    loading.value = false
  }
}
async function save() {
  saving.value = true
  try {
    assign(await updateAdminSite(form))
    showToast('站点资料已保存。')
  } finally {
    saving.value = false
  }
}
async function pick(event, type) {
  const file = event.target.files?.[0]
  if (!file) return
  try {
    if (type === 'avatar') form.avatarUrl = await uploadAvatar(file)
    else {
      const asset = await uploadImage(file)
      form.bgUrl = asset.originalUrl || String(asset)
    }
  } finally {
    event.target.value = ''
  }
}
onMounted(load)
</script>

<template>
  <div>
    <AdminSectionHeader title="站点资料" description="首页近况只显示你在这里填写的真实内容。"
      ><button class="admin-button primary" type="button" :disabled="saving" @click="save">
        <Save :size="16" />{{ saving ? '保存中' : '保存' }}
      </button></AdminSectionHeader
    >
    <div v-if="loading" class="admin-loading">正在读取站点资料</div>
    <form v-else class="admin-site-form" @submit.prevent="save">
      <section>
        <h2>基础资料</h2>
        <div class="admin-form-grid">
          <label>昵称<input v-model="form.nickname" class="admin-input" required /></label
          ><label
            >建站日期<input
              v-model="form.siteStartDate"
              class="admin-input"
              type="date"
              required /></label
          ><label class="wide"
            >简介<textarea v-model="form.bio" class="admin-textarea" rows="3" />
          </label>
        </div>
        <div class="admin-upload-row">
          <label class="admin-button secondary"
            ><ImageUp :size="16" />上传头像<input
              type="file"
              accept="image/*"
              @change="pick($event, 'avatar')" /></label
          ><code>{{ form.avatarUrl || '尚未设置头像' }}</code
          ><label class="admin-button secondary"
            ><ImageUp :size="16" />上传背景<input
              type="file"
              accept="image/*"
              @change="pick($event, 'background')"
          /></label>
        </div>
      </section>
      <section>
        <h2>首页近况</h2>
        <label
          >当前正在做什么<textarea
            v-model="form.currentStatus"
            class="admin-textarea"
            rows="4"
            maxlength="300"
            placeholder="留空时首页不生成替代宣传文案。"
          />
        </label>
      </section>
      <section>
        <h2>作者页</h2>
        <label
          >Markdown 正文<textarea
            v-model="form.aboutMarkdown"
            class="admin-textarea code"
            rows="10"
          />
        </label>
      </section>
      <section>
        <h2>社交链接</h2>
        <div class="admin-form-grid">
          <label
            >GitHub<input v-model="form.socialLinks.github" class="admin-input" type="url" /></label
          ><label
            >哔哩哔哩<input
              v-model="form.socialLinks.bilibili"
              class="admin-input"
              type="url" /></label
          ><label
            >邮箱<input v-model="form.socialLinks.email" class="admin-input" type="email" /></label
          ><label
            >个人网站<input v-model="form.socialLinks.website" class="admin-input" type="url"
          /></label>
        </div>
      </section>
    </form>
  </div>
</template>
