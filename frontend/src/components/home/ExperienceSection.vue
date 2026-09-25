<template>
  <section class="section-block" :id="sectionId">
    <h2 class="section-title">
      <el-icon><Briefcase /></el-icon>{{ title }}
    </h2>
    <!-- 仅一条经历：普通卡片，不使用时间轴 -->
    <div v-if="list.length === 1" class="single-list">
      <div v-for="item in list" :key="item.id" class="single-item">
        <span v-if="formatPeriod(item.startDate, item.endDate)" class="single-time">
          {{ formatPeriod(item.startDate, item.endDate) }}
        </span>
        <div class="timeline-card">
          <div class="card-header">
            <span class="company">{{ item.company }}</span>
            <el-tag size="small" type="primary" effect="plain" round>{{ item.position }}</el-tag>
          </div>
          <p v-if="item.description" class="desc">{{ item.description }}</p>
          <div v-if="techList(item.techStack).length" class="tech-list">
            <span v-for="tech in techList(item.techStack)" :key="tech" class="tech-tag">
              {{ tech }}
            </span>
          </div>
        </div>
      </div>
    </div>

    <!-- 多条经历：时间轴串联 -->
    <el-timeline v-else class="pretty-timeline">
      <el-timeline-item
        v-for="item in list"
        :key="item.id"
        :timestamp="formatPeriod(item.startDate, item.endDate)"
        placement="top"
        :color="themeColor"
      >
        <div class="timeline-card">
          <div class="card-header">
            <span class="company">{{ item.company }}</span>
            <el-tag size="small" type="primary" effect="plain" round>{{ item.position }}</el-tag>
          </div>
          <p v-if="item.description" class="desc">{{ item.description }}</p>
          <div v-if="techList(item.techStack).length" class="tech-list">
            <span v-for="tech in techList(item.techStack)" :key="tech" class="tech-tag">
              {{ tech }}
            </span>
          </div>
        </div>
      </el-timeline-item>
    </el-timeline>
  </section>
</template>

<script setup>
import { computed } from 'vue'
import { useThemeStore } from '@/store/theme'

const props = defineProps({
  title: { type: String, default: '工作经历' },
  sectionId: { type: String, default: 'experience' },
  list: { type: Array, default: () => [] }
})

const themeStore = useThemeStore()
const colorMap = {
  default: '#2563eb',
  dark: '#22d3ee',
  fresh: '#059669'
}
const themeColor = computed(() => colorMap[themeStore.theme] || colorMap.default)

function techList(techStack) {
  if (!techStack) return []
  return techStack.split(/[,，、]/).map((s) => s.trim()).filter(Boolean)
}

// 日期展示精简为 yyyy.MM
function toMonth(value) {
  if (!value) return ''
  const m = String(value).match(/^(\d{4})-(\d{1,2})/)
  return m ? `${m[1]}.${m[2].padStart(2, '0')}` : String(value)
}

function formatPeriod(start, end) {
  const s = toMonth(start)
  const e = toMonth(end)
  if (s && e) return `${s} ~ ${e}`
  if (s) return `${s} ~ 至今`
  if (e) return e
  return ''
}
</script>

<style scoped>
/* 单条经历：日期胶囊 + 卡片，无点无线 */
.single-list {
  display: flex;
  flex-direction: column;
}

.single-time {
  display: inline-block;
  align-self: flex-start;
  margin-bottom: 7px;
  padding: 2px 10px;
  font-size: 11.5px;
  font-weight: 600;
  white-space: nowrap;
  color: var(--color-primary-dark);
  background: var(--tag-bg);
  border-radius: 999px;
}

/* 时间轴卡片左缘与普通卡片完全对齐：
   清掉 el-timeline.is-start 容器自带的 40px 与 wrapper 的 28px 左缩进，
   节点与连线放到卡片左侧外的板块留白（section-block padding）区 */
.pretty-timeline.is-start {
  padding-left: 0;
}

.pretty-timeline :deep(.el-timeline-item__wrapper) {
  padding-left: 0;
}

.pretty-timeline :deep(.el-timeline-item__node) {
  left: -16px;
  box-shadow: 0 0 0 5px color-mix(in srgb, var(--color-primary) 14%, transparent);
}

.pretty-timeline :deep(.el-timeline-item__tail) {
  left: -13px;
  border-left: 2px dashed var(--color-border);
}

/* 窄屏板块留白只有 14px，节点同步收回 4px，仍在留白区内 */
@media (max-width: 640px) {
  .pretty-timeline :deep(.el-timeline-item__node) {
    left: -14px;
  }

  .pretty-timeline :deep(.el-timeline-item__tail) {
    left: -9px;
  }
}

.pretty-timeline :deep(.el-timeline-item__timestamp) {
  display: inline-block;
  margin-bottom: 7px;
  padding: 2px 10px;
  font-size: 11.5px;
  font-weight: 600;
  white-space: nowrap;
  color: var(--color-primary-dark);
  background: var(--tag-bg);
  border-radius: 999px;
}

.timeline-card {
  position: relative;
  overflow: hidden;
  padding: 12px 15px;
  background: linear-gradient(180deg, var(--color-surface), var(--color-bg-soft));
  border: 1px solid var(--color-border);
  border-radius: 12px;
  transition: transform 0.25s ease, box-shadow 0.25s ease, border-color 0.25s ease;
}

.timeline-card::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  background: linear-gradient(180deg, var(--color-primary-light), var(--color-primary-dark));
  opacity: 0;
  transition: opacity 0.25s ease;
}

.timeline-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-card-hover);
  border-color: color-mix(in srgb, var(--color-primary) 32%, var(--color-border));
}

.timeline-card:hover::before {
  opacity: 1;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.company {
  font-weight: 700;
  font-size: 14.5px;
  color: var(--color-text);
}

.desc {
  margin: 7px 0 0;
  font-size: 12.5px;
  line-height: 1.8;
  color: var(--color-text-secondary);
  white-space: pre-wrap;
}

.tech-list {
  margin-top: 8px;
}

/* 收紧时间轴条目之间的默认间距 */
.pretty-timeline :deep(.el-timeline-item) {
  padding-bottom: 12px;
}

.pretty-timeline :deep(.el-timeline-item:last-child) {
  padding-bottom: 0;
}

/* ========== PDF 导出：经历卡片紧凑，压缩整体高度 ========== */
.pdf-exporting .pretty-timeline :deep(.el-timeline-item) {
  padding-bottom: 7px;
}

.pdf-exporting .single-time,
.pdf-exporting .pretty-timeline :deep(.el-timeline-item__timestamp) {
  margin-bottom: 4px;
  padding: 1px 9px;
}

.pdf-exporting .timeline-card {
  padding: 9px 12px;
  border-radius: 10px;
}

.pdf-exporting .card-header {
  gap: 6px;
}

.pdf-exporting .company {
  font-size: 13.5px;
}

.pdf-exporting .desc {
  margin-top: 4px;
  font-size: 12px;
  line-height: 1.55;
}

.pdf-exporting .tech-list {
  margin-top: 5px;
}
</style>
