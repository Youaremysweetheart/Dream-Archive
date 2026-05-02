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
              <div class="stat-value">{{ stats.bannedDreamRooms }} / {{ stats.totalDreamRooms }}</div>
              <div class="stat-label">封禁辅导室 / 辅导室总数</div>
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
                <el-tag :type="row.status === 1 ? 'success' : 'info'">
                  {{ row.status === 1 ? '启用' : '禁用' }}
                </el-tag>
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

          <el-table :data="dreams" class="dream-admin-table" style="width: 100%" @selection-change="onDreamSelectionChange">
            <el-table-column type="selection" width="50" />
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column label="梦境信息" min-width="320">
              <template #default="{ row }">
                <div class="meta-stack">
                  <div class="meta-primary ellipsis-one">{{ row.title || '-' }}</div>
                  <div class="meta-secondary">
                    {{ row.username || '-' }} · {{ row.categoryName || '未分类' }} · {{ row.isPublic === 1 ? '公开' : '私密' }}
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="互动数据" width="150">
              <template #default="{ row }">
                <div class="stats-stack">
                  <div>浏览 {{ row.viewCount ?? 0 }}</div>
                  <div>点赞 {{ row.likeCount ?? 0 }}</div>
                  <div>评论 {{ row.commentCount ?? 0 }}</div>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="创建时间" width="180" show-overflow-tooltip />
            <el-table-column label="操作" width="120" align="center">
              <template #default="{ row }">
                <div class="table-actions">
                  <el-button size="small" @click="viewDream(row.id)">查看</el-button>
                  <el-button size="small" type="danger" @click="deleteDream(row.id)">删除</el-button>
                </div>
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

        <el-tab-pane label="心理辅导室管理" name="dreamRooms">
          <div class="toolbar">
            <el-input v-model="dreamRoomFilter.keyword" placeholder="搜索房间号/用户名/邮箱/梦境标题" clearable style="width: 280px" />
            <el-select v-model="dreamRoomFilter.status" placeholder="房间状态" clearable style="width: 140px">
              <el-option label="异常" :value="0" />
              <el-option label="首次进入" :value="1" />
              <el-option label="正常聊天" :value="2" />
              <el-option label="已封禁" :value="3" />
            </el-select>
            <el-button type="primary" @click="reloadDreamRooms">查询</el-button>
          </div>

          <el-table :data="dreamRooms" class="dream-room-table" style="width: 100%">
            <el-table-column label="用户信息" min-width="180">
              <template #default="{ row }">
                <div class="meta-stack">
                  <div class="meta-primary ellipsis-one">{{ row.username || '-' }}</div>
                  <div class="meta-secondary ellipsis-one">{{ row.email || '-' }}</div>
                </div>
              </template>
            </el-table-column>

            <el-table-column label="房间与梦境" min-width="220">
              <template #default="{ row }">
                <div class="meta-stack">
                  <div class="meta-primary ellipsis-one">{{ row.dreamTitle || '-' }}</div>
                  <div class="meta-secondary ellipsis-one">房间号：{{ row.dreamRoomId || '-' }}</div>
                </div>
              </template>
            </el-table-column>

            <el-table-column label="状态" width="110">
              <template #default="{ row }">
                <div class="status-cell">
                  <el-tag :type="dreamRoomStatusType(row.dreamRoomStatus)">
                    {{ dreamRoomStatusText(row.dreamRoomStatus) }}
                  </el-tag>
                  <div class="status-subtext">消息 {{ row.messageCount ?? 0 }}</div>
                </div>
              </template>
            </el-table-column>

            <el-table-column prop="updateTime" label="最后更新时间" width="165" show-overflow-tooltip />

            <el-table-column label="操作" width="96" align="center">
              <template #default="{ row }">
                <div class="table-actions">
                  <el-button size="small" @click="openDreamRoomDetail(row)">详情</el-button>
                  <el-button
                    v-if="row.dreamRoomStatus !== 3"
                    size="small"
                    type="danger"
                    @click="openBanDialog(row)"
                  >
                    封禁
                  </el-button>
                  <el-button
                    v-else
                    size="small"
                    type="success"
                    @click="unbanDreamRoom(row)"
                  >
                    解禁
                  </el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>

          <el-pagination
            v-model:current-page="dreamRoomPage.pageNum"
            v-model:page-size="dreamRoomPage.pageSize"
            :total="dreamRoomPage.total"
            layout="total, prev, pager, next"
            @current-change="fetchDreamRooms"
            style="margin-top: 16px; justify-content: center"
          />
        </el-tab-pane>
      </el-tabs>

      <el-dialog v-model="banDialog.visible" title="手动封禁心理辅导室" width="480px">
        <div class="dialog-tip">
          当前房间：{{ banDialog.room?.dreamRoomId || '-' }}，用户：{{ banDialog.room?.username || '-' }}
        </div>
        <el-input
          v-model="banDialog.reason"
          type="textarea"
          :rows="4"
          maxlength="255"
          show-word-limit
          placeholder="请输入封禁原因，用户和管理员都可用于后续追溯"
        />
        <template #footer>
          <el-button @click="banDialog.visible = false">取消</el-button>
          <el-button type="danger" @click="submitBanDreamRoom">确认封禁</el-button>
        </template>
      </el-dialog>

      <el-dialog v-model="detailDialog.visible" title="心理辅导室详情" width="560px">
        <div v-if="detailDialog.room" class="detail-grid">
          <div class="detail-item">
            <div class="detail-label">所属用户</div>
            <div class="detail-value">{{ detailDialog.room.username || '-' }}</div>
          </div>
          <div class="detail-item">
            <div class="detail-label">用户邮箱</div>
            <div class="detail-value">{{ detailDialog.room.email || '-' }}</div>
          </div>
          <div class="detail-item detail-item-full">
            <div class="detail-label">关联梦境</div>
            <div class="detail-value">{{ detailDialog.room.dreamTitle || '-' }}</div>
          </div>
          <div class="detail-item detail-item-full">
            <div class="detail-label">房间号</div>
            <div class="detail-value">{{ detailDialog.room.dreamRoomId || '-' }}</div>
          </div>
          <div class="detail-item">
            <div class="detail-label">房间状态</div>
            <div class="detail-value">{{ dreamRoomStatusText(detailDialog.room.dreamRoomStatus) }}</div>
          </div>
          <div class="detail-item">
            <div class="detail-label">消息数</div>
            <div class="detail-value">{{ detailDialog.room.messageCount ?? 0 }}</div>
          </div>
          <div class="detail-item">
            <div class="detail-label">最后更新时间</div>
            <div class="detail-value">{{ detailDialog.room.updateTime || '-' }}</div>
          </div>
          <div class="detail-item detail-item-full">
            <div class="detail-label">封禁原因</div>
            <div class="detail-value detail-pre">{{ detailDialog.room.bannedReason || '暂无' }}</div>
          </div>
        </div>
        <template #footer>
          <el-button @click="detailDialog.visible = false">关闭</el-button>
        </template>
      </el-dialog>
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
  totalDreamRooms: 0,
  bannedDreamRooms: 0,
  todayUsers: 0,
  todayDreams: 0,
  todayComments: 0
})

const users = ref([])
const dreams = ref([])
const dreamRooms = ref([])

const userFilter = reactive({
  keyword: '',
  role: '',
  status: null,
  sortBy: 'id',
  sortOrder: 'asc'
})

const dreamRoomFilter = reactive({
  keyword: '',
  status: null
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

const dreamRoomPage = reactive({
  pageNum: 1,
  pageSize: 20,
  total: 0
})

const banDialog = reactive({
  visible: false,
  room: null,
  reason: ''
})

const detailDialog = reactive({
  visible: false,
  room: null
})

const isAdminRole = (role) => String(role || '').toUpperCase() === 'ADMIN'

const dreamRoomStatusText = (status) => {
  const map = {
    0: '异常',
    1: '首次进入',
    2: '正常聊天',
    3: '已封禁'
  }
  return map[status] || '未知'
}

const dreamRoomStatusType = (status) => {
  const map = {
    0: 'info',
    1: 'warning',
    2: 'success',
    3: 'danger'
  }
  return map[status] || 'info'
}

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

const fetchDreamRooms = async () => {
  const res = await adminApi.getDreamRooms({
    pageNum: dreamRoomPage.pageNum,
    pageSize: dreamRoomPage.pageSize,
    keyword: dreamRoomFilter.keyword || undefined,
    status: dreamRoomFilter.status
  })

  dreamRooms.value = res.data?.records || []
  dreamRoomPage.total = res.data?.total || 0
}

const reloadUsers = async () => {
  userPage.pageNum = 1
  await fetchUsers()
}

const reloadDreamRooms = async () => {
  dreamRoomPage.pageNum = 1
  await fetchDreamRooms()
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

const openDreamRoomDetail = (room) => {
  detailDialog.room = room
  detailDialog.visible = true
}

const openBanDialog = (room) => {
  banDialog.room = room
  banDialog.reason = room.bannedReason || ''
  banDialog.visible = true
}

const submitBanDreamRoom = async () => {
  if (!banDialog.room) return
  if (!banDialog.reason.trim()) {
    ElMessage.warning('请输入封禁原因')
    return
  }

  await adminApi.banDreamRoom(banDialog.room.dreamRoomId, banDialog.reason.trim())
  banDialog.visible = false
  ElMessage.success('心理辅导室已封禁')
  await fetchDreamRooms()
  await fetchStats()
}

const unbanDreamRoom = async (room) => {
  await ElMessageBox.confirm(
    `确认解禁心理辅导室 ${room.dreamRoomId}（用户：${room.username || '-'}）？`,
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  )

  await adminApi.unbanDreamRoom(room.dreamRoomId)
  ElMessage.success('心理辅导室已解禁')
  await fetchDreamRooms()
  await fetchStats()
}

onMounted(async () => {
  if (!userStore.isAdmin) return
  try {
    await Promise.all([fetchStats(), fetchUsers(), fetchDreams(), fetchDreamRooms()])
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

.dialog-tip {
  margin-bottom: 12px;
  color: #c7d3eb;
}

.dream-admin-table :deep(.el-table__cell),
.dream-room-table :deep(.el-table__cell) {
  vertical-align: middle;
}

.dream-room-table :deep(.el-table__body-wrapper),
.dream-room-table :deep(.el-scrollbar__wrap),
.dream-admin-table :deep(.el-table__body-wrapper),
.dream-admin-table :deep(.el-scrollbar__wrap) {
  overflow-x: hidden !important;
}

.meta-stack {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.meta-primary {
  color: #f5f8ff;
  font-weight: 600;
  line-height: 1.35;
  min-width: 0;
}

.meta-secondary {
  color: #8ea3c0;
  font-size: 12px;
  line-height: 1.35;
  min-width: 0;
}

.ellipsis-one {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.stats-stack {
  display: flex;
  flex-direction: column;
  gap: 6px;
  color: #d6e2f4;
  font-size: 13px;
  line-height: 1.25;
}

.status-cell {
  display: flex;
  flex-direction: column;
  gap: 6px;
  align-items: flex-start;
}

.status-subtext {
  color: #90a4c3;
  font-size: 12px;
  line-height: 1;
}

.table-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: stretch;
}

.table-actions :deep(.el-button) {
  margin-left: 0;
  padding-left: 8px;
  padding-right: 8px;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px 18px;
}

.detail-item {
  min-width: 0;
}

.detail-item-full {
  grid-column: 1 / -1;
}

.detail-label {
  margin-bottom: 6px;
  color: #8ea3c0;
  font-size: 12px;
}

.detail-value {
  color: #f3f7ff;
  line-height: 1.5;
  word-break: break-word;
}

.detail-pre {
  white-space: pre-wrap;
}

.admin-dashboard :deep(.el-button) {
  border-radius: 10px;
  font-weight: 500;
  transition: all 0.2s ease;
  box-shadow: none;
}

.admin-dashboard :deep(.el-button:not(.is-disabled)) {
  border-color: rgba(148, 163, 184, 0.28);
  background: rgba(148, 163, 184, 0.12);
  color: #dbe5f4;
}

.admin-dashboard :deep(.el-button:not(.is-disabled):hover) {
  border-color: rgba(148, 163, 184, 0.4);
  background: rgba(148, 163, 184, 0.2);
  color: #f4f7fb;
}

.admin-dashboard :deep(.el-button--primary:not(.is-disabled)) {
  border-color: rgba(96, 165, 250, 0.28);
  background: rgba(59, 130, 246, 0.16);
  color: #d8e9ff;
}

.admin-dashboard :deep(.el-button--primary:not(.is-disabled):hover) {
  border-color: rgba(96, 165, 250, 0.42);
  background: rgba(59, 130, 246, 0.24);
  color: #eff6ff;
}

.admin-dashboard :deep(.el-button--success:not(.is-disabled)) {
  border-color: rgba(74, 222, 128, 0.24);
  background: rgba(34, 197, 94, 0.14);
  color: #d7fbe4;
}

.admin-dashboard :deep(.el-button--success:not(.is-disabled):hover) {
  border-color: rgba(74, 222, 128, 0.36);
  background: rgba(34, 197, 94, 0.22);
  color: #effdf4;
}

.admin-dashboard :deep(.el-button--danger:not(.is-disabled)) {
  border-color: rgba(248, 113, 113, 0.24);
  background: rgba(239, 68, 68, 0.14);
  color: #ffe0e0;
}

.admin-dashboard :deep(.el-button--danger:not(.is-disabled):hover) {
  border-color: rgba(248, 113, 113, 0.36);
  background: rgba(239, 68, 68, 0.22);
  color: #fff1f1;
}

.admin-dashboard :deep(.el-button.is-disabled) {
  border-color: rgba(100, 116, 139, 0.18);
  background: rgba(100, 116, 139, 0.08);
  color: rgba(203, 213, 225, 0.45);
}

.admin-dashboard :deep(.el-tag) {
  border-radius: 8px;
  font-weight: 500;
}

.admin-dashboard :deep(.el-tag--primary) {
  background: rgba(59, 130, 246, 0.14);
  border-color: rgba(96, 165, 250, 0.22);
  color: #d9eaff;
}

.admin-dashboard :deep(.el-tag--success) {
  background: rgba(34, 197, 94, 0.14);
  border-color: rgba(74, 222, 128, 0.2);
  color: #d8f8e3;
}

.admin-dashboard :deep(.el-tag--danger) {
  background: rgba(239, 68, 68, 0.14);
  border-color: rgba(248, 113, 113, 0.2);
  color: #ffd9d9;
}

.admin-dashboard :deep(.el-tag--warning) {
  background: rgba(245, 158, 11, 0.14);
  border-color: rgba(251, 191, 36, 0.2);
  color: #ffe8bf;
}

.admin-dashboard :deep(.el-tag--info) {
  background: rgba(148, 163, 184, 0.12);
  border-color: rgba(148, 163, 184, 0.18);
  color: #d7deea;
}
</style>
