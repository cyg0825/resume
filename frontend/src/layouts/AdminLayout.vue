<template>
  <el-container class="admin-layout">
    <!-- 移动端遮罩：抽屉打开时点击关闭 -->
    <div
      v-if="mobileMenuOpen"
      class="mobile-mask"
      @click="mobileMenuOpen = false"
    ></div>

    <el-aside
      :width="collapsed ? '64px' : '220px'"
      class="aside"
      :class="{ 'mobile-open': mobileMenuOpen }"
    >
      <div class="logo">
        <el-icon :size="24"><Document /></el-icon>
        <span v-if="!menuCollapsed" class="logo-text">简历管理后台</span>
      </div>
      <el-menu
        :default-active="route.path"
        :collapse="menuCollapsed"
        :collapse-transition="false"
        router
        class="side-menu"
        background-color="transparent"
        text-color="#cbd5e1"
        active-text-color="#ffffff"
        @select="mobileMenuOpen = false"
      >
        <el-menu-item
          v-for="item in menuItems"
          :key="item.path"
          :index="item.path"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          <template #title>{{ item.title }}</template>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-left">
          <!-- 桌面端：折叠/展开侧边栏 -->
          <el-icon class="collapse-btn desktop-only" @click="collapsed = !collapsed">
            <Fold v-if="!collapsed" />
            <Expand v-else />
          </el-icon>
          <!-- 移动端：打开抽屉菜单 -->
          <el-icon class="collapse-btn mobile-only" @click="mobileMenuOpen = true">
            <Menu />
          </el-icon>
          <el-page-header content="" @back="$router.push('/')">
            <template #content>
              <span class="header-title">{{ route.meta.title }}</span>
            </template>
          </el-page-header>
        </div>
        <div class="header-right">
          <el-button text @click="$router.push('/')">
            <el-icon><View /></el-icon><span class="hide-mobile">&nbsp;访问首页</span>
          </el-button>
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-avatar :size="30" class="user-avatar">
                {{ (userStore.nickname || 'A').slice(0, 1) }}
              </el-avatar>
              <span class="hide-mobile">{{ userStore.nickname || userStore.username }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="main">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed, watch, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'
// 后台页面移动端通用适配（弹窗/表单/表格/卡片等，非 scoped 全局规则）
import '@/styles/admin-responsive.css'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
// 桌面端侧边栏折叠态；移动端抽屉开关态（两套互不相干）
const collapsed = ref(false)
const mobileMenuOpen = ref(false)

// 响应式断点：与 admin-responsive.css 的 768px 保持一致
const mq = window.matchMedia('(max-width: 768px)')
const isMobile = ref(mq.matches)
const onMqChange = (e) => {
  isMobile.value = e.matches
  // 回到桌面端时收起可能仍开着的抽屉
  if (!e.matches) mobileMenuOpen.value = false
}
mq.addEventListener('change', onMqChange)
onUnmounted(() => mq.removeEventListener('change', onMqChange))

// 抽屉打开时手机端菜单必须是展开态（显示文字）；桌面端跟随折叠按钮
const menuCollapsed = computed(() => (isMobile.value ? false : collapsed.value))

// 路由切换后自动收起移动端抽屉
watch(() => route.path, () => {
  mobileMenuOpen.value = false
})

// 菜单直接由路由 children 生成，标题/图标只维护路由 meta 一份
const menuItems = router.getRoutes()
  .find((r) => r.path === '/admin')
  .children.map((child) => ({
    path: `/admin/${child.path}`,
    title: child.meta.title,
    icon: child.meta.icon
  }))

async function handleCommand(command) {
  if (command === 'logout') {
    await ElMessageBox.confirm('确定退出登录吗？', '提示', {
      confirmButtonText: '退出',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await userStore.logout()
    router.push('/login')
  }
}
</script>

<style scoped>
.admin-layout {
  height: 100vh;
}

.aside {
  background: linear-gradient(180deg, #0f172a 0%, #1e293b 100%);
  transition: width 0.25s ease;
  overflow: hidden;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #fff;
  font-weight: 700;
  font-size: 16px;
  white-space: nowrap;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.side-menu {
  border-right: none;
}

:deep(.side-menu .el-menu-item.is-active) {
  background: linear-gradient(90deg, #2563eb, #3b82f6);
  border-radius: 8px;
  margin: 4px 10px;
}

:deep(.side-menu .el-menu-item) {
  border-radius: 8px;
  margin: 4px 10px;
}

.header {
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.collapse-btn {
  font-size: 20px;
  cursor: pointer;
  color: #475569;
}

.header-title {
  font-weight: 600;
  color: #1e293b;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 18px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  color: #334155;
  font-size: 14px;
}

.user-avatar {
  background: #2563eb;
  color: #fff;
}

.main {
  background: #f1f5f9;
  padding: 20px;
}

/* —— 移动端显隐控制 —— */
.mobile-only {
  display: none;
}

.mobile-mask {
  display: none;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.18s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* —— 手机端（≤768px）：侧边栏变抽屉，头部压缩 —— */
@media (max-width: 768px) {
  .desktop-only {
    display: none;
  }

  .mobile-only {
    display: inline-flex;
  }

  /* 侧边栏脱离布局，从左侧滑入 */
  .aside {
    position: fixed;
    top: 0;
    left: 0;
    bottom: 0;
    z-index: 2001;
    transform: translateX(-100%);
    transition: transform 0.25s ease;
    box-shadow: 2px 0 12px rgba(0, 0, 0, 0.15);
  }

  /* 手机端强制展开态宽度，避免受桌面折叠态影响只剩 64px */
  .aside.el-aside {
    width: 230px !important;
  }

  .aside.mobile-open {
    transform: translateX(0);
  }

  .mobile-mask {
    display: block;
    position: fixed;
    inset: 0;
    background: rgba(15, 23, 42, 0.45);
    z-index: 2000;
  }

  .header {
    padding: 0 12px;
  }

  .header-left {
    gap: 8px;
  }

  .header-title {
    font-size: 14px;
    max-width: 42vw;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .header-right {
    gap: 8px;
  }

  /* 隐藏“访问首页”文字与用户名，只留图标/头像 */
  .hide-mobile {
    display: none;
  }

  .main {
    padding: 12px;
  }
}

</style>
