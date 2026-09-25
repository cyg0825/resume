<template>
  <el-row :gutter="16">
    <el-col :xs="24" :lg="14">
      <el-card shadow="never" v-loading="loading">
        <template #header>主题与站点配置</template>

        <el-form :model="form" label-width="130px" style="max-width: 640px" @submit.prevent>
          <el-form-item label="站点标题">
            <el-input v-model="form.siteTitle" maxlength="100" />
          </el-form-item>

          <el-form-item label="默认主题">
            <el-radio-group v-model="form.defaultTheme">
              <div v-for="t in THEMES" :key="t.key" class="theme-option">
                <el-radio :value="t.key">
                  <div class="theme-card">
                    <span class="theme-swatch" :style="{ background: swatches[t.key] }"></span>
                    <div>
                      <div class="theme-card-name">{{ t.name }}</div>
                      <div class="theme-card-desc">{{ t.desc }}</div>
                    </div>
                  </div>
                </el-radio>
              </div>
            </el-radio-group>
            <div class="field-tip">
              默认主题仅对未手动切换过主题的访客生效；访客自己选择的主题会保存在其浏览器中。
            </div>
          </el-form-item>

          <el-form-item label="AI 问答">
            <el-switch
              v-model="aiEnabledBool"
              active-text="开放简历 AI 问答"
              inactive-text="关闭 AI 问答入口"
              inline-prompt
            />
            <div class="field-tip">
              AI 问答基于当前默认版本简历内容。大模型接口需在后端 application.yml 配置 AI_API_KEY，
              未配置时自动使用本地关键词问答。
            </div>
          </el-form-item>

          <el-form-item>
            <el-button type="primary" :loading="saving" @click="save">保存配置</el-button>
            <el-button @click="load">重置</el-button>
            <el-button @click="preview">预览前台效果</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </el-col>

    <el-col :xs="24" :lg="10">
      <el-card shadow="never" class="preview-card">
        <template #header>主题预览</template>
        <div class="preview-box" :data-theme="form.defaultTheme">
          <div class="preview-hero">Hero</div>
          <div class="preview-blocks">
            <div class="preview-block"></div>
            <div class="preview-block small"></div>
            <div class="preview-block small"></div>
          </div>
        </div>
      </el-card>

      <el-card shadow="never" class="cache-card">
        <template #header>缓存管理</template>
        <div class="cache-desc">
          图片链接自带文件版本指纹，替换图片后访客会自动看到新图，一般无需手动刷新。
          若在服务器上直接覆盖了图片文件或页面仍显示旧内容，点此立即刷新。
        </div>
        <el-button type="warning" plain :loading="refreshing" @click="refreshCacheNow">
          一键刷新缓存
        </el-button>
      </el-card>
    </el-col>
  </el-row>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getSiteConfig, updateSiteConfig, refreshCache } from '@/api'
import { THEMES } from '@/store/theme'

const router = useRouter()
const loading = ref(false)
const saving = ref(false)
const refreshing = ref(false)

const form = reactive({
  siteTitle: '',
  defaultTheme: 'default',
  aiEnabled: 1
})

const aiEnabledBool = computed({
  get: () => form.aiEnabled === 1,
  set: (v) => { form.aiEnabled = v ? 1 : 0 }
})

const swatches = {
  default: 'linear-gradient(135deg, #1e3a8a, #38bdf8)',
  dark: 'linear-gradient(135deg, #0f172a, #22d3ee)',
  fresh: 'linear-gradient(135deg, #064e3b, #34d399)'
}

async function load() {
  loading.value = true
  try {
    const res = await getSiteConfig()
    Object.assign(form, {
      siteTitle: '个人简历',
      defaultTheme: 'default',
      aiEnabled: 1,
      ...res.data
    })
  } finally {
    loading.value = false
  }
}

async function save() {
  saving.value = true
  try {
    await updateSiteConfig({ ...form })
    ElMessage.success('配置保存成功')
  } finally {
    saving.value = false
  }
}

function preview() {
  router.push('/')
}

async function refreshCacheNow() {
  refreshing.value = true
  try {
    await refreshCache()
    ElMessage.success('缓存已刷新，全站图片将按最新文件重新加载')
  } finally {
    refreshing.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.theme-option {
  padding: 6px 0;
}

:deep(.theme-option .el-radio) {
  height: auto;
  align-items: flex-start;
  white-space: normal;
}

.theme-card {
  display: flex;
  align-items: center;
  gap: 12px;
}

.theme-swatch {
  width: 46px;
  height: 46px;
  border-radius: 10px;
  flex-shrink: 0;
}

.theme-card-name {
  font-weight: 600;
  font-size: 14px;
}

.theme-card-desc {
  font-size: 12px;
  color: #94a3b8;
}

.field-tip {
  font-size: 12px;
  color: #94a3b8;
  line-height: 1.6;
  margin-top: 4px;
}

.preview-card {
  position: sticky;
  top: 80px;
}

.cache-card {
  margin-top: 16px;
}

.cache-desc {
  font-size: 12px;
  color: #94a3b8;
  line-height: 1.6;
  margin-bottom: 12px;
}

.preview-box {
  border-radius: 12px;
  padding: 14px;
  background: var(--color-bg);
  transition: background 0.3s;
}

.preview-hero {
  height: 90px;
  border-radius: 10px;
  background: var(--hero-gradient);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  font-weight: 700;
  letter-spacing: 4px;
}

.preview-blocks {
  margin-top: 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.preview-block {
  height: 42px;
  border-radius: 8px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
}

.preview-block.small {
  height: 18px;
  width: 70%;
}
</style>
