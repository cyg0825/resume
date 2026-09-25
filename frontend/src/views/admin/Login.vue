<template>
  <div class="login-page">
    <div class="login-card">
      <div class="brand">
        <el-icon :size="34" color="#2563eb"><Document /></el-icon>
        <h1>个人简历 · 管理后台</h1>
        <p>Personal Resume Admin</p>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        size="large"
        @submit.prevent="handleLogin"
      >
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            name="username"
            autocomplete="username"
            placeholder="管理员账号"
            :prefix-icon="User"
            clearable
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            name="password"
            autocomplete="current-password"
            type="password"
            placeholder="密码"
            :prefix-icon="Lock"
            show-password
          />
        </el-form-item>
        <el-button
          type="primary"
          native-type="submit"
          class="login-btn"
          :loading="loading"
        >
          登 录
        </el-button>
      </el-form>

      <div class="login-footer">
        <el-button link @click="$router.push('/')">← 返回简历首页</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { User, Lock } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const formRef = ref()
const loading = ref(false)
const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  await formRef.value.validate()
  loading.value = true
  try {
    await userStore.login({ ...form })
    ElMessage.success('登录成功')
    router.push(route.query.redirect || '/admin/dashboard')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1e3a8a 0%, #2563eb 55%, #38bdf8 100%);
  padding: 20px;
}

.login-card {
  width: 400px;
  max-width: 100%;
  background: #fff;
  border-radius: 18px;
  padding: 40px 36px 28px;
  box-shadow: 0 24px 60px rgba(0, 0, 0, 0.25);
}

@media (max-width: 480px) {
  .login-page {
    padding: 12px;
  }

  .login-card {
    padding: 28px 20px 22px;
    border-radius: 14px;
  }
}

.brand {
  text-align: center;
  margin-bottom: 30px;
}

.brand h1 {
  margin: 12px 0 6px;
  font-size: 20px;
  color: #1e293b;
}

.brand p {
  margin: 0;
  font-size: 12px;
  color: #94a3b8;
  letter-spacing: 2px;
}

.login-btn {
  width: 100%;
}

.login-footer {
  margin-top: 18px;
  display: flex;
  justify-content: center;
  align-items: center;
}
</style>
