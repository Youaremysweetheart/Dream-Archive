<template>
  <div class="profile-container">
    <el-card class="profile-hero-card" shadow="hover">
      <div class="profile-hero">
        <div class="hero-left">
          <el-avatar :src="user?.avatar" :size="88">{{ user?.username }}</el-avatar>
          <div class="hero-meta">
            <div class="hero-name">{{ user?.username || '个人主页' }}</div>
            <div class="hero-subtitle">
              <span>{{ user?.email || '暂未填写邮箱' }}</span>
              <el-divider direction="vertical" />
              <span>加入于 {{ formatDate(user?.createTime) }}</span>
              <el-divider direction="vertical" />
              <span>已陪伴 {{ daysSinceJoin }} 天</span>
              <el-divider direction="vertical" />
              <span>已记录 {{ profileStats.totalDreams }} 个梦境</span>
            </div>
          </div>
        </div>

        <div class="hero-right">
          <el-radio-group v-model="activeTab" @change="handleTabChange">
            <el-radio-button label="profile">概览</el-radio-button>
            <el-radio-button label="dreams">我的梦境</el-radio-button>
          </el-radio-group>
          <el-button v-if="isOwner" type="primary" @click="showEditDialog = true">编辑资料</el-button>
          <el-button v-if="isOwner && activeTab === 'dreams'" type="success" @click="router.push('/dream/create')">记录新梦境</el-button>
        </div>
      </div>
    </el-card>

    <template v-if="activeTab === 'profile'">
      <div class="stats-grid">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-label">梦境总数</div>
          <div class="stat-value">{{ profileStats.totalDreams }}</div>
          <div class="stat-desc">公开 {{ profileStats.publicDreams }} / 私密 {{ profileStats.privateDreams }}</div>
        </el-card>

        <el-card class="stat-card" shadow="hover">
          <div class="stat-label">我的评论</div>
          <div class="stat-value">{{ profileStats.commentCount }}</div>
          <div class="stat-desc">最近 30 天 {{ profileStats.recentCommentCount }} 条</div>
        </el-card>

        <el-card class="stat-card" shadow="hover">
          <div class="stat-label">情绪均分</div>
          <div class="stat-value">{{ profileStats.avgMood }}</div>
          <div class="stat-desc">近 30 天 {{ profileStats.recentMoodAvg }}</div>
        </el-card>

        <el-card class="stat-card" shadow="hover">
          <div class="stat-label">常见标签</div>
          <div class="stat-tags" v-if="topTags.length">
            <el-tag v-for="tag in topTags" :key="tag.name" size="small" effect="plain">{{ tag.name }} ({{ tag.count }})</el-tag>
          </div>
          <div class="stat-desc" v-else>暂无标签数据</div>
        </el-card>
      </div>

      <div class="overview-grid">
        <el-card class="section-card" shadow="never">
          <template #header>
            <div class="section-title">梦境活跃度（近 6 个月）</div>
          </template>

          <div v-if="monthlyTrend.length" class="trend-list">
            <div v-for="item in monthlyTrend" :key="item.month" class="trend-row">
              <div class="trend-month">{{ item.month }}</div>
              <div class="trend-bar-wrap">
                <div class="trend-bar" :style="{ width: `${item.percent}%` }"></div>
              </div>
              <div class="trend-value">{{ item.count }}</div>
            </div>
          </div>
          <el-empty v-else description="暂无活跃度数据" />
        </el-card>

        <el-card class="section-card" shadow="never">
          <template #header>
            <div class="section-title">最近梦境</div>
          </template>

          <div v-if="recentDreams.length" class="timeline">
            <div v-for="dream in recentDreams" :key="dream.id" class="timeline-item" @click="router.push(`/dream/${dream.id}`)">
              <div class="timeline-main">
                <span class="timeline-title">{{ dream.title || '未命名梦境' }}</span>
                <el-tag size="small" :type="dream.isPublic === 1 ? 'success' : 'info'">{{ dream.isPublic === 1 ? '公开' : '私密' }}</el-tag>
              </div>
              <div class="timeline-sub">{{ formatDateTime(dream.createTime) }}</div>
            </div>
          </div>
          <el-empty v-else description="暂无梦境记录" />
        </el-card>

        <el-card class="section-card" shadow="never">
          <template #header>
            <div class="section-title">我的最新评论</div>
          </template>

          <div v-if="recentComments.length" class="comment-list">
            <div v-for="comment in recentComments" :key="comment.id" class="comment-item">
              <div class="comment-content">{{ comment.content }}</div>
              <div class="comment-time">{{ formatDateTime(comment.createTime) }}</div>
            </div>
          </div>
          <el-empty v-else description="暂无评论记录" />
        </el-card>
      </div>
    </template>

    <el-card v-else class="dream-list-card" shadow="never">
      <div class="filter-tabs">
        <el-radio-group v-model="filterType" @change="loadDreams">
          <el-radio-button label="all">全部</el-radio-button>
          <el-radio-button label="public">公开</el-radio-button>
          <el-radio-button v-if="isOwner" label="private">私密</el-radio-button>
        </el-radio-group>
      </div>

      <div class="dream-list">
        <DreamCard
          v-for="dream in dreams"
          :key="dream.id"
          :dream="dream"
          @click="router.push(`/dream/${dream.id}`)"
        />
        <el-empty v-if="dreams.length === 0" description="还没有发布任何梦境" />
      </div>

      <el-pagination
        v-if="total > 0"
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="loadDreams"
        style="margin-top: 20px; text-align: center"
      />
    </el-card>

    <el-dialog v-model="showEditDialog" title="编辑资料" width="520px">
      <el-form ref="editFormRef" :model="editForm" label-width="80px">
        <el-form-item label="头像">
          <el-avatar :src="editForm.avatar" :size="80">{{ user?.username }}</el-avatar>
          <el-upload
            :show-file-list="false"
            :auto-upload="false"
            :on-change="handleAvatarSelected"
            accept="image/*"
            style="margin-left: 20px"
          >
            <el-button size="small" :loading="avatarUploading">更换头像</el-button>
          </el-upload>
        </el-form-item>

        <el-form-item label="邮箱">
          <el-input v-model="editForm.email" placeholder="请输入邮箱" />
        </el-form-item>

        <el-form-item label="用户名">
          <el-input v-model="editForm.username" placeholder="请输入用户名" maxlength="30" />
        </el-form-item>

        <el-divider>修改密码（可选）</el-divider>

        <el-form-item label="旧密码">
          <el-input v-model="editForm.oldPassword" type="password" show-password placeholder="不修改可留空" />
        </el-form-item>

        <el-form-item label="新密码">
          <el-input v-model="editForm.newPassword" type="password" show-password placeholder="请输入新密码" />
        </el-form-item>

        <el-form-item label="确认密码">
          <el-input v-model="editForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSaveProfile">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { userApi, dreamApi, commentApi } from '@/api'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import DreamCard from '@/components/DreamCard.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const user = ref(null)
const dreams = ref([])
const allDreams = ref([])
const userComments = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const filterType = ref('all')
const activeTab = ref('profile')

const showEditDialog = ref(false)
const editFormRef = ref(null)
const saving = ref(false)
const avatarUploading = ref(false)
const editForm = reactive({
  username: '',
  email: '',
  avatar: '',
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const isOwner = computed(() => userStore.userId === Number(route.params.id))

const normalizeAvatar = (url) => {
  if (!url) return url
  if (url.startsWith('/uploads/')) return `/api${url}`
  return url
}

const parseDate = (v) => {
  if (!v) return null
  const d = new Date(v)
  return Number.isNaN(d.getTime()) ? null : d
}

const daysSinceJoin = computed(() => {
  const d = parseDate(user.value?.createTime)
  if (!d) return 0
  return Math.max(1, Math.floor((Date.now() - d.getTime()) / (24 * 3600 * 1000)))
})

const recentDreams = computed(() => {
  return [...allDreams.value]
    .sort((a, b) => new Date(b.createTime) - new Date(a.createTime))
    .slice(0, 6)
})

const recentComments = computed(() => {
  return [...userComments.value]
    .sort((a, b) => new Date(b.createTime) - new Date(a.createTime))
    .slice(0, 6)
})

const topTags = computed(() => {
  const counter = new Map()
  allDreams.value.forEach((d) => {
    if (!d.tags) return
    String(d.tags)
      .split(',')
      .map((t) => t.trim())
      .filter(Boolean)
      .forEach((t) => counter.set(t, (counter.get(t) || 0) + 1))
  })

  return [...counter.entries()]
    .sort((a, b) => b[1] - a[1])
    .slice(0, 5)
    .map(([name, count]) => ({ name, count }))
})

const profileStats = computed(() => {
  const totalDreams = allDreams.value.length
  const publicDreams = allDreams.value.filter((d) => d.isPublic === 1).length
  const privateDreams = allDreams.value.filter((d) => d.isPublic === 0).length

  const moodList = allDreams.value
    .map((d) => Number(d.moodScore))
    .filter((n) => !Number.isNaN(n) && n > 0)
  const avgMood = moodList.length ? (moodList.reduce((a, b) => a + b, 0) / moodList.length).toFixed(1) : '-'

  const thirtyDaysAgo = new Date()
  thirtyDaysAgo.setDate(thirtyDaysAgo.getDate() - 30)

  const recentMoodList = allDreams.value
    .filter((d) => parseDate(d.createTime) && parseDate(d.createTime) >= thirtyDaysAgo)
    .map((d) => Number(d.moodScore))
    .filter((n) => !Number.isNaN(n) && n > 0)

  const recentMoodAvg = recentMoodList.length
    ? (recentMoodList.reduce((a, b) => a + b, 0) / recentMoodList.length).toFixed(1)
    : '-'

  const recentCommentCount = userComments.value.filter(
    (c) => parseDate(c.createTime) && parseDate(c.createTime) >= thirtyDaysAgo
  ).length

  return {
    totalDreams,
    publicDreams,
    privateDreams,
    commentCount: userComments.value.length,
    recentCommentCount,
    avgMood,
    recentMoodAvg
  }
})

const monthlyTrend = computed(() => {
  const monthKeys = []
  const now = new Date()

  for (let i = 5; i >= 0; i -= 1) {
    const d = new Date(now.getFullYear(), now.getMonth() - i, 1)
    monthKeys.push(`${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`)
  }

  const counter = new Map(monthKeys.map((m) => [m, 0]))
  allDreams.value.forEach((d) => {
    const t = parseDate(d.createTime)
    if (!t) return
    const key = `${t.getFullYear()}-${String(t.getMonth() + 1).padStart(2, '0')}`
    if (counter.has(key)) counter.set(key, counter.get(key) + 1)
  })

  const max = Math.max(...counter.values(), 1)
  return [...counter.entries()].map(([month, count]) => ({
    month,
    count,
    percent: Math.max(8, Math.round((count / max) * 100))
  }))
})

const loadUser = async () => {
  try {
    const res = await userApi.getUserById(route.params.id)
    const data = res.data || {}
    data.avatar = normalizeAvatar(data.avatar)
    user.value = data

    editForm.username = data.username || ''
    editForm.email = data.email || ''
    editForm.avatar = data.avatar || ''
    editForm.oldPassword = ''
    editForm.newPassword = ''
    editForm.confirmPassword = ''
  } catch (error) {
    ElMessage.error('用户信息加载失败')
    router.push('/')
  }
}

const loadDreams = async () => {
  try {
    const res = await dreamApi.getUserDreams(route.params.id, {
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })

    let list = res.data?.records || []
    if (filterType.value === 'public') list = list.filter((d) => d.isPublic === 1)
    if (filterType.value === 'private') list = list.filter((d) => d.isPublic === 0)

    dreams.value = list
    total.value = res.data?.total || 0
  } catch (error) {
    ElMessage.error('梦境加载失败')
  }
}

const loadAllDreamsForStats = async () => {
  try {
    const res = await dreamApi.getUserDreams(route.params.id, { pageNum: 1, pageSize: 300 })
    allDreams.value = res.data?.records || []
  } catch (error) {
    allDreams.value = []
  }
}

const loadUserComments = async () => {
  try {
    const res = await commentApi.getUserComments(route.params.id)
    userComments.value = res.data || []
  } catch (error) {
    userComments.value = []
  }
}

const handleAvatarSelected = async (uploadFile) => {
  if (!isOwner.value) return
  if (!uploadFile?.raw) return
  if (!userStore.userId) return ElMessage.error('请先登录')

  avatarUploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', uploadFile.raw)

    const res = await userApi.uploadAvatar(formData)
    const avatarUrl = normalizeAvatar(res.data)
    editForm.avatar = avatarUrl
    user.value = { ...user.value, avatar: avatarUrl }
    userStore.updateUser({ avatar: avatarUrl })
    ElMessage.success('头像上传成功')
  } catch (error) {
    ElMessage.error(error.message || '头像上传失败')
  } finally {
    avatarUploading.value = false
  }
}

const handleSaveProfile = async () => {
  if (!userStore.userId) return ElMessage.error('请先登录')

  saving.value = true
  try {
    if (!editForm.username?.trim()) {
      return ElMessage.warning('用户名不能为空')
    }
    if (editForm.newPassword || editForm.confirmPassword) {
      if (!editForm.oldPassword) {
        return ElMessage.warning('修改密码时请先输入旧密码')
      }
      if (editForm.newPassword.length < 6 || editForm.newPassword.length > 20) {
        return ElMessage.warning('新密码长度需为 6-20 个字符')
      }
      if (editForm.newPassword !== editForm.confirmPassword) {
        return ElMessage.warning('两次输入的新密码不一致')
      }
    }

    const payload = {
      username: editForm.username.trim(),
      email: editForm.email,
      avatar: editForm.avatar?.replace('/api/uploads/', '/uploads/') || editForm.avatar,
      oldPassword: editForm.oldPassword || undefined,
      newPassword: editForm.newPassword || undefined
    }

    const res = await userApi.updateProfile(payload)
    const updated = res.data || payload
    updated.avatar = normalizeAvatar(updated.avatar)
    userStore.updateUser(updated)

    ElMessage.success('保存成功')
    showEditDialog.value = false
    await loadUser()
  } catch (error) {
    ElMessage.error(error.message || '保存失败')
  } finally {
    saving.value = false
  }
}

const handleTabChange = (tab) => {
  router.replace({ path: route.path, query: { ...route.query, tab } })
}

const syncTabFromRoute = () => {
  activeTab.value = route.query.tab === 'dreams' ? 'dreams' : 'profile'
  if (activeTab.value === 'dreams') {
    loadDreams()
  }
}

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' })
}

const formatDateTime = (dateStr) => {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  return d.toLocaleString('zh-CN', { hour12: false })
}

const initPage = async () => {
  await loadUser()
  await Promise.all([loadAllDreamsForStats(), loadUserComments()])
  syncTabFromRoute()
}

watch(() => route.query.tab, syncTabFromRoute)
watch(() => route.params.id, initPage)

onMounted(initPage)
</script>

<style scoped>
.profile-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.profile-hero-card {
  margin-bottom: 16px;
  border-radius: 16px;
  border: none;
  overflow: hidden;
}

.profile-hero {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  padding: 8px;
}

.hero-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.hero-meta {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.hero-name {
  font-size: 28px;
  font-weight: 700;
  color: #eef3ff;
}

.hero-subtitle {
  display: flex;
  align-items: center;
  color: #a4b4ce;
  font-size: 13px;
}

.hero-right {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 14px;
}

.stat-card {
  border-radius: 14px;
}

.stat-label {
  color: #9fb0cc;
  font-size: 13px;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 30px;
  font-weight: 700;
  color: #f3f8ff;
  line-height: 1.1;
}

.stat-desc {
  color: #acbad1;
  margin-top: 8px;
  font-size: 12px;
}

.stat-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.overview-grid {
  display: grid;
  grid-template-columns: 1.2fr 1fr;
  gap: 14px;
}

.overview-grid .section-card:last-child {
  grid-column: span 2;
}

.section-card {
  border-radius: 14px;
}

.section-title {
  font-weight: 600;
  color: #eaf1ff;
}

.trend-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.trend-row {
  display: grid;
  grid-template-columns: 80px 1fr 28px;
  align-items: center;
  gap: 8px;
}

.trend-month {
  color: #a3b3cb;
  font-size: 12px;
}

.trend-bar-wrap {
  height: 10px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.1);
  overflow: hidden;
}

.trend-bar {
  height: 100%;
  border-radius: 10px;
  background: linear-gradient(90deg, #22c55e, #3b82f6);
}

.trend-value {
  text-align: right;
  color: #cad8ee;
  font-size: 12px;
}

.timeline {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.timeline-item {
  padding: 10px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: rgba(14, 21, 31, 0.7);
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
}

.timeline-item:hover {
  background: rgba(0, 229, 255, 0.08);
  border-color: rgba(0, 229, 255, 0.3);
}

.timeline-main {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}

.timeline-title {
  color: #edf3ff;
  font-weight: 600;
}

.timeline-sub {
  color: #a2b3cb;
  font-size: 12px;
  margin-top: 4px;
}

.comment-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.comment-item {
  border-left: 3px solid #00c8ff;
  background: rgba(14, 21, 31, 0.78);
  border-radius: 8px;
  padding: 10px 12px;
}

.comment-content {
  color: #eaf1ff;
  line-height: 1.5;
}

.comment-time {
  margin-top: 4px;
  color: #a0b2cc;
  font-size: 12px;
}

.dream-list-card {
  border-radius: 14px;
}

.filter-tabs {
  margin-bottom: 16px;
}

.dream-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 20px;
}

@media (max-width: 992px) {
  .stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .overview-grid {
    grid-template-columns: 1fr;
  }

  .overview-grid .section-card:last-child {
    grid-column: span 1;
  }
}

@media (max-width: 768px) {
  .profile-container {
    padding: 12px;
  }

  .profile-hero {
    flex-direction: column;
    align-items: flex-start;
  }

  .hero-right {
    width: 100%;
    justify-content: flex-start;
  }

  .stats-grid {
    grid-template-columns: 1fr;
  }
}

:deep(.el-dialog) {
  background: linear-gradient(160deg, rgba(20, 27, 40, 0.96), rgba(15, 22, 34, 0.96));
  border: 1px solid rgba(148, 163, 184, 0.24);
  border-radius: 18px;
  box-shadow: 0 20px 45px rgba(2, 8, 20, 0.5);
  overflow: hidden;
}

:deep(.el-dialog__title) {
  color: #eef3ff;
}

:deep(.el-dialog__header) {
  padding: 18px 22px 12px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.16);
}

:deep(.el-dialog__body) {
  padding: 20px 22px 16px;
}

:deep(.el-dialog__footer) {
  padding: 12px 22px 18px;
  border-top: 1px solid rgba(148, 163, 184, 0.14);
}

:deep(.el-form-item__label) {
  color: #c8d6ee;
}

:deep(.el-form-item) {
  margin-bottom: 18px;
}

:deep(.el-divider__text) {
  border-radius: 999px;
  padding: 4px 12px;
  background: rgba(19, 28, 44, 0.9);
  color: #d9e7fb;
  border: 1px solid rgba(148, 163, 184, 0.22);
}

:deep(.el-input__wrapper) {
  border-radius: 12px !important;
  background: rgba(8, 14, 25, 0.86) !important;
  box-shadow: inset 0 0 0 1px rgba(148, 163, 184, 0.22) !important;
}

:deep(.el-input__wrapper.is-focus) {
  box-shadow: inset 0 0 0 1px rgba(56, 189, 248, 0.55) !important;
}

:deep(.el-input__inner) {
  color: #edf3ff !important;
}

:deep(.el-input__inner::placeholder) {
  color: #8fa4c5 !important;
}

:deep(.el-dialog .el-button) {
  border-radius: 10px;
}
</style>
