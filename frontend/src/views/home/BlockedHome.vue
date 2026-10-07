<template>
  <div class="blocked-page">
    <div class="blocked-card">
      <div class="lock-badge">
        <el-icon :size="34"><Lock /></el-icon>
      </div>
      <h1>{{ info.title }}</h1>
      <p class="desc">{{ info.desc }}</p>
      <p class="hint">
        <el-icon><InfoFilled /></el-icon>
        <span>
          本简历为<strong>私密简历，不对外公开</strong>，请通过站长发给你的<strong>专属链接</strong>打开。
        </span>
      </p>
    </div>
    <div class="bottom-line">
      <span>链接打不开？请联系站长重新获取</span>
      <span class="dot">·</span>
      <router-link to="/login" class="admin-entry">管理入口</router-link>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { Lock, InfoFilled } from '@element-plus/icons-vue'

const route = useRoute()

const INFO_MAP = {
  invalid: {
    title: '链接无效或已被停用',
    desc: '你打开的链接不存在，或已被站长手动停用，无法查看简历。'
  },
  expired: {
    title: '链接访问已失效',
    desc: '你的访客凭证已经过期，或该链接刚被站长停用。请重新点击站长发给你的专属链接打开。'
  },
  views: {
    title: '访问次数已用完',
    desc: '该链接设置了最大访问次数，目前次数已用完。如需继续查看，请联系站长重新生成链接。'
  },
  limit: {
    title: '访问过于频繁',
    desc: '短时间内验证次数过多，请等待约 1 分钟后，再点击专属链接打开。'
  },
  forbidden: {
    title: '请通过专属链接访问',
    desc: '这里是私密简历站点，直接访问网址看不到任何内容。'
  }
}

const info = computed(() => INFO_MAP[route.query.reason] || INFO_MAP.forbidden)
</script>

<style scoped>
.blocked-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 20px;
  background: linear-gradient(160deg, #f5f7fb 0%, #eef2ff 100%);
  padding: 24px;
}

.blocked-card {
  background: #fff;
  border-radius: 20px;
  padding: 46px 48px;
  text-align: center;
  box-shadow: 0 16px 48px rgba(37, 99, 235, 0.12);
  max-width: 460px;
}

@media (max-width: 480px) {
  .blocked-page {
    padding: 16px;
  }

  .blocked-card {
    padding: 32px 22px;
    border-radius: 16px;
  }

  .blocked-card h1 {
    font-size: 19px;
  }
}

.lock-badge {
  width: 72px;
  height: 72px;
  margin: 0 auto 20px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  background: linear-gradient(135deg, #1d4ed8, #3b82f6);
  box-shadow: 0 12px 28px rgba(37, 99, 235, 0.32);
}

.blocked-card h1 {
  margin: 0 0 12px;
  font-size: 21px;
  color: #1e293b;
}

.desc {
  margin: 0 0 22px;
  font-size: 14px;
  line-height: 1.9;
  color: #64748b;
}

.hint {
  margin: 0;
  display: flex;
  align-items: flex-start;
  gap: 8px;
  text-align: left;
  font-size: 13px;
  line-height: 1.9;
  color: #64748b;
  background: #f1f5ff;
  border: 1px solid #dbeafe;
  border-radius: 12px;
  padding: 12px 14px;
}

.hint .el-icon {
  margin-top: 3px;
  color: #2563eb;
  flex-shrink: 0;
}

.hint strong {
  color: #1d4ed8;
}

.bottom-line {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 12.5px;
  color: #94a3b8;
}

.dot {
  color: #cbd5e1;
}

.admin-entry {
  color: #94a3b8;
  text-decoration: none;
  border-bottom: 1px dashed #cbd5e1;
  transition: color 0.2s;
}

.admin-entry:hover {
  color: #2563eb;
  border-bottom-color: #2563eb;
}
</style>
