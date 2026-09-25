/**
 * 专属链接访客凭证：
 * - 访客通过 /r/:token 进入，后端校验链接后签发短期访客 JWT，存在 localStorage
 * - 之后 12 小时内（SHARE_TOKEN_EXPIRE 可配）打开简历无需再次点链接
 * - 过期/吊销后由后端返回 401，前端清除凭证并回到拦截页
 */

const TOKEN_KEY = 'resume-share-token'
const VERSION_KEY = 'resume-share-version'
const REMARK_KEY = 'resume-share-remark'

export function getShareToken() {
  return localStorage.getItem(TOKEN_KEY) || ''
}

export function saveShareAccess({ visitorToken, versionId, remark }) {
  localStorage.setItem(TOKEN_KEY, visitorToken)
  if (versionId) {
    localStorage.setItem(VERSION_KEY, String(versionId))
  } else {
    localStorage.removeItem(VERSION_KEY)
  }
  if (remark) {
    localStorage.setItem(REMARK_KEY, remark)
  }
}

export function getShareVersionId() {
  const v = localStorage.getItem(VERSION_KEY)
  return v ? Number(v) : null
}

export function getShareRemark() {
  return localStorage.getItem(REMARK_KEY) || ''
}

export function clearShareAccess() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(VERSION_KEY)
  localStorage.removeItem(REMARK_KEY)
}
