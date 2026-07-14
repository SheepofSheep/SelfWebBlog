import { createApp } from 'vue'
import './style.css'
import App from './App.vue'
import { router } from './app/router'
import { legacyHashTarget } from './app/legacyHashRedirect'
import { useAppearance } from './composables/useAppearance'

useAppearance().initializeAppearance()
const legacyTarget = legacyHashTarget(window.location.href)
if (legacyTarget) window.history.replaceState({}, '', legacyTarget)
const app = createApp(App)
app.use(router)
app.mount('#app')
