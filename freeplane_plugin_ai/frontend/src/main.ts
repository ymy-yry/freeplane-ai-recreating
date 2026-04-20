import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'

const app = createApp(App)
app.use(createPinia()) // 挂载 Pinia ✅ 必须加
app.mount('#app')
