<template>
  <div class="resume-page">
    <!-- 顶部导航 -->
    <header class="top-nav">
      <div class="nav-inner">
        <span class="nav-logo" @click="scrollToTop">
          <el-icon><Document /></el-icon>
          {{ siteTitle }}
        </span>
        <nav class="nav-links">
          <a
            v-for="link in navLinks"
            :key="link.target"
            :class="{ active: activeSection === link.target }"
            @click="scrollTo(link.target)"
          >
            {{ link.label }}
          </a>
        </nav>
        <div class="nav-actions">
          <el-tooltip content="导出 PDF" placement="bottom" :show-after="200">
            <button
              class="nav-icon-btn"
              :disabled="exporting || loading"
              aria-label="导出 PDF"
              @click="handleExport"
            >
              <el-icon><Download /></el-icon>
            </button>
          </el-tooltip>
          <el-tooltip content="后台管理" placement="bottom" :show-after="200">
            <button class="nav-icon-btn" aria-label="后台管理" @click="goAdmin">
              <el-icon><Setting /></el-icon>
            </button>
          </el-tooltip>
        </div>
      </div>
    </header>

    <!-- 数据加载完成前只显示加载态，避免“暂未设置姓名”等兜底文案一闪而过 -->
    <div v-if="loading" class="page-loading">
      <div class="loading-card">
        <span class="loading-logo"><el-icon><Document /></el-icon></span>
        <el-icon class="loading-spinner"><Loading /></el-icon>
        <p class="loading-text">简历加载中…</p>
      </div>
    </div>

    <template v-else>
      <!-- 版本预览提示：仅管理员从后台跳转 ?versionId=x 时显示；访客专属链接/手动拼参数均不显示，避免暴露存在其他版本 -->
      <el-alert
        v-if="isAdmin && route.query.versionId"
        class="preview-tip"
        type="warning"
        :closable="false"
        show-icon
      >
        <template #title>
          当前正在预览指定版本（ID: {{ versionId }}），访客默认看到的是「{{ defaultVersionName }}」。
          <el-button link type="primary" @click="$router.push('/')">返回默认版本</el-button>
        </template>
      </el-alert>

      <!-- 简历主体（PDF 导出范围）：左侧个人信息栏 + 右侧正文流 -->
      <main id="resume-content" class="resume-main">
        <div class="resume-layout">
          <!-- 左栏：头像信息 / 技能 / 教育 / 联系 -->
          <aside class="side-column">
            <HeroSection :profile="profile" />

            <SkillsSection
              v-if="visible.skills"
              v-reveal
              :list="skills"
              class="side-card side-section"
            />
            <EducationSection
              v-if="visible.education"
              v-reveal
              :list="educations"
              class="side-card side-section"
            />
            <ContactSection
              v-if="visible.contact"
              v-reveal
              :profile="profile"
              class="side-card side-section"
            />
          </aside>

          <!-- 右栏：关于 / 经历 / 荣誉 / 作品 -->
          <div class="content-column">
            <AboutSection v-if="visible.about" v-reveal :profile="profile" class="section-gap" />

            <ExperienceSection
              v-if="hasWork"
              v-reveal
              section-id="experience"
              title="工作经历"
              :list="workExperiences"
              class="section-gap"
            />
            <ExperienceSection
              v-if="hasProject"
              v-reveal
              section-id="project"
              title="项目经历"
              :list="projectExperiences"
              class="section-gap"
            />

            <HonorsSection
              v-if="visible.honors"
              v-reveal
              :list="honors"
              :version-id="versionId"
              class="section-gap"
            />
            <PortfolioSection v-if="visible.portfolio" v-reveal :list="portfolios" class="section-gap" />
          </div>
        </div>

        <footer class="page-footer">
          <p class="copyright">© {{ year }} · 基于 Vue 3 + Spring Boot 3 构建</p>
        </footer>
      </main>
    </template>

    <!-- 扩展功能悬浮组件 -->
    <ThemeSwitcher />
    <AiChatWidget v-if="!loading && aiEnabled" :version-id="versionId" />
  </div>
</template>

<script setup>
import { ref, computed, nextTick, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { exportResumePdf } from '@/utils/pdf'
import {
  getProfile,
  getEducations,
  getExperiences,
  getSkills,
  getHonors,
  getPortfolios,
  getSiteConfig,
  getVersions,
  reportVisit
} from '@/api'
import { useThemeStore } from '@/store/theme'
import HeroSection from '@/components/home/HeroSection.vue'
import AboutSection from '@/components/home/AboutSection.vue'
import EducationSection from '@/components/home/EducationSection.vue'
import ExperienceSection from '@/components/home/ExperienceSection.vue'
import SkillsSection from '@/components/home/SkillsSection.vue'
import HonorsSection from '@/components/home/HonorsSection.vue'
import PortfolioSection from '@/components/home/PortfolioSection.vue'
import ContactSection from '@/components/home/ContactSection.vue'
import ThemeSwitcher from '@/components/home/ThemeSwitcher.vue'
import AiChatWidget from '@/components/home/AiChatWidget.vue'

const route = useRoute()
const router = useRouter()
const themeStore = useThemeStore()

const activeSection = ref('')
const exporting = ref(false)
const loading = ref(true)
let spyObserver = null

const profile = ref({})
const educations = ref([])
const experiences = ref([])
const skills = ref([])
const honors = ref([])
const portfolios = ref([])
const siteTitle = ref('个人简历')
const aiEnabled = ref(true)
const defaultVersionName = ref('默认版本')

// 版本来源优先级：管理员后台预览 ?versionId= ＞ 专属链接绑定版本 ＞ 默认版本。
// 注意：?versionId= 仅对管理员生效——访客即使手动拼接该参数也不认
// （后端同样强制以访客令牌绑定版本为准，双保险防越权）
const isAdmin = computed(() => Boolean(localStorage.getItem('token')))
const versionId = computed(() => {
  if (isAdmin.value && route.query.versionId) return Number(route.query.versionId)
  const shareVersion = localStorage.getItem('resume-share-version')
  return shareVersion ? Number(shareVersion) : null
})
const workExperiences = computed(() => experiences.value.filter((e) => e.type === 1))
const projectExperiences = computed(() =>
  experiences.value.filter((e) => e.type === 2 || e.type == null)
)
const year = new Date().getFullYear()

// 各栏目是否有内容：无内容的区块整体隐藏（不显示“暂无…”占位）
const visible = computed(() => {
  const p = profile.value || {}
  const hasWork = workExperiences.value.length > 0
  const hasProject = projectExperiences.value.length > 0
  return {
    about: Boolean(p.name || p.about),
    education: educations.value.length > 0,
    experience: hasWork || hasProject,
    experienceTarget: hasWork ? 'experience' : 'project',
    skills: skills.value.length > 0,
    honors: honors.value.length > 0,
    portfolio: portfolios.value.length > 0,
    contact: Boolean(p.github || p.gitee || p.csdn)
  }
})
const hasWork = computed(() => workExperiences.value.length > 0)
const hasProject = computed(() => projectExperiences.value.length > 0)

// 导航根据实际存在的栏目动态生成（教育/联系在侧栏，不进顶部导航）
const navLinks = computed(() => {
  const v = visible.value
  const links = []
  if (v.about) links.push({ label: '评价', target: 'about' })
  if (v.experience) links.push({ label: '经历', target: v.experienceTarget })
  if (v.skills) links.push({ label: '技能', target: 'skills' })
  if (v.honors) links.push({ label: '荣誉', target: 'honors' })
  if (v.portfolio) links.push({ label: '作品', target: 'portfolio' })
  return links
})

async function handleExport() {
  exporting.value = true
  try {
    await exportResumePdf(document.getElementById('resume-content'))
    ElMessage.success('PDF 导出成功')
  } catch (e) {
    console.error(e)
    ElMessage.error('PDF 导出失败，请重试')
  } finally {
    exporting.value = false
  }
}

function scrollTo(id) {
  document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

/** 进入后台：已登录直达控制台，未登录由路由守卫带去登录页（登录后回到后台） */
function goAdmin() {
  router.push('/admin/dashboard')
}

function scrollToTop() {
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function getSessionId() {
  let sid = localStorage.getItem('resume-sid')
  if (!sid) {
    sid = `${Date.now()}-${Math.random().toString(36).slice(2, 10)}`
    localStorage.setItem('resume-sid', sid)
  }
  return sid
}

async function loadAll() {
  const vid = versionId.value
  const [profileRes, eduRes, expRes, skillRes, honorRes, portfolioRes] = await Promise.all([
    getProfile(vid),
    getEducations(vid),
    getExperiences(vid),
    getSkills(vid),
    getHonors(vid),
    getPortfolios(vid)
  ])
  profile.value = profileRes.data || {}
  educations.value = eduRes.data || []
  experiences.value = expRes.data || []
  skills.value = skillRes.data || []
  honors.value = honorRes.data || []
  portfolios.value = portfolioRes.data || []
}

async function loadConfigAndReport() {
  try {
    const res = await getSiteConfig()
    const config = res.data || {}
    siteTitle.value = config.siteTitle || '个人简历'
    aiEnabled.value = config.aiEnabled !== 0
    // 初始化主题：本地持久化优先，否则使用后台默认主题
    themeStore.init(config.defaultTheme)

    if (config.siteTitle) {
      document.title = config.siteTitle
    }
  } catch (e) {
    themeStore.init('default')
  }

  // 查询默认版本名称（仅管理员后台预览时请求；访客侧不调用，避免暴露版本列表）
  if (isAdmin.value && route.query.versionId) {
    try {
      const versionsRes = await getVersions()
      const def = (versionsRes.data || []).find((v) => v.isDefault === 1)
      if (def) defaultVersionName.value = def.versionName
    } catch (e) {
      /* 忽略查询失败 */
    }
  }

  // 上报一次访问：同一浏览器会话（标签页生命周期）只上报一次，
  // 避免刷新/HMR/前端路由跳转把 PV 刷高；服务端另有会话窗口去重兜底（默认 30 分钟，可配）
  try {
    if (!sessionStorage.getItem('resume-visit-reported')) {
      await reportVisit({ path: window.location.pathname, sessionId: getSessionId() })
      sessionStorage.setItem('resume-visit-reported', '1')
    }
  } catch (e) {
    /* 忽略统计失败 */
  }
}

// 监听各栏目进入视口，高亮对应导航项（scrollspy）
function setupScrollSpy() {
  if (typeof IntersectionObserver === 'undefined') return
  spyObserver = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) activeSection.value = entry.target.id
      })
    },
    { rootMargin: '-42% 0px -52% 0px', threshold: 0 }
  )
  navLinks.value.forEach((link) => {
    document.querySelectorAll(`#${link.target}`).forEach((el) => spyObserver.observe(el))
  })
  if (navLinks.value.length) activeSection.value = navLinks.value[0].target
}

onMounted(async () => {
  try {
    // 等所有栏目数据与站点配置就绪后再整体渲染，杜绝兜底文案闪烁
    await Promise.all([loadAll(), loadConfigAndReport()])
  } catch (e) {
    console.error('简历数据加载失败', e)
  } finally {
    loading.value = false
    // 空栏目已按数据 v-if 移除，数据渲染完成后再绑定 scrollspy
    await nextTick()
    setupScrollSpy()
  }
})

onUnmounted(() => {
  spyObserver?.disconnect()
})
</script>

<style scoped>
.resume-page {
  min-height: 100vh;
  padding-bottom: 40px;
}

.top-nav {
  position: sticky;
  top: 0;
  z-index: 900;
  background: color-mix(in srgb, var(--color-surface) 82%, transparent);
  backdrop-filter: blur(16px);
  border-bottom: 1px solid var(--color-border);
  box-shadow: 0 2px 18px rgba(15, 23, 42, 0.05);
}

.nav-inner {
  max-width: 1080px;
  margin: 0 auto;
  height: 60px;
  padding: 0 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.nav-logo {
  display: flex;
  align-items: center;
  gap: 9px;
  font-weight: 700;
  font-size: 17px;
  color: var(--color-primary);
  cursor: pointer;
  /* 防止空间紧张时被 flex 压缩成竖排文字 */
  flex-shrink: 0;
  white-space: nowrap;
  min-width: 0;
}

.nav-logo .el-icon {
  padding: 6px;
  width: 30px;
  height: 30px;
  border-radius: 9px;
  color: #fff;
  background: linear-gradient(135deg, var(--color-primary-dark), var(--color-primary-light));
  box-shadow: 0 4px 10px color-mix(in srgb, var(--color-primary) 32%, transparent);
}

.nav-links {
  display: flex;
  gap: 4px;
}

.nav-links a {
  padding: 6px 14px;
  border-radius: 999px;
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-secondary);
  cursor: pointer;
  white-space: nowrap;
  transition: color 0.2s ease, background-color 0.2s ease;
}

.nav-links a:hover {
  color: var(--color-primary);
  background: var(--tag-bg);
}

.nav-links a.active {
  color: #fff;
  background: linear-gradient(135deg, var(--color-primary-dark), var(--color-primary-light));
  box-shadow: 0 4px 12px color-mix(in srgb, var(--color-primary) 35%, transparent);
}

.nav-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.nav-icon-btn {
  width: 36px;
  height: 36px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--color-border);
  border-radius: 10px;
  background: var(--color-card, #fff);
  color: var(--color-primary);
  font-size: 18px;
  cursor: pointer;
  transition: color 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.nav-icon-btn:hover {
  color: #fff;
  border-color: transparent;
  background: linear-gradient(135deg, var(--color-primary-dark), var(--color-primary-light));
  box-shadow: 0 4px 12px color-mix(in srgb, var(--color-primary) 35%, transparent);
  transform: translateY(-1px);
}

.nav-icon-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

/* 首屏数据加载态：居中品牌图标 + 旋转指示，避免兜底文案闪烁 */
.page-loading {
  min-height: calc(100vh - 60px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
}

.loading-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 18px;
  animation: loading-fade 0.3s ease;
}

.loading-logo {
  width: 58px;
  height: 58px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 16px;
  color: #fff;
  font-size: 28px;
  background: linear-gradient(135deg, var(--color-primary-dark), var(--color-primary-light));
  box-shadow: 0 10px 26px color-mix(in srgb, var(--color-primary) 32%, transparent);
}

.loading-spinner {
  font-size: 30px;
  color: var(--color-primary);
  animation: loading-rotate 0.9s linear infinite;
}

.loading-text {
  margin: 0;
  font-size: 14px;
  letter-spacing: 1px;
  color: var(--color-text-secondary);
}

@keyframes loading-rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

@keyframes loading-fade {
  from {
    opacity: 0;
    transform: translateY(6px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.preview-tip {
  max-width: 1080px;
  margin: 16px auto 0;
  border-radius: 10px;
}

.resume-main {
  max-width: 1080px;
  margin: 18px auto 0;
  padding: 0 20px;
}

/* 左信息栏（264px）+ 右正文流 */
.resume-layout {
  display: grid;
  grid-template-columns: 264px minmax(0, 1fr);
  gap: 18px;
  align-items: start;
}

.content-column {
  min-width: 0;
}

.content-column > :first-child {
  margin-top: 0 !important;
}

/* 板块间距统一由 .section-gap 控制，清掉 section-block 默认的 18px 下外边距 */
.content-column > * {
  margin-bottom: 0;
}

.side-column {
  position: sticky;
  top: 74px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
}

.side-card {
  border-radius: 14px;
}

/* 侧栏板块卡片：紧凑一档 */
.side-section.section-block {
  max-width: none;
  margin: 0;
  padding: 15px 15px 16px;
}

.side-section :deep(.section-title) {
  font-size: 15px;
  margin-bottom: 12px;
  gap: 8px;
}

.side-section :deep(.section-title)::before {
  width: 4px;
  height: 15px;
}

.side-section :deep(.section-title .el-icon) {
  width: 27px;
  height: 27px;
  border-radius: 8px;
  font-size: 14px;
}

/* 侧栏教育卡片再收紧一档 */
.side-section :deep(.edu-list) {
  gap: 10px;
}

.side-section :deep(.edu-card) {
  padding: 11px 12px;
}

.side-section :deep(.school) {
  font-size: 13.5px;
}

.side-section :deep(.major) {
  font-size: 12.5px;
  margin-top: 5px;
}

.side-section :deep(.edu-date) {
  font-size: 11.5px;
  margin-top: 5px;
}

.side-section :deep(.desc) {
  font-size: 12px;
  line-height: 1.75;
  margin-top: 6px;
}

/* 侧栏联系卡：三个横排小方块，图标在上名称在下 */
.side-section :deep(.contact-grid) {
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.side-section :deep(.contact-card) {
  flex-direction: column;
  justify-content: center;
  gap: 5px;
  padding: 10px 4px;
  border-radius: 12px;
}

.side-section :deep(.contact-icon) {
  width: 32px;
  height: 32px;
  border-radius: 9px;
  font-size: 15px;
}

.side-section :deep(.contact-name) {
  font-size: 11.5px;
  text-align: center;
  line-height: 1.3;
}

.side-section :deep(.contact-arrow) {
  display: none;
}

.section-gap {
  margin-top: 8px;
}

/* 窄屏：单栏流式排列，头像信息 → 正文 → 教育 → 联系（侧栏板块沉底） */
@media (max-width: 880px) {
  .resume-layout {
    display: flex;
    flex-direction: column;
    align-items: stretch;
    /* 单栏下间距统一由各板块 margin-top 控制，清掉双栏的 18px 栅格间距 */
    gap: 0;
  }

  /* 解除两栏容器包裹，让所有板块按 order 参与同一个流式布局 */
  .side-column,
  .content-column {
    display: contents;
  }

  /* .section-block 默认左右 auto 外边距会阻止 flex 拉伸，单栏下清零；
     flex 中默认 18px 下外边距不再折叠，也一并清零，间距统一交给 margin-top */
  .side-column > *,
  .content-column > * {
    margin-left: 0;
    margin-right: 0;
    margin-bottom: 0;
  }

  /* 显式排列：头像 → 评价 → 工作 → 项目 → 技能 → 荣誉 → 作品 → 教育 → 联系 */
  .side-column > .hero {
    order: 1;
  }

  #about {
    order: 2;
  }

  #experience {
    order: 3;
  }

  #project {
    order: 4;
  }

  #skills {
    order: 5;
    margin-top: 8px;
  }

  #honors {
    order: 6;
  }

  #portfolio {
    order: 7;
  }

  #education {
    order: 8;
    margin-top: 8px;
  }

  #contact {
    order: 9;
    margin-top: 8px;
  }

  .content-column > :first-child {
    margin-top: 8px !important;
  }

  /* 单栏下技能板块恢复普通大卡片尺寸（双栏时才用侧栏紧凑样式） */
  #skills.side-section.section-block {
    padding: 20px 22px;
  }

  #skills.side-section :deep(.section-title) {
    font-size: 17px;
    margin-bottom: 14px;
    gap: 9px;
  }

  #skills.side-section :deep(.section-title)::before {
    width: 5px;
    height: 18px;
  }

  #skills.side-section :deep(.section-title .el-icon) {
    width: 32px;
    height: 32px;
    border-radius: 9px;
    font-size: 16px;
  }
}

@media (max-width: 768px) {
  #skills.side-section.section-block {
    padding: 16px 14px;
  }
}

/* PDF 导出：固定桌面设计宽，保证双栏比例与网格列数不受导出时窗口宽度影响 */
.pdf-exporting .resume-main {
  width: 1080px;
  max-width: none;
}

/* PDF 导出：保持左右双栏（左：信息/技能/教育，右：评价/经历/荣誉），间距收紧以压缩页数 */
.pdf-exporting .resume-layout {
  grid-template-columns: 290px minmax(0, 1fr);
  gap: 10px;
}

.pdf-exporting .side-column {
  position: static;
  gap: 6px;
}

.pdf-exporting .section-gap {
  margin-top: 4px;
}

/* 侧栏板块随全局导出态再收紧一档 */
.pdf-exporting .side-section.section-block {
  padding: 11px 12px 12px;
}

/* PDF 只导出简历主体，页脚（按钮/版权）不出现 */
.pdf-exporting .page-footer {
  display: none;
}

.page-footer {
  text-align: center;
  margin-top: 24px;
  padding: 18px 0 8px;
  color: var(--color-text-secondary);
  border-top: 1px solid var(--color-border);
}

.copyright {
  margin: 0;
  font-size: 12.5px;
}

@media (max-width: 768px) {
  /* 导航两行化：第一行 logo + 操作按钮，第二行锚点链接横向滑动 */
  .nav-inner {
    flex-wrap: wrap;
    row-gap: 4px;
    height: auto;
    padding: 8px 14px;
  }

  .nav-logo {
    font-size: 15px;
    gap: 7px;
  }

  .nav-logo .el-icon {
    width: 26px;
    height: 26px;
    padding: 5px;
    border-radius: 8px;
  }

  .nav-actions {
    gap: 6px;
  }

  .nav-icon-btn {
    width: 32px;
    height: 32px;
    border-radius: 9px;
    font-size: 16px;
  }

  /* 锚点行独占一行，可横向滑动（隐藏滚动条，保留滑动惯性） */
  .nav-links {
    order: 3;
    flex: 0 0 100%;
    gap: 4px;
    overflow-x: auto;
    scrollbar-width: none;
    -webkit-overflow-scrolling: touch;
    padding-bottom: 2px;
  }

  .nav-links::-webkit-scrollbar {
    display: none;
  }

  .nav-links a {
    white-space: nowrap;
    font-size: 13px;
    padding: 5px 12px;
  }

  .resume-main {
    margin-top: 12px;
    padding: 0 14px;
  }

  .preview-tip {
    margin: 12px 14px 0;
  }

  .page-loading {
    min-height: calc(100vh - 96px);
  }
}
</style>
