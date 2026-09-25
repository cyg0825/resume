<template>
  <el-card shadow="never">
    <template #header>
      <div class="card-header">
        <span>简历版本管理</span>
        <div class="header-actions">
          <el-button @click="openImport">
            <el-icon><UploadFilled /></el-icon>&nbsp;导入 PDF 简历
          </el-button>
          <el-button type="primary" @click="openCreate">
            <el-icon><Plus /></el-icon>&nbsp;创建新版本
          </el-button>
        </div>
      </div>
    </template>

    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="versionName" label="版本名称" min-width="160">
        <template #default="{ row }">
          <strong>{{ row.versionName }}</strong>
          <el-tag v-if="row.isDefault === 1" type="success" size="small" style="margin-left: 8px">
            当前默认
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="description" label="说明" min-width="220" show-overflow-tooltip />
      <el-table-column label="创建时间" width="180">
        <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="340" fixed="right">
        <template #default="{ row }">
          <el-button
            link type="success"
            :disabled="row.isDefault === 1"
            @click="setDefault(row)"
          >设为默认</el-button>
          <el-button link type="primary" @click="edit(row)">重命名</el-button>
          <el-button link type="primary" @click="previewVersion(row)">预览</el-button>
          <el-button
            link type="danger"
            :disabled="row.isDefault === 1"
            @click="remove(row)"
          >删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 创建版本对话框 -->
    <el-dialog v-model="createVisible" title="创建简历版本" width="520px">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="100px">
        <el-form-item label="版本名称" prop="versionName">
          <el-input v-model="createForm.versionName" placeholder="如：前端开发版 / Java 后端版"
                    maxlength="100" />
        </el-form-item>
        <el-form-item label="版本说明">
          <el-input v-model="createForm.description" type="textarea" :rows="2"
                    placeholder="该版本的定位说明" maxlength="500" />
        </el-form-item>
        <el-form-item label="复制内容自">
          <el-select v-model="createForm.sourceVersionId" clearable placeholder="留空则创建空白版本"
                     style="width: 100%">
            <el-option
              v-for="v in list"
              :key="v.id"
              :value="v.id"
              :label="v.versionName + (v.isDefault === 1 ? '（默认）' : '')"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="confirmCreate">创建</el-button>
      </template>
    </el-dialog>

    <!-- PDF 导入对话框 -->
    <el-dialog v-model="importVisible" title="导入 PDF 简历" width="540px" @closed="resetImport">
      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="AI 会自动识别 PDF 中的个人信息、教育、工作/项目经历、技能与荣誉，生成一个全新的简历版本，不会覆盖任何现有版本。导入后请校对内容，确认无误再设为默认。"
        style="margin-bottom: 16px"
      />

      <el-upload
        ref="uploadRef"
        drag
        :auto-upload="false"
        accept=".pdf"
        :limit="1"
        :file-list="pdfFileList"
        :on-change="onPdfChange"
        :on-exceed="onPdfExceed"
      >
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text">拖拽 PDF 到此处，或<em>点击选择</em></div>
        <template #tip>
          <div class="el-upload__tip">仅支持文字版 PDF，单文件不超过 10MB；扫描件/纯图片 PDF 无法识别</div>
        </template>
      </el-upload>

      <div v-if="importing" class="import-progress">
        <el-progress :percentage="uploadPct" :status="uploadPct >= 100 ? 'success' : ''" />
        <p class="import-hint">
          {{ uploadPct >= 100 ? '文件已上传，AI 正在解析简历内容，通常需要 20~90 秒，请耐心等待…' : '正在上传文件…' }}
        </p>
      </div>

      <el-result
        v-if="importResult"
        icon="success"
        :title="`已生成新版本：${importResult.versionName}`"
        sub-title="请前往各栏目校对识别内容，确认无误后再将其设为默认版本"
      >
        <template #extra>
          <div class="result-counts">
            <el-tag
              v-for="(count, key) in importResult.counts"
              :key="key"
              type="info"
              effect="plain"
              size="large"
            >
              {{ countLabels[key] || key }} {{ count }} 条
            </el-tag>
          </div>
        </template>
      </el-result>

      <template #footer>
        <el-button @click="importVisible = false">关闭</el-button>
        <el-button v-if="importResult" @click="previewImported">预览新版本</el-button>
        <el-button
          type="primary"
          :loading="importing"
          :disabled="!currentPdfFile || importing"
          @click="startImport"
        >{{ importing ? '导入中…' : '开始导入' }}</el-button>
      </template>
    </el-dialog>

    <!-- 重命名对话框 -->
    <el-dialog v-model="editVisible" title="编辑版本" width="480px">
      <el-form label-width="90px">
        <el-form-item label="版本名称">
          <el-input v-model="editForm.versionName" maxlength="100" />
        </el-form-item>
        <el-form-item label="版本说明">
          <el-input v-model="editForm.description" type="textarea" :rows="2" maxlength="500" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="confirmEdit">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getVersions,
  createVersion,
  updateVersion,
  setDefaultVersion,
  deleteVersion,
  importResumePdf
} from '@/api'

const loading = ref(false)
const saving = ref(false)
const list = ref([])

/* ==================== PDF 导入 ==================== */
const importVisible = ref(false)
const importing = ref(false)
const uploadRef = ref()
const currentPdfFile = ref(null)
const pdfFileList = ref([])
const uploadPct = ref(0)
const importResult = ref(null)
const MAX_PDF_SIZE = 10 * 1024 * 1024

const countLabels = {
  education: '教育经历',
  workExperience: '工作经历',
  projectExperience: '项目经历',
  skill: '技能',
  honor: '荣誉'
}

function openImport() {
  resetImport()
  importVisible.value = true
}

function pickPdf(fileLike) {
  if (!fileLike || !fileLike.name) return false
  if (!fileLike.name.toLowerCase().endsWith('.pdf')) {
    ElMessage.error('仅支持 PDF 格式文件')
    return false
  }
  if (fileLike.size > MAX_PDF_SIZE) {
    ElMessage.error('文件大小不能超过 10MB')
    return false
  }
  currentPdfFile.value = fileLike.raw || fileLike
  importResult.value = null
  uploadPct.value = 0
  return true
}

function onPdfChange(file, fileList) {
  if (pickPdf(file)) {
    pdfFileList.value = fileList.slice(-1)
  } else {
    pdfFileList.value = []
    uploadRef.value?.clearFiles()
  }
}

function onPdfExceed(files) {
  uploadRef.value?.clearFiles()
  const raw = files[0]
  if (raw && raw.name.toLowerCase().endsWith('.pdf') && raw.size <= MAX_PDF_SIZE) {
    uploadRef.value?.handleStart(raw)
  } else if (raw) {
    ElMessage.error(raw.name.toLowerCase().endsWith('.pdf') ? '文件大小不能超过 10MB' : '仅支持 PDF 格式文件')
  }
}

async function startImport() {
  if (!currentPdfFile.value) {
    ElMessage.warning('请先选择 PDF 文件')
    return
  }
  importing.value = true
  uploadPct.value = 0
  try {
    const res = await importResumePdf(currentPdfFile.value, (e) => {
      if (e.total) {
        // 预留 5% 给 AI 解析阶段，避免进度条提前到 100%
        uploadPct.value = Math.min(95, Math.round((e.loaded / e.total) * 95))
      }
    })
    uploadPct.value = 100
    importResult.value = res.data
    ElMessage.success('PDF 导入成功')
    await loadList()
  } finally {
    importing.value = false
  }
}

function previewImported() {
  if (importResult.value?.versionId) {
    window.open(`/#/?versionId=${importResult.value.versionId}`, '_blank')
  }
}

function resetImport() {
  currentPdfFile.value = null
  pdfFileList.value = []
  uploadPct.value = 0
  importResult.value = null
  uploadRef.value?.clearFiles()
}

const createVisible = ref(false)
const editVisible = ref(false)
const createFormRef = ref()
const createForm = reactive({ versionName: '', description: '', sourceVersionId: null })
const createRules = {
  versionName: [{ required: true, message: '请输入版本名称', trigger: 'blur' }]
}
const editForm = reactive({ id: null, versionName: '', description: '' })

function formatTime(t) {
  return t ? String(t).replace('T', ' ').slice(0, 16) : ''
}

async function loadList() {
  loading.value = true
  try {
    const res = await getVersions()
    list.value = res.data || []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.assign(createForm, {
    versionName: '',
    description: '',
    sourceVersionId: list.value.find((v) => v.isDefault === 1)?.id || null
  })
  createVisible.value = true
}

async function confirmCreate() {
  await createFormRef.value.validate()
  saving.value = true
  try {
    await createVersion({ ...createForm })
    ElMessage.success('版本创建成功')
    createVisible.value = false
    loadList()
  } finally {
    saving.value = false
  }
}

function previewVersion(row) {
  window.open(`/#/?versionId=${row.id}`, '_blank')
}

function edit(row) {
  Object.assign(editForm, { id: row.id, versionName: row.versionName, description: row.description })
  editVisible.value = true
}

async function confirmEdit() {
  saving.value = true
  try {
    await updateVersion({ ...editForm })
    ElMessage.success('保存成功')
    editVisible.value = false
    loadList()
  } finally {
    saving.value = false
  }
}

async function setDefault(row) {
  await ElMessageBox.confirm(
    `确定将「${row.versionName}」设为默认展示版本吗？访客首页将立即切换。`,
    '切换默认版本',
    { type: 'warning', confirmButtonText: '切换' }
  )
  await setDefaultVersion(row.id)
  ElMessage.success('默认版本已切换')
  loadList()
}

async function remove(row) {
  await ElMessageBox.confirm(
    `确定删除版本「${row.versionName}」吗？该版本下的所有简历内容将一并删除，且不可恢复！`,
    '删除版本',
    { type: 'error', confirmButtonText: '永久删除' }
  )
  await deleteVersion(row.id)
  ElMessage.success('删除成功')
  loadList()
}

onMounted(loadList)
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.import-progress {
  margin-top: 18px;
  padding: 14px 18px 6px;
  background: var(--el-fill-color-light);
  border-radius: 10px;
}

.import-hint {
  margin: 10px 0 8px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
  text-align: center;
}

.result-counts {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
  max-width: 380px;
}
</style>
