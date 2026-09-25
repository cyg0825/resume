<template>
  <section class="section-block" id="honors">
    <h2 class="section-title"><el-icon><Trophy /></el-icon>荣誉证书</h2>
    <div class="honor-grid">
      <div
        v-for="item in list"
        :key="item.id"
        class="honor-card"
        :class="levelClass(item.level)"
        role="button"
        tabindex="0"
        @click="openDetail(item)"
        @keyup.enter="openDetail(item)"
      >
        <div class="honor-badge">
          <el-icon :size="18"><Trophy /></el-icon>
        </div>
        <div class="honor-body">
          <h3 class="honor-title">{{ item.title }}</h3>
          <el-tag
            v-if="item.level"
            size="small"
            effect="dark"
            round
            class="level-tag"
          >{{ item.level }}</el-tag>
        </div>
        <el-icon class="honor-arrow"><ArrowRight /></el-icon>
      </div>
    </div>
  </section>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { ArrowRight } from '@element-plus/icons-vue'

const props = defineProps({
  list: { type: Array, default: () => [] },
  versionId: { type: [Number, String], default: null }
})

const router = useRouter()

function openDetail(item) {
  router.push({
    path: `/honor/${item.id}`,
    query: props.versionId ? { v: props.versionId } : {}
  })
}

// 配色：国家级红橙、省级金黄、校级蓝；高级/中级/初级 沿用金/蓝/绿
function levelClass(level) {
  if (!level) return 'level-other'
  if (level.includes('国家') || level.includes('国际')) return 'level-national'
  if (level.includes('省') || level.includes('高级')) return 'level-province'
  if (level.includes('校') || level.includes('中级')) return 'level-school'
  return 'level-other'
}
</script>

<style scoped>
.honor-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(205px, 1fr));
  gap: 10px;
}

.honor-card {
  position: relative;
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 10px 12px;
  border-radius: 10px;
  background: linear-gradient(180deg, var(--color-surface), var(--color-bg-soft));
  border: 1px solid var(--color-border);
  overflow: hidden;
  cursor: pointer;
  outline: none;
  transition: transform 0.25s ease, box-shadow 0.25s ease, border-color 0.25s ease;
}

/* 左侧等级色条，悬停时显现 */
.honor-card::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  opacity: 0;
  transition: opacity 0.25s ease;
}

.honor-card:hover,
.honor-card:focus-visible {
  transform: translateY(-3px);
  box-shadow: var(--shadow-card-hover);
}

.honor-card:hover::before,
.honor-card:focus-visible::before {
  opacity: 1;
}

.honor-badge {
  flex-shrink: 0;
  width: 30px;
  height: 30px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.honor-badge .el-icon {
  font-size: 15px;
}

.level-national .honor-badge {
  background: linear-gradient(135deg, #dc2626, #f97316);
  box-shadow: 0 6px 16px rgba(220, 38, 38, 0.35);
}

.level-province .honor-badge {
  background: linear-gradient(135deg, #d97706, #fbbf24);
  box-shadow: 0 6px 16px rgba(217, 119, 6, 0.35);
}

.level-school .honor-badge {
  background: linear-gradient(135deg, var(--color-primary-dark), var(--color-primary-light));
  box-shadow: 0 6px 16px color-mix(in srgb, var(--color-primary) 35%, transparent);
}

.level-other .honor-badge {
  background: linear-gradient(135deg, #059669, #34d399);
  box-shadow: 0 6px 16px rgba(5, 150, 105, 0.32);
}

.level-national::before {
  background: linear-gradient(180deg, #dc2626, #f97316);
}

.level-province::before {
  background: linear-gradient(180deg, #d97706, #fbbf24);
}

.level-school::before {
  background: linear-gradient(180deg, var(--color-primary-light), var(--color-primary-dark));
}

.level-other::before {
  background: linear-gradient(180deg, #059669, #34d399);
}

.level-national:hover,
.level-province:hover {
  border-color: rgba(217, 119, 6, 0.35);
}

.level-school:hover {
  border-color: color-mix(in srgb, var(--color-primary) 35%, transparent);
}

.honor-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 5px;
}

.honor-title {
  margin: 0;
  font-size: 13px;
  font-weight: 700;
  line-height: 1.45;
  color: var(--color-text);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.level-tag {
  flex-shrink: 0;
}

.honor-arrow {
  flex-shrink: 0;
  font-size: 14px;
  color: var(--color-text-secondary);
  opacity: 0;
  transform: translateX(-4px);
  transition: opacity 0.25s ease, transform 0.25s ease;
}

.honor-card:hover .honor-arrow,
.honor-card:focus-visible .honor-arrow {
  opacity: 1;
  transform: translateX(0);
}

/* ========== PDF 导出：去奖杯图标/箭头，紧凑排版以压缩页数 ========== */
.pdf-exporting .honor-grid {
  gap: 7px;
}

.pdf-exporting .honor-card {
  gap: 0;
  padding: 7px 10px;
  border-radius: 8px;
}

/* 奖杯图标块与右箭头不导出 */
.pdf-exporting .honor-badge,
.pdf-exporting .honor-arrow {
  display: none;
}

/* 等级颜色改由常驻左色条表达（网页端悬停才显示） */
.pdf-exporting .honor-card::before {
  opacity: 1;
}

.pdf-exporting .honor-body {
  gap: 3px;
}

.pdf-exporting .honor-title {
  font-size: 12px;
  line-height: 1.35;
  -webkit-line-clamp: 2;
}

.pdf-exporting .honor-body :deep(.el-tag) {
  transform: scale(0.9);
  transform-origin: left center;
}
</style>
