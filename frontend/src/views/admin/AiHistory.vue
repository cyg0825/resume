<template>
  <el-card shadow="never">
    <template #header>
      <div class="card-header">
        <span>AI 问答历史记录</span>
        <div>
          <el-input
            v-model="sessionId"
            placeholder="按会话 ID 过滤"
            clearable
            style="width: 240px; margin-right: 10px"
            @keyup.enter="loadList(1)"
            @clear="loadList(1)"
          />
          <el-button type="primary" @click="loadList(1)">查询</el-button>
          <el-button type="danger" plain @click="clearAll">清空全部</el-button>
        </div>
      </div>
    </template>

    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column label="会话" width="180">
        <template #default="{ row }">
          <el-tooltip :content="row.sessionId" placement="top">
            <el-tag size="small" effect="plain">{{ String(row.sessionId).slice(0, 12) }}…</el-tag>
          </el-tooltip>
        </template>
      </el-table-column>
      <el-table-column label="问答内容" min-width="420">
        <template #default="{ row }">
          <div class="qa">
            <p class="question"><strong>问：</strong>{{ row.question }}</p>
            <p class="answer"><strong>答：</strong>{{ row.answer }}</p>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="来源" width="100">
        <template #default="{ row }">
          <el-tag :type="row.source === 'llm' ? 'success' : 'info'" size="small">
            {{ row.source === 'llm' ? '大模型' : '本地问答' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="时间" width="170">
        <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ row }">
          <el-button link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination">
      <el-pagination
        background
        layout="total, prev, pager, next"
        :total="total"
        :page-size="size"
        :current-page="current"
        @current-change="loadList"
      />
    </div>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAiHistory, deleteAiHistory, clearAiHistory } from '@/api'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const current = ref(1)
const size = 10
const sessionId = ref('')

function formatTime(t) {
  return t ? String(t).replace('T', ' ').slice(0, 16) : ''
}

async function loadList(page = 1) {
  loading.value = true
  current.value = page
  try {
    const res = await getAiHistory({
      current: page,
      size,
      sessionId: sessionId.value || undefined
    })
    list.value = res.data.records || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

async function remove(row) {
  await ElMessageBox.confirm('确定删除该条问答记录吗？', '提示', { type: 'warning' })
  await deleteAiHistory(row.id)
  ElMessage.success('删除成功')
  loadList(current.value)
}

async function clearAll() {
  await ElMessageBox.confirm('确定清空全部问答记录吗？此操作不可恢复！', '危险操作', {
    type: 'error',
    confirmButtonText: '全部清空'
  })
  await clearAiHistory()
  ElMessage.success('已清空')
  loadList(1)
}

onMounted(() => loadList(1))
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.qa p {
  margin: 0 0 6px;
  font-size: 13px;
  line-height: 1.7;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.question strong {
  color: #2563eb;
}

.answer strong {
  color: #059669;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
