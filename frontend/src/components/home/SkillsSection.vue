<template>
  <section class="section-block" id="skills">
    <h2 class="section-title"><el-icon><Histogram /></el-icon>技能特长</h2>
    <div class="skill-groups">
      <div v-for="group in groups" :key="group.category" class="skill-group">
        <h3 class="group-title">{{ group.category }}</h3>
        <div class="skill-list">
          <div v-for="skill in group.items" :key="skill.id" class="skill-item">
            <div class="skill-label">
              <span class="skill-name">{{ skill.name }}</span>
              <span class="level-text">{{ skill.level }}%</span>
            </div>
            <el-progress
              class="skill-bar"
              :percentage="Number(skill.level) || 0"
              :stroke-width="7"
              :color="progressColors"
              :show-text="false"
            />
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed } from 'vue'
import { useThemeStore } from '@/store/theme'

const props = defineProps({
  list: { type: Array, default: () => [] }
})

const themeStore = useThemeStore()
const progressColorMap = {
  default: [
    { color: '#2563eb', percentage: 60 },
    { color: '#f59e0b', percentage: 85 },
    { color: '#10b981', percentage: 100 }
  ],
  dark: [
    { color: '#22d3ee', percentage: 60 },
    { color: '#a78bfa', percentage: 85 },
    { color: '#34d399', percentage: 100 }
  ],
  fresh: [
    { color: '#059669', percentage: 60 },
    { color: '#f97316', percentage: 85 },
    { color: '#10b981', percentage: 100 }
  ]
}
const progressColors = computed(() => progressColorMap[themeStore.theme] || progressColorMap.default)

// 按分类分组，保持顺序
const groups = computed(() => {
  const map = new Map()
  for (const skill of props.list) {
    const category = skill.category || '其他'
    if (!map.has(category)) {
      map.set(category, { category, items: [] })
    }
    map.get(category).items.push(skill)
  }
  return Array.from(map.values())
})
</script>

<style scoped>
/* 分组卡片流式排列、高度随内容自适应 */
.skill-groups {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(190px, 1fr));
  gap: 12px;
  align-items: start;
}

.skill-group {
  position: relative;
  overflow: hidden;
  padding: 13px 14px 11px;
  background: linear-gradient(180deg, var(--color-surface), var(--color-bg-soft));
  border: 1px solid var(--color-border);
  border-radius: 12px;
  transition: transform 0.25s ease, box-shadow 0.25s ease, border-color 0.25s ease;
}

.skill-group::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, var(--color-primary-light), var(--color-primary-dark));
  opacity: 0.85;
}

.skill-group:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-card-hover);
  border-color: color-mix(in srgb, var(--color-primary) 30%, var(--color-border));
}

.group-title {
  display: flex;
  align-items: center;
  gap: 7px;
  margin: 0 0 10px;
  font-size: 14px;
  font-weight: 700;
  color: var(--color-primary);
}

.group-title::before {
  content: '';
  width: 8px;
  height: 8px;
  border-radius: 2px;
  background: linear-gradient(135deg, var(--color-primary-dark), var(--color-primary-light));
  transform: rotate(45deg);
}

/* 组内技能两列排布，窄卡片自动单列；行间距由 skill-item 底部留白提供 */
.skill-list {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(130px, 1fr));
  column-gap: 14px;
}

.skill-item {
  margin-bottom: 10px;
  min-width: 0;
}

.skill-label {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 6px;
  font-size: 12.5px;
  line-height: 1.45;
  margin-bottom: 5px;
  color: var(--color-text);
}

.skill-name {
  font-weight: 600;
  min-width: 0;
}

.level-text {
  flex-shrink: 0;
  min-width: 38px;
  text-align: center;
  padding: 0 7px;
  font-size: 11px;
  font-weight: 700;
  color: var(--color-primary-dark);
  background: var(--tag-bg);
  border-radius: 999px;
  font-variant-numeric: tabular-nums;
}

/* 进度条不撑满整行，紧凑短小 */
.skill-bar {
  max-width: 130px;
}

/* 进度条圆角与微光 */
.skill-group :deep(.el-progress-bar__outer) {
  border-radius: 999px;
  background-color: color-mix(in srgb, var(--color-text) 7%, transparent);
}

.skill-group :deep(.el-progress-bar__inner) {
  border-radius: 999px;
  box-shadow: 0 0 8px color-mix(in srgb, var(--color-primary) 45%, transparent);
}

/* ========== PDF 导出：分组/进度条紧凑，组内技能两列排布压缩高度 ========== */
.pdf-exporting .skill-groups {
  gap: 7px;
}

.pdf-exporting .skill-group {
  padding: 9px 11px 8px;
  border-radius: 10px;
}

.pdf-exporting .group-title {
  margin-bottom: 7px;
  font-size: 13px;
}

.pdf-exporting .skill-list {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  column-gap: 10px;
}

.pdf-exporting .skill-item {
  margin-bottom: 5px;
}

/* PDF 只保留技能名称：进度条与百分比不导出 */
.pdf-exporting .skill-bar,
.pdf-exporting .level-text {
  display: none;
}

.pdf-exporting .skill-label {
  margin-bottom: 0;
  font-size: 12px;
  line-height: 1.4;
}

/* 单栏（窄屏）时技能恢复普通大卡片：分组多列排布、同排等高，不随字数伸缩；
   双栏桌面态保持上面的紧凑流式样式不变 */
@media (max-width: 880px) {
  .skill-groups {
    align-items: stretch;
  }
}
</style>
