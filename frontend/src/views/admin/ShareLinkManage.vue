<template>
  <div>
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>专属链接（{{ list.length }}）</span>
          <el-button type="primary" @click="openDialog">
            <el-icon><Plus /></el-icon>&nbsp;生成链接
          </el-button>
        </div>
      </template>

      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="remark" label="备注（发给谁）" min-width="150">
          <template #default="{ row }">
            <strong>{{ row.remark || '-' }}</strong>
          </template>
        </el-table-column>
        <el-table-column label="简历版本" width="130">
          <template #default="{ row }">
            <el-tag v-if="row.versionId" size="small" type="info" effect="plain">
              {{ versionName(row.versionId) }}
            </el-tag>
            <span v-else class="text-secondary">默认版本</span>
          </template>
        </el-table-column>
        <el-table-column label="访问次数" width="110">
          <template #default="{ row }">
            <span :class="{ 'count-full': isViewFull(row) }">
              {{ row.viewCount }}<template v-if="row.maxViews"> / {{ row.maxViews }}</template>
            </span>
          </template>
        </el-table-column>
        <el-table-column label="有效期至" width="170">
          <template #default="{ row }">
            <span v-if="row.expireTime">{{ formatTime(row.expireTime) }}</span>
            <el-tag v-else size="small" type="success" effect="plain">永久</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="最后访问" width="170">
          <template #default="{ row }">
            <span v-if="row.lastViewTime">{{ formatTime(row.lastViewTime) }}</span>
            <span v-else class="text-secondary">暂无访问</span>
          </template>
        </el-table-column>
        <el-table-column prop="lastViewIp" label="最后 IP" width="140">
          <template #default="{ row }">{{ row.lastViewIp || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusOf(row).type" size="small">{{ statusOf(row).text }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="copyLink(row)">复制链接</el-button>
            <el-button
              link
              type="danger"
              :disabled="row.enabled !== 1"
              @click="remove(row)"
            >吊销</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" title="生成专属链接" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="发给谁" prop="remark">
          <el-input v-model="form.remark" maxlength="100" placeholder="如：某公司 HR（仅后台可见，用于区分链接）" />
        </el-form-item>
        <el-form-item label="简历版本">
          <el-select v-model="form.versionId" clearable placeholder="不绑定=默认版本" style="width: 100%">
            <el-option
              v-for="v in versions"
              :key="v.id"
              :label="v.versionName + (v.isDefault === 1 ? '（默认）' : '')"
              :value="v.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="有效期至">
          <el-date-picker
            v-model="form.expireTime"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="不选=永久有效"
            format="YYYY-MM-DD HH:mm"
            style="width: 100%"
          />
          <div class="field-tip">
            这里控制的是链接本身的有效期；访客换到的登录态另有固定时长（后端默认 60 天），到期后重新点链接即可。
          </div>
        </el-form-item>
        <el-form-item label="最大访问次数">
          <el-input-number
            v-model="form.maxViews"
            :min="1"
            :max="999999"
            placeholder="不限"
            controls-position="right"
            style="width: 200px"
          />
          <span class="field-tip">&nbsp;留空=不限次数</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">生成</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getShareLinks, createShareLink, disableShareLink, getVersions } from '@/api'

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const versions = ref([])
const dialogVisible = ref(false)
const formRef = ref()

const emptyForm = () => ({
  remark: '',
  versionId: null,
  expireTime: null,
  maxViews: null
})
const form = reactive(emptyForm())

const rules = {
  remark: [{ required: true, message: '请填写备注（发给谁）', trigger: 'blur' }]
}

function versionName(id) {
  const v = versions.value.find((item) => item.id === id)
  return v ? v.versionName : `版本${id}`
}

function formatTime(t) {
  return t ? String(t).slice(0, 16) : ''
}

function isViewFull(row) {
  return row.maxViews && row.viewCount >= row.maxViews
}

function statusOf(row) {
  if (row.enabled !== 1) return { type: 'info', text: '已吊销' }
  if (row.expireTime && new Date(row.expireTime.replace(/-/g, '/')) < new Date()) {
    return { type: 'danger', text: '已过期' }
  }
  if (isViewFull(row)) return { type: 'danger', text: '次数用尽' }
  return { type: 'success', text: '生效中' }
}

async function loadAll() {
  loading.value = true
  try {
    const [linkRes, versionRes] = await Promise.all([getShareLinks(), getVersions()])
    list.value = linkRes.data || []
    versions.value = versionRes.data || []
  } finally {
    loading.value = false
  }
}

function openDialog() {
  Object.assign(form, emptyForm())
  dialogVisible.value = true
}

async function save() {
  await formRef.value.validate()
  saving.value = true
  try {
    const res = await createShareLink({ ...form })
    ElMessage.success('链接已生成')
    dialogVisible.value = false
    await loadAll()
    // 生成后立即复制，方便直接发给对方
    copyText(buildUrl(res.data.token))
    ElMessage.success('专属链接已复制到剪贴板，可直接粘贴发送')
  } finally {
    saving.value = false
  }
}

async function remove(row) {
  await ElMessageBox.confirm(
    `确定吊销发给「${row.remark}」的链接吗？吊销后对方立即无法查看，且不可恢复。`,
    '吊销确认',
    { type: 'warning', confirmButtonText: '吊销', cancelButtonText: '取消' }
  )
  await disableShareLink(row.id)
  ElMessage.success('已吊销')
  loadAll()
}

function buildUrl(token) {
  return `${window.location.origin}/#/r/${token}`
}

function copyLink(row) {
  copyText(buildUrl(row.token))
  ElMessage.success('链接已复制：' + buildUrl(row.token))
}

// navigator.clipboard 在非安全上下文不可用，兜底用临时 textarea
function copyText(text) {
  if (navigator.clipboard && window.isSecureContext) {
    navigator.clipboard.writeText(text).catch(() => fallbackCopy(text))
  } else {
    fallbackCopy(text)
  }
}

function fallbackCopy(text) {
  const textarea = document.createElement('textarea')
  textarea.value = text
  textarea.style.position = 'fixed'
  textarea.style.opacity = '0'
  document.body.appendChild(textarea)
  textarea.select()
  document.execCommand('copy')
  document.body.removeChild(textarea)
}

onMounted(loadAll)
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.text-secondary {
  color: var(--el-text-color-secondary);
}

.count-full {
  color: var(--el-color-danger);
  font-weight: 700;
}

.field-tip {
  font-size: 12px;
  color: #94a3b8;
}
</style>
