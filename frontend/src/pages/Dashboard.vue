<template>
  <div class="dashboard">
    <div class="page-header">
      <h1 class="page-title">仪表盘</h1>
      <p class="page-desc">内容发布概览</p>
    </div>

    <div class="stats-row">
      <div class="stat-item">
        <span class="stat-value">{{ stats.totalArticles }}</span>
        <span class="stat-label">文章总数</span>
      </div>
      <div class="stat-item">
        <span class="stat-value">{{ stats.successTasks }}</span>
        <span class="stat-label">发布成功</span>
      </div>
      <div class="stat-item">
        <span class="stat-value">{{ stats.failedTasks }}</span>
        <span class="stat-label">发布失败</span>
      </div>
      <div class="stat-item">
        <span class="stat-value">{{ stats.draftArticles }}</span>
        <span class="stat-label">草稿</span>
      </div>
    </div>

    <section class="section">
      <h2 class="section-title">最近文章</h2>
      <div v-if="recentArticles.length === 0" class="empty-state">
        <FileTextIcon :size="28" />
        <p>暂无文章</p>
        <el-button type="primary" @click="$router.push('/articles/create')">创建第一篇文章</el-button>
      </div>
      <div v-else class="article-list">
        <div v-for="article in recentArticles" :key="article.id" class="article-row" @click="$router.push(`/articles/${article.id}/edit`)">
          <span class="article-title">{{ article.title }}</span>
          <div class="article-tags">
            <el-tag size="small" :type="article.status === 'PUBLISHED' ? 'success' : 'info'">
              {{ article.status === 'PUBLISHED' ? '已发布' : '草稿' }}
            </el-tag>
          </div>
          <span class="article-date">{{ formatDate(article.createdAt) }}</span>
        </div>
      </div>
      <div class="section-footer" v-if="recentArticles.length > 0">
        <el-button type="text" @click="$router.push('/articles')">查看全部</el-button>
      </div>
    </section>

    <section class="section">
      <h2 class="section-title">文章趋势</h2>
      <div ref="trendChartRef" class="chart-container"></div>
    </section>

    <section class="section">
      <h2 class="section-title">平台统计</h2>
      <div class="platform-list">
        <div v-for="platform in platformStats" :key="platform.platform" class="platform-row">
          <span class="platform-name">{{ platform.platformName || platform.platform }}</span>
          <span class="platform-count">{{ platform.totalCount || 0 }}</span>
        </div>
        <div v-if="platformStats.length === 0" class="empty-text">暂无数据</div>
      </div>
    </section>

    <section class="section">
      <h2 class="section-title">各平台发布量</h2>
      <div ref="platformChartRef" class="chart-container"></div>
    </section>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onActivated, onBeforeUnmount, shallowRef, nextTick } from 'vue'
import { getRecentArticles } from '@/api/article'
import { getUserPlatforms } from '@/api/platform'
import { getOverviewStats } from '@/api/stats'
import dayjs from 'dayjs'
import * as echarts from 'echarts'
import { FileText as FileTextIcon } from 'lucide-vue-next'

const stats = reactive({ totalArticles: 0, publishedArticles: 0, draftArticles: 0, successTasks: 0, failedTasks: 0, pendingTasks: 0 })
const recentArticles = ref([])
const trendChartRef = ref(null)
const trendChart = shallowRef(null)
const platformChartRef = ref(null)
const platformChart = shallowRef(null)
const draftTrend = ref({})
const publishedTrend = ref({})
const platformStats = ref([])

const formatDate = (date) => dayjs(date).format('MM-DD HH:mm')

const loadData = async () => {
  try {
    const [statsRes, articlesRes, platformsRes] = await Promise.all([
      getOverviewStats().catch(() => ({ data: { overview: stats } })),
      getRecentArticles(5).catch(() => ({ data: [] })),
      getUserPlatforms().catch(() => ({ data: [] }))
    ])
    if (statsRes.data?.overview) Object.assign(stats, statsRes.data.overview)
    if (statsRes.data?.draftTrend) draftTrend.value = statsRes.data.draftTrend
    if (statsRes.data?.publishedTrend) publishedTrend.value = statsRes.data.publishedTrend
    if (statsRes.data?.platforms) platformStats.value = statsRes.data.platforms
    if (articlesRes.data) recentArticles.value = Array.isArray(articlesRes.data) ? articlesRes.data : []
    await nextTick()
    initTrendChart()
    initPlatformChart()
  } catch (e) {
    console.error('Failed to load dashboard data:', e)
  }
}

const getChartColors = () => {
  const isDark = document.documentElement.getAttribute('data-theme') === 'dark'
  return {
    text: isDark ? '#F0F0F0' : '#111111',
    muted: isDark ? '#555555' : '#999999',
    border: isDark ? '#222222' : '#E5E5E5',
    grid: isDark ? '#1A1A1A' : '#F0F0F0'
  }
}

const initTrendChart = () => {
  if (!trendChartRef.value) return
  trendChart.value = echarts.init(trendChartRef.value)
  const c = getChartColors()
  const days = [], draftData = [], publishedData = []
  const today = dayjs()
  for (let i = 6; i >= 0; i--) {
    const d = today.subtract(i, 'day').format('YYYY-MM-DD')
    days.push(today.subtract(i, 'day').format('MM-DD'))
    draftData.push(draftTrend.value[d] || 0)
    publishedData.push(publishedTrend.value[d] || 0)
  }
  trendChart.value.setOption({
    tooltip: { trigger: 'axis', backgroundColor: c.text, borderColor: c.border, textStyle: { color: c.text } },
    legend: { data: ['草稿', '已发布'], top: 0, textStyle: { color: c.muted, fontSize: 12 } },
    grid: { left: 40, right: 20, top: 36, bottom: 30 },
    xAxis: { type: 'category', data: days, axisLine: { lineStyle: { color: c.border } }, axisLabel: { color: c.muted } },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: c.grid } }, axisLabel: { color: c.muted } },
    series: [
      { name: '草稿', type: 'line', data: draftData, smooth: true, symbol: 'circle', symbolSize: 6, lineStyle: { color: c.muted, width: 2 }, itemStyle: { color: c.muted, borderColor: c.text, borderWidth: 2 } },
      { name: '已发布', type: 'line', data: publishedData, smooth: true, symbol: 'circle', symbolSize: 6, lineStyle: { color: c.text, width: 2 }, itemStyle: { color: c.text, borderColor: c.text, borderWidth: 2 } }
    ]
  })
}

const initPlatformChart = () => {
  if (!platformChartRef.value) return
  platformChart.value = echarts.init(platformChartRef.value)
  const c = getChartColors()
  const platforms = platformStats.value.filter(p => p.totalCount > 0)
  platformChart.value?.setOption({
    tooltip: { trigger: 'axis', backgroundColor: c.text, borderColor: c.border, textStyle: { color: c.text } },
    grid: { left: 50, right: 20, top: 20, bottom: 30 },
    xAxis: { type: 'category', data: platforms.map(p => p.platformName || p.platform), axisLine: { lineStyle: { color: c.border } }, axisLabel: { color: c.muted, rotate: 30 } },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: c.grid } }, axisLabel: { color: c.muted } },
    series: [{ type: 'bar', data: platforms.map(p => p.totalCount || 0), itemStyle: { color: c.text } }]
  })
}

onMounted(() => { loadData(); window.addEventListener('resize', onResize) })
onActivated(() => { loadData() })
onBeforeUnmount(() => { window.removeEventListener('resize', onResize); trendChart.value?.dispose(); platformChart.value?.dispose() })
const onResize = () => { trendChart.value?.resize(); platformChart.value?.resize() }
</script>

<style lang="scss" scoped>
.dashboard { animation: fadeIn 0.2s ease; }
@keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }

.page-header { margin-bottom: 0; }
.page-title { font-size: 20px; font-weight: 500; color: var(--text-primary); margin-bottom: 4px; letter-spacing: -0.01em; }
.page-desc { color: var(--text-muted); font-size: 13px; }

.stats-row { display: flex; align-items: center; padding: 24px 0; border-top: 1px solid var(--border-color); border-bottom: 1px solid var(--border-color); margin-bottom: 0; }
.stat-item { flex: 1; display: flex; flex-direction: column; align-items: center; padding: 0 24px; &:not(:first-child) { border-left: 1px solid var(--border-color); } }
.stat-value { font-size: 24px; font-weight: 500; color: var(--text-primary); line-height: 1.2; letter-spacing: -0.02em; }
.stat-label { font-size: 12px; color: var(--text-muted); margin-top: 4px; text-transform: uppercase; letter-spacing: 0.06em; }

.section { border-top: 1px solid var(--border-color); padding-top: 24px; margin-bottom: 40px; }
.section-title { font-size: 12px; font-weight: 500; color: var(--text-muted); margin-bottom: 16px; text-transform: uppercase; letter-spacing: 0.06em; }
.section-footer { margin-top: 16px; padding-top: 16px; border-top: 1px solid var(--border-light); }

.article-list { display: flex; flex-direction: column; }
.article-row { display: flex; align-items: center; padding: 12px 0; border-bottom: 1px solid var(--border-light); cursor: pointer; &:last-child { border-bottom: none; } &:hover .article-title { color: var(--text-secondary); } }
.article-title { flex: 1; font-size: 14px; color: var(--text-primary); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; margin-right: 16px; transition: color var(--transition-fast); }
.article-tags { display: flex; gap: 8px; margin-right: 16px; }
.article-date { font-size: 13px; color: var(--text-muted); white-space: nowrap; }

.chart-container { height: 220px; }

.platform-list { display: flex; flex-direction: column; }
.platform-row { display: flex; align-items: center; justify-content: space-between; padding: 12px 0; border-bottom: 1px solid var(--border-light); &:last-child { border-bottom: none; } }
.platform-name { font-size: 14px; color: var(--text-primary); }
.platform-count { font-size: 14px; font-weight: 500; color: var(--text-primary); }

.empty-state { text-align: center; padding: 32px 20px; color: var(--text-muted); svg { margin-bottom: 12px; opacity: 0.5; } p { margin-bottom: 12px; } }
.empty-text { font-size: 13px; color: var(--text-muted); padding: 16px 0; }

@media (max-width: 768px) {
  .stats-row { flex-wrap: wrap; gap: 16px; padding: 16px 0; }
  .stat-item { width: calc(50% - 8px); border-left: none !important; }
  .article-row { flex-wrap: wrap; gap: 8px; }
  .article-title { width: 100%; margin-right: 0; }
}
</style>
