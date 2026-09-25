import { createRouter, createWebHashHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/home/ResumeHome.vue'),
    meta: { title: '个人简历', requiresShare: true }
  },
  {
    // 专属链接唯一入口：校验通过后跳转首页
    path: '/r/:token',
    name: 'ShareEntry',
    component: () => import('@/views/home/ShareEntry.vue'),
    meta: { title: '正在打开简历…' }
  },
  {
    path: '/blocked',
    name: 'Blocked',
    component: () => import('@/views/home/BlockedHome.vue'),
    meta: { title: '无法访问' }
  },
  {
    path: '/honor/:id',
    name: 'HonorDetail',
    component: () => import('@/views/home/HonorDetail.vue'),
    meta: { title: '荣誉证书详情', requiresShare: true }
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/admin/Login.vue'),
    meta: { title: '管理员登录' }
  },
  {
    path: '/admin',
    component: () => import('@/layouts/AdminLayout.vue'),
    redirect: '/admin/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/admin/Dashboard.vue'),
        meta: { title: '数据概览', icon: 'DataLine' }
      },
      {
        path: 'profile',
        name: 'AdminProfile',
        component: () => import('@/views/admin/ProfileEdit.vue'),
        meta: { title: '基本信息', icon: 'User' }
      },
      {
        path: 'education',
        name: 'AdminEducation',
        component: () => import('@/views/admin/EducationManage.vue'),
        meta: { title: '教育经历', icon: 'School' }
      },
      {
        path: 'experience',
        name: 'AdminExperience',
        component: () => import('@/views/admin/ExperienceManage.vue'),
        meta: { title: '工作/项目', icon: 'Briefcase' }
      },
      {
        path: 'skill',
        name: 'AdminSkill',
        component: () => import('@/views/admin/SkillManage.vue'),
        meta: { title: '技能管理', icon: 'Histogram' }
      },
      {
        path: 'honor',
        name: 'AdminHonor',
        component: () => import('@/views/admin/HonorManage.vue'),
        meta: { title: '荣誉证书', icon: 'Trophy' }
      },
      {
        path: 'portfolio',
        name: 'AdminPortfolio',
        component: () => import('@/views/admin/PortfolioManage.vue'),
        meta: { title: '作品集', icon: 'Picture' }
      },
      {
        path: 'ai-history',
        name: 'AdminAiHistory',
        component: () => import('@/views/admin/AiHistory.vue'),
        meta: { title: 'AI 问答记录', icon: 'ChatLineRound' }
      },
      {
        path: 'version',
        name: 'AdminVersion',
        component: () => import('@/views/admin/VersionManage.vue'),
        meta: { title: '简历版本', icon: 'Files' }
      },
      {
        path: 'share',
        name: 'AdminShare',
        component: () => import('@/views/admin/ShareLinkManage.vue'),
        meta: { title: '专属链接', icon: 'Link' }
      },
      {
        path: 'theme',
        name: 'AdminTheme',
        component: () => import('@/views/admin/ThemeSetting.vue'),
        meta: { title: '主题与站点', icon: 'Brush' }
      }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  }
})

// 全局前置守卫：
// - 后台页面需要管理员登录；已登录时访问登录页直接进后台
// - 简历页面（requiresShare）需持有管理员令牌或专属链接访客令牌，否则进入拦截页
router.beforeEach((to, from, next) => {
  document.title = to.meta.title ? `${to.meta.title} - 个人简历` : '个人简历'
  const token = localStorage.getItem('token')
  if (to.path.startsWith('/admin')) {
    if (!token) {
      next({ path: '/login', query: { redirect: to.fullPath } })
      return
    }
  }
  if (to.path === '/login' && token) {
    next('/admin/dashboard')
    return
  }
  if (to.meta.requiresShare && !token && !localStorage.getItem('resume-share-token')) {
    next({ path: '/blocked', query: { reason: 'forbidden' } })
    return
  }
  next()
})

export default router
