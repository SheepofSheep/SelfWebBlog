<script setup>
import { Image, LoaderCircle } from 'lucide-vue-next'
import { onMounted, ref } from 'vue'
import { listArticleAssets } from '../../../api/uploads'
import AppDialog from '../../../components/ui/AppDialog.vue'
import EmptyState from '../../../components/ui/EmptyState.vue'
import { toAbsoluteUrl } from '../../../utils/url'

defineProps({ open: { type: Boolean, default: false } })
defineEmits(['close', 'select'])
const assets = ref([])
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    assets.value = await listArticleAssets()
  } catch {
    assets.value = []
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <AppDialog
    :open="open"
    title="文章图片"
    description="选择已上传图片，不会重复上传文件。"
    @close="$emit('close')"
  >
    <div v-if="loading" class="asset-loading"><LoaderCircle :size="20" /></div>
    <div v-else-if="assets.length" class="asset-grid">
      <button v-for="asset in assets" :key="asset.id" type="button" @click="$emit('select', asset)">
        <img :src="toAbsoluteUrl(asset.smallWebpUrl || asset.originalUrl)" alt="" loading="lazy" />
        <span>{{ asset.width }} × {{ asset.height }}</span>
      </button>
    </div>
    <EmptyState v-else title="还没有文章图片" description="粘贴或拖入图片后会出现在这里。">
      <template #icon><Image :size="20" /></template>
    </EmptyState>
  </AppDialog>
</template>

<style scoped>
.asset-loading {
  min-height: 180px;
  display: grid;
  place-items: center;
  color: var(--text-secondary);
}
.asset-loading svg {
  animation: spin 0.8s linear infinite;
}
.asset-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 9px;
}
.asset-grid button {
  min-width: 0;
  display: grid;
  gap: 6px;
  padding: 6px;
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-control);
  background: transparent;
  color: var(--text-secondary);
  font: inherit;
  font-size: 0.7rem;
  cursor: pointer;
}
.asset-grid button:hover {
  border-color: var(--accent);
}
.asset-grid img {
  width: 100%;
  aspect-ratio: 4 / 3;
  border-radius: 4px;
  object-fit: cover;
}
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
@media (max-width: 520px) {
  .asset-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
