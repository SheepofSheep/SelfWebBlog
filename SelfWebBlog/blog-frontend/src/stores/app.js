import { reactive } from 'vue'
import { api } from '@/api/client.js'

export const appStore = reactive({
  user: null,
  site: null,
  ready: false,
  toast: '',
  navigationExpanded: false
})

export async function bootstrap() {
  const [user, site] = await Promise.allSettled([api.session(), api.site()])
  appStore.user = user.status === 'fulfilled' ? user.value : null
  appStore.site = site.status === 'fulfilled' ? site.value : null
  appStore.ready = true
}

export function notify(message) {
  appStore.toast = message
  window.clearTimeout(notify.timer)
  notify.timer = window.setTimeout(() => {
    appStore.toast = ''
  }, 2800)
}
