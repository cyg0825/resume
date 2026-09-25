<template>
  <section class="section-block" id="education">
    <h2 class="section-title"><el-icon><School /></el-icon>教育经历</h2>
    <div class="edu-list">
      <div v-for="item in list" :key="item.id" class="edu-card">
        <div class="edu-main">
          <div class="edu-head">
            <span class="school">{{ item.school }}</span>
            <el-tag v-if="item.degree" size="small" effect="plain" round>{{ item.degree }}</el-tag>
          </div>
          <p v-if="item.major" class="major">{{ item.major }}</p>
          <p v-if="formatPeriod(item.startDate, item.endDate)" class="edu-date">
            <el-icon><Calendar /></el-icon>{{ formatPeriod(item.startDate, item.endDate) }}
          </p>
          <p v-if="item.description" class="desc">{{ item.description }}</p>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
// 后端日期为 yyyy-MM-dd，简历展示统一精简为 yyyy.MM
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

defineProps({
  list: { type: Array, default: () => [] }
})
</script>

<style scoped>
.edu-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.edu-card {
  position: relative;
  overflow: hidden;
  padding: 13px 15px;
  background: linear-gradient(180deg, var(--color-surface), var(--color-bg-soft));
  border: 1px solid var(--color-border);
  border-radius: 12px;
  transition: transform 0.25s ease, box-shadow 0.25s ease, border-color 0.25s ease;
}

/* 左侧主题色条 */
.edu-card::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  background: linear-gradient(180deg, var(--color-primary-light), var(--color-primary-dark));
  opacity: 0.85;
}

.edu-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-card-hover);
  border-color: color-mix(in srgb, var(--color-primary) 32%, var(--color-border));
}

.edu-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}

.school {
  font-weight: 700;
  font-size: 15px;
  color: var(--color-text);
}

.major {
  margin: 7px 0 0;
  color: var(--color-primary);
  font-size: 13.5px;
  font-weight: 600;
}

.edu-date {
  display: flex;
  align-items: center;
  gap: 5px;
  margin: 7px 0 0;
  font-size: 12.5px;
  font-weight: 600;
  color: var(--color-text-secondary);
  white-space: nowrap;
}

.desc {
  margin: 8px 0 0;
  font-size: 12.5px;
  line-height: 1.85;
  color: var(--color-text-secondary);
  white-space: pre-wrap;
}
</style>
