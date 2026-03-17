# 🌙 梦境档案馆系统

> 一个基于 Spring Boot 3.5.9 + Vue 3 的梦境记录与分享平台

## 项目简介

梦境档案馆是一个用于记录、管理与分享梦境内容的 Web 系统。  
用户可创建梦境记录、标注分类与情绪、上传图片、评论互动，并在个人主页查看统计数据。

## 技术栈

### 后端
- Spring Boot 3.5.9
- Spring Security
- MyBatis 3.0.3
- MySQL 8.0
- Maven

### 前端
- Vue 3
- Element Plus
- Pinia
- Vue Router 4
- Axios
- Vite

## 核心功能

### 已实现
- 用户注册 / 登录 / 个人资料维护
- 密码规则统一校验（6-20位）
- 头像上传与展示
- 梦境发布 / 编辑 / 删除
- 梦境分类、标签、公开/私密、情绪评分
- 梦境图片上传与展示
- 点赞、评论、回复
- 点赞状态持久化（刷新或重进详情后保持正确状态）
- 个人主页（概览、我的梦境、评论记录）
- 管理后台（用户管理、梦境管理、角色状态管理）

### 管理端增强
- 用户状态禁用后禁止登录
- 用户列表支持按 `ID` 或 `用户名` 升降序排序

## 目录结构

```text
dream-archive/
├── backend/
│   ├── src/main/java/com/dreamarchive/
│   │   ├── config/
│   │   ├── controller/
│   │   ├── service/
│   │   ├── mapper/
│   │   ├── entity/
│   │   └── utils/
│   ├── src/main/resources/
│   ├── init.sql
│   └── pom.xml
├── frontend/
│   ├── src/
│   │   ├── api/
│   │   ├── components/
│   │   ├── router/
│   │   ├── stores/
│   │   └── views/
│   ├── package.json
│   └── vite.config.js
└── docs/
```

## 快速开始

### 环境要求
- JDK 17+
- Maven 3.6+
- MySQL 8.0+
- Node.js 16+
- npm 8+

### 1. 克隆项目

```bash
git clone https://github.com/your-username/dream-archive.git
cd dream-archive
```

### 2. 初始化数据库

```sql
CREATE DATABASE dream_archive CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

```bash
mysql -u root -p dream_archive < backend/init.sql
```

### 3. 启动后端

```bash
cd backend
mvn spring-boot:run
```

后端地址：`http://localhost:8080/api`

### 4. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端地址：`http://localhost:5173`

## 关键接口（节选）

### 用户
- `POST /api/user/register`
- `POST /api/user/login`
- `GET /api/user/{id}`
- `PUT /api/user/profile`
- `POST /api/user/avatar`

### 梦境
- `POST /api/dream/create`
- `GET /api/dream/{id}`
- `PUT /api/dream/{id}`
- `DELETE /api/dream/{id}`
- `POST /api/dream/{id}/like`
- `POST /api/dream/upload-image`

### 评论
- `POST /api/comment/create`
- `GET /api/comment/dream/{id}`
- `DELETE /api/comment/{id}`

### 管理
- `GET /api/admin/stats`
- `GET /api/admin/users`
- `PUT /api/admin/user/{id}/role`
- `PUT /api/admin/user/{id}/status`
- `DELETE /api/admin/user/{id}`

## 最近更新（2026-03）

- 修复前后端乱码/BOM引发的编译问题。
- 修复点赞逻辑：未点赞蓝色，已点赞粉色，可取消点赞。
- 修复点赞状态持久化：重新进入帖子后状态与数量一致。
- 修复禁用账号仍可登录问题：禁用用户登录被拦截。
- 管理员用户列表新增排序（ID/用户名，升序/降序）。
- 优化全局暗色主题下标签与按钮可读性，标签统一高对比蓝色。
- 修复了异常乱码问题

## License

仅用于学习与交流。
