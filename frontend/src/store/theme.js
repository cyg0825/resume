import { defineStore } from 'pinia'

/**
 * 主题状态管理：
 * - 内置 3 套主题：商务蓝(default) / 暗夜科技(dark) / 清新绿(fresh)
 * - 选择持久化到 localStorage（key: resume-theme）
 * - 后台可配置默认主题，仅在用户本地没有选择记录时生效
 */
export const THEMES = [
  { key: 'default', name: '商务蓝', desc: '专业沉稳的蓝色系' },
  { key: 'dark', name: '暗夜科技', desc: '深色背景的极客风格' },
  { key: 'fresh', name: '清新绿', desc: '自然明亮的绿色系' }
]

const STORAGE_KEY = 'resume-theme'

export const useThemeStore = defineStore('theme', {
  state: () => ({
    theme: localStorage.getItem(STORAGE_KEY) || 'default',
    /** 后台配置的默认主题（仅在本地无记录时使用） */
    serverDefault: 'default'
  }),
  getters: {
    currentTheme: (state) => THEMES.find((t) => t.key === state.theme) || THEMES[0]
  },
  actions: {
    apply(theme) {
      this.theme = theme
      document.documentElement.setAttribute('data-theme', theme)
      localStorage.setItem(STORAGE_KEY, theme)
    },
    init(serverDefault) {
      this.serverDefault = serverDefault || 'default'
      const saved = localStorage.getItem(STORAGE_KEY)
      // 本地有持久化选择时优先用户选择，否则使用后台默认主题
      this.apply(saved || this.serverDefault)
    }
  }
})
