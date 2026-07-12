import { createApp } from 'vue'
import './style.css'
import App from './App.vue'
import { useAppearance } from './composables/useAppearance'

useAppearance().initializeAppearance()
const app = createApp(App)
app.mount('#app')
