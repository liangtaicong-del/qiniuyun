<template>
  <div class="not-found">
    <div class="content">
      <div class="code">404</div>
      <div class="message">页面不存在</div>
      <el-button type="primary" @click="$router.push('/dashboard')">返回首页</el-button>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.not-found {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
}
.content {
  text-align: center;
}
.code {
  font-size: 64px;
  font-weight: 500;
  color: var(--text-primary);
  letter-spacing: -0.04em;
  line-height: 1;
  margin-bottom: 8px;
}
.message {
  font-size: 14px;
  color: var(--text-muted);
  margin-bottom: 24px;
}
</style>
