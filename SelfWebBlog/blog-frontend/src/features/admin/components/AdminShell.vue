<script setup>
import {
  Activity,
  FileText,
  Images,
  LayoutDashboard,
  MessageSquareText,
  Settings,
  ShieldCheck,
  Users
} from 'lucide-vue-next'

defineProps({ section: { type: String, required: true } })
defineEmits(['update:section', 'write'])

const navigation = [
  { key: 'overview', label: '概览', icon: LayoutDashboard },
  { key: 'content', label: '内容', icon: FileText },
  { key: 'interactions', label: '互动', icon: MessageSquareText },
  { key: 'library', label: '标签与图片', icon: Images },
  { key: 'users', label: '用户', icon: Users },
  { key: 'site', label: '站点', icon: Settings },
  { key: 'security', label: '安全', icon: ShieldCheck }
]
</script>

<template>
  <div class="admin-layout">
    <aside class="admin-sidebar">
      <div class="admin-brand">
        <Activity :size="18" />
        <div><strong>Gabriel</strong><span>管理工作台</span></div>
      </div>
      <nav aria-label="管理导航">
        <button
          v-for="item in navigation"
          :key="item.key"
          type="button"
          :class="{ active: section === item.key }"
          :aria-current="section === item.key ? 'page' : undefined"
          @click="$emit('update:section', item.key)"
        >
          <component :is="item.icon" :size="17" />
          <span>{{ item.label }}</span>
        </button>
      </nav>
      <button class="admin-write" type="button" @click="$emit('write')">
        <FileText :size="17" /><span>写文章</span>
      </button>
    </aside>

    <section class="admin-workspace">
      <slot />
    </section>
  </div>
</template>
