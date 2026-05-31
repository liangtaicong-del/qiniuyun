<template>
  <div class="stats-page">
    <div class="page-header">
      <h1 class="page-title">数据统计</h1>
      <p class="page-desc">查看内容发布数据和趋势</p>
    </div>

    <div class="stats-overview">
      <div class="overview-card">
        <div class="overview-icon"><FileTextIcon :size="16" /></div>
        <div class="overview-info">
          <span class="overview-value">{{ stats.overview?.totalArticles || 0 }}</span>
          <span class="overview-label">文章总数</span>
        </div>
      </div>
      <div class="overview-card">
        <div class="overview-icon"><CheckCircleIcon :size="16" /></div>
        <div class="overview-info">
          <span class="overview-value">{{ stats.overview?.successTasks || 0 }}</span>
          <span class="overview-label">成功发布</span>
        </div>
      </div>
      <div class="overview-card">
        <div class="overview-icon"><TrendingUpIcon :size="16" /></div>
        <div class="overview-info">
          <span class="overview-value">{{ successRate }}%</span>
          <span class="overview-label">成功率</span>
        </div>
      </div>
      <div class="overview-card">
        <div class="overview-icon"><TargetIcon :size="16" /></div>
        <div class="overview-info">
          <span class="overview-value">{{ stats.overview?.totalTasks || 0 }}</span>
          <span class="overview-label">总发布量</span>
        </div>
      </div>
    </div>

    <div class="charts-grid">
      <div class="chart-card">
        <h3 class="chart-title">发布趋势</h3>
        <div ref="trendChartRef" class="chart-container"></div>
      </div>
      <div class="chart-card">
        <h3 class="chart-title">各平台发布量</h3>
        <div ref="platformChartRef" class="chart-container"></div>
      </div>
    </div>

    <div class="platform-section">
      <h3 class="section-title">平台详情</h3>
      <div class="platform-table">
        <el-table :data="stats.platforms || []">
          <el-table-column prop="platformName" label="平台" min-width="120">
            <template #default="{ row }">
              <div class="platform-cell">
                <component :is="getPlatformIcon(row.platform)" :size="16" />
                <span>{{ row.platformName }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="totalCount" label="总发布量" width="120" align="center">
            <template #default="{ row }"><span class="stat-value">{{ row.totalCount || 0 }}</span></template>
          </el-table-column>
          <el-table-column prop="successCount" label="成功" width="100" align="center">
            <template #default="{ row }"><span class="stat-value">{{ row.successCount || 0 }}</span></template>
          </el-table-column>
          <el-table-column prop="failedCount" label="失败" width="100" align="center">
            <template #default="{ row }"><span class="stat-value stat-muted">{{ row.failedCount || 0 }}</span></template>
          </el-table-column>
          <el-table-column label="成功率" min-width="180">
            <template #default="{ row }">
              <div class="progress-cell">
                <el-progress :percentage="getPlatformRate(row)" :stroke-width="4" />
                <span class="rate-text">{{ getPlatformRate(row) }}%</span>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onActivated, onBeforeUnmount, shallowRef, nextTick } from 'vue'
import { getOverviewStats, getPlatformStats } from '@/api/stats'
import * as echarts from 'echarts'
import dayjs from 'dayjs'
import { FileText as FileTextIcon, CheckCircle as CheckCircleIcon, TrendingUp as TrendingUpIcon, Target as TargetIcon, MessageCircle as MessageCircleIcon, Share2 as Share2Icon, HelpCircle as HelpCircleIcon, BookOpen as BookOpenIcon, Monitor as MonitorIcon, Gem as GemIcon, Globe as GlobeIcon } from 'lucide-vue-next'

const stats = reactive({ overview: {}, platforms: [], trend: {} })
const trendChartRef = ref(null)
const platformChartRef = ref(null)
const trendChart = shallowRef(null)
const platformChart = shallowRef(null)

const successRate = computed(() => {
  const total = stats.overview?.totalTasks || 0
  const success = stats.overview?.successTasks || 0
  if (!total) return 0
  return Math.round((success / total) * 100)
})

const platformIcons = { WECHAT: MessageCircleIcon, WEIBO: Share2Icon, ZHIHU: HelpCircleIcon, JIANSHU: BookOpenIcon, CSDN: MonitorIcon, JUEJIN: GemIcon, BAIJIA: GlobeIcon, TOUTIAO: FileTextIcon }
const getPlatformIcon = (platform) => platformIcons[platform] || GlobeIcon
const getPlatformRate = (row) => { if (!row.totalCount) return 0; return Math.round((row.successCount / row.totalCount) * 100) }

const getChartColors = () => {
  const isDark = document.documentElement.getAttribute('data-theme') === 'dark'
  return { text: isDark ? '#F0F0F0' : '#111111', muted: isDark ? '#555555' : '#999999', border: isDark ? '#222222' : '#E5E5E5', grid: isDark ? '#1A1A1A' : '#F0F0F0', tooltipBg: isDark ? '#1E1E1E' : '#FFFFFF', tooltipText: isDark ? '#E0E0E0' : '#222222' }
}

const loadStats = async () => {
  try {
    const [overviewRes, platformRes] = await Promise.all([getOverviewStats().catch(() => ({ data: { data: {} } })), getPlatformStats().catch(() => ({ data: { data: [] } }))])
    if (overviewRes.data?.data?.overview) stats.overview = overviewRes.data.data.overview
    if (overviewRes.data?.data?.draftTrend) stats.trend = overviewRes.data.data.draftTrend
    if (platformRes.data?.data) stats.platforms = platformRes.data.data
    await nextTick()
    initTrendChart()
    initPlatformChart()
  } catch (e) { console.error('Failed to load stats:', e) }
}

const initTrendChart = () => {
  if (!trendChartRef.value) return
  trendChart.value = echarts.init(trendChartRef.value)
  const c = getChartColors()
  const days = [], success = [], fail = []
  const today = dayjs()
  for (let i = 29; i >= 0; i--) {
    const d = today.subtract(i, 'day').format('YYYY-MM-DD')
    days.push(today.subtract(i, 'day').format('MM-DD'))
    success.push(stats.trend?.[d] || 0)
    fail.push(0)
  }
  trendChart.value.setOption({
    tooltip: { trigger: 'axis', backgroundColor: c.tooltipBg, borderColor: c.border, textStyle: { color: c.tooltipText } },
    legend: { data: ['成功', '失败'], top: 0, textStyle: { color: c.muted, fontSize: 12 } },
    grid: { left: 40, right: 20, top: 36, bottom: 30 },
    xAxis: { type: 'category', data: days, axisLine: { lineStyle: { color: c.border } }, axisLabel: { color: c.muted } },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: c.grid } }, axisLabel: { color: c.muted } },
    series: [
      { name: '成功', type: 'bar', data: success, itemStyle: { color: c.text } },
      { name: '失败', type: 'bar', data: fail, itemStyle: { color: c.muted } }
    ]
  })
}

const initPlatformChart = () => {
  if (!platformChartRef.value) return
  platformChart.value = echarts.init(platformChartRef.value)
  const c = getChartColors()
  const data = stats.platforms.filter(p => p.totalCount > 0)
  platformChart.value.setOption({
    tooltip: { trigger: 'axis', backgroundColor: c.tooltipBg, borderColor: c.border, textStyle: { color: c.tooltipText } },
    grid: { left: 50, right: 20, top: 20, bottom: 60 },
    xAxis: { type: 'category', data: data.map(p => p.platformName), axisLine: { lineStyle: { color: c.border } }, axisLabel: { color: c.muted, rotate: 0 } },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: c.grid } }, axisLabel: { color: c.muted } },
    series: [{ type: 'bar', data: data.map(p => p.totalCount), itemStyle: { color: c.text } }]
  })
}

onMounted(() => { loadStats(); window.addEventListener('resize', () => { trendChart.value?.resize(); platformChart.value?.resize() }) })
onActivated(() => { loadStats() })
onBeforeUnmount(() => { window.removeEventListener('resize', () => { trendChart.value?.resize(); platformChart.value?.resize() }); trendChart.value?.dispose(); platformChart.value?.dispose() })
</script>

<style lang="scss" scoped>
.stats-page { padding: 0; }
.page-header { padding: 24px 0; border-top: 1px solid var(--border-color); border-bottom: 1px solid var(--border-color); margin-bottom: 0; }
.page-title { font-size: 20px; font-weight: 500; color: var(--text-primary); margin-bottom: 4px; letter-spacing: -0.01em; }
.page-desc { color: var(--text-muted); font-size: 13px; }

.stats-overview { display: grid; grid-template-columns: repeat(4, 1fr); border: 1px solid var(--border-color); border-bottom: none; }
.overview-card { padding: 20px 24px; display: flex; align-items: center; gap: 16px; border-right: 1px solid var(--border-color); border-bottom: 1px solid var(--border-color); &:last-child { border-right: none; } }
.overview-icon { color: var(--text-muted); flex-shrink: 0; }
.overview-info { display: flex; flex-direction: column; }
.overview-value { font-size: 24px; font-weight: 500; color: var(--text-primary); line-height: 1.2; letter-spacing: -0.02em; }
.overview-label { font-size: 12px; color: var(--text-muted); text-transform: uppercase; letter-spacing: 0.06em; margin-top: 2px; }

.charts-grid { display: grid; grid-template-columns: 1fr 1fr; border: 1px solid var(--border-color); border-bottom: none; }
.chart-card { padding: 24px; border-right: 1px solid var(--border-color); border-bottom: 1px solid var(--border-color); &:last-child { border-right: none; } }
.chart-title { font-size: 12px; font-weight: 500; color: var(--text-muted); margin-bottom: 16px; text-transform: uppercase; letter-spacing: 0.06em; }
.chart-container { height: 200px; }

.platform-section { border-top: 1px solid var(--border-color); padding-top: 24px; }
.section-title { font-size: 12px; font-weight: 500; color: var(--text-muted); margin-bottom: 16px; text-transform: uppercase; letter-spacing: 0.06em; }
.platform-cell { display: flex; align-items: center; gap: 8px; color: var(--text-secondary); }
.stat-value { font-weight: 500; color: var(--text-primary); }
.stat-muted { color: var(--text-muted); }
.progress-cell { display: flex; align-items: center; gap: 12px; :deep(.el-progress) { flex: 1; } }
.rate-text { font-size: 12px; font-weight: 500; color: var(--text-muted); min-width: 36px; }

@media (max-width: 1200px) {
  .stats-overview { grid-template-columns: repeat(2, 1fr); }
  .stats-overview .overview-card:nth-child(2) { border-right: none; }
  .stats-overview .overview-card:nth-child(1), .stats-overview .overview-card:nth-child(2) { border-bottom: 1px solid var(--border-color); }
  .charts-grid { grid-template-columns: 1fr; }
  .chart-card { border-right: none; }
}
</style>
