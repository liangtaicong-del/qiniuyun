<template>
  <div class="settings-page">
    <div class="page-header">
      <h1 class="page-title">系统设置</h1>
      <p class="page-desc">配置您的账号偏好</p>
    </div>

    <el-tabs v-model="activeTab" class="settings-tabs">
      <el-tab-pane label="发布设置" name="publish" />
      <el-tab-pane label="通知设置" name="notification" />
      <el-tab-pane label="外观设置" name="appearance" />
    </el-tabs>

    <div class="settings-content">
      <div v-if="activeTab === 'publish'" class="settings-section">
        <div class="setting-group">
          <div class="setting-item">
            <div class="setting-info">
              <span class="setting-label">默认发布平台</span>
              <span class="setting-hint">创建文章时的默认目标平台</span>
            </div>
            <el-select v-model="settings.defaultPlatform" placeholder="不设置默认" clearable>
              <el-option v-for="p in platforms" :key="p.value" :label="p.label" :value="p.value" />
            </el-select>
          </div>
          <div class="setting-item">
            <div class="setting-info">
              <span class="setting-label">自动发布</span>
              <span class="setting-hint">启用后，定时任务将在指定时间自动发布</span>
            </div>
            <el-switch v-model="settings.autoPublish" />
          </div>
          <div class="setting-item">
            <div class="setting-info">
              <span class="setting-label">发布间隔</span>
              <span class="setting-hint">多平台发布时的间隔时间（分钟）</span>
            </div>
            <el-input-number v-model="settings.publishInterval" :min="1" :max="1440" />
          </div>
          <div class="setting-item setting-item-last">
            <div class="setting-info">
              <span class="setting-label">失败自动重试</span>
              <span class="setting-hint">发布失败时自动重试一次</span>
            </div>
            <el-switch v-model="settings.publishAutoRetry" />
          </div>
        </div>
        <div class="settings-footer"><el-button :loading="saving" @click="handleSave">保存设置</el-button></div>
      </div>

      <div v-else-if="activeTab === 'notification'" class="settings-section">
        <div class="setting-group">
          <div class="setting-item">
            <div class="setting-info">
              <span class="setting-label">邮件通知</span>
              <span class="setting-hint">发布成功或失败时发送邮件通知</span>
            </div>
            <el-switch v-model="settings.emailNotification" />
          </div>
          <div class="setting-item setting-item-last">
            <div class="setting-info">
              <span class="setting-label">站内通知</span>
              <span class="setting-hint">在系统中显示推送通知</span>
            </div>
            <el-switch v-model="settings.pushNotification" />
          </div>
        </div>
        <div class="settings-footer"><el-button :loading="saving" @click="handleSave">保存设置</el-button></div>
      </div>

      <div v-else-if="activeTab === 'appearance'" class="settings-section">
        <div class="setting-group">
          <div class="setting-item">
            <div class="setting-info">
              <span class="setting-label">主题</span>
              <span class="setting-hint">选择您喜欢的界面主题</span>
            </div>
            <el-radio-group v-model="settings.theme" class="text-radio-group">
              <el-radio label="light">浅色</el-radio>
              <el-radio label="dark">深色</el-radio>
              <el-radio label="auto">跟随系统</el-radio>
            </el-radio-group>
          </div>
          <div class="setting-item setting-item-last">
            <div class="setting-info">
              <span class="setting-label">语言</span>
              <span class="setting-hint">选择界面显示语言</span>
            </div>
            <el-select v-model="settings.language" style="width: 140px">
              <el-option label="简体中文" value="zh-CN" />
              <el-option label="English" value="en-US" />
            </el-select>
          </div>
        </div>
        <div class="settings-footer"><el-button :loading="saving" @click="handleSave">保存设置</el-button></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { getSettings, updateSettings } from '@/api/settings'
import { useThemeStore } from '@/stores/theme'
import { ElMessage } from 'element-plus'

const activeTab = ref('publish')
const saving = ref(false)
const loaded = ref(false)
const themeStore = useThemeStore()

const platforms = [
  { value: 'WECHAT', label: '微信公众号' }, { value: 'WEIBO', label: '微博' },
  { value: 'ZHIHU', label: '知乎' }, { value: 'JIANSHU', label: '简书' },
  { value: 'CSDN', label: 'CSDN' }, { value: 'JUEJIN', label: '掘金' },
  { value: 'BAIJIA', label: '百家号' }, { value: 'TOUTIAO', label: '头条号' }
]

const settings = reactive({
  defaultPlatform: '', autoPublish: false, publishInterval: 10,
  emailNotification: true, pushNotification: true, publishAutoRetry: true,
  theme: 'light', language: 'zh-CN'
})

const loadSettings = async () => {
  try {
    const res = await getSettings()
    const data = res.data?.data || {}
    Object.assign(settings, {
      defaultPlatform: data.defaultPlatform || '',
      autoPublish: data.autoPublish ?? false,
      publishInterval: data.publishInterval ?? 10,
      emailNotification: data.emailNotification ?? true,
      pushNotification: data.pushNotification ?? true,
      publishAutoRetry: data.publishAutoRetry ?? true,
      theme: data.theme || 'light',
      language: data.language || 'zh-CN'
    })
    themeStore.setTheme(settings.theme)
    loaded.value = true
  } catch (e) { console.error('Failed to load settings:', e) }
}

const handleSave = async () => {
  saving.value = true
  try {
    await updateSettings({
      defaultPlatform: settings.defaultPlatform || null,
      autoPublish: settings.autoPublish,
      publishInterval: settings.publishInterval,
      emailNotification: settings.emailNotification,
      pushNotification: settings.pushNotification,
      publishAutoRetry: settings.publishAutoRetry,
      theme: settings.theme,
      language: settings.language
    })
    ElMessage.success('设置保存成功')
  } catch (e) { console.error('Failed to save settings:', e) }
  finally { saving.value = false }
}

watch(() => settings.theme, (newTheme) => {
  if (!loaded.value) return
  if (newTheme === 'auto') {
    const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches
    themeStore.setTheme(prefersDark ? 'dark' : 'light')
  } else if (newTheme) {
    themeStore.setTheme(newTheme)
  }
})

onMounted(() => { loadSettings() })
</script>

<style lang="scss" scoped>
.settings-page { padding: 0; }
.page-header { padding: 24px 0; border-top: 1px solid var(--border-color); border-bottom: 1px solid var(--border-color); margin-bottom: 0; }
.page-title { font-size: 20px; font-weight: 500; color: var(--text-primary); margin-bottom: 4px; letter-spacing: -0.01em; }
.page-desc { color: var(--text-muted); font-size: 13px; }

.settings-tabs { border-bottom: 1px solid var(--border-color); padding-top: 8px; }
:deep(.el-tabs__header) { margin-bottom: 0; }
:deep(.el-tabs__nav-wrap::after) { display: none; }
:deep(.el-tabs__item) { font-size: 13px; font-weight: 400; color: var(--text-muted); padding: 0 16px 0 0; height: 40px; line-height: 40px; &.is-active { color: var(--text-primary); font-weight: 500; } }
:deep(.el-tabs__active-bar) { background-color: var(--text-primary); height: 1px; }

.settings-content { padding-top: 8px; min-height: 400px; }
.settings-section { animation: fadeIn 0.2s ease; }
@keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }

.setting-group { border-top: 1px solid var(--border-color); }
.setting-item { display: flex; align-items: center; justify-content: space-between; padding: 16px 0; border-bottom: 1px solid var(--border-color); }
.setting-item-last { border-bottom: none; }
.setting-info { display: flex; flex-direction: column; gap: 2px; }
.setting-label { font-size: 14px; font-weight: 500; color: var(--text-primary); }
.setting-hint { font-size: 12px; color: var(--text-muted); }

.text-radio-group {
  :deep(.el-radio) { margin-right: 20px; &:last-child { margin-right: 0; } .el-radio__label { font-size: 13px; color: var(--text-muted); } &.is-checked .el-radio__label { color: var(--text-primary); } }
}

.settings-footer { padding-top: 20px; display: flex; justify-content: flex-start; }
:deep(.el-button) { padding: 8px 24px; border: 1px solid var(--border-color); background: transparent; color: var(--text-primary); font-size: 13px; &:hover { border-color: var(--text-primary); } }
</style>
