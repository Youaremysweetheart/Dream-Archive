# 梦境档案馆系统

> 基于 Spring Boot 3.5.9 + Vue 3 的梦境记录与分享平台，整合情感分析服务（Flask + PyTorch）。

## 项目简介

梦境档案馆用于记录、管理与分享梦境内容，支持梦境发布、评论互动、点赞、分类标签与个人主页统计，并通过独立的情感分析服务为梦境文本生成分析结果。

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

### 情感分析服务
- Flask + Flask-CORS
- PyTorch / Transformers

## 目录结构

```text
docs/
dream-archive/
  backend/
  frontend/
  docs/
  uploads/
dream-sentiment/
  app.py
  predict.py
  train.py
  train_bert.py
```

## 快速开始

### 环境要求
- JDK 17+
- Maven 3.6+
- MySQL 8.0+
- Node.js 16+
- npm 8+
- Python 3.9+

### 1. 初始化数据库

```sql
CREATE DATABASE dream_archive CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

```bash
mysql -u root -p dream_archive < dream-archive/backend/init.sql
```

### 2. 启动后端

```bash
cd dream-archive/backend
mvn spring-boot:run
```

后端地址：`http://localhost:8080/api`

### 3. 启动前端

```bash
cd dream-archive/frontend
npm install
npm run dev
```

前端地址：`http://localhost:5173`

### 4. 启动情感分析服务

```bash
cd dream-sentiment
pip install -r requirements.txt
python app.py
```

默认服务地址：`http://localhost:5000`

## 配置说明

- 数据库账号密码支持环境变量覆盖：`DB_USERNAME`、`DB_PASSWORD`
- 上传路径使用相对目录：`./uploads/`
- 情感分析服务地址：`sentiment.base-url`（默认 `http://localhost:5000`）

## License

仅用于学习与交流。