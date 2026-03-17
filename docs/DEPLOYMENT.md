# 🌙 梦境档案馆系统 - 完整部署指南

## 📋 项目概述

梦境档案馆是一个基于 Spring Boot 3.5.9 + Vue 3 的全栈Web应用，用户可以匿名记录、分享和浏览梦境，支持梦境分类、评论互动、点赞等功能。

## 🛠️ 技术栈

### 后端
- **框架**: Spring Boot 3.5.9
- **数据库**: MySQL 8.0
- **ORM**: MyBatis 3.0.3
- **安全**: Spring Security + JWT
- **构建工具**: Maven

### 前端
- **框架**: Vue 3
- **UI组件**: Element Plus
- **状态管理**: Pinia
- **路由**: Vue Router 4
- **HTTP客户端**: Axios
- **构建工具**: Vite

## 📦 项目结构

```
dream-archive/
├── backend/                    # 后端项目
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/dreamarchive/
│   │   │   │   ├── config/           # 配置类
│   │   │   │   ├── controller/       # 控制器
│   │   │   │   ├── service/          # 服务层
│   │   │   │   ├── mapper/           # 数据访问层
│   │   │   │   ├── entity/           # 实体类
│   │   │   │   ├── common/           # 公共类
│   │   │   │   └── utils/            # 工具类
│   │   │   └── resources/
│   │   │       ├── mapper/           # MyBatis XML
│   │   │       └── application.yml   # 配置文件
│   │   └── test/                     # 测试代码
│   ├── pom.xml                       # Maven配置
│   └── init.sql                      # 数据库初始化脚本
│
├── frontend/                   # 前端项目
│   ├── src/
│   │   ├── api/                      # API接口
│   │   ├── components/               # 公共组件
│   │   ├── views/                    # 页面组件
│   │   ├── router/                   # 路由配置
│   │   ├── stores/                   # 状态管理
│   │   ├── utils/                    # 工具函数
│   │   ├── App.vue                   # 根组件
│   │   └── main.js                   # 入口文件
│   ├── package.json                  # 依赖配置
│   └── vite.config.js                # Vite配置
│
└── docs/                       # 文档目录
```

## 🚀 本地开发环境搭建

### 1. 环境要求

- **Java**: JDK 17 或更高版本
- **Maven**: 3.6 或更高版本
- **MySQL**: 8.0 或更高版本
- **Node.js**: 16.0 或更高版本
- **npm**: 8.0 或更高版本

### 2. 数据库初始化

```bash
# 登录MySQL
mysql -u root -p

# 执行初始化脚本
source /path/to/dream-archive/backend/init.sql

# 或者直接执行SQL
mysql -u root -p < /path/to/dream-archive/backend/init.sql
```

**默认管理员账号**：
- 用户名: `admin`
- 密码: `admin123`

### 3. 后端启动

```bash
# 进入后端目录
cd backend

# 修改配置文件 src/main/resources/application.yml
# 更新数据库连接信息：
#   - url: 数据库地址
#   - username: 数据库用户名
#   - password: 数据库密码

# 使用Maven启动
mvn spring-boot:run

# 或者打包后运行
mvn clean package
java -jar target/dream-archive-backend-1.0.0.jar
```

后端启动成功后访问: `http://localhost:8080/api`

### 4. 前端启动

```bash
# 进入前端目录
cd frontend

# 安装依赖
npm install

# 启动开发服务器
npm run dev
```

前端启动成功后访问: `http://localhost:5173`

## 🔧 配置说明

### 后端配置 (application.yml)

```yaml
# 数据库配置
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/dream_archive
    username: root
    password: your_password

# JWT配置
jwt:
  secret: dreamArchiveSecretKey2024ForJWTTokenGeneration
  expiration: 604800000  # Token过期时间（毫秒）

# 文件上传配置
file:
  upload-path: /data/dream-archive/uploads/  # 文件存储路径
```

### 前端配置 (vite.config.js)

```javascript
server: {
  port: 5173,
  proxy: {
    '/api': {
      target: 'http://localhost:8080',  # 后端API地址
      changeOrigin: true
    }
  }
}
```

## 📱 主要功能模块

### 1. 用户模块
- ✅ 用户注册（匿名昵称）
- ✅ 用户登录/登出
- ✅ 个人信息管理
- ✅ 密码修改
- ✅ 个人主页展示

### 2. 梦境记录模块
- ✅ 创建梦境记录
- ✅ 梦境分类（快乐梦、恐怖梦、焦虑梦等）
- ✅ 梦境编辑/删除
- ✅ 公开/私密设置
- ✅ 情绪评分
- ✅ 标签标记
- ✅ 图片上传

### 3. 梦境社区模块
- ✅ 浏览公开梦境列表
- ✅ 梦境详情查看
- ✅ 按分类筛选
- ✅ 关键词搜索
- ✅ 热门梦境推荐
- ✅ 点赞功能

### 4. 评论互动模块
- ✅ 发表评论
- ✅ 回复评论
- ✅ 评论点赞
- ✅ 评论删除
- ✅ 解梦互动

### 5. 后台管理模块
- ⏳ 用户管理
- ⏳ 梦境审核
- ⏳ 评论管理
- ⏳ 分类管理
- ⏳ 数据统计

### 6. 扩展功能（规划中）
- ⏳ 心理问卷评测
- ⏳ 心理疏导文章
- ⏳ AI梦境分析
- ⏳ 情绪管理工具

## 🌐 阿里云服务器部署

### 1. 服务器环境准备

```bash
# 更新系统
sudo apt update && sudo apt upgrade -y

# 安装Java 17
sudo apt install openjdk-17-jdk -y

# 安装MySQL 8.0
sudo apt install mysql-server -y

# 安装Nginx
sudo apt install nginx -y

# 安装Node.js (用于前端构建)
curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
sudo apt install nodejs -y
```

### 2. 数据库部署

```bash
# 登录MySQL
sudo mysql -u root

# 创建数据库用户
CREATE USER 'dreamarchive'@'localhost' IDENTIFIED BY 'your_strong_password';
GRANT ALL PRIVILEGES ON dream_archive.* TO 'dreamarchive'@'localhost';
FLUSH PRIVILEGES;

# 导入数据库
mysql -u dreamarchive -p dream_archive < init.sql
```

### 3. 后端部署

```bash
# 上传后端代码到服务器
scp -r backend/ user@your-server-ip:/home/user/

# SSH登录服务器
ssh user@your-server-ip

# 进入后端目录
cd /home/user/backend

# 修改配置文件
vim src/main/resources/application.yml
# 更新数据库连接信息和文件上传路径

# 打包
mvn clean package -DskipTests

# 创建systemd服务
sudo vim /etc/systemd/system/dream-archive.service
```

**systemd服务配置**:
```ini
[Unit]
Description=Dream Archive Backend Service
After=syslog.target network.target

[Service]
Type=simple
User=ubuntu
WorkingDirectory=/home/user/backend
ExecStart=/usr/bin/java -jar /home/user/backend/target/dream-archive-backend-1.0.0.jar
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

```bash
# 启动服务
sudo systemctl daemon-reload
sudo systemctl start dream-archive
sudo systemctl enable dream-archive

# 查看状态
sudo systemctl status dream-archive
```

### 4. 前端部署

```bash
# 本地构建前端
cd frontend
npm install
npm run build

# 上传构建产物到服务器
scp -r dist/ user@your-server-ip:/var/www/dream-archive/

# 配置Nginx
sudo vim /etc/nginx/sites-available/dream-archive
```

**Nginx配置**:
```nginx
server {
    listen 80;
    server_name your-domain.com;

    root /var/www/dream-archive;
    index index.html;

    # 前端路由
    location / {
        try_files $uri $uri/ /index.html;
    }

    # API代理
    location /api/ {
        proxy_pass http://localhost:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # 静态文件
    location /uploads/ {
        alias /data/dream-archive/uploads/;
        expires 7d;
    }
}
```

```bash
# 启用站点
sudo ln -s /etc/nginx/sites-available/dream-archive /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl restart nginx
```

### 5. SSL证书配置（可选）

```bash
# 安装Certbot
sudo apt install certbot python3-certbot-nginx -y

# 获取证书
sudo certbot --nginx -d your-domain.com

# 自动续期
sudo certbot renew --dry-run
```

## 🔒 安全加固

### 1. 数据库安全
```bash
# 运行安全脚本
sudo mysql_secure_installation

# 禁止远程root登录
# 删除匿名用户
# 删除测试数据库
```

### 2. 防火墙配置
```bash
# 启用UFW
sudo ufw enable

# 允许SSH
sudo ufw allow 22/tcp

# 允许HTTP/HTTPS
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp

# 查看状态
sudo ufw status
```

### 3. 应用安全
- 修改JWT密钥（`jwt.secret`）
- 设置强密码策略
- 启用HTTPS
- 定期更新依赖
- 配置日志监控

## 📊 性能优化

### 1. 数据库优化
```sql
-- 添加索引
CREATE INDEX idx_dream_create_time ON dream(create_time);
CREATE INDEX idx_dream_category ON dream(category_id, is_public);

-- 定期清理日志
PURGE MASTER LOGS BEFORE DATE_SUB(NOW(), INTERVAL 7 DAY);
```

### 2. 应用优化
```yaml
# application.yml
spring:
  # 数据库连接池
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
```

### 3. Nginx优化
```nginx
# 启用Gzip压缩
gzip on;
gzip_types text/plain text/css application/json application/javascript;

# 启用缓存
location ~* \.(jpg|jpeg|png|gif|ico|css|js)$ {
    expires 7d;
}
```

## 🐛 常见问题

### 1. 数据库连接失败
- 检查MySQL服务是否启动: `sudo systemctl status mysql`
- 验证数据库用户权限
- 确认防火墙端口开放

### 2. 跨域问题
- 检查SecurityConfig中CORS配置
- 确认前端代理配置正确

### 3. 文件上传失败
- 检查上传目录权限: `chmod 755 /data/dream-archive/uploads`
- 验证文件大小限制配置

### 4. 前端页面空白
- 清除浏览器缓存
- 检查浏览器控制台错误
- 验证API接口连接

## 📝 API文档

### 用户接口
```
POST   /api/user/register     # 用户注册
POST   /api/user/login        # 用户登录
GET    /api/user/info         # 获取当前用户信息
GET    /api/user/{id}         # 获取用户信息
PUT    /api/user/update       # 更新用户信息
PUT    /api/user/password     # 修改密码
```

### 梦境接口
```
POST   /api/dream/create      # 创建梦境
GET    /api/dream/{id}        # 获取梦境详情
PUT    /api/dream/update      # 更新梦境
DELETE /api/dream/{id}        # 删除梦境
GET    /api/dream/list        # 获取梦境列表（分页）
GET    /api/dream/user/{id}   # 获取用户梦境列表
GET    /api/dream/hot         # 获取热门梦境
POST   /api/dream/{id}/like   # 点赞/取消点赞
```

### 分类接口
```
GET    /api/category/list     # 获取所有分类
GET    /api/category/list/count  # 获取分类（含数量）
```

### 评论接口
```
POST   /api/comment/create    # 创建评论
GET    /api/comment/dream/{id}  # 获取梦境评论
DELETE /api/comment/{id}      # 删除评论
POST   /api/comment/{id}/like  # 点赞评论
```

## 📞 联系方式

- 开发者: 张怀民
- 项目GitHub: (待添加)
- 问题反馈: (待添加)

## 📄 许可证

本项目采用 MIT 许可证
