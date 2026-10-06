<template>
  <div class="share-entry">
    <div class="entry-card">
      <el-icon v-if="loading" class="entry-icon" :size="42"><Loading /></el-icon>
      <el-icon v-else class="entry-icon error-icon" :size="42"><CircleCloseFilled /></el-icon>
      <h1>{{ loading ? '正在打开简历…' : '无法打开简历' }}</h1>
      <p class="entry-msg">{{ loading ? '正在验证专属链接，请稍候' : errorMsg }}</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Loading, CircleCloseFilled } from '@element-plus/icons-vue'
import { accessShareLink } from '@/api'
import { saveShareAccess } from '@/utils/share'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const errorMsg = ref('')

// 失效原因取自后端返回的业务码（ShareLinkService.CODE_LINK_*），
// 不匹配中文提示文案，后端改文案不会影响这里
function reasonFromCode(code) {
  if (code === 4031) return 'expired'
  if (code === 4032) return 'views'
  if (code === 429) return 'limit'
  return 'invalid'
}

onMounted(async () => {
  try {
    const res = await accessShareLink(route.params.token)
    saveShareAccess(res.data || {})
    router.replace('/')
  } catch (e) {
    loading.value = false
    errorMsg.value = e.msg || '链接无效或已过期'
    setTimeout(() => {
      router.replace({ path: '/blocked', query: { reason: reasonFromCode(e.code) } })
    }, 900)
  }
})
</script>

<style scoped>
.share-entry {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(160deg, #f5f7fb  0%, #eef2ff 100%);
  padding: 24px;
}

.entry-card {
  background: #fff;
  border-radius: 18px;
  padding: 44px 48px;
  text-align: center;
  box-shadow: 0 16px 48px rgba(37, 99, 235, 0.12);
  max-width: 420px;
}

.entry-icon {
  color: #2563eb;
  animation: spin 1s linear infinite;
}

.error-icon {
  color: #ef4444;
  animation: none;
}

.entry-card h1 {
  margin: 18px 0 10px;
  font-size: 20px;
  color: #1e293b;
}

.entry-msg {
  margin: 0;
  font-size: 14px;
  color: #64748b;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
