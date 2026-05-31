<template>
  <div class="profile-page">
    <div class="page-header">
      <h1 class="page-title">个人信息</h1>
      <p class="page-desc">管理您的账号资料</p>
    </div>

    <div class="profile-layout">
      <div class="profile-main">
        <div class="section">
          <h2 class="section-title">基本资料</h2>
          <el-form ref="profileFormRef" :model="profileForm" :rules="profileRules" label-width="80px">
            <el-form-item label="用户头像">
              <div class="avatar-upload">
                <div class="avatar-preview" @click="triggerAvatarUpload">
                  <img v-if="avatarPreview" :src="avatarPreview" alt="avatar" />
                  <div v-else class="avatar-placeholder">{{ profileForm.username?.charAt(0)?.toUpperCase() || 'U' }}</div>
                </div>
                <input ref="avatarInputRef" type="file" accept="image/*" style="display:none" @change="handleAvatarChange" />
              </div>
            </el-form-item>
            <el-form-item label="用户名" prop="username">
              <el-input v-model="profileForm.username" placeholder="请输入用户名" maxlength="50" show-word-limit />
            </el-form-item>
            <el-form-item label="邮箱">
              <el-input :model-value="userInfo?.email" disabled />
            </el-form-item>
            <el-form-item label="个人简介" prop="bio">
              <el-input v-model="profileForm.bio" type="textarea" placeholder="介绍一下自己..." :rows="3" maxlength="500" show-word-limit />
            </el-form-item>
            <el-form-item>
              <el-button :loading="savingProfile" @click="handleSaveProfile">保存修改</el-button>
            </el-form-item>
          </el-form>
        </div>

        <div class="section">
          <h2 class="section-title">修改密码</h2>
          <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-width="80px">
            <el-form-item label="原密码" prop="oldPassword">
              <el-input v-model="passwordForm.oldPassword" type="password" placeholder="请输入当前密码" show-password />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="passwordForm.newPassword" type="password" placeholder="请输入新密码" show-password />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input v-model="passwordForm.confirmPassword" type="password" placeholder="请再次输入新密码" show-password />
            </el-form-item>
            <el-form-item>
              <el-button :loading="savingPassword" @click="handleChangePassword">修改密码</el-button>
            </el-form-item>
          </el-form>
        </div>
      </div>

      <div class="profile-sidebar">
        <div class="section">
          <h2 class="section-title">账号信息</h2>
          <div class="info-list">
            <div class="info-row"><span class="info-label">用户ID</span><span class="info-value">#{{ userInfo?.id }}</span></div>
            <div class="info-row"><span class="info-label">注册时间</span><span class="info-value">{{ formatDate(userInfo?.createdAt) }}</span></div>
            <div class="info-row"><span class="info-label">账号状态</span><span class="info-value">正常</span></div>
          </div>
        </div>

        <div class="section danger-zone">
          <h2 class="section-title">危险操作</h2>
          <div class="danger-desc">注销账号后，所有数据将被永久删除。</div>
          <el-button @click="handleDeleteAccount">注销账号</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { getProfile, updateProfile, changePassword } from '@/api/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'

const userStore = useUserStore()
const userInfo = ref(null)
const profileFormRef = ref(null)
const passwordFormRef = ref(null)
const avatarInputRef = ref(null)
const savingProfile = ref(false)
const savingPassword = ref(false)
const avatarPreview = ref('')

const profileForm = reactive({ username: '', bio: '', avatar: '' })
const passwordForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== passwordForm.newPassword) callback(new Error('两次输入的密码不一致'))
  else callback()
}

const profileRules = { username: [{ required: true, message: '请输入用户名', trigger: 'blur' }, { min: 2, max: 50, message: '用户名长度应为2-50个字符', trigger: 'blur' }] }
const passwordRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [{ required: true, message: '请输入新密码', trigger: 'blur' }, { min: 6, message: '新密码长度应不少于6个字符', trigger: 'blur' }],
  confirmPassword: [{ required: true, message: '请确认密码', trigger: 'blur' }, { validator: validateConfirmPassword, trigger: 'blur' }]
}

const formatDate = (date) => date ? dayjs(date).format('YYYY-MM-DD') : '-'
const triggerAvatarUpload = () => avatarInputRef.value?.click()
const handleAvatarChange = (e) => {
  const file = e.target.files[0]
  if (!file) return
  if (!file.type.startsWith('image/')) { ElMessage.warning('请上传图片文件'); return }
  if (file.size > 2 * 1024 * 1024) { ElMessage.warning('图片大小不能超过 2MB'); return }
  const reader = new FileReader()
  reader.onload = (event) => { avatarPreview.value = event.target.result; profileForm.avatar = event.target.result }
  reader.readAsDataURL(file)
}

const loadProfile = async () => {
  try {
    const res = await getProfile()
    const data = res.data?.data
    userInfo.value = data
    profileForm.username = data?.username || ''
    profileForm.bio = data?.bio || ''
    profileForm.avatar = data?.avatar || ''
    if (data?.avatar) avatarPreview.value = data.avatar
  } catch (e) { console.error('Failed to load profile:', e) }
}

const handleSaveProfile = async () => {
  if (!profileFormRef.value) return
  await profileFormRef.value.validate(async (valid) => {
    if (valid) {
      savingProfile.value = true
      try {
        await updateProfile({ username: profileForm.username, bio: profileForm.bio, avatar: profileForm.avatar })
        ElMessage.success('保存成功')
        userStore.fetchUserInfo()
      } catch (e) { console.error('Failed to save profile:', e); ElMessage.error('保存失败') }
      finally { savingProfile.value = false }
    }
  })
}

const handleChangePassword = async () => {
  if (!passwordFormRef.value) return
  await passwordFormRef.value.validate(async (valid) => {
    if (valid) {
      savingPassword.value = true
      try {
        await changePassword({ oldPassword: passwordForm.oldPassword, newPassword: passwordForm.newPassword })
        ElMessage.success('密码修改成功')
        passwordFormRef.value?.resetFields()
      } catch (e) { console.error('Failed to change password:', e); ElMessage.error(e.response?.data?.message || '修改失败') }
      finally { savingPassword.value = false }
    }
  })
}

const handleDeleteAccount = () => {
  ElMessageBox.confirm('确定要注销您的账号吗？注销后所有数据将被永久删除，且无法恢复。', '危险操作', { confirmButtonText: '确认注销', cancelButtonText: '取消', type: 'error' })
    .then(() => ElMessage.info('账号注销功能暂未开放，请联系管理员'))
    .catch(() => {})
}

onMounted(() => { loadProfile() })
</script>

<style lang="scss" scoped>
.profile-page { padding: 0; }
.page-header { padding: 24px 0; border-top: 1px solid var(--border-color); border-bottom: 1px solid var(--border-color); margin-bottom: 0; }
.page-title { font-size: 20px; font-weight: 500; color: var(--text-primary); margin-bottom: 4px; letter-spacing: -0.01em; }
.page-desc { color: var(--text-muted); font-size: 13px; }

.profile-layout { display: grid; grid-template-columns: 1fr 240px; gap: 40px; align-items: start; padding-top: 24px; border-top: 1px solid var(--border-color); }
.profile-main { display: flex; flex-direction: column; gap: 32px; }
.profile-sidebar { display: flex; flex-direction: column; gap: 24px; }

.section { .section-title { font-size: 12px; font-weight: 500; color: var(--text-muted); margin-bottom: 16px; text-transform: uppercase; letter-spacing: 0.06em; } }
:deep(.el-form-item) { margin-bottom: 16px; &:last-child { margin-bottom: 0; } }
:deep(.el-button) { padding: 8px 20px; border: 1px solid var(--border-color); background: transparent; color: var(--text-primary); &:hover { border-color: var(--text-primary); } }

.avatar-upload { display: flex; align-items: center; gap: 16px; }
.avatar-preview { width: 64px; height: 64px; overflow: hidden; cursor: pointer; flex-shrink: 0; img { width: 100%; height: 100%; object-fit: cover; } }
.avatar-placeholder { width: 100%; height: 100%; background: var(--bg-hover); display: flex; align-items: center; justify-content: center; font-size: 20px; font-weight: 500; color: var(--text-secondary); }
:deep(.el-input.is-disabled .el-input__wrapper) { background-color: var(--bg-hover); .el-input__inner { color: var(--text-secondary); } }

.info-list { .info-row { display: flex; justify-content: space-between; align-items: center; padding: 10px 0; border-bottom: 1px solid var(--border-light); &:last-child { border-bottom: none; } } }
.info-label { font-size: 13px; color: var(--text-muted); }
.info-value { font-size: 13px; color: var(--text-primary); }

.danger-zone { .danger-desc { font-size: 12px; color: var(--text-muted); margin-bottom: 12px; line-height: 1.6; } }

@media (max-width: 900px) {
  .profile-layout { grid-template-columns: 1fr; }
  .profile-sidebar { order: -1; }
}
</style>
