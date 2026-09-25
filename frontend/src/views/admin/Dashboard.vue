<template>
  <div class="dashboard">
    <!-- 指标卡片 -->
    <el-row :gutter="16" class="stat-row">
      <el-col :xs="12" :sm="12" :md="6" v-for="card in cards" :key="card.label">
        <div class="stat-card" :style="{ '--card-color': card.color }">
          <el-icon :size="30" class="stat-icon"><component :is="card.icon" /></el-icon>
          <div class="stat-meta">
            <div class="stat-value">{{ card.value }}</div>
            <div class="stat-label" :title="card.tip">{{ card.label }}</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :xs="24" :lg="16">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="card-header">
              <span>近 {{ days }} 天访问趋势</span>
              <el-radio-group v-model="days" size="small" @change="loadTrend">
                <el-radio-button :value="7">7 天</el-radio-button>
                <el-radio-button :value="15">15 天</el-radio-button>
                <el-radio-button :value="30">30 天</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div ref="trendChartRef" class="chart"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="8">
        <el-card shadow="never" class="chart-card">
          <template #header>访问来源分布</template>
          <div ref="sourceChartRef" class="chart"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="bottom-row">
      <el-col :xs="24" :lg="12">
        <el-card shadow="never">
          <template #header>近 7 天活跃 IP Top 10（独立访客）</template>
          <el-table :data="topIps" size="small" stripe>
            <el-table-column type="index" label="#" width="50" />
            <el-table-column prop="ip" label="IP 地址" />
            <el-table-column prop="count" label="访问次数" width="100" sortable />
          </el-table>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="12">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span>快捷操作</span>
            </div>
          </template>
          <div class="quick-actions">
            <el-button @click="$router.push('/admin/profile')">
              <el-icon><User /></el-icon>&nbsp;编辑基本信息
            </el-button>
            <el-button @click="$router.push('/admin/version')">
              <el-icon><Files /></el-icon>&nbsp;简历版本管理
            </el-button>
            <el-button @click="$router.push('/admin/theme')">
              <el-icon><Brush /></el-icon>&nbsp;主题与站点配置
            </el-button>
            <el-button @click="$router.push('/admin/experience')">
              <el-icon><Briefcase /></el-icon>&nbsp;维护工作/项目
            </el-button>
            <el-button type="primary" @click="$router.push('/')">
              <el-icon><View /></el-icon>&nbsp;预览简历首页
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from 'echarts'
import {
  getStatsOverview,
  getStatsTrend,
  getStatsSources,
  getStatsTopIps
} from '@/api'

const days = ref(7)
const overview = reactive({
  totalVisits: 0,
  uniqueVisitors: 0,
  todayVisits: 0,
  todayUniqueVisitors: 0
})
const topIps = ref([])

const trendChartRef = ref()
const sourceChartRef = ref()
let trendChart = null
let sourceChart = null

const cards = ref([
  {
    label: '累计访问量',
    tip: '会话口径：同一访客 30 分钟内多次打开或刷新页面只计一次；关闭页面超过 30 分钟再次访问计为新的一次',
    value: 0,
    icon: 'View',
    color: '#2563eb'
  },
  { label: '独立访客(IP去重)', tip: '按访客 IP 全局去重后的累计人数', value: 0, icon: 'UserFilled', color: '#059669' },
  { label: '今日访问', tip: '今日 0 点起的会话访问次数（30 分钟去重）', value: 0, icon: 'Sunny', color: '#f59e0b' },
  { label: '今日独立访客', tip: '今日 0 点起按 IP 去重的访客数', value: 0, icon: 'Aim', color: '#8b5cf6' }
])

function syncCards() {
  cards.value[0].value = overview.totalVisits
  cards.value[1].value = overview.uniqueVisitors
  cards.value[2].value = overview.todayVisits
  cards.value[3].value = overview.todayUniqueVisitors
}

async function loadOverview() {
  const res = await getStatsOverview()
  Object.assign(overview, res.data)
  syncCards()
}

async function loadTrend() {
  const res = await getStatsTrend(days.value)
  const data = res.data || []
  trendChart?.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['访问次数', '独立访客'], top: 0 },
    grid: { left: 40, right: 20, top: 40, bottom: 30 },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: data.map((d) => d.date.slice(5))
    },
    yAxis: { type: 'value', minInterval: 1 },
    series: [
      {
        name: '访问次数',
        type: 'line',
        smooth: true,
        areaStyle: { opacity: 0.15 },
        itemStyle: { color: '#2563eb' },
        data: data.map((d) => d.visits)
      },
      {
        name: '独立访客',
        type: 'bar',
        barMaxWidth: 18,
        itemStyle: { color: '#34d399', borderRadius: [4, 4, 0, 0] },
        data: data.map((d) => d.uniqueVisitors)
      }
    ]
  })
}

async function loadSources() {
  const res = await getStatsSources()
  const data = (res.data || []).map((d) => ({ name: d.source, value: d.count }))
  sourceChart?.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 0, type: 'scroll' },
    color: ['#2563eb', '#34d399', '#f59e0b', '#a78bfa', '#f472b6', '#94a3b8'],
    series: [
      {
        type: 'pie',
        radius: ['42%', '68%'],
        center: ['50%', '44%'],
        avoidLabelOverlap: true,
        itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
        label: { show: false },
        emphasis: { label: { show: true, fontSize: 14, fontWeight: 'bold' } },
        data: data.length ? data : [{ name: '暂无数据', value: 1, itemStyle: { color: '#e2e8f0' } }]
      }
    ]
  })
}

async function loadTopIps() {
  const res = await getStatsTopIps(7)
  topIps.value = res.data || []
}

function handleResize() {
  trendChart?.resize()
  sourceChart?.resize()
}

onMounted(async () => {
  await nextTick()
  trendChart = echarts.init(trendChartRef.value)
  sourceChart = echarts.init(sourceChartRef.value)
  await Promise.all([loadOverview(), loadTrend(), loadSources(), loadTopIps()])
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  sourceChart?.dispose()
})
</script>

<style scoped>
.stat-row {
  margin-bottom: 16px;
}

.stat-card {
  background: #fff;
  border-radius: 12px;
  padding: 18px 16px;
  display: flex;
  align-items: center;
  gap: 14px;
  box-shadow: 0 2px 10px rgba(15, 23, 42, 0.05);
  margin-bottom: 12px;
}

.stat-icon {
  color: var(--card-color);
  background: color-mix(in srgb, var(--card-color) 12%, transparent);
  padding: 10px;
  border-radius: 10px;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #0f172a;
  line-height: 1.2;
}

.stat-label {
  font-size: 12px;
  color: #64748b;
  margin-top: 4px;
}

.chart-card {
  margin-bottom: 16px;
}

.chart {
  height: 340px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.bottom-row {
  margin-top: 0;
}

.quick-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  padding: 6px 0;
}

.badge {
  margin-left: 6px;
}
</style>
