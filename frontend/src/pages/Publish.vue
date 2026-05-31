<template>
  <div class="publish-page">
    <div class="page-header">
      <h1 class="page-title">发布管理</h1>
      <el-button type="text" @click="$router.push('/articles')">新建发布</el-button>
    </div>

    <el-tabs v-model="activeTab" @tab-change="handleTabChange" class="publish-tabs">
      <el-tab-pane label="全部任务" name="all" />
      <el-tab-pane label="待发布" name="PENDING" />
      <el-tab-pane label="已发布" name="SUCCESS" />
      <el-tab-pane label="发布失败" name="FAILED" />
    </el-tabs>

    <div class="publish-table">
      <el-table v-loading="loading" :data="tasks" row-key="id">
        <el-table-column label="文章" min-width="250">
          <template #default="{ row }"><span class="article-title">{{ row.articleTitle || `文章 #${row.articleId}` }}</span></template>
        </el-table-column>
        <el-table-column label="平台" width="120" align="center">
          <template #default="{ row }">
            <div class="platform-cell">
              <component :is="getPlatformIcon(row.platform)" :size="14" />
              <span>{{ row.platformName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="statusName" label="状态" width="100" align="center">
          <template #default="{ row }">
            <span :class="['status-label', `status-${row.status?.toLowerCase()}`]">{{ row.statusName }}</span>
          </template>
        </el-table-column>
        <el-table-column label="计划时间" width="160" align="center">
          <template #default="{ row }"><span class="date">{{ formatDate(row.scheduledAt) }}</span></template>
        </el-table-column>
        <el-table-column label="发布时间" width="160" align="center">
          <template #default="{ row }">
            <span class="date" v-if="row.publishedAt">{{ formatDate(row.publishedAt) }}</span>
            <span class="date-placeholder" v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center">
          <template #default="{ row }">
            <template v-if="row.platformUrl">
              <span class="action-link" @click="openUrl(row.platformUrl)">查看</span>
            </template>
            <template v-if="row.status === 'PENDING'">
              <span class="action-divider" v-if="row.platformUrl">/</span>
              <span class="action-link action-danger" @click="handleCancel(row)">取消</span>
            </template>
            <template v-if="row.status === 'FAILED'">
              <span class="action-divider" v-if="row.platformUrl">/</span>
              <span class="action-link" @click="handleRetry(row)">重试</span>
            </template>
          </template>
        </el-table-column>
        <el-table-column v-if="activeTab === 'all'" label="结果" min-width="200">
          <template #default="{ row }">
            <template v-if="row.errorMsg">
              <el-tooltip :content="row.errorMsg" placement="top"><span class="error-msg">{{ row.errorMsg }}</span></el-tooltip>
            </template>
            <template v-else-if="row.status === 'SUCCESS'"><span class="result-text">已发布</span></template>
            <template v-else><span class="result-text result-pending">等待中</span></template>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper" v-if="total > 0">
        <el-pagination v-model:current-page="currentPage" v-model:page-size="pageSize" :total="total" layout="total, prev, pager, next" @current-change="loadTasks" @size-change="loadTasks" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onActivated, onBeforeUnmount } from 'vue'
import { getPublishTasks, cancelTask, retryTask } from '@/api/publish'
import { ElMessage, ElMessageBox } from 'element-plus'
import { MessageCircle as MessageCircleIcon, Share2 as Share2Icon, HelpCircle as HelpCircleIcon, BookOpen as BookOpenIcon, Monitor as MonitorIcon, Gem as GemIcon, FileText as FileTextIcon, Globe as GlobeIcon } from 'lucide-vue-next'
import dayjs from 'dayjs'

const loading = ref(false)
const tasks = ref([])
const activeTab = ref('all')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const platformIcons = { WECHAT: MessageCircleIcon, WEIBO: Share2Icon, ZHIHU: HelpCircleIcon, JIANSHU: BookOpenIcon, CSDN: MonitorIcon, JUEJIN: GemIcon, BAIJIA: GlobeIcon, TOUTIAO: FileTextIcon }
const getPlatformIcon = (platform) => platformIcons[platform] || GlobeIcon
const formatDate = (date) => dayjs(date).format('YYYY-MM-DD HH:mm')

const handleTabChange = () => { currentPage.value = 1; loadTasks() }

const loadTasks = async () => {
  loading.value = true
  try {
    const params = { page: currentPage.value - 1, size: pageSize.value }
    if (activeTab.value !== 'all') params.status = activeTab.value
    const res = await getPublishTasks(params)
    tasks.value = res.data?.data?.content || res.data?.data || []
    total.value = res.data?.data?.totalElements || tasks.value.length
  } catch (e) { console.error('Failed to load tasks:', e); ElMessage.error('加载失败'); tasks.value = [] }
  finally { loading.value = false }
}

const handleCancel = async (task) => {
  try {
    await ElMessageBox.confirm('确定要取消这个发布任务吗？', '提示', { confirmButtonText: '取消任务', cancelButtonText: '不取消', type: 'warning' })
    await cancelTask(task.id)
    ElMessage.success('任务已取消')
    loadTasks()
  } catch (e) { if (e !== 'cancel') { console.error('Cancel failed:', e); ElMessage.error('取消失败') } }
}

const handleRetry = async (task) => {
  try {
    await ElMessageBox.confirm(`确定要重试发布到 ${task.platformName} 吗？`, '重试发布', { confirmButtonText: '重试', cancelButtonText: '取消', type: 'warning' })
    const res = await retryTask(task.id)
    ElMessage.success('重试任务已创建')
    loadTasks()
  } catch (e) { if (e !== 'cancel') { console.error('Retry failed:', e); ElMessage.error(e.response?.data?.message || '重试失败') } }
}
const openUrl = (url) => { window.open(url, '_blank') }

onMounted(() => { loadTasks() })
onActivated(() => { loadTasks() })
</script>

<style lang="scss" scoped>
.publish-page { padding: 0; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 0; padding: 24px 0; border-top: 1px solid var(--border-color); border-bottom: 1px solid var(--border-color); }
.page-title { font-size: 20px; font-weight: 500; color: var(--text-primary); margin: 0; letter-spacing: -0.01em; }

.publish-tabs { border-bottom: 1px solid var(--border-color); }
:deep(.el-tabs__header) { margin-bottom: 0; }
:deep(.el-tabs__nav-wrap::after) { display: none; }
:deep(.el-tabs__item) { height: 44px; line-height: 44px; font-size: 13px; font-weight: 400; color: var(--text-muted); padding: 0 16px 0 0; &.is-active { color: var(--text-primary); font-weight: 500; } }
:deep(.el-tabs__active-bar) { background-color: var(--text-primary); height: 1px; }

.publish-table {
  :deep(.el-table) { background: transparent; border-radius: 0; box-shadow: none; border: none; }
  :deep(.el-table::before) { display: none; }
  :deep(.el-table__header-wrapper th) { background: transparent !important; font-weight: 500; color: var(--text-secondary) !important; font-size: 12px; text-transform: uppercase; letter-spacing: 0.06em; border-bottom: 1px solid var(--border-color) !important; padding: 14px 0 !important; }
  :deep(.el-table__body-wrapper tr) { border-bottom: 1px solid var(--border-light); td { background: transparent !important; border-bottom: 1px solid var(--border-light) !important; padding: 16px 0; } }
  :deep(.el-table__body-wrapper tr:last-child td) { border-bottom: 1px solid var(--border-color) !important; }
}

.article-title { font-weight: 500; color: var(--text-primary); }
.platform-cell { display: flex; align-items: center; justify-content: center; gap: 6px; color: var(--text-secondary); font-size: 13px; }
.status-label { font-size: 13px; }
.status-success { color: var(--text-primary); font-weight: 500; }
.status-pending, .status-processing { color: var(--text-muted); }
.status-failed, .status-cancelled { color: var(--text-muted); }
.date { font-size: 13px; color: var(--text-secondary); }
.date-placeholder { font-size: 13px; color: var(--text-muted); }
.action-link { color: var(--text-muted); cursor: pointer; font-size: 13px; transition: color var(--transition-fast); &:hover { color: var(--text-primary); } }
.action-danger:hover { color: var(--error); }
.action-divider { color: var(--border-color); margin: 0 4px; font-size: 13px; }
.error-msg { color: var(--error); font-size: 13px; cursor: pointer; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.result-text { font-size: 13px; color: var(--text-primary); }
.result-pending { color: var(--text-muted); }
.pagination-wrapper { padding: 16px 0; border-top: 1px solid var(--border-color); display: flex; justify-content: flex-end; }
</style>
