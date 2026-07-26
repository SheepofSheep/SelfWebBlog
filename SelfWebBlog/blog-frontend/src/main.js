import { createApp } from 'vue'
import App from './App.vue'
import router from './router.js'
import { installAppearance } from './core/appearance.js'
import { bootstrap } from './stores/app.js'
import './styles.css'

installAppearance()
createApp(App).use(router).mount('#app')
bootstrap()
