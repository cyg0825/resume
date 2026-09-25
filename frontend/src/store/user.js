import { defineStore } from 'pinia'
import { login as loginApi, logout as logoutApi } from '@/api'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    username: localStorage.getItem('username') || '',
    nickname: localStorage.getItem('nickname') || ''
  }),
  getters: {
    isLoggedIn: (state) => !!state.token
  },
  actions: {
    async login(loginForm) {
      const res = await loginApi(loginForm)
      const { token, username, nickname } = res.data
      this.token = token
      this.username = username
      this.nickname = nickname || username
      localStorage.setItem('token', token)
      localStorage.setItem('username', username)
      localStorage.setItem('nickname', nickname || username)
    },
    async logout() {
      try {
        await logoutApi()
      } catch (e) {
        // 即使后端调用失败，前端也要清理登录态
      }
      this.reset()
    },
    reset() {
      this.token = ''
      this.username = ''
      this.nickname = ''
      localStorage.removeItem('token')
      localStorage.removeItem('username')
      localStorage.removeItem('nickname')
    }
  }
})
