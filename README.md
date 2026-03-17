# 梦境档案馆系统

> 一个基于 Spring Boot 3.5.9 + Vue 3 的梦境记录与分享平台

## 项目简介

梦境档案馆是一个用于记录、管理与分享梦境内容的 Web 系统。 
用户可创建梦境记录、标注分类与情绪、上传图片、评论互动、梦境分析，并在个人主页查看统计数据。

## 技术栈

### 后端
- Spring Boot 3.5.9
- Spring Security
- MyBatis 3.0.3
- MySQL 8.0
- Maven
- PyTorch（模型训练/推理）
- Flask + Flask-CORS（API 服务）
- Transformers（BERT/中文 RoBERTa）
- scikit‑learn（指标计算：accuracy / macro‑F1）
- jieba（分词，仅用于 CNN/RNN）

### 前端
- Vue 3
- Element Plus
- Pinia
- Vue Router 4
- Axios
- Vite

## 核心功能
### 已实现
- 用户注册、登录、退出
- 用户资料查看与编辑（用户名、邮箱）
- 密码规则校验（长度与格式限制）
- 用户头像上传、显示与更新
- 梦境发布（标题、正文、标签、分类、情绪评分、做梦日期、公开/私密）
- 梦境编辑与删除
- 梦境图片上传与展示
- 梦境详情页完整展示（作者信息、发布时间、分类标签、内容、图片、标签）
- 梦境浏览量统计与显示
- 梦境点赞与取消点赞
- 点赞状态持久化（刷新/重进详情仍正确）
- 梦境评论发布
- 评论列表加载与展示
- 评论回复（楼中楼）
- 评论数量统计与显示
- 公开梦境列表（分页、筛选、搜索）
- 热门梦境列表
- 个人主页（概览、我的梦境列表、我的评论记录）
- 用户梦境数量统计
- 管理后台：用户管理（分页、搜索、排序）
- 管理后台：用户角色变更（USER/ADMIN）
- 管理后台：用户状态禁用/启用
- 管理后台：梦境管理（分页、搜索、批量删除）
- 账号禁用后禁止登录
- 后端统一返回结构（Result/PageResult）
- 图片访问静态资源映射
- 统一上传路径配置（支持绝对路径）
#### AI 相关功能
- Flask 统一提供梦境分析 API
- Spring Boot 通过 HTTP 调用 Flask 分析服务
- 新建梦境后自动分析并写回数据库
- 梦境更新后自动重新分析并写回数据库
- 手动“重新分析”接口
- 批量补分析接口（仅分析未分析记录）
- BERT 模型训练与推理支持（可通过 MODEL_TYPE=bert 切换）
- 模型“其他”回退阈值可通过环境变量调节
- “其他”类文案更积极、鼓励性反馈
#### 前端展示（分析模块）
- 梦境详情页展示分析卡片（分类/置信度/强度/反馈）
- “重新分析”按钮
- 如果分类为“其他”，弹窗询问睡眠与压力（1-5）
- 根据睡眠/压力分数生成鼓励文本并展示 
#### 数据库与数据
- dream 表扩展分析字段（label/name/confidence/intensity/feedback/updated_at）
- 初始化脚本修复与补充分析字段

### 管理端增强
- 用户状态禁用后禁止登录
- 用户列表支持按 `ID` 或 `用户名` 升降序排序

## 目录结构

```text
D:\111work\Projects\Dream-Archive-sys
├── README.md
├── dream-archive
│   ├── backend
│   │   ├── src\main\java\com\dreamarchive
│   │   │   ├── common
│   │   │   ├── config
│   │   │   ├── controller
│   │   │   ├── dto
│   │   │   ├── entity
│   │   │   ├── mapper
│   │   │   ├── service
│   │   │   └── utils
│   │   ├── src\main\resources
│   │   ├── init.sql
│   │   └── pom.xml
│   ├── frontend
│   │   ├── src
│   │   │   ├── api
│   │   │   ├── components
│   │   │   ├── router
│   │   │   ├── stores
│   │   │   ├── utils
│   │   │   └── views
│   │   ├── index.html
│   │   ├── package.json
│   │   └── vite.config.js
│   ├── uploads
│   │   ├── avatars
│   │   └── dreams
│   └── docs
├── dream-sentiment
│   ├── app.py
│   ├── predict.py
│   ├── train.py
│   ├── train_bert.py
│   ├── update_model.ps1
│   ├── requirements.txt
│   ├── data
│   ├── model
│   ├── utils
│   └── checkpoints
└── 梦境分类素材库800条.docx


```

## 关键接口（节选）

### 用户
- `POST /api/user/register`注册
- `POST /api/user/login`登录
- `GET /api/user/{id}`获取用户信息
- `PUT /api/user/profile`更新用户资料
- `POST /api/user/avatar`上传头像
- `POST /api/user/reset-password-dev`重置密码 
- 
### 梦境
- `POST /api/dream/create`发布梦境
- `GET /api/dream/{id}`获取梦境
- `PUT /api/dream/{id}`更新梦境
- `DELETE /api/dream/{id}`删除梦境
- `GET /api/dream/public`获取公开梦境列表
- `GET /api/dream/hot`获取热门梦境列表
- `POST /api/dream/{id}/like`点赞
- `POST /api/dream/{id}/analyze`手动重新分析
- `POST /api/dream/analyze/batch?limit=50`批量补分析
- `POST /api/dream/upload-image`上传梦境图片

### 评论
- `POST /api/comment/create`发表评论/回复
- `GET /api/comment/dream/{dreamId}`获取梦境评论
- `DELETE /api/comment/{id}`删除评论
- `GET /api/comment/user/{userId}`获取用户评论

### 分类（Category）
- `GET /api/category/list` 获取分类列表
- `GET /api/category/list/count` 获取分类及数量
- `GET /api/category/{id}` 获取分类详情
- `POST /api/category/create` 新增分类
- `PUT /api/category/{id}` 更新分类

### 管理后台（Admin）
- `GET /api/admin/stats` 统计信息
- `GET /api/admin/users` 用户列表
- `GET /api/admin/dreams` 梦境列表
- `DELETE /api/admin/user/{id}` 删除用户
- `DELETE /api/admin/dream/{id}` 删除梦境
- `PUT /api/admin/user/{id}/role` 修改角色
- `PUT /api/admin/user/{id}/status` 修改状态
- `POST /api/admin/dreams/batch-delete` 批量删除梦境

### 模型服务（Flask）
- `GET /health` 模型健康状态
- `POST /analyze` 梦境文本分析

### 管理
- `GET /api/admin/stats`管理员状态
- `GET /api/admin/users` 用户列表
- `PUT /api/admin/user/{id}/role` 修改角色
- `PUT /api/admin/user/{id}/status` 修改状态
- `DELETE /api/admin/user/{id}` 删除用户

## 最近更新
### 3月7日更新内容
- 修复前后端乱码/BOM引发的编译问题。
- 修复点赞逻辑：未点赞蓝色，已点赞粉色，可取消点赞。
- 修复点赞状态持久化：重新进入帖子后状态与数量一致。
- 修复禁用账号仍可登录问题：禁用用户登录被拦截。
- 管理员用户列表新增排序（ID/用户名，升序/降序）。
- 优化全局暗色主题下标签与按钮可读性，标签统一高对比蓝色。
- 修复了异常乱码问题

### 3月17日更新内容
- 将「梦境档案馆」与「梦境文本分析程序」进行整合，实现统一系统架构
- 基于 Flask 封装 PyTorch 模型推理接口，实现梦境文本的自动分析功能
- 在 Spring Boot 后端中通过 HTTP 调用模型服务，完成 AI 能力的服务化接入
- 前端新增梦境分析结果展示模块，支持用户查看分析结果
- 新增梦境情感分析联动：Spring Boot 自动调用 Flask `/analyze`
- 梦境详情页新增“梦境分析”卡片展示（分类/置信度/强度/安慰文案）
- 新增“重新分析”按钮 + 批量补分析接口
- Flask 支持 `MODEL_TYPE=bert` 切换 BERT 分析
- 优化前端分析展示样式

### 修复问题（Fix）
#### 3月17日修复内容
- 修复图片无法正常显示的问题（路径/资源加载异常）
- 优化前端资源加载逻辑，提高页面稳定性

### 当前状态
- 模型准确率仍有提升空间，后续计划通过增加训练数据与参数调优优化效果
- 推理性能尚未做专项优化，未来可考虑模型压缩或异步处理方案

### 项目亮点
- 实现 AI 模型与 Web 系统的解耦，通过接口形式进行调用
- 完成从“模型训练”到“业务系统落地”的完整链路实践

## License

仅用于学习与交流。
