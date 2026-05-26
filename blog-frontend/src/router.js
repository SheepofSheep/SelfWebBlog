import { computed, ref } from 'vue'

// 立即同步设置 hash 值，确保在 state 初始化之前就有正确的 hash
if (!window.location.hash) {
  window.history.replaceState({}, '', '#/')
}

function parseHash() {
  const raw = window.location.hash || '#/'
  const hash = raw.startsWith('#') ? raw.slice(1) : raw
  const [pathPart, queryPart] = hash.split('?')
  const path = pathPart || '/'
  const query = new URLSearchParams(queryPart || '')
  return { path, query }
}

const state = ref(parseHash())

window.addEventListener('hashchange', () => {
  state.value = parseHash()
})

export function useRoute() {
  const path = computed(() => state.value.path)
  const query = computed(() => state.value.query)
  return { path, query }
}

export function navigate(to) {
  const next = to.startsWith('#') ? to : `#${to.startsWith('/') ? '' : '/'}${to}`
  if (window.location.hash === next) return
  window.location.hash = next
}

