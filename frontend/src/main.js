import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

import App from './App.vue'
import router from './router'
import './styles/theme.css'

const app = createApp(App)

// 全量注册 Element Plus 图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: zhCn })

// v-reveal：元素滚动进入视口时淡入上移（配合 theme.css 的 .reveal 样式）
app.directive('reveal', {
  mounted(el) {
    el.classList.add('reveal')
    if (typeof IntersectionObserver === 'undefined') {
      el.classList.add('reveal-in')
      return
    }
    const io = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            el.classList.add('reveal-in')
            io.disconnect()
          }
        })
      },
      { threshold: 0.06, rootMargin: '0px 0px -36px 0px' }
    )
    io.observe(el)
    el._revealIO = io
  },
  unmounted(el) {
    el._revealIO?.disconnect()
  }
})

app.mount('#app')
