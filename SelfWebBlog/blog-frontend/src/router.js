import { computed, ref } from 'vue'

export const routeRecords = [
  { path: '/', name: 'home', meta: { public: true } },
  { path: '/login', name: 'login', meta: { public: true } },
  { path: '/archive', name: 'archive', meta: { public: true } },
  { path: '/post', name: 'post', prefix: true, meta: { public: true } },
  { path: '/me', name: 'me', meta: { requiresAuth: true } },
  { path: '/profile', name: 'profile', meta: { requiresAuth: true, requiresRole: 'ADMIN' } },
  { path: '/write', name: 'write', meta: { requiresAuth: true, requiresRole: 'ADMIN' } }
]

const notFoundMeta = { public: true, notFound: true }

if (!window.location.hash) {
  window.history.replaceState({}, '', '#/')
}

function parseHash() {
  const raw = window.location.hash || '#/'
  const hash = raw.startsWith('#') ? raw.slice(1) : raw
  const [pathPart, queryPart] = hash.split('?')
  const normalizedPath = pathPart || '/'
  const path = normalizedPath.startsWith('/') ? normalizedPath : `/${normalizedPath}`
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

export function matchRoute(path) {
  return routeRecords.find((route) => {
    if (route.prefix) return path === route.path || path.startsWith(`${route.path}/`)
    return route.path === path
  })
}

export function getRouteMeta(path) {
  return matchRoute(path)?.meta || notFoundMeta
}
