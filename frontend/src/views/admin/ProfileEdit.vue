<template>
  <div>
    <VersionBar v-model="currentVersionId" :versions="versions" />

    <el-card shadow="never" v-loading="loading">
      <el-form
        ref="formRef"
        :model="form"
        label-width="100px"
        style="max-width: 780px"
        @submit.prevent
      >
        <el-form-item label="头像">
          <el-upload
            class="avatar-uploader"
            :show-file-list="false"
            :http-request="handleUpload"
            accept="image/*"
          >
            <div class="avatar-box">
              <el-avatar v-if="form.avatar" :size="90" :src="form.avatar" />
              <div v-else class="avatar-placeholder">
                <el-icon :size="24"><Plus /></el-icon>
              </div>
              <el-tooltip v-if="form.avatar" content="删除头像" placement="top">
                <span class="avatar-delete" @click.stop="removeAvatar">
                  <el-icon :size="16"><CircleCloseFilled /></el-icon>
                </span>
              </el-tooltip>
            </div>
          </el-upload>
          <div class="upload-tip">点击上传头像（建议正方形，jpg/png/webp）</div>
        </el-form-item>

        <el-form-item label="姓名">
          <el-input v-model="form.name" placeholder="请输入姓名" maxlength="50" />
        </el-form-item>
        <el-form-item label="职位">
          <el-input v-model="form.jobTitle" placeholder="如：软件测试工程师" maxlength="100" />
        </el-form-item>
        <el-form-item label="一句话简介">
          <el-input v-model="form.slogan" placeholder="一句话介绍自己" maxlength="255" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" maxlength="100" />
        </el-form-item>
        <el-form-item label="电话">
          <el-input v-model="form.phone" maxlength="20" />
        </el-form-item>
        <el-form-item label="微信">
          <el-input v-model="form.wechat" maxlength="50" />
        </el-form-item>
        <el-form-item label="GitHub">
          <el-input v-model="form.github" placeholder="https://github.com/xxx" maxlength="255" />
        </el-form-item>
        <el-form-item label="Gitee">
          <el-input v-model="form.gitee" placeholder="https://gitee.com/xxx" maxlength="255" />
        </el-form-item>
        <el-form-item label="CSDN">
          <el-input v-model="form.csdn" placeholder="https://blog.csdn.net/xxx" maxlength="255" />
        </el-form-item>
        <el-form-item label="所在地">
          <el-input v-model="form.address" maxlength="100" />
        </el-form-item>
        <el-form-item label="个人评价">
          <el-input
            v-model="form.about"
            type="textarea"
            :rows="6"
            placeholder="详细介绍你的背景、优势与求职意向"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="save">保存修改</el-button>
          <el-button @click="loadProfile">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { CircleCloseFilled } from '@element-plus/icons-vue'
import { getProfile, updateProfile, uploadFile } from '@/api'
import { useVersionScope } from '@/composables/useVersionScope'
import VersionBar from '@/components/admin/VersionBar.vue'

const { versions, currentVersionId } = useVersionScope()
const loading = ref(false)
const saving = ref(false)
const formRef = ref()

const form = reactive({
  id: null,
  versionId: null,
  name: '',
  jobTitle: '',
  slogan: '',
  avatar: '',
  email: '',
  phone: '',
  wechat: '',
  github: '',
  gitee: '',
  csdn: '',
  address: '',
  about: ''
})

async function loadProfile() {
  if (!currentVersionId.value) return
  loading.value = true
  try {
    const res = await getProfile(currentVersionId.value)
    Object.assign(form, {
      id: null,
      versionId: currentVersionId.value,
      name: '',
      jobTitle: '',
      slogan: '',
      avatar: '',
      email: '',
      phone: '',
      wechat: '',
      github: '',
      gitee: '',
      csdn: '',
      address: '',
      about: '',
      ...res.data
    })
    form.versionId = currentVersionId.value
  } finally {
    loading.value = false
  }
}

async function handleUpload(option) {
  try {
    const res = await uploadFile(option.file)
    form.avatar = res.data.url
    ElMessage.success('头像上传成功')
  } catch (e) {
    /* 错误提示已由拦截器处理 */
  }
}

async function removeAvatar() {
  try {
    await ElMessageBox.confirm('确定删除当前头像吗？删除后需点击"保存修改"生效。', '提示', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch (e) {
    return
  }
  form.avatar = ''
  ElMessage.success('已移除头像，请点击"保存修改"')
}

async function save() {
  saving.value = true
  try {
    form.versionId = currentVersionId.value
    const payload = { ...form }
    if (!payload.avatar) payload.avatar = null
    await updateProfile(payload)
    ElMessage.success('保存成功')
    loadProfile()
  } finally {
    saving.value = false
  }
}

watch(currentVersionId, () => loadProfile(), { immediate: false })
</script>

<style scoped>
.avatar-uploader {
  cursor: pointer;
}

.avatar-box {
  position: relative;
  width: 90px;
  height: 90px;
}

.avatar-delete {
  position: absolute;
  top: -7px;
  right: -7px;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--el-color-danger);
  color: #fff;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.22);
  transition: transform 0.15s ease;
}

.avatar-delete:hover {
  transform: scale(1.12);
}

.avatar-placeholder {
  width: 90px;
  height: 90px;
  border: 1px dashed #c0c4cc;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
  transition: border-color 0.2s;
}

.avatar-placeholder:hover {
  border-color: #2563eb;
  color: #2563eb;
}

.upload-tip {
  font-size: 12px;
  color: #94a3b8;
  margin-left: 14px;
}
</style>
