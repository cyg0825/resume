<template>
  <div>
    <VersionBar v-model="currentVersionId" :versions="versions" @change="loadList" />

    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>技能特长（{{ list.length }}）</span>
          <el-button type="primary" :disabled="!currentVersionId" @click="openDialog()">
            <el-icon><Plus /></el-icon>&nbsp;新增技能
          </el-button>
        </div>
      </template>

      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="category" label="分类" width="140">
          <template #default="{ row }">
            <el-tag effect="plain">{{ row.category || '未分类' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="技能名称" min-width="160" />
        <el-table-column label="熟练度" min-width="220">
          <template #default="{ row }">
            <el-progress
              :percentage="Number(row.level) || 0"
              :stroke-width="12"
              :color="progressColor"
            />
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

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑技能' : '新增技能'" width="480px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="分类" prop="category">
          <el-select
            v-model="form.category"
            allow-create
            filterable
            default-first-option
            placeholder="选择或输入新分类"
            style="width: 100%"
          >
            <el-option v-for="c in categoryOptions" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="技能名称" prop="name">
          <el-input v-model="form.name" maxlength="50" placeholder="如：Java / Vue / MySQL" />
        </el-form-item>
        <el-form-item label="熟练度" prop="level">
          <el-slider v-model="form.level" show-input :min="0" :max="100" :step="1" />
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
import { ref, reactive, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getSkills, createSkill, updateSkill, deleteSkill } from '@/api'
import { useVersionScope } from '@/composables/useVersionScope'
import VersionBar from '@/components/admin/VersionBar.vue'

const { versions, currentVersionId } = useVersionScope()
const loading = ref(false)
const saving = ref(false)
const list = ref([])
const dialogVisible = ref(false)
const formRef = ref()

const emptyForm = () => ({ id: null, category: '后端', name: '', level: 80, sort: 0 })
const form = reactive(emptyForm())

const rules = {
  category: [{ required: true, message: '请选择或输入分类', trigger: 'change' }],
  name: [{ required: true, message: '请输入技能名称', trigger: 'blur' }]
}

const progressColor = [
  { color: '#f56c6c', percentage: 50 },
  { color: '#e6a23c', percentage: 75 },
  { color: '#67c23a', percentage: 100 }
]

const categoryOptions = computed(() => {
  const set = new Set()
  list.value.forEach((s) => s.category && set.add(s.category))
  // 常用分类兜底
  ;['前端', '后端', '数据库', '运维', '工具'].forEach((c) => set.add(c))
  return Array.from(set)
})

async function loadList() {
  if (!currentVersionId.value) return
  loading.value = true
  try {
    const res = await getSkills(currentVersionId.value)
    list.value = res.data || []
  } finally {
    loading.value = false
  }
}

function openDialog(row) {
  Object.assign(form, emptyForm(), row ? { ...row } : {})
  dialogVisible.value = true
}

async function save() {
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = { ...form, versionId: currentVersionId.value }
    if (form.id) {
      await updateSkill(payload)
    } else {
      await createSkill(payload)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadList()
  } finally {
    saving.value = false
  }
}

async function remove(row) {
  await ElMessageBox.confirm(`确定删除技能「${row.name}」吗？`, '提示', { type: 'warning' })
  await deleteSkill(row.id)
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
