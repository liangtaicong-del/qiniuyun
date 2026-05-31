<template>
  <div class="platforms-page">
    <div class="page-header">
      <h1 class="page-title">平台管理</h1>
      <p class="page-desc">绑定和管理您的内容发布平台</p>
    </div>

    <div class="platforms-grid">
      <div v-for="platform in platforms" :key="platform.id" class="platform-card" :class="{ bound: platform.bound }">
        <div class="platform-header">
          <div class="platform-icon">
            <component :is="getPlatformIcon(platform.id)" :size="20" />
          </div>
          <div class="platform-info">
            <div class="platform-name">{{ platform.name }}</div>
            <div class="platform-desc">{{ platform.description }}</div>
          </div>
        </div>

        <div class="platform-status">
          <div class="status-info">
            <template v-if="platform.bound">
              <div class="account-info">
                <div class="account-detail">
                  <span class="account-name">{{ platform.accountName }}</span>
                  <span class="bind-time">绑定于 {{ formatDate(platform.bindTime) }}</span>
                </div>
              </div>
            </template>
            <template v-else>
              <span class="not-bound">未绑定</span>
            </template>
          </div>
        </div>

        <div class="platform-actions">
          <template v-if="platform.bound">
            <el-button size="small" @click="handleUnbind(platform)">解绑</el-button>
          </template>
          <template v-else>
            <el-button type="primary" size="small" @click="handleBind(platform)">绑定</el-button>
          </template>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getAllPlatforms, getUserPlatforms, bindPlatform, unbindPlatform } from '@/api/platform'
import { ElMessage, ElMessageBox } from 'element-plus'
import { MessageCircle as MessageCircleIcon, Share2 as Share2Icon, HelpCircle as HelpCircleIcon, BookOpen as BookOpenIcon, Monitor as MonitorIcon, Gem as GemIcon, Globe as GlobeIcon, FileText as FileTextIcon } from 'lucide-vue-next'
import dayjs from 'dayjs'

const platforms = ref([])
const platformIcons = {
  WECHAT: MessageCircleIcon, WEIBO: Share2Icon, ZHIHU: HelpCircleIcon,
  JIANSHU: BookOpenIcon, CSDN: MonitorIcon, JUEJIN: GemIcon,
  BAIJIA: GlobeIcon, TOUTIAO: FileTextIcon
}
const getPlatformIcon = (id) => platformIcons[id] || GlobeIcon
const formatDate = (date) => dayjs(date).format('YYYY-MM-DD')

const allPlatforms = [
  { id: 'WECHAT', name: '微信公众号', description: '微信公众平台内容发布' },
  { id: 'WEIBO', name: '微博', description: '新浪微博内容同步' },
  { id: 'ZHIHU', name: '知乎', description: '知乎专栏文章发布' },
  { id: 'JIANSHU', name: '简书', description: '简书社区文章发布' },
  { id: 'CSDN', name: 'CSDN', description: 'CSDN 博客文章发布' },
  { id: 'JUEJIN', name: '掘金', description: '稀土掘金社区文章发布' },
  { id: 'BAIJIA', name: '百家号', description: '百度百家号内容发布' },
  { id: 'TOUTIAO', name: '头条号', description: '今日头条内容发布' }
]

const loadPlatforms = async () => {
  try {
    const [allRes, userRes] = await Promise.all([
      getAllPlatforms().catch(() => ({ data: { data: allPlatforms } })),
      getUserPlatforms().catch(() => ({ data: { data: [] } }))
    ])
    const userPlatforms = userRes.data?.data || []
    platforms.value = allPlatforms.map(p => {
      const bound = userPlatforms.find(up => up.platform === p.id)
      return { ...p, bound: !!bound, accountName: bound?.accountName, bindTime: bound?.bindTime }
    })
  } catch (e) { console.error('Failed to load platforms:', e) }
}

const handleBind = async (platform) => {
  try {
    await bindPlatform(platform.id, '')
    ElMessage.success(`${platform.name} 绑定成功`)
    loadPlatforms()
  } catch (e) {
    console.error('Bind failed:', e)
    ElMessage.error(e.response?.data?.message || '绑定失败')
  }
}

const handleUnbind = async (platform) => {
  try {
    await ElMessageBox.confirm(`确定要解绑 ${platform.name} 吗？`, '提示', { confirmButtonText: '解绑', cancelButtonText: '取消', type: 'warning' })
    await unbindPlatform(platform.id)
    ElMessage.success('解绑成功')
    loadPlatforms()
  } catch (e) { if (e !== 'cancel') { console.error('Unbind failed:', e); ElMessage.error(e.response?.data?.message || '解绑失败') } }
}

onMounted(() => { loadPlatforms() })
</script>

<style lang="scss" scoped>
.platforms-page { padding: 0; }
.page-header { padding: 24px 0; border-top: 1px solid var(--border-color); border-bottom: 1px solid var(--border-color); margin-bottom: 0; }
.page-title { font-size: 20px; font-weight: 500; color: var(--text-primary); margin-bottom: 4px; letter-spacing: -0.01em; }
.page-desc { color: var(--text-muted); font-size: 13px; }

.platforms-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 0; border: 1px solid var(--border-color); border-bottom: none; border-right: none; }
.platform-card { padding: 24px; border-right: 1px solid var(--border-color); border-bottom: 1px solid var(--border-color); transition: background-color var(--transition-fast); &:hover { background: var(--bg-hover); } }
.platform-header { display: flex; gap: 12px; margin-bottom: 16px; }
.platform-icon { width: 36px; height: 36px; display: flex; align-items: center; justify-content: center; color: var(--text-muted); flex-shrink: 0; .bound & { color: var(--text-primary); } }
.platform-info { flex: 1; min-width: 0; }
.platform-name { font-size: 14px; font-weight: 500; color: var(--text-primary); margin-bottom: 2px; }
.platform-desc { font-size: 12px; color: var(--text-muted); }
.platform-status { padding: 12px 0; border-top: 1px solid var(--border-light); margin-bottom: 12px; }
.account-detail { display: flex; flex-direction: column; }
.account-name { font-weight: 500; color: var(--text-primary); font-size: 13px; }
.bind-time { font-size: 12px; color: var(--text-muted); }
.not-bound { font-size: 13px; color: var(--text-muted); }
.platform-actions { display: flex; justify-content: flex-end; }
</style>
