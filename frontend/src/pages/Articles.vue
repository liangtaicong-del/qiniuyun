<template>
  <div class="articles-page">
    <div class="page-header">
      <h1 class="page-title">文章管理</h1>
      <el-button type="text" @click="$router.push('/articles/create')">新建文章</el-button>
    </div>

    <div class="filter-bar">
      <el-input v-model="searchKeyword" placeholder="搜索文章标题..." class="search-input" clearable @input="handleSearch" />
      <el-select v-model="filterStatus" placeholder="状态" clearable @change="loadArticles">
        <el-option label="全部" value="" />
        <el-option label="已发布" value="PUBLISHED" />
        <el-option label="草稿" value="DRAFT" />
      </el-select>
    </div>

    <div class="articles-table">
      <el-table v-loading="loading" :data="articles" row-key="id" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="50" />
        <el-table-column label="文章" min-width="300">
          <template #default="{ row }">
            <div class="article-cell" @click="$router.push(`/articles/${row.id}/edit`)">
              <div class="article-cover" v-if="row.coverImage">
                <img :src="row.coverImage" :alt="row.title" />
              </div>
              <div class="article-info">
                <span class="article-title">{{ row.title }}</span>
                <span class="article-summary" v-if="row.summary">{{ row.summary }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <span :class="['status-label', row.status === 'PUBLISHED' ? 'status-published' : 'status-draft']">
              {{ row.status === 'PUBLISHED' ? '已发布' : '草稿' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="标签" width="200">
          <template #default="{ row }">
            <div class="tags-cell">
              <span v-for="tag in (row.tags || []).slice(0, 2)" :key="tag" class="tag">{{ tag }}</span>
              <span v-if="(row.tags || []).length > 2" class="tag tag-more">+{{ row.tags.length - 2 }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="viewCount" label="浏览" width="80" align="center">
          <template #default="{ row }"><span class="count">{{ row.viewCount || 0 }}</span></template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="160" align="center">
          <template #default="{ row }"><span class="date">{{ formatDate(row.createdAt) }}</span></template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center">
          <template #default="{ row }">
            <span class="action-link" @click="$router.push(`/articles/${row.id}/edit`)">编辑</span>
            <span class="action-divider">/</span>
            <span class="action-link action-delete" @click="handleDelete(row)">删除</span>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper" v-if="total > 0">
        <el-pagination v-model:current-page="currentPage" v-model:page-size="pageSize" :total="total" layout="total, prev, pager, next" @current-change="loadArticles" @size-change="loadArticles" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getArticles, deleteArticle } from '@/api/article'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'

const loading = ref(false)
const articles = ref([])
const searchKeyword = ref('')
const filterStatus = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const selectedArticles = ref([])
let searchTimer = null

const formatDate = (date) => dayjs(date).format('YYYY-MM-DD HH:mm')

const handleSearch = () => {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => { currentPage.value = 1; loadArticles() }, 300)
}

const loadArticles = async () => {
  loading.value = true
  try {
    const res = await getArticles({ page: currentPage.value - 1, size: pageSize.value })
    articles.value = res.data?.content || res.data || []
    total.value = res.data?.totalElements || articles.value.length
  } catch (e) { console.error('Failed to load articles:', e); articles.value = [] }
  finally { loading.value = false }
}

const handleSelectionChange = (selection) => { selectedArticles.value = selection }

const handleDelete = async (article) => {
  try {
    await ElMessageBox.confirm('确定要删除这篇文章吗？删除后无法恢复。', '提示', { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' })
    await deleteArticle(article.id)
    ElMessage.success('删除成功')
    loadArticles()
  } catch (e) { if (e !== 'cancel') console.error('Failed to delete article:', e) }
}

onMounted(() => { loadArticles() })
</script>

<style lang="scss" scoped>
.articles-page { padding: 0; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 0; padding: 24px 0; border-top: 1px solid var(--border-color); border-bottom: 1px solid var(--border-color); }
.page-title { font-size: 20px; font-weight: 500; color: var(--text-primary); margin: 0; letter-spacing: -0.01em; }

.filter-bar { display: flex; gap: 24px; align-items: center; padding: 12px 0; border-bottom: 1px solid var(--border-color); }
.search-input { width: 240px; }
:deep(.el-input__wrapper) { box-shadow: none; background: transparent; border-radius: 0; border: none; border-bottom: 1px solid var(--border-color); padding: 4px 0; }
:deep(.el-input__inner) { border: none; background: transparent; }
:deep(.el-select .el-input__wrapper) { border: none; border-bottom: 1px solid var(--border-color); }

.articles-table {
  :deep(.el-table) { background: transparent; border-radius: 0; box-shadow: none; border: none; }
  :deep(.el-table::before) { display: none; }
  :deep(.el-table__header-wrapper th) { background: transparent !important; font-weight: 500; color: var(--text-secondary) !important; font-size: 12px; text-transform: uppercase; letter-spacing: 0.06em; border-bottom: 1px solid var(--border-color) !important; padding: 14px 0 !important; }
  :deep(.el-table__body-wrapper tr) { border-bottom: 1px solid var(--border-light); td { background: transparent !important; border-bottom: 1px solid var(--border-light) !important; padding: 16px 0; } }
  :deep(.el-table__body-wrapper tr:last-child td) { border-bottom: 1px solid var(--border-color) !important; }
}

.article-cell { display: flex; align-items: center; gap: 16px; cursor: pointer; }
.article-cover { width: 48px; height: 48px; overflow: hidden; flex-shrink: 0; img { width: 100%; height: 100%; object-fit: cover; } }
.article-info { display: flex; flex-direction: column; gap: 2px; min-width: 0; }
.article-title { font-size: 14px; color: var(--text-primary); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.article-summary { font-size: 12px; color: var(--text-muted); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.status-label { font-size: 13px; }
.status-published { color: var(--text-primary); font-weight: 500; }
.status-draft { color: var(--text-muted); }
.tags-cell { display: flex; flex-wrap: wrap; gap: 6px; .tag { font-size: 12px; color: var(--text-muted); } .tag-more { color: var(--text-muted); } }
.count, .date { font-size: 13px; color: var(--text-secondary); }
.action-link { color: var(--text-muted); cursor: pointer; font-size: 13px; transition: color var(--transition-fast); &:hover { color: var(--text-primary); } }
.action-delete:hover { color: var(--error); }
.action-divider { color: var(--border-color); margin: 0 4px; font-size: 13px; }
.pagination-wrapper { padding: 16px 0; border-top: 1px solid var(--border-color); display: flex; justify-content: flex-end; }
</style>
