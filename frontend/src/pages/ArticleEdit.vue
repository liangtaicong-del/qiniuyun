<template>
  <div class="article-edit-page">
    <div class="page-header">
      <div class="header-left">
        <el-button text @click="$router.push('/articles')">
          <ArrowLeftIcon :size="16" /> 返回
        </el-button>
        <span class="page-title">{{ isEdit ? '编辑文章' : '创建文章' }}</span>
      </div>
      <div class="header-right">
        <el-button @click="handleSaveDraft" :loading="savingDraft">保存草稿</el-button>
        <el-button type="primary" @click="handlePublish" :loading="publishing">发布</el-button>
      </div>
    </div>

    <div class="editor-layout">
      <div class="editor-main">
        <div class="title-input">
          <el-input v-model="form.title" placeholder="输入文章标题..." size="large" />
        </div>

        <div class="content-editor">
          <div class="editor-toolbar">
            <button v-for="tool in toolbar" :key="tool.name" class="toolbar-btn" :class="{ active: tool.active }" @click="tool.action" :title="tool.title" :disabled="!editor">
              <component :is="tool.icon" :size="16" />
            </button>
          </div>
          <div class="editor-content">
            <editor-content :editor="editor" />
          </div>
        </div>
      </div>

      <div class="editor-sidebar">
        <div class="sidebar-card">
          <div class="card-title">发布设置</div>
          <div class="setting-item">
            <span class="setting-label">平台</span>
            <div class="platform-list">
              <div
                v-for="platform in availablePlatforms"
                :key="platform.id"
                class="platform-item"
                :class="{ selected: selectedPlatforms.includes(platform.id) }"
                @click="togglePlatform(platform.id)"
              >
                <component :is="platform.icon" :size="14" />
                <span>{{ platform.name }}</span>
                <CheckIcon v-if="selectedPlatforms.includes(platform.id)" :size="12" class="check-icon" />
              </div>
            </div>
          </div>
          <div class="setting-item">
            <span class="setting-label">定时发布</span>
            <el-switch v-model="form.scheduled" />
          </div>
          <div v-if="form.scheduled" class="setting-item">
            <el-date-picker v-model="form.scheduledAt" type="datetime" placeholder="选择时间" style="width: 100%" />
          </div>
        </div>

        <div class="sidebar-card">
          <div class="card-title">摘要</div>
          <el-input v-model="form.summary" type="textarea" :rows="3" placeholder="文章摘要（可选）" />
        </div>

        <div class="sidebar-card">
          <div class="card-title">标签</div>
          <el-input v-model="form.tagsInput" placeholder="用逗号分隔标签" />
          <div v-if="form.tags.length" class="tags-preview">
            <el-tag v-for="tag in form.tags" :key="tag" size="small" closable @close="removeTag(tag)">{{ tag }}</el-tag>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { createArticle, updateArticle, getArticle } from '@/api/article'
import { publishArticle } from '@/api/publish'
import { ElMessage } from 'element-plus'
import { useEditor, EditorContent } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import Placeholder from '@tiptap/extension-placeholder'
import Link from '@tiptap/extension-link'
import Image from '@tiptap/extension-image'
import {
  ArrowLeft as ArrowLeftIcon, Bold as BoldIcon, Italic as ItalicIcon,
  Strikethrough as StrikeIcon, Code as CodeIcon, List as ListIcon,
  ListOrdered as OrderedListIcon, Quote as QuoteIcon, Minus as MinusIcon,
  Link as LinkIcon, Check as CheckIcon,
  MessageCircle as MessageCircleIcon, Share2 as Share2Icon, HelpCircle as HelpCircleIcon,
  BookOpen as BookOpenIcon, Monitor as MonitorIcon, Gem as GemIcon,
  Globe as GlobeIcon, FileText as FileTextIcon
} from 'lucide-vue-next'
import dayjs from 'dayjs'

const route = useRoute()
const router = useRouter()
const isEdit = computed(() => !!route.params.id)
const savingDraft = ref(false)
const publishing = ref(false)

const form = reactive({ title: '', summary: '', content: '', tags: [], tagsInput: '', scheduled: false, scheduledAt: null })

const availablePlatforms = [
  { id: 'WECHAT', name: '微信公众号', icon: MessageCircleIcon },
  { id: 'WEIBO', name: '微博', icon: Share2Icon },
  { id: 'ZHIHU', name: '知乎', icon: HelpCircleIcon },
  { id: 'JIANSHU', name: '简书', icon: BookOpenIcon },
  { id: 'CSDN', name: 'CSDN', icon: MonitorIcon },
  { id: 'JUEJIN', name: '掘金', icon: GemIcon },
  { id: 'BAIJIA', name: '百家号', icon: GlobeIcon },
  { id: 'TOUTIAO', name: '头条号', icon: FileTextIcon }
]
const selectedPlatforms = ref([])

const editor = useEditor({
  extensions: [
    StarterKit,
    Placeholder.configure({ placeholder: '开始写作...' }),
    Link.configure({ openOnClick: false }),
    Image
  ],
  content: '',
  onUpdate: ({ editor }) => { form.content = editor.getHTML() }
})

watch(() => form.tagsInput, (val) => {
  form.tags = val.split(',').map(t => t.trim()).filter(t => t)
})

const togglePlatform = (id) => {
  const idx = selectedPlatforms.value.indexOf(id)
  if (idx > -1) selectedPlatforms.value.splice(idx, 1)
  else selectedPlatforms.value.push(id)
}

const removeTag = (tag) => { form.tags = form.tags.filter(t => t !== tag) }

const toolbar = computed(() => [
  { name: 'bold', icon: BoldIcon, title: '粗体', action: () => editor.value?.chain().focus().toggleBold().run(), active: editor.value?.isActive('bold') },
  { name: 'italic', icon: ItalicIcon, title: '斜体', action: () => editor.value?.chain().focus().toggleItalic().run(), active: editor.value?.isActive('italic') },
  { name: 'strike', icon: StrikeIcon, title: '删除线', action: () => editor.value?.chain().focus().toggleStrike().run(), active: editor.value?.isActive('strike') },
  { name: 'code', icon: CodeIcon, title: '行内代码', action: () => editor.value?.chain().focus().toggleCode().run(), active: editor.value?.isActive('code') },
  { name: 'bullet', icon: ListIcon, title: '无序列表', action: () => editor.value?.chain().focus().toggleBulletList().run(), active: editor.value?.isActive('bulletList') },
  { name: 'ordered', icon: OrderedListIcon, title: '有序列表', action: () => editor.value?.chain().focus().toggleOrderedList().run(), active: editor.value?.isActive('orderedList') },
  { name: 'blockquote', icon: QuoteIcon, title: '引用', action: () => editor.value?.chain().focus().toggleBlockquote().run(), active: editor.value?.isActive('blockquote') },
  { name: 'codeBlock', icon: MinusIcon, title: '代码块', action: () => editor.value?.chain().focus().toggleCodeBlock().run(), active: editor.value?.isActive('codeBlock') },
])

const loadArticle = async () => {
  if (!isEdit.value) return
  try {
    const res = await getArticle(route.params.id)
    const data = res.data?.data
    form.title = data?.title || ''
    form.summary = data?.summary || ''
    form.tags = data?.tags || []
    form.tagsInput = (data?.tags || []).join(', ')
    if (editor.value && data?.content) editor.value.commands.setContent(data.content)
  } catch (e) { console.error('Failed to load article:', e) }
}

const handleSaveDraft = async () => {
  if (!form.title.trim()) { ElMessage.warning('请输入文章标题'); return }
  savingDraft.value = true
  try {
    const payload = { title: form.title, summary: form.summary, content: form.content, tags: form.tags, status: 'DRAFT' }
    if (isEdit.value) await updateArticle(route.params.id, payload)
    else {
      const res = await createArticle(payload)
      router.replace(`/articles/${res.data?.data?.id}/edit`)
    }
    ElMessage.success('草稿已保存')
  } catch (e) { console.error('Save draft failed:', e) }
  finally { savingDraft.value = false }
}

const handlePublish = async () => {
  if (!form.title.trim()) { ElMessage.warning('请输入文章标题'); return }
  publishing.value = true
  try {
    let articleId = route.params.id
    if (!isEdit.value) {
      const res = await createArticle({ title: form.title, summary: form.summary, content: form.content, tags: form.tags, status: 'DRAFT' })
      articleId = res.data?.data?.id
    }
    if (selectedPlatforms.value.length > 0) {
      await publishArticle(articleId, selectedPlatforms.value, form.scheduled ? dayjs(form.scheduledAt).valueOf() : null)
      ElMessage.success('发布成功')
      router.push('/publish')
    } else {
      await updateArticle(articleId, { title: form.title, summary: form.summary, content: form.content, tags: form.tags, status: 'PUBLISHED' })
      ElMessage.success('已发布')
      router.push('/articles')
    }
  } catch (e) { console.error('Publish failed:', e) }
  finally { publishing.value = false }
}

onMounted(() => { loadArticle() })
onBeforeUnmount(() => { editor.value?.destroy() })
</script>

<style lang="scss" scoped>
.article-edit-page { animation: fadeIn 0.2s ease; }
@keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }

.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 32px; }
.header-left { display: flex; align-items: center; gap: 16px; }
.header-right { display: flex; gap: 8px; }
.page-title { font-size: 16px; font-weight: 500; color: var(--text-primary); }

.editor-layout { display: grid; grid-template-columns: 1fr 280px; gap: 32px; align-items: start; }
.editor-main {}

.title-input { margin-bottom: 24px; }
:deep(.el-input__inner) { font-size: 24px; font-weight: 500; border: none; padding-left: 0; &::placeholder { color: var(--text-placeholder); } }
:deep(.el-input__wrapper) { box-shadow: none !important; }

.editor-toolbar { display: flex; align-items: center; gap: 2px; padding: 0 0 12px; border-bottom: 1px solid var(--border-color); margin-bottom: 12px; }
.toolbar-btn { display: flex; align-items: center; justify-content: center; width: 32px; height: 32px; border: none; background: transparent; color: var(--text-secondary); cursor: pointer; transition: color var(--transition-fast); &:hover { color: var(--text-primary); } &.active { color: var(--text-primary); } &:disabled { opacity: 0.3; cursor: not-allowed; } }

.editor-content { min-height: 400px; }
:deep(.tiptap) { min-height: 400px; padding: 16px 0; font-size: 15px; line-height: 1.8; outline: none; p { margin: 0 0 1em; } h1, h2, h3 { font-weight: 600; margin: 1.5em 0 0.5em; color: var(--text-primary); } ul, ol { padding-left: 24px; } blockquote { border-left: 2px solid var(--border-color); padding-left: 16px; margin-left: 0; color: var(--text-secondary); } pre { background: var(--bg-hover); padding: 16px; overflow-x: auto; } code { background: var(--bg-hover); padding: 2px 6px; font-family: monospace; font-size: 13px; } a { color: var(--text-primary); text-decoration: underline; } img { max-width: 100%; height: auto; } .is-editor-empty:first-child::before { content: attr(data-placeholder); float: left; color: var(--text-placeholder); pointer-events: none; height: 0; } }

.editor-sidebar { display: flex; flex-direction: column; gap: 24px; }
.sidebar-card { background: transparent; padding: 0; }
.card-title { font-size: 12px; font-weight: 500; color: var(--text-muted); margin-bottom: 12px; text-transform: uppercase; letter-spacing: 0.06em; }

.platform-list { border: 1px solid var(--border-color); margin-top: 8px; }
.platform-item { display: flex; align-items: center; gap: 8px; padding: 8px 10px; cursor: pointer; transition: background-color var(--transition-fast); color: var(--text-secondary); font-size: 13px; border-bottom: 1px solid var(--border-light); &:last-child { border-bottom: none; } &:hover { background: var(--bg-hover); } &.selected { color: var(--text-primary); } span { flex: 1; } .check-icon { color: var(--text-primary); } }

.setting-item { margin-top: 12px; }
.setting-label { font-size: 12px; color: var(--text-muted); display: block; margin-bottom: 6px; }
.tags-preview { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 8px; }

@media (max-width: 1024px) {
  .editor-layout { grid-template-columns: 1fr; }
  .editor-sidebar { order: -1; flex-direction: row; flex-wrap: wrap; > * { flex: 1; min-width: 240px; } }
}
</style>
