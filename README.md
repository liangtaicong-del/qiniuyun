# ContentHub

多平台内容发布管理工具，支持将文章一键同步发布到微信公众号、微博、知乎、简书、CSDN、掘金、百家号、头条号等平台。

## 技术栈

**后端**
- Spring Boot 3
- Spring Security + JWT 认证
- Spring Data JPA
- MySQL

**前端**
- Vue 3 + Vite
- Pinia 状态管理
- Element Plus UI
- Tiptap 富文本编辑器
- ECharts 数据可视化

## 功能特性

- 文章管理：创建、编辑、删除文章，支持富文本编辑
- 多平台发布：一篇文章同时发布到多个平台
- 定时发布：支持设置定时发布时间
- 平台管理：绑定/解绑各平台账号
- 数据统计：发布趋势图、各平台发布量统计
- 用户设置：发布偏好、通知设置、主题切换

## 快速开始

### 后端

```bash
cd backend
# 配置数据库连接 (src/main/resources/application.yml)
./mvnw spring-boot:run
```

后端启动地址：`http://localhost:8080`

### 前端

```bash
cd frontend
npm install
npm run dev
```

前端启动地址：`http://localhost:5173`

默认管理员账号：`admin / 123456`

## 项目结构

```
backend/
  src/main/java/com/contenthub/
    config/      # 安全配置、拦截器
    controller/  # REST 接口
    dto/         # 数据传输对象
    entity/      # 数据库实体
    repository/  # 数据访问层
    service/     # 业务逻辑层

frontend/
  src/
    api/         # API 接口封装
    layouts/     # 页面布局
    pages/       # 页面组件
    stores/      # Pinia 状态管理
    utils/       # 工具函数
```
