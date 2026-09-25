<template>
  <div>
    <VersionBar v-model="currentVersionId" :versions="versions" @change="loadList" />

    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <el-radio-group v-model="activeType" @change="loadList">
            <el-radio-button :value="1">工作经历</el-radio-button>
            <el-radio-button :value="2">项目经历</el-radio-button>
          </el-radio-group>
          <el-button type="primary" :disabled="!currentVersionId" @click="openDialog()">
            <el-icon><Plus /></el-icon>&nbsp;新增
          </el-button>
        </div>
      </template>

      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="company" :label="activeType === 1 ? '公司' : '项目'" min-width="150" />
        <el-table-column prop="position" label="职位/角色" min-width="130" />
        <el-table-column label="时间" width="200">
          <template #default="{ row }">
            {{ row.startDate || '?' }} ~ {{ row.endDate || '至今' }}
          </template>
        </el-table-column>
        <el-table-column prop="techStack" label="技术栈" min-width="180" show-overflow-tooltip />
        <el-table-column prop="sort" label="排序" width="70" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="(form.id ? '编辑' : '新增') + (activeType === 1 ? '工作经历' : '项目经历')"
      width="640px"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item :label="activeType === 1 ? '公司名称' : '项目名称'" prop="company">
          <el-input v-model="form.company" maxlength="100" />
        </el-form-item>
        <el-form-item label="职位/角色" prop="position">
          <el-input v-model="form.position" maxlength="100" />
        </el-form-item>
        <el-form-item label="开始时间">
          <el-date-picker v-model="form.startDate" type="date" value-format="YYYY-MM-DD"
                          placeholder="开始日期" style="width: 100%" />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-date-picker v-model="form.endDate" type="date" value-format="YYYY-MM-DD"
                          placeholder="留空表示至今" style="width: 100%" />
        </el-form-item>
        <el-form-item label="技术栈">
          <el-input v-model="form.techStack" placeholder="多个技术用逗号分隔，如：Java, Vue, MySQL"
                    maxlength="500" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" :max="9999" />
        </el-form-item>
        <el-form-item label="职责/描述">
          <el-input v-model="form.description" type="textarea" :rows="5"
                    placeholder="工作内容、业绩或项目描述" />
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
  getExperiences,
  createExperience,
  updateExperience,
  deleteExperience
} from '@/api'
import { useVersionScope } from '@/composables/useVersionScope'
import VersionBar from '@/components/admin/VersionBar.vue'

const { versions, currentVersionId } = useVersionScope()
const activeType = ref(1)
const loading = ref(false)
const saving = ref(false)
const list = ref([])
const dialogVisible = ref(false)
const formRef = ref()

const emptyForm = () => ({
  id: null,
  type: 1,
  company: '',
  position: '',
  startDate: '',
  endDate: '',
  description: '',
  techStack: '',
  sort: 0
})
const form = reactive(emptyForm())

const rules = {
  company: [{ required: true, message: '请输入名称', trigger: 'blur' }]
}

async function loadList() {
  if (!currentVersionId.value) return
  loading.value = true
  try {
    const res = await getExperiences(currentVersionId.value, activeType.value)
    list.value = res.data || []
  } finally {
    loading.value = false
  }
}

function openDialog(row) {
  Object.assign(form, emptyForm(), row ? { ...row } : { type: activeType.value })
  dialogVisible.value = true
}

async function save() {
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = { ...form, versionId: currentVersionId.value, type: activeType.value }
    if (form.id) {
      await updateExperience(payload)
    } else {
      await createExperience(payload)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadList()
  } finally {
    saving.value = false
  }
}

async function remove(row) {
  await ElMessageBox.confirm(`确定删除「${row.company}」吗？`, '提示', { type: 'warning' })
  await deleteExperience(row.id)
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
