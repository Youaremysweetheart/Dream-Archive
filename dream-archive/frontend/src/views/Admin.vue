<template>
  <div class="admin-dashboard">
    <el-alert
      v-if="!userStore.isAdmin"
      title="你没有管理员权限"
      type="error"
      :closable="false"
      style="margin-bottom: 16px"
    />

    <template v-else>
      <div class="stats-grid">
        <el-card class="stat-card">
          <div class="stat-content">
            <el-icon :size="36" color="#2563eb"><User /></el-icon>
            <div>
              <div class="stat-value">{{ stats.totalUsers }}</div>
              <div class="stat-label">用户总数（今日 +{{ stats.todayUsers }}）</div>
            </div>
          </div>
        </el-card>

        <el-card class="stat-card">
          <div class="stat-content">
            <el-icon :size="36" color="#16a34a"><Document /></el-icon>
            <div>
              <div class="stat-value">{{ stats.totalDreams }}</div>
              <div class="stat-label">梦境总数（今日 +{{ stats.todayDreams }}）</div>
            </div>
          </div>
        </el-card>

        <el-card class="stat-card">
          <div class="stat-content">
            <el-icon :size="36" color="#d97706"><ChatDotRound /></el-icon>
            <div>
              <div class="stat-value">{{ stats.totalComments }}</div>
              <div class="stat-label">评论总数（今日 +{{ stats.todayComments }}）</div>
            </div>
          </div>
        </el-card>

        <el-card class="stat-card">
          <div class="stat-content">
            <el-icon :size="36" color="#dc2626"><DataAnalysis /></el-icon>
            <div>
              <div class="stat-value">{{ stats.adminUsers }} / {{ stats.normalUsers }}</div>
              <div class="stat-label">管理员 / 普通用户</div>
            </div>
          </div>
        </el-card>
      </div>

      <el-tabs v-model="activeTab" class="admin-tabs">
        <el-tab-pane label="用户管理" name="users">
          <div class="toolbar">
            <el-input v-model="userFilter.keyword" placeholder="搜索用户名/邮箱" clearable style="width: 220px" />
            <el-select v-model="userFilter.role" placeholder="角色" clearable style="width: 120px">
              <el-option label="管理员" value="ADMIN" />
              <el-option label="普通用户" value="USER" />
            </el-select>
            <el-select v-model="userFilter.status" placeholder="状态" clearable style="width: 120px">
              <el-option label="启用" :value="1" />
              <el-option label="禁用" :value="0" />
            </el-select>
            <el-select v-model="userFilter.sortBy" placeholder="排序字段" style="width: 140px">
              <el-option label="按 ID" value="id" />
              <el-option label="按用户名" value="username" />
            </el-select>
            <el-select v-model="userFilter.sortOrder" placeholder="排序方式" style="width: 120px">
              <el-option label="升序" value="asc" />
              <el-option label="降序" value="desc" />
            </el-select>
            <el-button type="primary" @click="reloadUsers">查询</el-button>
          </div>

          <el-table :data="users" style="width: 100%">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="username" label="用户名" width="150" />
            <el-table-column prop="email" label="邮箱" min-width="200" />
            <el-table-column prop="role" label="角色" width="110">
              <template #default="{ row }">
                <el-tag :type="isAdminRole(row.role) ? 'danger' : 'primary'">
                  {{ isAdminRole(row.role) ? '管理员' : '普通用户' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="注册时间" width="180" />
            <el-table-column label="操作" min-width="260">
              <template #default="{ row }">
                <el-button v-if="row.id !== userStore.userId" size="small" @click="changeRole(row)">
                  {{ isAdminRole(row.role) ? '设为普通用户' : '设为管理员' }}
                </el-button>
                <el-button v-if="row.id !== userStore.userId" size="small" @click="toggleStatus(row)">
                  {{ row.status === 1 ? '禁用' : '启用' }}
                </el-button>
                <el-button v-if="row.id !== userStore.userId" size="small" type="danger" @click="deleteUser(row.id)">
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-pagination
            v-model:current-page="userPage.pageNum"
            v-model:page-size="userPage.pageSize"
            :total="userPage.total"
            layout="total, prev, pager, next"
            @current-change="fetchUsers"
            style="margin-top: 16px; justify-content: center"
          />
        </el-tab-pane>

        <el-tab-pane label="梦境管理" name="dreams">
          <div class="toolbar">
            <el-button type="danger" :disabled="selectedDreamIds.length === 0" @click="batchDeleteDreams">
              批量删除（{{ selectedDreamIds.length }}）
            </el-button>
          </div>

          <el-table :data="dreams" style="width: 100%" @selection-change="onDreamSelectionChange">
            <el-table-column type="selection" width="50" />
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="title" label="标题" min-width="220" />
            <el-table-column prop="username" label="作者" width="120" />
            <el-table-column prop="categoryName" label="分类" width="100" />
            <el-table-column prop="isPublic" label="可见性" width="100">
              <template #default="{ row }">
                <el-tag :type="row.isPublic === 1 ? 'success' : 'info'">{{ row.isPublic === 1 ? '公开' : '私密' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="viewCount" label="浏览" width="80" />
            <el-table-column prop="likeCount" label="点赞" width="80" />
            <el-table-column prop="commentCount" label="评论" width="80" />
            <el-table-column prop="createTime" label="创建时间" width="180" />
            <el-table-column label="操作" width="150">
              <template #default="{ row }">
                <el-button size="small" @click="viewDream(row.id)">查看</el-button>
                <el-button size="small" type="danger" @click="deleteDream(row.id)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-pagination
            v-model:current-page="dreamPage.pageNum"
            v-model:page-size="dreamPage.pageSize"
            :total="dreamPage.total"
            layout="total, prev, pager, next"
            @current-change="fetchDreams"
            style="margin-top: 16px; justify-content: center"
          />
        </el-tab-pane>
      </el-tabs>
    </template>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { User, Document, ChatDotRound, DataAnalysis } from '@element-plus/icons-vue'
import { adminApi } from '@/api'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const activeTab = ref('users')
const selectedDreamIds = ref([])

const stats = reactive({
  totalUsers: 0,
  totalDreams: 0,
  totalComments: 0,
  publicDreams: 0,
  privateDreams: 0,
  adminUsers: 0,
  normalUsers: 0,
  todayUsers: 0,
  todayDreams: 0,
  todayComments: 0
})

const users = ref([])
const dreams = ref([])

const userFilter = reactive({
  keyword: '',
  role: '',
  status: null,
  sortBy: 'id',
  sortOrder: 'asc'
})

const userPage = reactive({
  pageNum: 1,
  pageSize: 20,
  total: 0
})

const dreamPage = reactive({
  pageNum: 1,
  pageSize: 20,
  total: 0
})

const isAdminRole = (role) => String(role || '').toUpperCase() === 'ADMIN'

const fetchStats = async () => {
  const res = await adminApi.getStats()
  Object.assign(stats, res.data || {})
}

const fetchUsers = async () => {
  const res = await adminApi.getUsers({
    pageNum: userPage.pageNum,
    pageSize: userPage.pageSize,
    keyword: userFilter.keyword || undefined,
    role: userFilter.role || undefined,
    status: userFilter.status,
    sortBy: userFilter.sortBy,
    sortOrder: userFilter.sortOrder
  })

  users.value = res.data?.records || []
  userPage.total = res.data?.total || 0
}

const fetchDreams = async () => {
  const res = await adminApi.getDreams({
    pageNum: dreamPage.pageNum,
    pageSize: dreamPage.pageSize
  })

  dreams.value = res.data?.records || []
  dreamPage.total = res.data?.total || 0
}

const reloadUsers = async () => {
  userPage.pageNum = 1
  await fetchUsers()
}

const changeRole = async (user) => {
  const newRole = isAdminRole(user.role) ? 'USER' : 'ADMIN'
  const actionText = newRole === 'ADMIN' ? '设为管理员' : '设为普通用户'

  await ElMessageBox.confirm(`确认将用户 ${user.username} ${actionText}？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })

  await adminApi.updateUserRole(user.id, newRole)
  ElMessage.success('角色更新成功')
  await fetchUsers()
  await fetchStats()
}

const toggleStatus = async (user) => {
  const nextStatus = user.status === 1 ? 0 : 1
  const actionText = nextStatus === 1 ? '启用' : '禁用'

  await ElMessageBox.confirm(`确认${actionText}用户 ${user.username}？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })

  await adminApi.updateUserStatus(user.id, nextStatus)
  ElMessage.success('状态更新成功')
  await fetchUsers()
}

const deleteUser = async (id) => {
  await ElMessageBox.confirm('确认删除该用户？此操作不可恢复。', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'error'
  })

  await adminApi.deleteUser(id)
  ElMessage.success('删除成功')
  await fetchUsers()
  await fetchStats()
}

const viewDream = (id) => {
  router.push(`/dream/${id}`)
}

const deleteDream = async (id) => {
  await ElMessageBox.confirm('确认删除该梦境？此操作不可恢复。', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'error'
  })

  await adminApi.deleteDream(id)
  ElMessage.success('删除成功')
  await fetchDreams()
  await fetchStats()
}

const onDreamSelectionChange = (rows) => {
  selectedDreamIds.value = rows.map((r) => r.id)
}

const batchDeleteDreams = async () => {
  await ElMessageBox.confirm(`确认批量删除 ${selectedDreamIds.value.length} 条梦境？`, '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'error'
  })

  await adminApi.batchDeleteDreams(selectedDreamIds.value)
  ElMessage.success('批量删除成功')
  selectedDreamIds.value = []
  await fetchDreams()
  await fetchStats()
}

onMounted(async () => {
  if (!userStore.isAdmin) return
  try {
    await Promise.all([fetchStats(), fetchUsers(), fetchDreams()])
  } catch (error) {
    ElMessage.error(error.message || '管理员数据加载失败')
  }
})
</script>

<style scoped>
.admin-dashboard {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 14px;
  margin-bottom: 20px;
}

.stat-card {
  border-radius: 12px;
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 14px;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #eef3ff;
}

.stat-label {
  color: #9fb2cd;
  font-size: 13px;
}

.admin-tabs {
  background: rgba(20, 27, 39, 0.82);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  padding: 12px;
}

.toolbar {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}
</style>
