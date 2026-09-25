<template>
  <div>
    <el-card shadow="never" class="owner-bar">
      <div class="owner-row">
        <span class="owner-label">
          <el-icon><User /></el-icon>&nbsp;作品归属人：
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
        <span class="owner-tip">同一人名下的所有简历版本共享这里的作品</span>
      </div>
    </el-card>

    <el-card shadow="never" style="margin-top: 14px">
      <template #header>
        <div class="card-header">
          <span>{{ currentOwner || '未选择归属人' }} 的作品集（{{ list.length }}）</span>
          <el-button type="primary" :disabled="!currentOwner" @click="openDialog()">
            <el-icon><Plus /></el-icon>&nbsp;新增作品
          </el-button>
        </div>
      </template>

      <el-row :gutter="16" v-loading="loading">
        <el-col :xs="24" :sm="12" :lg="8" v-for="row in list" :key="row.id">
          <el-card class="portfolio-card" shadow="hover">
            <div class="cover">
              <el-image v-if="row.cover" :src="row.cover" fit="cover" class="cover-img" />
              <div v-else class="cover-empty">
                <el-icon :size="36"><PictureFilled /></el-icon>
              </div>
            </div>
            <h3 class="title">{{ row.title }}</h3>
            <p class="desc">{{ row.description }}</p>
            <div class="card-actions">
              <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
              <el-button link type="danger" @click="remove(row)">删除</el-button>
            </div>
          </el-card>
        </el-col>
        <el-col :span="24" v-if="!loading && !list.length">
          <el-empty description="暂无作品" />
        </el-col>
      </el-row>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑作品' : '新增作品'" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="封面图">
          <el-upload
            class="cover-uploader"
            :show-file-list="false"
            :http-request="handleUpload"
            accept="image/*"
          >
            <el-image v-if="form.cover" :src="form.cover" fit="cover" class="cover-preview" />
            <div v-else class="cover-upload-placeholder">
              <el-icon :size="24"><Plus /></el-icon>
              <span>上传封面</span>
            </div>
          </el-upload>
        </el-form-item>
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" maxlength="100" />
        </el-form-item>
        <el-form-item label="项目链接">
          <el-input v-model="form.url" placeholder="https://..." maxlength="255" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" :max="9999" />
        </el-form-item>
        <el-form-item label="项目描述">
          <el-input v-model="form.description" type="textarea" :rows="4" />
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
  getAdminPortfolios,
  createPortfolio,
  updatePortfolio,
  deletePortfolio,
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
  cover: '',
  url: '',
  description: '',
  sort: 0
})
const form = reactive(emptyForm())

const rules = {
  title: [{ required: true, message: '请输入作品标题', trigger: 'blur' }]
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
    const res = await getAdminPortfolios(currentOwner.value)
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
    form.cover = res.data.url
    ElMessage.success('封面上传成功')
  } catch (e) {
    /* ignore */
  }
}

async function save() {
  await formRef.value.validate()
  saving.value = true
  try {
    if (form.id) {
      await updatePortfolio({ ...form })
    } else {
      await createPortfolio({ ...form, ownerName: currentOwner.value })
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    // 新增归属人后刷新候选列表并保持当前选中
    await loadOwners(currentOwner.value)
  } finally {
    saving.value = false
  }
}

async function remove(row) {
  await ElMessageBox.confirm(`确定删除作品「${row.title}」吗？`, '提示', { type: 'warning' })
  await deletePortfolio(row.id)
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

.portfolio-card {
  margin-bottom: 16px;
}

.cover {
  height: 160px;
  border-radius: 8px;
  overflow: hidden;
  background: #f1f5f9;
}

.cover-img {
  width: 100%;
  height: 100%;
}

.cover-empty {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #94a3b8;
}

.title {
  margin: 12px 0 6px;
  font-size: 16px;
}

.desc {
  font-size: 13px;
  color: #64748b;
  line-height: 1.6;
  min-height: 42px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-actions {
  display: flex;
  justify-content: flex-end;
}

.cover-uploader {
  width: 100%;
}

.cover-preview {
  width: 260px;
  height: 150px;
  border-radius: 8px;
  display: block;
}

.cover-upload-placeholder {
  width: 260px;
  height: 150px;
  border: 1px dashed #c0c4cc;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #909399;
  cursor: pointer;
}

.cover-upload-placeholder:hover {
  border-color: #2563eb;
  color: #2563eb;
}
</style>
