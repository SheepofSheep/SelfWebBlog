import { createRouter, createWebHistory } from 'vue-router'
import { showToast } from '../composables/toast'
import { useAuthStore } from '../stores/authStore'

const HomePage = () => import('../pages/HomePage.vue')
const ArchivePage = () => import('../pages/ArchivePage.vue')
const PostPage = () => import('../pages/PostPage.vue')
const LoginPage = () => import('../pages/LoginPage.vue')
const WritePage = () => import('../pages/WritePage.vue')
const AdminPage = () => import('../pages/ProfilePage.vue')
const VisitorProfilePage = () => import('../pages/VisitorProfilePage.vue')
const AboutPage = () => import('../pages/AboutPage.vue')
const RoutePlaceholderPage = () => import('../pages/RoutePlaceholderPage.vue')

export const router = createRouter({
  history: createWebHistory(),
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) return savedPosition
    if (to.hash) return { el: to.hash, behavior: 'smooth' }
    return { top: 0 }
  },
  routes: [
    { path: '/', name: 'home', component: HomePage, meta: { public: true } },
    { path: '/archive', name: 'archive', component: ArchivePage, meta: { public: true } },
    {
      path: '/tags',
      name: 'tags',
      component: RoutePlaceholderPage,
      props: { title: '标签', description: '按主题浏览文章。' },
      meta: { public: true }
    },
    {
      path: '/calendar',
      name: 'calendar',
      component: RoutePlaceholderPage,
      props: { title: '内容日历', description: '按发布日期查找文章。' },
      meta: { public: true }
    },
    {
      path: '/guestbook',
      name: 'guestbook',
      component: RoutePlaceholderPage,
      props: { title: '留言板', description: '给 Gabriel 留下一段话。' },
      meta: { public: true }
    },
    {
      path: '/about',
      name: 'about',
      component: AboutPage,
      meta: { public: true }
    },
    { path: '/post/:id(\\d+)', name: 'post', component: PostPage, meta: { public: true } },
    {
      path: '/post',
      redirect: (to) => {
        const id = String(to.query.id || '')
        return /^\d+$/.test(id) ? { name: 'post', params: { id } } : { name: 'archive' }
      }
    },
    { path: '/login', name: 'login', component: LoginPage, meta: { public: true } },
    {
      path: '/me',
      name: 'me',
      component: VisitorProfilePage,
      meta: { requiresAuth: true }
    },
    {
      path: '/write',
      name: 'write',
      component: WritePage,
      meta: { requiresAuth: true, requiresRole: 'ADMIN' }
    },
    {
      path: '/admin',
      name: 'admin',
      component: AdminPage,
      meta: { requiresAuth: true, requiresRole: 'ADMIN' }
    },
    { path: '/profile', redirect: { name: 'admin' } },
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: RoutePlaceholderPage,
      props: { title: '页面不存在', description: '这个地址没有对应的内容。' },
      meta: { public: true, notFound: true }
    }
  ]
})

let authRestored = false

router.beforeEach(async (to) => {
  const { user, restoreUser, clearUserState, loadUserInfo } = useAuthStore()

  if (!authRestored) {
    restoreUser()
    if (localStorage.getItem('token')) {
      try {
        await loadUserInfo()
      } catch {
        clearUserState()
      }
    }
    authRestored = true
  }

  if (to.meta.requiresAuth && !user.value) {
    showToast('登录后就可以继续访问这里。', 'warning')
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  if (to.meta.requiresRole && user.value?.role !== to.meta.requiresRole) {
    showToast('这里是博主工作台，当前账号不能进入。', 'warning')
    return { name: 'home' }
  }

  return true
})
