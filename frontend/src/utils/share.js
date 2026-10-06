/**
 * 专属链接访客凭证：
 * - 访客通过 /r/:token 进入，后端校验链接后签发访客 JWT，存在 localStorage
 * - 令牌有效期由后端 app.jwt.share-expire 决定（默认 60 天），期内打开简历无需再次点链接
 * - 链接自身的 expire_time 每次请求都会被后端回查，比令牌更早到期时以链接为准
 * - 过期/吊销后由后端返回 401，前端清除凭证并回到拦截页
 */

const TOKEN_KEY = 'resume-share-token'
const VERSION_KEY = 'resume-share-version'

export function getShareToken() {
  return localStorage.getItem(TOKEN_KEY) || ''
}

export function saveShareAccess({ visitorToken, versionId }) {
  localStorage.setItem(TOKEN_KEY, visitorToken)
  if (versionId) {
    localStorage.setItem(VERSION_KEY, String(versionId))
  } else {
    localStorage.removeItem(VERSION_KEY)
  }
}

export function getShareVersionId() {
  const v = localStorage.getItem(VERSION_KEY)
  return v ? Number(v) : null
}

export function clearShareAccess() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(VERSION_KEY)
}
