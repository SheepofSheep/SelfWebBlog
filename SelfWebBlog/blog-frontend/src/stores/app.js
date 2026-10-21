import { reactive } from 'vue'
import { api } from '@/api/client.js'
import { DEFAULT_BACKGROUND_BLUR, normalizeBackgroundBlur } from '@/core/backgroundBlur.js'

function readBackgroundBlur() {
  try {
    return normalizeBackgroundBlur(localStorage.getItem('background-blur'))
  } catch {
    return DEFAULT_BACKGROUND_BLUR
  }
}

export const appStore = reactive({
  user: null,
  site: null,
  ready: false,
  toast: '',
  navigationExpanded: false,
  backgroundBlur: readBackgroundBlur()
})

export function setBackgroundBlur(value) {
  const normalized = normalizeBackgroundBlur(value)
  appStore.backgroundBlur = normalized
  try {
    localStorage.setItem('background-blur', String(normalized))
  } catch {
    // A blocked localStorage should not make the slider unusable.
  }
}

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
