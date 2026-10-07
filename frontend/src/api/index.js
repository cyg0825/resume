import request from '@/utils/request'

/* ===================== 认证 ===================== */
export const login = (data) => request.post('/auth/login', data)
export const logout = () => request.post('/auth/logout')
// 管理员改密：校验原密码，新密码 8~64 位；已签发 token 不失效
export const changePassword = (data) => request.put('/admin/account/password', data)

/* ===================== 前台公开接口 ===================== */
export const getProfile = (versionId) =>
  request.get('/profile', { params: { versionId } })
export const getEducations = (versionId) =>
  request.get('/educations', { params: { versionId } })
export const getExperiences = (versionId, type) =>
  request.get('/experiences', { params: { versionId, type } })
export const getSkills = (versionId) =>
  request.get('/skills', { params: { versionId } })
export const getHonors = (versionId) =>
  request.get('/honors', { params: { versionId } })
// 作品集按人（姓名）归档：前台按版本对应的姓名展示同一份作品
export const getPortfolios = (versionId) =>
  request.get('/portfolios', { params: { versionId } })
export const getSiteConfig = () => request.get('/config')

// 访问上报
export const reportVisit = (data) => request.post('/visit', data)

/* ===================== AI 问答（公开提问） ===================== */
export const aiChat = (data) => request.post('/ai/chat', data)

/* ===================== 后台：个人信息 ===================== */
export const updateProfile = (data) => request.put('/admin/profile', data)

/* ===================== 后台：教育经历 ===================== */
export const createEducation = (data) => request.post('/admin/educations', data)
export const updateEducation = (data) => request.put('/admin/educations', data)
export const deleteEducation = (id) => request.delete(`/admin/educations/${id}`)

/* ===================== 后台：工作/项目经历 ===================== */
export const createExperience = (data) => request.post('/admin/experiences', data)
export const updateExperience = (data) => request.put('/admin/experiences', data)
export const deleteExperience = (id) => request.delete(`/admin/experiences/${id}`)

/* ===================== 后台：技能 ===================== */
export const createSkill = (data) => request.post('/admin/skills', data)
export const updateSkill = (data) => request.put('/admin/skills', data)
export const deleteSkill = (id) => request.delete(`/admin/skills/${id}`)

/* ===================== 后台：荣誉证书 ===================== */
export const getAdminHonors = (ownerName) =>
  request.get('/admin/honors', { params: { ownerName } })
export const createHonor = (data) => request.post('/admin/honors', data)
export const updateHonor = (data) => request.put('/admin/honors', data)
export const deleteHonor = (id) => request.delete(`/admin/honors/${id}`)

/* ===================== 后台：按归属人管理的模块（作品集、荣誉证书共用） ===================== */
export const getOwners = () => request.get('/admin/owners')

/* ===================== 后台：作品集（按归属人姓名管理，与版本无关） ===================== */
export const getAdminPortfolios = (ownerName) =>
  request.get('/admin/portfolios', { params: { ownerName } })
export const createPortfolio = (data) => request.post('/admin/portfolios', data)
export const updatePortfolio = (data) => request.put('/admin/portfolios', data)
export const deletePortfolio = (id) => request.delete(`/admin/portfolios/${id}`)

/* ===================== 后台：版本管理 ===================== */
export const getVersions = () => request.get('/admin/versions')
export const createVersion = (data) => request.post('/admin/versions', data)
export const updateVersion = (data) => request.put('/admin/versions', data)
export const setDefaultVersion = (id) => request.put(`/admin/versions/${id}/default`)
export const deleteVersion = (id) => request.delete(`/admin/versions/${id}`)

/* ===================== 专属分享链接 ===================== */
// 访客凭链接 token 换取访客令牌（公开接口；错误由调用方自行展示）
export const accessShareLink = (token) =>
  request.get('/share/access', { params: { token }, silentError: true })
export const getShareLinks = () => request.get('/admin/share-links')
export const createShareLink = (data) => request.post('/admin/share-links', data)
export const disableShareLink = (id) => request.put(`/admin/share-links/${id}/disable`)
export const updateShareLinkToken = (id, data) => request.put(`/admin/share-links/${id}/token`, data)

/* ===================== 后台：站点配置（主题默认值等） ===================== */
export const updateSiteConfig = (data) => request.put('/admin/config', data)
export const refreshCache = () => request.post('/admin/cache/refresh')

/* ===================== 后台：访问统计 ===================== */
export const getStatsOverview = () => request.get('/admin/stats/overview')
export const getStatsTrend = (days = 7) =>
  request.get('/admin/stats/trend', { params: { days } })
export const getStatsSources = () => request.get('/admin/stats/sources')
export const getStatsTopIps = (days = 7) =>
  request.get('/admin/stats/top-ips', { params: { days } })

/* ===================== 后台：AI 问答历史 ===================== */
export const getAiHistory = (params) =>
  request.get('/admin/ai/history', { params })
export const deleteAiHistory = (id) => request.delete(`/admin/ai/history/${id}`)
export const clearAiHistory = () => request.delete('/admin/ai/history')

/* ===================== 后台：文件上传 ===================== */
// 注意：上传使用 FormData，由拦截器自动加 JWT
export const uploadFile = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/admin/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/* ===================== 后台：PDF 简历导入 ===================== */
// AI 解析 PDF 并生成新简历版本，onUploadProgress 可回传进度
export const importResumePdf = (file, onUploadProgress) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/admin/import/pdf', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 180000, // AI 结构化耗时较长
    onUploadProgress
  })
}
