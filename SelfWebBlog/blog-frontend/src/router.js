import { createRouter, createWebHistory } from 'vue-router'
import HomePage from '@/pages/HomePage.vue'
import { legacyRouteTarget } from '@/core/legacyRoute.js'

const routes = [
  { path: '/', name: 'home', component: HomePage },
  { path: '/articles', name: 'articles', component: () => import('@/pages/ArticlesPage.vue') },
  { path: '/archive', name: 'archive', component: () => import('@/pages/ArchivePage.vue') },
  { path: '/tags', name: 'tags', component: () => import('@/pages/TagsPage.vue') },
  { path: '/tags/:slug', name: 'tag', component: () => import('@/pages/TagsPage.vue') },
  { path: '/calendar', name: 'calendar', component: () => import('@/pages/CalendarPage.vue') },
  { path: '/guestbook', name: 'guestbook', component: () => import('@/pages/GuestbookPage.vue') },
  { path: '/about', name: 'about', component: () => import('@/pages/AboutPage.vue') },
  { path: '/post/:id', name: 'post', component: () => import('@/pages/PostPage.vue') },
  { path: '/login', name: 'login', component: () => import('@/pages/LoginPage.vue') },
  { path: '/me', name: 'me', component: () => import('@/pages/ProfilePage.vue') },
  {
    path: '/write/:id?',
    name: 'write',
    component: () => import('@/pages/WritePage.vue'),
    meta: { immersive: true }
  },
  {
    path: '/admin',
    name: 'admin',
    component: () => import('@/pages/AdminPage.vue'),
    meta: { immersive: true }
  },
  { path: '/profile', redirect: '/admin' },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('@/pages/NotFoundPage.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})
router.beforeEach((to) => {
  const legacyTarget = legacyRouteTarget(location.href)
  if (legacyTarget && to.path === '/') return legacyTarget
  if (to.path === '/post' && to.query.id) return `/post/${to.query.id}`
})
export default router
