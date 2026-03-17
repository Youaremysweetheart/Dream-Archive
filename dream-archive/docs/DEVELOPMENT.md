# 梦境档案馆系统 - 开发指南

## 📝 项目概述

这是一个完整的全栈Web应用，包含后端API服务和前端Vue应用。

## 🎯 已实现的核心功能

### 后端 (Spring Boot 3.5.9)

#### 1. 数据库设计 ✅
- **用户表 (user)**: 存储用户信息
- **梦境表 (dream)**: 存储梦境记录
- **梦境分类表 (dream_category)**: 8种预设分类
- **评论表 (comment)**: 支持多级评论
- **点赞记录表 (like_record)**: 记录点赞
- **心理问卷表 (questionnaire)**: 扩展功能
- **系统配置表 (system_config)**: 系统设置

#### 2. 核心组件 ✅
- **实体类 (Entity)**: User, Dream, DreamCategory, Comment
- **Mapper层**: 使用MyBatis注解式开发
- **Service层**: 业务逻辑实现
- **Controller层**: RESTful API接口
- **工具类**: JWT认证工具
- **安全配置**: Spring Security + JWT

#### 3. API接口 ✅
- 用户注册/登录
- 梦境CRUD操作
- 评论系统
- 点赞功能
- 分类查询

### 前端 (Vue 3 + Element Plus)

#### 1. 核心组件 ✅
- **App.vue**: 主应用组件
- **Header.vue**: 导航栏
- **Footer.vue**: 页脚（需补充）
- **DreamCard.vue**: 梦境卡片组件

#### 2. 页面组件 ✅
- **Home.vue**: 首页
- **Login.vue**: 登录页（需补充）
- **Register.vue**: 注册页（需补充）
- **DreamCreate.vue**: 创建梦境（需补充）
- **DreamDetail.vue**: 梦境详情（需补充）
- **Profile.vue**: 个人主页（需补充）
- **Explore.vue**: 探索页面（需补充）

#### 3. 核心功能 ✅
- Vue Router路由管理
- Pinia状态管理
- Axios HTTP请求
- Element Plus UI组件

## 🚧 需要补充的组件

为了让项目完全可运行，还需要创建以下组件：

### 前端页面组件

1. **Login.vue** - 登录页面
2. **Register.vue** - 注册页面
3. **DreamCreate.vue** - 创建/编辑梦境
4. **DreamDetail.vue** - 梦境详情页
5. **Profile.vue** - 个人主页
6. **Explore.vue** - 探索页面
7. **Footer.vue** - 页脚组件

### 后端组件

1. **CommentService** - 评论服务接口及实现
2. **CategoryController** - 分类控制器
3. **CommentController** - 评论控制器
4. **DreamCategoryService** - 分类服务

## 📚 开发规范

### 代码规范

#### 后端规范
```java
// 1. 类命名使用大驼峰
public class UserService {}

// 2. 方法命名使用小驼峰
public User getUserById(Long id) {}

// 3. 常量使用全大写下划线
public static final String DEFAULT_AVATAR = "/default.png";

// 4. 注释要清晰
/**
 * 获取用户信息
 * @param id 用户ID
 * @return User对象
 */
```

#### 前端规范
```javascript
// 1. 组件命名使用大驼峰
export default {
  name: 'UserProfile'
}

// 2. 变量命名使用小驼峰
const userName = 'test'

// 3. 常量使用全大写下划线
const API_BASE_URL = '/api'

// 4. Vue3 Composition API推荐使用setup语法
<script setup>
import { ref, onMounted } from 'vue'
</script>
```

### Git提交规范

```bash
# 功能开发
git commit -m "feat: 添加用户登录功能"

# Bug修复
git commit -m "fix: 修复梦境列表加载问题"

# 文档更新
git commit -m "docs: 更新README文档"

# 样式调整
git commit -m "style: 优化首页布局"

# 代码重构
git commit -m "refactor: 重构用户服务层"

# 性能优化
git commit -m "perf: 优化数据库查询性能"

# 测试相关
git commit -m "test: 添加用户服务单元测试"
```

## 🔧 常用开发命令

### 后端开发

```bash
# 编译项目
mvn compile

# 运行测试
mvn test

# 打包项目
mvn package

# 清理并打包
mvn clean package

# 跳过测试打包
mvn clean package -DskipTests

# 启动应用
mvn spring-boot:run

# 查看依赖树
mvn dependency:tree
```

### 前端开发

```bash
# 安装依赖
npm install

# 启动开发服务器
npm run dev

# 构建生产版本
npm run build

# 预览构建结果
npm run preview

# 检查代码规范
npm run lint

# 自动修复代码规范
npm run lint:fix
```

## 🐛 调试技巧

### 后端调试

1. **使用日志**
```java
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class UserService {
    public User getUser(Long id) {
        log.debug("获取用户信息, id: {}", id);
        log.info("用户查询成功");
        log.error("用户查询失败", e);
    }
}
```

2. **使用断点调试**
   - IDEA中点击行号左侧设置断点
   - Debug模式启动应用
   - 观察变量值和执行流程

### 前端调试

1. **使用Vue DevTools**
   - 安装浏览器扩展
   - 查看组件状态
   - 追踪事件流

2. **使用console**
```javascript
console.log('普通日志')
console.warn('警告信息')
console.error('错误信息')
console.table(data) // 表格形式展示
console.time('计时器') // 性能测试
console.timeEnd('计时器')
```

## 📊 数据库操作

### 常用SQL

```sql
-- 查看所有表
SHOW TABLES;

-- 查看表结构
DESC user;

-- 查询最近的梦境
SELECT * FROM dream ORDER BY create_time DESC LIMIT 10;

-- 查询用户的梦境数量
SELECT user_id, COUNT(*) as count 
FROM dream 
GROUP BY user_id 
ORDER BY count DESC;

-- 查询热门梦境
SELECT * FROM dream 
WHERE is_public = 1 
ORDER BY like_count DESC, view_count DESC 
LIMIT 10;

-- 清空测试数据（谨慎使用）
TRUNCATE TABLE dream;
TRUNCATE TABLE comment;
TRUNCATE TABLE like_record;
```

## 🔐 安全注意事项

1. **密码存储**: 使用BCrypt加密
2. **SQL注入**: 使用MyBatis参数化查询
3. **XSS攻击**: 前端输入验证和转义
4. **CSRF攻击**: 使用JWT而不是Session
5. **文件上传**: 验证文件类型和大小

## 🚀 性能优化建议

### 后端优化

1. **数据库优化**
   - 添加合适的索引
   - 使用分页查询
   - 避免N+1查询

2. **缓存策略**
   - 使用Redis缓存热点数据
   - 配置HTTP缓存头

3. **异步处理**
   - 使用@Async处理耗时操作
   - 消息队列处理非核心业务

### 前端优化

1. **代码分割**
   - 路由懒加载
   - 组件按需导入

2. **资源优化**
   - 图片压缩和懒加载
   - 使用CDN加速

3. **请求优化**
   - 接口合并
   - 请求防抖和节流

## 📖 学习资源

### 后端
- [Spring Boot官方文档](https://spring.io/projects/spring-boot)
- [MyBatis官方文档](https://mybatis.org/mybatis-3/zh/)
- [MySQL官方文档](https://dev.mysql.com/doc/)

### 前端
- [Vue 3官方文档](https://cn.vuejs.org/)
- [Element Plus文档](https://element-plus.org/zh-CN/)
- [Pinia文档](https://pinia.vuejs.org/zh/)

## 🤝 团队协作

### 分支管理

```bash
# 主分支
main - 生产环境代码
develop - 开发分支

# 功能分支
feature/user-system - 用户系统
feature/dream-module - 梦境模块
feature/comment-system - 评论系统

# 修复分支
hotfix/login-bug - 登录Bug修复
```

### 代码审查要点

1. 代码规范是否符合要求
2. 是否有明显的Bug
3. 是否有性能问题
4. 是否有安全隐患
5. 注释是否清晰
6. 测试是否充分

## 📞 技术支持

遇到问题可以通过以下方式获取帮助：

1. 查看项目文档
2. 搜索相关技术文档
3. 在GitHub提Issue
4. 联系项目负责人

---

最后更新: 2024年12月
