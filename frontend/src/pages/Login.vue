<template>
  <div class="login-page">
    <div class="login-container">
      <div class="login-header">
        <div class="logo">ContentHub</div>
        <p class="subtitle">多平台内容发布工具</p>
      </div>

      <el-tabs v-model="activeTab" class="login-tabs">
        <el-tab-pane label="登录" name="login" />
        <el-tab-pane label="注册" name="register" />
      </el-tabs>

      <div class="login-content">
        <el-form
          v-if="activeTab === 'login'"
          ref="loginFormRef"
          :model="loginForm"
          :rules="loginRules"
          @submit.prevent="handleLogin"
        >
          <el-form-item prop="username">
            <label class="form-label">用户名</label>
            <el-input v-model="loginForm.username" placeholder="请输入用户名" size="large" />
          </el-form-item>
          <el-form-item prop="password">
            <label class="form-label">密码</label>
            <el-input v-model="loginForm.password" type="password" placeholder="请输入密码" size="large" show-password @keyup.enter="handleLogin" />
          </el-form-item>
          <el-form-item>
            <button type="button" class="submit-btn" :class="{ loading: loginLoading }" :disabled="loginLoading" @click="handleLogin">
              {{ loginLoading ? '登录中...' : '登录' }}
            </button>
          </el-form-item>
        </el-form>

        <el-form
          v-else
          ref="registerFormRef"
          :model="registerForm"
          :rules="registerRules"
          @submit.prevent="handleRegister"
        >
          <el-form-item prop="username">
            <label class="form-label">用户名</label>
            <el-input v-model="registerForm.username" placeholder="请输入用户名" size="large" />
          </el-form-item>
          <el-form-item prop="email">
            <label class="form-label">邮箱</label>
            <el-input v-model="registerForm.email" placeholder="请输入邮箱" size="large" />
          </el-form-item>
          <el-form-item prop="password">
            <label class="form-label">密码</label>
            <el-input v-model="registerForm.password" type="password" placeholder="请输入密码" size="large" show-password />
          </el-form-item>
          <el-form-item prop="confirmPassword">
            <label class="form-label">确认密码</label>
            <el-input v-model="registerForm.confirmPassword" type="password" placeholder="请确认密码" size="large" show-password @keyup.enter="handleRegister" />
          </el-form-item>
          <el-form-item>
            <button type="button" class="submit-btn" :class="{ loading: registerLoading }" :disabled="registerLoading" @click="handleRegister">
              {{ registerLoading ? '注册中...' : '注册' }}
            </button>
          </el-form-item>
        </el-form>
      </div>

      <div class="demo-hint">
        <span class="hint-text">默认账号: admin / 123456</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

const activeTab = ref('login')
const loginLoading = ref(false)
const registerLoading = ref(false)
const loginFormRef = ref(null)
const registerFormRef = ref(null)

const loginForm = reactive({ username: '', password: '' })
const registerForm = reactive({ username: '', email: '', password: '', confirmPassword: '' })

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== registerForm.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const loginRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const registerRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 50, message: '用户名长度应为3-50个字符', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度应不少于6个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

const handleLogin = async () => {
  if (!loginFormRef.value) return
  await loginFormRef.value.validate(async (valid) => {
    if (valid) {
      loginLoading.value = true
      try {
        await userStore.login(loginForm.username, loginForm.password)
        ElMessage.success('登录成功')
        router.push('/dashboard')
      } catch (e) {
        ElMessage.error(e.response?.data?.message || '登录失败')
      } finally {
        loginLoading.value = false
      }
    }
  })
}

const handleRegister = async () => {
  if (!registerFormRef.value) return
  await registerFormRef.value.validate(async (valid) => {
    if (valid) {
      registerLoading.value = true
      try {
        await userStore.register(registerForm.username, registerForm.email, registerForm.password)
        ElMessage.success('注册成功')
        router.push('/dashboard')
      } catch (e) {
        ElMessage.error(e.response?.data?.message || '注册失败')
      } finally {
        registerLoading.value = false
      }
    }
  })
}
</script>

<style lang="scss" scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-main);
}

.login-container {
  width: 100%;
  max-width: 360px;
  padding: 20px;
}

.login-header {
  text-align: center;
  margin-bottom: 32px;
}

.logo {
  font-size: 20px;
  font-weight: 500;
  color: var(--text-primary);
  letter-spacing: -0.01em;
}

.subtitle {
  color: var(--text-muted);
  font-size: 13px;
  margin: 6px 0 0;
}

.login-tabs {
  margin-bottom: 24px;

  :deep(.el-tabs__header) {
    margin-bottom: 0;
  }

  :deep(.el-tabs__nav-wrap::after) {
    display: none;
  }

  :deep(.el-tabs__item) {
    font-size: 13px;
    font-weight: 400;
    color: var(--text-muted);
    padding: 0 16px 0 0;
    height: 40px;
    line-height: 40px;

    &:hover {
      color: var(--text-primary);
    }

    &.is-active {
      color: var(--text-primary);
      font-weight: 500;
    }
  }

  :deep(.el-tabs__active-bar) {
    background: var(--text-primary);
    height: 1px;
  }
}

.login-content {
  padding: 24px 0;
}

:deep(.el-form-item) {
  margin-bottom: 20px;
}

.form-label {
  display: block;
  font-size: 13px;
  color: var(--text-muted);
  margin-bottom: 8px;
}

:deep(.el-input__wrapper) {
  padding: 10px 0;
  background: transparent;
  box-shadow: none;
  border-radius: 0;
  border-bottom: 1px solid var(--border-color);
  transition: border-color var(--transition-fast);

  &:hover {
    border-color: var(--text-muted);
  }

  &.is-focus {
    border-color: var(--text-primary);
    box-shadow: none;
  }
}

.submit-btn {
  width: 100%;
  height: 44px;
  border: none;
  background: var(--text-primary);
  color: var(--bg-main);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: opacity var(--transition-fast);
  margin-top: 8px;
  letter-spacing: 0.02em;

  &:hover {
    opacity: 0.85;
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
}

.demo-hint {
  text-align: center;
  padding-top: 24px;
  border-top: 1px solid var(--border-color);
}

.hint-text {
  font-size: 13px;
  color: var(--text-muted);
}
</style>
