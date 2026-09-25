<template>
  <el-dropdown trigger="click" placement="top-end" @command="handleSelect">
    <div class="theme-switcher" title="切换主题">
      <el-icon :size="20"><Brush /></el-icon>
    </div>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item
          v-for="t in THEMES"
          :key="t.key"
          :command="t.key"
          :class="{ active: themeStore.theme === t.key }"
        >
          <span class="theme-dot" :style="{ background: dotColors[t.key] }"></span>
          <div class="theme-text">
            <div class="theme-name">{{ t.name }}</div>
            <div class="theme-desc">{{ t.desc }}</div>
          </div>
          <el-icon v-if="themeStore.theme === t.key" class="check-icon"><Check /></el-icon>
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<script setup>
import { useThemeStore, THEMES } from '@/store/theme'

const themeStore = useThemeStore()

const dotColors = {
  default: 'linear-gradient(135deg, #1e3a8a, #38bdf8)',
  dark: 'linear-gradient(135deg, #0f172a, #22d3ee)',
  fresh: 'linear-gradient(135deg, #064e3b, #34d399)'
}

function handleSelect(key) {
  // 一键切换并持久化到 localStorage
  themeStore.apply(key)
}
</script>

<style scoped>
.theme-switcher {
  width: 46px;
  height: 46px;
  border-radius: 50%;
  background: var(--color-surface);
  color: var(--color-primary);
  box-shadow: var(--shadow-card-hover);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: transform 0.25s ease;
}

.theme-switcher:hover {
  transform: rotate(-20deg) scale(1.08);
}

:deep(.el-dropdown-menu__item) {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 210px;
  padding: 8px 14px;
}

:deep(.el-dropdown-menu__item.active) {
  color: var(--el-color-primary);
  font-weight: 600;
}

.theme-dot {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  flex-shrink: 0;
}

.theme-text {
  flex: 1;
}

.theme-name {
  font-size: 14px;
}

.theme-desc {
  font-size: 11px;
  color: #94a3b8;
}

.check-icon {
  color: var(--el-color-primary);
}
</style>
