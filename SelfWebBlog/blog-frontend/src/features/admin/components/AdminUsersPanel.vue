<script setup>
import { computed, inject, onMounted, ref } from 'vue'
import { Search, Save, Trash2 } from 'lucide-vue-next'
import { deleteUser, grantTitle, listUsers } from '../../../api/admin'
import { formatTime } from '../../../utils/format'
import { toAbsoluteUrl } from '../../../utils/url'
import AdminSectionHeader from './AdminSectionHeader.vue'

const confirmAction = inject('adminConfirm')
const users = ref([])
const query = ref('')
const loading = ref(true)
const rows = computed(() => {
  const keyword = query.value.trim().toLowerCase()
  return users.value.filter(
    (user) => !keyword || `${user.username} ${user.email || ''}`.toLowerCase().includes(keyword)
  )
})
async function load() {
  loading.value = true
  try {
    const data = await listUsers()
    users.value = (data.users || []).map((user) => ({
      ...user,
      editTitle: user.titleName || '',
      editStyle: user.titleStyle || 'default'
    }))
  } finally {
    loading.value = false
  }
}
async function saveTitle(user) {
  await grantTitle(user.id, user.editTitle.trim(), user.editStyle)
  await load()
}
async function remove(user) {
  if (
    !(await confirmAction({
      title: '删除用户',
      message: `确定删除「${user.username}」吗？文章和互动不会被批量删除。`,
      confirmText: '删除用户'
    }))
  )
    return
  await deleteUser(user.id)
  await load()
}
onMounted(load)
</script>

<template>
  <div>
    <AdminSectionHeader
      title="用户管理"
      description="管理称号与账号风险标记，不展示注册 IP 明文。"
    />
    <div class="admin-toolbar">
      <label class="admin-search"
        ><Search :size="16" /><input v-model="query" placeholder="搜索用户名或邮箱"
      /></label>
    </div>
    <div v-if="loading" class="admin-loading">正在读取用户</div>
    <div v-else class="admin-table-list">
      <article v-for="user in rows" :key="user.id" class="admin-user-row">
        <img
          :src="toAbsoluteUrl(user.avatarUrl || `/images/default-avatar.svg`)"
          :alt="user.username"
        />
        <div class="admin-row-main">
          <strong>{{ user.username }}</strong>
          <p>{{ user.email || '未填写邮箱' }}</p>
          <small
            >{{ user.role === 'ADMIN' ? '管理员' : '访客' }} · 注册于
            {{ formatTime(user.createTime) }}</small
          ><span v-if="user.duplicateIp" class="admin-risk">检测到同来源注册账号</span>
        </div>
        <div class="admin-user-title">
          <input v-model="user.editTitle" class="admin-input" placeholder="称号" /><select
            v-model="user.editStyle"
            class="admin-select"
          >
            <option value="default">默认</option>
            <option value="gold">暖金</option>
            <option value="nature">叶绿</option>
            <option value="candy">樱粉</option></select
          ><button title="保存称号" aria-label="保存称号" @click="saveTitle(user)">
            <Save :size="15" />
          </button>
        </div>
        <button
          v-if="user.role !== 'ADMIN'"
          class="admin-icon-danger"
          title="删除用户"
          aria-label="删除用户"
          @click="remove(user)"
        >
          <Trash2 :size="15" />
        </button>
      </article>
    </div>
  </div>
</template>
