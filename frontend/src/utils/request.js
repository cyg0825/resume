import axios from 'axios'
import { ElMessage } from 'element-plus'
import { getShareToken, clearShareAccess } from '@/utils/share'

// 统一 axios 实例，baseURL 与后端约定一致为 /api
const request = axios.create({
  baseURL: '/api',
  timeout: 30000
})

// 请求拦截器：自动携带 JWT（后台管理员优先，其次专属链接访客令牌）
request.interceptors.request.use(
  (config) => {
    const adminToken = localStorage.getItem('token')
    if (adminToken) {
      config.headers.Authorization = `Bearer ${adminToken}`
    } else {
      const shareToken = getShareToken()
      if (shareToken) {
        config.headers.Authorization = `Bearer ${shareToken}`
      }
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器：直接返回统一响应体 { code, msg, data }，并处理 401
request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code !== undefined && res.code !== 200) {
      // 专属链接访问等调用方可通过 silentError 自行展示错误
      if (!response.config?.silentError) {
        ElMessage.error(res.msg || '请求失败')
      }
      const err = new Error(res.msg || 'Error')
      err.code = res.code
      err.msg = res.msg
      return Promise.reject(err)
    }
    return res
  },
  (error) => {
    const status = error.response?.status
    const msg = error.response?.data?.msg
    const silent = error.config?.silentError

    if (status === 401) {
      if (localStorage.getItem('token')) {
        // 管理员登录态失效
        localStorage.removeItem('token')
        if (!window.location.hash.startsWith('#/login')) {
          if (!silent) ElMessage.error(msg || '登录已过期，请重新登录')
          window.location.hash = '#/login'
        }
      } else if (getShareToken()) {
        // 访客凭证失效：清除后回到拦截页，提示重新通过链接打开
        clearShareAccess()
        if (!window.location.hash.startsWith('#/blocked')) {
          window.location.hash = '#/blocked?reason=expired'
        }
      } else {
        // 无任何凭证访问受保护内容
        if (!window.location.hash.startsWith('#/blocked')) {
          window.location.hash = '#/blocked?reason=forbidden'
        }
      }
    } else if (!silent) {
      ElMessage.error(msg || error.message || '网络异常')
    }
    const err = new Error(msg || error.message || 'Network Error')
    err.status = status
    err.msg = msg
    return Promise.reject(err)
  }
)

export default request
