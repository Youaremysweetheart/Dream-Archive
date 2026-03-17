<template>
  <header class="header">
    <div class="header-container">
      <div class="header-left">
        <div class="logo" @click="$router.push('/')">
          <span class="logo-icon">🌙</span>
          <span class="logo-text">梦境档案馆</span>
        </div>
      </div>

      <nav class="header-nav">
        <router-link to="/" class="nav-item">首页</router-link>
        <router-link to="/explore" class="nav-item">探索梦境</router-link>
      </nav>

      <div class="header-right">
        <template v-if="userStore.isLoggedIn">
          <el-button
            v-if="userStore.isAdmin"
            type="danger"
            size="small"
            @click="goToAdmin"
            class="admin-btn"
          >
            <el-icon><Setting /></el-icon>
            管理后台
          </el-button>

          <el-button type="primary" size="small" @click="$router.push('/dream/create')">
            <el-icon><EditPen /></el-icon>
            记录新梦境
          </el-button>

          <el-dropdown @command="handleCommand">
            <div class="user-info">
              <el-avatar :size="36" :src="userStore.user?.avatar || '/default-avatar.png'" />
              <span class="username">{{ userStore.user?.username }}</span>
              <el-icon class="el-icon--right"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon>
                  个人主页
                </el-dropdown-item>
                <el-dropdown-item command="my-dreams">
                  <el-icon><Document /></el-icon>
                  我的梦境
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>

        <template v-else>
          <el-button size="small" @click="$router.push('/login')">登录</el-button>
          <el-button type="primary" size="small" @click="$router.push('/register')">注册</el-button>
        </template>
      </div>
    </div>
  </header>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import { User, Document, Setting, SwitchButton, ArrowDown, EditPen } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()

const goToAdmin = () => {
  router.push('/admin')
}

const handleCommand = (command) => {
  switch (command) {
    case 'profile':
      if (userStore.userId) {
        router.push({
          path: `/profile/${userStore.userId}`,
          query: { tab: 'profile' }
        })
      }
      break
    case 'my-dreams':
      if (userStore.userId) {
        router.push({
          path: `/profile/${userStore.userId}`,
          query: { tab: 'dreams' }
        })
      }
      break
    case 'logout':
      handleLogout()
      break
    default:
      break
  }
}

const handleLogout = async () => {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    userStore.logout()
    ElMessage.success('已退出登录')
    router.push('/')
  } catch (error) {
    // cancel
  }
}
</script>

<style scoped>
.header {
  position: sticky;
  top: 0;
  z-index: 1000;
  background: rgba(10, 12, 18, 0.86);
  backdrop-filter: blur(10px);
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.45);
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.header-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 20px;
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-left {
  display: flex;
  align-items: center;
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  transition: transform 0.3s;
}

.logo:hover {
  transform: scale(1.05);
}

.logo-icon {
  font-size: 32px;
}

.logo-text {
  font-size: 20px;
  font-weight: bold;
  background: linear-gradient(90deg, #00e5ff 0%, #7c4dff 45%, #ff3d81 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.header-nav {
  display: flex;
  align-items: center;
  gap: 30px;
}

.nav-item {
  color: #d7deea;
  text-decoration: none;
  font-size: 16px;
  font-weight: 500;
  padding: 8px 16px;
  border-radius: 8px;
  transition: all 0.3s;
  position: relative;
}

.nav-item:hover {
  color: #fff;
  background: rgba(0, 229, 255, 0.12);
}

.nav-item.router-link-active {
  color: #ffffff;
  background: linear-gradient(90deg, rgba(0, 229, 255, 0.2), rgba(255, 61, 129, 0.2));
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.14);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.admin-btn {
  background: linear-gradient(135deg, #ff6a00 0%, #ff1744 100%);
  border: none;
  color: white;
  font-weight: 500;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 12px;
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.3s;
}

.user-info:hover {
  background: rgba(0, 229, 255, 0.1);
}

.username {
  font-size: 14px;
  font-weight: 500;
  color: #e6edf7;
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 768px) {
  .header-nav {
    display: none;
  }

  .logo-text {
    display: none;
  }

  .username {
    display: none;
  }
}

:deep(.el-dropdown-menu__item) {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
}

:deep(.el-dropdown-menu__item:hover) {
  background: rgba(0, 229, 255, 0.08);
  color: #00c8ff;
}
</style>
