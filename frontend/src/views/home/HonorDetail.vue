<template>
  <div class="honor-detail-page">
    <header class="detail-topbar">
      <div class="topbar-inner">
        <button class="back-btn" @click="goBack">
          <el-icon><ArrowLeft /></el-icon>&nbsp;返回简历
        </button>
        <span v-if="siteTitle" class="site-name" @click="goBack">{{ siteTitle }}</span>
      </div>
    </header>

    <main class="detail-main" v-loading="loading">
      <template v-if="item">
        <div class="detail-card">
          <div class="detail-head">
            <div class="head-badge" :class="levelClass">
              <el-icon :size="22"><Trophy /></el-icon>
            </div>
            <div class="head-text">
              <h1 class="detail-title">{{ item.title }}</h1>
              <el-tag v-if="item.level" effect="dark" round size="default" class="head-tag">
                {{ item.level }}
              </el-tag>
            </div>
          </div>

          <div class="detail-meta">
            <span v-if="item.issuer" class="meta-chip">
              <el-icon><OfficeBuilding /></el-icon>{{ item.issuer }}
            </span>
            <span v-if="item.honorDate" class="meta-chip">
              <el-icon><Calendar /></el-icon>{{ formatDate(item.honorDate) }}
            </span>
          </div>

          <el-image
            v-if="item.image"
            :src="item.image"
            fit="contain"
            :preview-src-list="[item.image]"
            :preview-teleported="true"
            hide-on-click-modal
            class="detail-image"
          />
          <div v-else class="no-image">
            <el-icon :size="40"><Picture /></el-icon>
            <span>该证书暂未上传图片</span>
          </div>

          <div v-if="item.description" class="detail-desc">
            <h2 class="desc-title">补充说明</h2>
            <p>{{ item.description }}</p>
          </div>
        </div>
      </template>

      <el-empty v-else-if="!loading" description="未找到该荣誉证书">
        <el-button type="primary" round @click="goBack">返回简历首页</el-button>
      </el-empty>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Trophy, OfficeBuilding, Calendar, Picture } from '@element-plus/icons-vue'
import { getHonors, getSiteConfig } from '@/api'
import { useThemeStore } from '@/store/theme'

const route = useRoute()
const router = useRouter()
const themeStore = useThemeStore()

const loading = ref(false)
const item = ref(null)
const siteTitle = ref('')

const levelClass = computed(() => {
  const level = item.value?.level
  if (!level) return 'lv-other'
  if (level.includes('国家') || level.includes('国际')) return 'lv-national'
  if (level.includes('省') || level.includes('高级')) return 'lv-province'
  if (level.includes('校') || level.includes('中级')) return 'lv-school'
  return 'lv-other'
})

function formatDate(d) {
  if (!d) return ''
  return String(d).slice(0, 10)
}

function goBack() {
  const v = route.query.v
  router.push(v ? `/?versionId=${v}` : '/')
}

onMounted(async () => {
  loading.value = true
  try {
    const [honorRes, configRes] = await Promise.all([
      getHonors(route.query.v || undefined),
      getSiteConfig()
    ])
    const cfg = configRes.data || {}
    siteTitle.value = cfg.siteTitle || '个人简历'
    themeStore.init(cfg.defaultTheme)
    const id = Number(route.params.id)
    item.value = (honorRes.data || []).find((h) => h.id === id) || null
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.honor-detail-page {
  min-height: 100vh;
  background: var(--color-bg);
}

.detail-topbar {
  position: sticky;
  top: 0;
  z-index: 10;
  background: color-mix(in srgb, var(--color-surface) 88%, transparent);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid var(--color-border);
}

.topbar-inner {
  max-width: 860px;
  margin: 0 auto;
  padding: 12px 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  border: 1px solid var(--color-border);
  background: var(--color-surface);
  color: var(--color-primary);
  border-radius: 999px;
  padding: 7px 16px;
  font-size: 13.5px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.back-btn:hover {
  color: #fff;
  border-color: transparent;
  background: linear-gradient(135deg, var(--color-primary-dark), var(--color-primary-light));
}

.site-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-secondary);
  cursor: pointer;
}

.detail-main {
  max-width: 860px;
  margin: 0 auto;
  padding: 28px 20px 60px;
}

.detail-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: 18px;
  padding: 28px 30px;
  box-shadow: var(--shadow-card);
}

.detail-head {
  display: flex;
  align-items: center;
  gap: 16px;
}

.head-badge {
  flex-shrink: 0;
  width: 52px;
  height: 52px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.head-text {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
}

.detail-title {
  margin: 0;
  font-size: 22px;
  font-weight: 800;
  line-height: 1.4;
  color: var(--color-text);
}

.head-tag {
  font-weight: 600;
}

.detail-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 18px;
}

.meta-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--color-text-secondary);
  background: var(--color-bg-soft);
  border: 1px solid var(--color-border);
  border-radius: 999px;
  padding: 6px 14px;
}

.detail-image {
  display: block;
  width: 100%;
  max-height: 620px;
  margin-top: 22px;
  border-radius: 14px;
  border: 1px solid var(--color-border);
  background: var(--color-bg-soft);
  cursor: zoom-in;
}

.no-image {
  margin-top: 22px;
  border: 1px dashed var(--color-border);
  border-radius: 14px;
  min-height: 240px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: var(--color-text-secondary);
  background: var(--color-bg-soft);
}

.detail-desc {
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid var(--color-border);
}

.desc-title {
  margin: 0 0 10px;
  font-size: 15px;
  font-weight: 700;
  color: var(--color-text);
}

.detail-desc p {
  margin: 0;
  font-size: 14px;
  line-height: 1.9;
  color: var(--color-text-secondary);
  white-space: pre-wrap;
}

.lv-national {
  background: linear-gradient(135deg, #dc2626, #f97316);
  box-shadow: 0 10px 24px rgba(220, 38, 38, 0.3);
}

.lv-province {
  background: linear-gradient(135deg, #d97706, #fbbf24);
  box-shadow: 0 10px 24px rgba(217, 119, 6, 0.3);
}

.lv-school {
  background: linear-gradient(135deg, var(--color-primary-dark), var(--color-primary-light));
  box-shadow: 0 10px 24px color-mix(in srgb, var(--color-primary) 30%, transparent);
}

.lv-other {
  background: linear-gradient(135deg, #059669, #34d399);
  box-shadow: 0 10px 24px rgba(5, 150, 105, 0.28);
}

@media (max-width: 640px) {
  .detail-card {
    padding: 20px 16px;
  }

  .detail-title {
    font-size: 18px;
  }

  .head-badge {
    width: 44px;
    height: 44px;
  }
}
</style>
