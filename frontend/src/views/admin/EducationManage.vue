<template>
  <div>
    <VersionBar v-model="currentVersionId" :versions="versions" />

    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>教育经历（{{ list.length }}）</span>
          <el-button type="primary" :disabled="!currentVersionId" @click="openDialog()">
            <el-icon><Plus /></el-icon>&nbsp;新增教育经历
          </el-button>
        </div>
      </template>

      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="school" label="学校" min-width="150" />
        <el-table-column prop="major" label="专业" min-width="120" />
        <el-table-column prop="degree" label="学历" width="90" />
        <el-table-column label="时间" width="200">
          <template #default="{ row }">
            {{ row.startDate || '?' }} ~ {{ row.endDate || '至今' }}
          </template>
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

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑教育经历' : '新增教育经历'" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="学校" prop="school">
          <el-input v-model="form.school" maxlength="100" />
        </el-form-item>
        <el-form-item label="专业" prop="major">
          <el-input v-model="form.major" maxlength="100" />
        </el-form-item>
        <el-form-item label="学历" prop="degree">
          <el-select v-model="form.degree" placeholder="请选择" style="width: 100%">
            <el-option label="大专" value="大专" />
            <el-option label="本科" value="本科" />
            <el-option label="硕士" value="硕士" />
            <el-option label="博士" value="博士" />
          </el-select>
        </el-form-item>
        <el-form-item label="开始时间">
          <el-date-picker
            v-model="form.startDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="开始日期"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-date-picker
            v-model="form.endDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="留空表示至今"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" :max="9999" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" />
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
import { ref, reactive, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getEducations,
  createEducation,
  updateEducation,
  deleteEducation
} from '@/api'
import { useVersionScope } from '@/composables/useVersionScope'
import VersionBar from '@/components/admin/VersionBar.vue'

const { versions, currentVersionId } = useVersionScope()
const loading = ref(false)
const saving = ref(false)
const list = ref([])
const dialogVisible = ref(false)
const formRef = ref()

const emptyForm = () => ({
  id: null,
  versionId: null,
  school: '',
  major: '',
  degree: '本科',
  startDate: '',
  endDate: '',
  description: '',
  sort: 0
})
const form = reactive(emptyForm())

const rules = {
  school: [{ required: true, message: '请输入学校名称', trigger: 'blur' }]
}

async function loadList() {
  if (!currentVersionId.value) return
  loading.value = true
  try {
    const res = await getEducations(currentVersionId.value)
    list.value = res.data || []
  } finally {
    loading.value = false
  }
}

function openDialog(row) {
  Object.assign(form, emptyForm(), row ? { ...row } : {})
  form.versionId = currentVersionId.value
  dialogVisible.value = true
}

async function save() {
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = { ...form, versionId: currentVersionId.value }
    if (form.id) {
      await updateEducation(payload)
    } else {
      await createEducation(payload)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadList()
  } finally {
    saving.value = false
  }
}

async function remove(row) {
  await ElMessageBox.confirm(`确定删除「${row.school}」吗？`, '提示', { type: 'warning' })
  await deleteEducation(row.id)
  ElMessage.success('删除成功')
  loadList()
}

watch(currentVersionId, () => loadList(), { immediate: false })
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
