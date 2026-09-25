<template>
  <div>
    <el-card shadow="never" class="owner-bar">
      <div class="owner-row">
        <span class="owner-label">
          <el-icon><User /></el-icon>&nbsp;荣誉归属人：
        </span>
        <el-select
          v-model="currentOwner"
          filterable
          allow-create
          default-first-option
          placeholder="选择姓名，或直接输入新姓名后回车"
          class="owner-select"
          @change="loadList"
        >
          <el-option v-for="name in owners" :key="name" :label="name" :value="name" />
        </el-select>
        <span class="owner-tip">同一人名下的所有简历版本共享这里的荣誉</span>
      </div>
    </el-card>

    <el-card shadow="never" style="margin-top: 14px">
      <template #header>
        <div class="card-header">
          <span>{{ currentOwner || '未选择归属人' }} 的荣誉证书（{{ list.length }}）</span>
          <el-button type="primary" :disabled="!currentOwner" @click="openDialog()">
            <el-icon><Plus /></el-icon>&nbsp;新增荣誉
          </el-button>
        </div>
      </template>

      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column label="证书图片" width="96">
          <template #default="{ row }">
            <el-image
              v-if="row.image"
              :src="row.image"
              fit="cover"
              preview-src-list="[row.image]"
              :preview-teleported="true"
              class="row-thumb"
            />
            <span v-else class="text-secondary">无</span>
          </template>
        </el-table-column>
        <el-table-column label="级别" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.level" size="small" effect="plain">{{ row.level }}</el-tag>
            <span v-else class="text-secondary">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="荣誉名称" min-width="220" />
        <el-table-column prop="issuer" label="颁发机构" min-width="160">
          <template #default="{ row }">{{ row.issuer || '-' }}</template>
        </el-table-column>
        <el-table-column prop="honorDate" label="获奖日期" width="120">
          <template #default="{ row }">{{ row.honorDate ? String(row.honorDate).slice(0, 10) : '-' }}</template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="70" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑荣誉证书' : '新增荣誉证书'" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="荣誉名称" prop="title">
          <el-input v-model="form.title" maxlength="200" placeholder="如：蓝桥杯大赛 · 省赛三等奖" />
        </el-form-item>
        <el-form-item label="证书图片">
          <el-upload
            class="cert-uploader"
            :show-file-list="false"
            :http-request="handleUpload"
            accept="image/*"
          >
            <el-image v-if="form.image" :src="form.image" fit="cover" class="cert-preview" />
            <div v-else class="cert-upload-placeholder">
              <el-icon :size="24"><Plus /></el-icon>
              <span>上传证书图片</span>
            </div>
          </el-upload>
          <el-button v-if="form.image" link type="danger" size="small" @click="form.image = ''">
            删除图片
          </el-button>
          <div class="field-tip">前台列表只显示名称，访客点击卡片后可查看证书大图</div>
        </el-form-item>
        <el-form-item label="级别" prop="level">
          <el-select
            v-model="form.level"
            allow-create
            filterable
            default-first-option
            clearable
            placeholder="选择或输入级别"
            style="width: 100%"
          >
            <el-option v-for="l in levelOptions" :key="l" :label="l" :value="l" />
          </el-select>
        </el-form-item>
        <el-form-item label="颁发机构">
          <el-input v-model="form.issuer" maxlength="100" placeholder="如：工业和信息化部人才交流中心（选填）" />
        </el-form-item>
        <el-form-item label="获奖日期">
          <el-date-picker
            v-model="form.honorDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择日期（选填）"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="补充说明">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="2"
            maxlength="500"
            show-word-limit
            placeholder="获奖背景、负责内容等（选填）"
          />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" :max="9999" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getOwners,
  getAdminHonors,
  createHonor,
  updateHonor,
  deleteHonor,
  uploadFile
} from '@/api'

const loading = ref(false)
const saving = ref(false)
const owners = ref([])
const currentOwner = ref('')
const list = ref([])
const dialogVisible = ref(false)
const formRef = ref()

const emptyForm = () => ({
  id: null,
  title: '',
  image: '',
  level: '校级',
  issuer: '',
  honorDate: '',
  description: '',
  sort: 0
})
const form = reactive(emptyForm())

const levelOptions = ['国家级', '省级', '市级', '校级', '高级', '中级', '初级', '荣誉证书']

const rules = {
  title: [{ required: true, message: '请输入荣誉名称', trigger: 'blur' }]
}

async function loadOwners(selectName) {
  const res = await getOwners()
  owners.value = res.data || []
  if (selectName) {
    currentOwner.value = selectName
  } else if (!currentOwner.value && owners.value.length) {
    currentOwner.value = owners.value[0]
  }
  if (currentOwner.value) await loadList()
}

async function loadList() {
  if (!currentOwner.value) {
    list.value = []
    return
  }
  currentOwner.value = currentOwner.value.trim()
  loading.value = true
  try {
    const res = await getAdminHonors(currentOwner.value)
    list.value = res.data || []
  } finally {
    loading.value = false
  }
}

function openDialog(row) {
  Object.assign(form, emptyForm(), row ? { ...row } : {})
  dialogVisible.value = true
}

async function handleUpload(option) {
  try {
    const res = await uploadFile(option.file)
    form.image = res.data.url
    ElMessage.success('证书图片上传成功')
  } catch (e) {
    /* 错误提示已由拦截器处理 */
  }
}

async function save() {
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = { ...form }
    if (!payload.honorDate) payload.honorDate = null
    if (!payload.image) payload.image = null
    if (form.id) {
      await updateHonor(payload)
    } else {
      await createHonor({ ...payload, ownerName: currentOwner.value })
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    await loadOwners(currentOwner.value)
  } finally {
    saving.value = false
  }
}

async function remove(row) {
  await ElMessageBox.confirm(`确定删除荣誉「${row.title}」吗？`, '提示', { type: 'warning' })
  await deleteHonor(row.id)
  ElMessage.success('删除成功')
  loadList()
}

onMounted(() => loadOwners())
</script>

<style scoped>
.owner-bar {
  margin-bottom: 0;
}

.owner-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.owner-label {
  font-size: 15px;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  white-space: nowrap;
}

.owner-select {
  width: 280px;
}

.owner-tip {
  font-size: 12px;
  color: #94a3b8;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.text-secondary {
  color: var(--el-text-color-secondary);
}

.row-thumb {
  width: 64px;
  height: 44px;
  border-radius: 6px;
  cursor: zoom-in;
}

.cert-preview {
  width: 180px;
  height: 120px;
  border-radius: 8px;
  border: 1px solid var(--el-border-color);
  cursor: pointer;
}

.cert-upload-placeholder {
  width: 180px;
  height: 120px;
  border: 1px dashed var(--el-border-color);
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  color: var(--el-text-color-secondary);
  transition: border-color 0.2s, color 0.2s;
}

.cert-uploader:hover .cert-upload-placeholder {
  border-color: var(--el-color-primary);
  color: var(--el-color-primary);
}

.field-tip {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 4px;
}
</style>
