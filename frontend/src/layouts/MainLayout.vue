<template>
  <div class="main-layout">
    <header class="header">
      <div class="header-left">
        <div class="logo" @click="$router.push('/dashboard')">ContentHub</div>
      </div>
      <nav class="header-center">
        <router-link
          v-for="item in navItems"
          :key="item.path"
          :to="item.path"
          class="nav-link"
          :class="{ 'nav-link--active': isActive(item.path) }"
        >
          {{ item.label }}
        </router-link>
      </nav>
      <div class="header-right">
        <button class="create-btn" @click="$router.push('/articles/create')">
          <PlusIcon :size="14" />
          <span>新建文章</span>
        </button>
        <el-dropdown @command="handleUserCommand" trigger="click">
          <div class="user-info">
            <el-avatar v-if="userStore.userInfo?.avatar" :size="28" :src="userStore.userInfo?.avatar" />
            <span class="username">{{ userStore.userInfo?.username }}</span>
            <ChevronDownIcon :size="14" />
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">个人信息</el-dropdown-item>
              <el-dropdown-item command="settings">系统设置</el-dropdown-item>
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>
    <main class="content">
      <div class="content-wrapper">
        <router-view v-slot="{ Component }">
          <keep-alive :include="['Dashboard', 'Articles', 'Publish', 'Stats']">
            <transition name="fade" mode="out-in">
              <component :is="Component" />
            </transition>
          </keep-alive>
        </router-view>
      </div>
    </main>
  </div>
</template>

<script setup>
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessageBox, ElMessage } from 'element-plus'
import { ChevronDownIcon, PlusIcon } from 'lucide-vue-next'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const navItems = [
  { path: '/dashboard', label: '仪表盘' },
  { path: '/articles', label: '内容管理' },
  { path: '/platforms', label: '平台管理' },
  { path: '/publish', label: '发布管理' },
  { path: '/stats', label: '数据统计' }
]

const isActive = (path) => {
  return route.path === path || route.path.startsWith(path + '/')
}

const handleUserCommand = (command) => {
  switch (command) {
    case 'logout':
      ElMessageBox.confirm('确定要退出登录吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'info'
      }).then(() => {
        userStore.logout()
        router.push('/login')
        ElMessage.success('已退出登录')
      }).catch(() => {})
      break
    case 'profile':
      router.push('/profile')
      break
    case 'settings':
      router.push('/settings')
      break
  }
}
</script>

<style lang="scss" scoped>
.main-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.header {
  height: 56px;
  background: var(--bg-header);
  border-bottom: 1px solid var(--border-color);
  display: flex;
  align-items: center;
  padding: 0 32px;
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
}

.header-left {
  flex: 0 0 auto;
}

.logo {
  font-size: 15px;
  font-weight: 500;
  color: var(--text-primary);
  cursor: pointer;
  transition: color var(--transition-fast);
  letter-spacing: -0.01em;

  &:hover {
    color: var(--text-secondary);
  }
}

.header-center {
  flex: 1;
  display: flex;
  justify-content: center;
  gap: 40px;
}

.nav-link {
  font-size: 13px;
  font-weight: 400;
  color: var(--text-muted);
  text-decoration: none;
  padding: 18px 0;
  border-bottom: 1px solid transparent;
  transition: color var(--transition-fast), border-color var(--transition-fast);

  &:hover {
    color: var(--text-primary);
  }

  &--active {
    color: var(--text-primary);
    border-bottom-color: var(--text-primary);
  }
}

.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.create-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border: 1px solid var(--border-color);
  background: transparent;
  color: var(--text-primary);
  font-size: 13px;
  cursor: pointer;
  transition: border-color var(--transition-fast);

  &:hover {
    border-color: var(--text-primary);
  }
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  transition: color var(--transition-fast);

  .username {
    font-size: 13px;
    font-weight: 400;
    color: var(--text-primary);
  }
}

.content {
  flex: 1;
  margin-top: 56px;
  padding: 48px 0;
}

.content-wrapper {
  max-width: 960px;
  margin: 0 auto;
  padding: 0 24px;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity var(--transition-fast);
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

:deep(.el-dropdown-menu) {
  border: 1px solid var(--border-color);
  border-radius: 0;
  padding: 4px 0;

  .el-dropdown-menu__item {
    padding: 8px 16px;
    font-size: 13px;
    border-radius: 0;

    &:hover {
      background: var(--bg-hover);
      color: var(--text-primary);
    }
  }
}
</style>
