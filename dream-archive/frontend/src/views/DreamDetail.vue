<template>
  <div class="dream-detail-container">
    <el-card v-if="dream" class="dream-detail-card">
      <div class="dream-header">
        <div class="user-info">
          <el-avatar :src="toDisplayUrl(dream.userAvatar)" :size="50">{{ dream.username }}</el-avatar>
          <div class="user-meta">
            <div class="username" @click="router.push(`/profile/${dream.userId}`)">{{ dream.username }}</div>
            <div class="meta-info">
              <span>{{ formatDate(dream.dreamDate) }} 做的梦</span>
              <el-divider direction="vertical" />
              <span>{{ formatTime(dream.createTime) }} 发布</span>
            </div>
          </div>
        </div>
        <el-tag class="category-tag" size="large">{{ dream.categoryName }}</el-tag>
      </div>

      <h1 class="dream-title">{{ dream.title }}</h1>

      <div class="mood-section">
        <span class="mood-label">情绪评分:</span>
        <el-rate v-model="dream.moodScore" disabled :texts="['很糟糕', '不太好', '一般', '不错', '很愉快']" show-text />
      </div>

      <div class="dream-content">{{ dream.content }}</div>

      <div v-if="dream.imageUrls && dream.imageUrls.length" class="dream-images">
        <img
          v-for="(img, idx) in dream.imageUrls"
          :key="`${img}-${idx}`"
          :src="toDisplayUrl(img)"
          alt="dream image"
        />
      </div>

      <div v-if="dream.tags" class="tags-section">
        <el-tag v-for="tag in dream.tags.split(',')" :key="tag" class="dream-tag" style="margin-right: 10px">
          # {{ tag }}
        </el-tag>
      </div>

      <div class="analysis-section">
        <div class="analysis-header">
          <div class="analysis-title">梦境分析</div>
          <div class="analysis-actions">
            <div v-if="analysisUpdatedAt" class="analysis-time">更新于 {{ formatTime(analysisUpdatedAt) }}</div>
            <el-button v-if="isAuthor && hasAnalysis" size="small" type="primary" @click="goDreamRoom">
              进入辅导室
            </el-button>
            <el-button size="small" :loading="analysisLoading" @click="handleAnalyze">重新分析</el-button>
          </div>
        </div>
        <div v-if="hasAnalysis" class="analysis-body">
          <div class="analysis-metrics">
            <div class="analysis-chip">
              <span class="chip-label">分类</span>
              <span class="chip-value">{{ dream.analysisLabelName }}</span>
            </div>
            <div class="analysis-chip">
              <span class="chip-label">置信度</span>
              <span class="chip-value">{{ analysisConfidenceText }}</span>
            </div>
            <div class="analysis-chip">
              <span class="chip-label">强度</span>
              <span class="chip-value">{{ dream.analysisIntensity || '—' }}</span>
            </div>
          </div>
          <div class="analysis-feedback">
            {{ dream.analysisFeedback || '暂无分析反馈' }}
          </div>
          <div v-if="otherEncourageText" class="analysis-feedback other-encourage">
            {{ otherEncourageText }}
          </div>
        </div>
        <div v-else class="analysis-empty">
          梦境已记录，系统正在分析中。
        </div>
      </div>

      <div class="action-bar">
        <div class="stats">
          <span class="stat-item"><el-icon><View /></el-icon>{{ dream.viewCount }} 浏览</span>
          <span class="stat-item"><el-icon><ChatDotRound /></el-icon>{{ dream.commentCount }} 评论</span>
        </div>
        <div class="actions">
          <el-button
            :class="['like-btn', { liked: dream.isLiked }]"
            :style="dream.isLiked ? likedButtonStyle : normalButtonStyle"
            @click="handleLike"
          >
            {{ dream.isLiked ? '已点赞' : '点赞' }} ({{ dream.likeCount }})
          </el-button>
          <el-button v-if="isAuthor" type="primary" @click="handleEdit">编辑</el-button>
          <el-button v-if="isAuthor" type="danger" @click="handleDelete">删除</el-button>
        </div>
      </div>
    </el-card>

    <el-card class="comment-section">
      <template #header>
        <div class="comment-header">
          <span class="comment-title">评论 ({{ comments.length }})</span>
        </div>
      </template>

      <div v-if="isLoggedIn" class="comment-form">
        <el-input
          v-model="commentContent"
          type="textarea"
          :rows="3"
          placeholder="写下你的解梦或评论..."
          maxlength="500"
          show-word-limit
        />
        <el-button type="primary" :loading="commentLoading" style="margin-top: 10px" @click="handleComment">发表评论</el-button>
      </div>
      <div v-else class="login-tip">
        <el-alert title="请先登录后再发表评论" type="info" :closable="false">
          <el-button type="primary" size="small" @click="router.push('/login')">去登录</el-button>
        </el-alert>
      </div>

      <div class="comment-list">
        <div v-for="comment in comments" :key="comment.id" class="comment-item">
          <el-avatar :src="toDisplayUrl(comment.userAvatar)" :size="40">{{ comment.username }}</el-avatar>
          <div class="comment-content-wrap">
            <div class="comment-user">{{ comment.username }}</div>
            <div class="comment-text">{{ comment.content }}</div>
            <div class="comment-footer">
              <span class="comment-time">{{ formatTime(comment.createTime) }}</span>
              <el-button text size="small" @click="toggleReply(comment.id)">回复</el-button>
            </div>

            <div v-if="activeReplyId === comment.id" class="reply-editor">
              <el-input
                v-model="replyContent"
                type="textarea"
                :rows="2"
                placeholder="输入回复内容..."
                maxlength="500"
                show-word-limit
              />
              <div class="reply-actions">
                <el-button size="small" @click="cancelReply">取消</el-button>
                <el-button type="primary" size="small" :loading="replyLoading" @click="submitReply(comment.id)">
                  发送回复
                </el-button>
              </div>
            </div>

            <div v-if="comment.replies && comment.replies.length" class="reply-list">
              <div v-for="reply in comment.replies" :key="reply.id" class="reply-item">
                <span class="reply-user">{{ reply.username }}：</span>
                <span class="reply-text">{{ reply.content }}</span>
                <span class="reply-time">{{ formatTime(reply.createTime) }}</span>
              </div>
            </div>
          </div>
        </div>
        <el-empty v-if="comments.length === 0" description="暂无评论" />
      </div>
    </el-card>

    <el-dialog v-model="otherDialogVisible" title="补充感受" width="420px">
      <div class="other-dialog-body">
        <div class="other-dialog-item">
          <div class="other-dialog-label">昨晚睡得好吗？</div>
          <el-rate v-model="sleepQuality" :max="5" />
        </div>
        <div class="other-dialog-item">
          <div class="other-dialog-label">现在压力等级（1-5）</div>
          <el-rate v-model="stressLevel" :max="5" />
        </div>
      </div>
      <template #footer>
        <el-button @click="otherDialogVisible = false">稍后再说</el-button>
        <el-button type="primary" @click="submitOtherFeedback">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { dreamApi, commentApi } from '@/api'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import { View, ChatDotRound } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const dream = ref(null)
const comments = ref([])
const commentContent = ref('')
const commentLoading = ref(false)
const activeReplyId = ref(null)
const replyContent = ref('')
const replyLoading = ref(false)
const analysisLoading = ref(false)
const otherDialogVisible = ref(false)
const sleepQuality = ref(3)
const stressLevel = ref(3)
const otherEncourageText = ref('')

const isLoggedIn = computed(() => userStore.isLoggedIn)
const isAuthor = computed(() => dream.value && userStore.userId === dream.value.userId)
const hasAnalysis = computed(() => Boolean(dream.value?.analysisLabelName))
const analysisUpdatedAt = computed(() => dream.value?.analysisUpdatedAt || '')
const analysisConfidenceText = computed(() => {
  const value = dream.value?.analysisConfidence
  if (typeof value !== 'number') return '—'
  return `${Math.round(value * 100)}%`
})

const shouldPromptOther = computed(() => dream.value?.analysisLabelName === '其他')
const normalButtonStyle = {
  '--el-button-bg-color': '#2563eb',
  '--el-button-border-color': '#60a5fa',
  '--el-button-text-color': '#eff6ff',
  '--el-button-hover-bg-color': '#1d4ed8',
  '--el-button-hover-border-color': '#93c5fd',
  '--el-button-active-bg-color': '#1e40af',
  '--el-button-active-border-color': '#93c5fd'
}
const likedButtonStyle = {
  '--el-button-bg-color': '#ec4899',
  '--el-button-border-color': '#f472b6',
  '--el-button-text-color': '#fff1f8',
  '--el-button-hover-bg-color': '#db2777',
  '--el-button-hover-border-color': '#f9a8d4',
  '--el-button-active-bg-color': '#be185d',
  '--el-button-active-border-color': '#fbcfe8'
}

const toDisplayUrl = (url) => {
  if (!url) return url
  if (url.startsWith('/uploads/')) return `/api${url}`
  return url
}

const loadDream = async () => {
  try {
    const res = await dreamApi.getDreamById(route.params.id)
    const d = res.data || {}
    d.userAvatar = toDisplayUrl(d.userAvatar)
    d.isLiked = d.isLiked === true
    dream.value = d
    maybePromptOther()
  } catch (error) {
    ElMessage.error('梦境加载失败')
    router.push('/')
  }
}

const loadComments = async () => {
  try {
    const res = await commentApi.getComments(route.params.id, { pageNum: 1, pageSize: 100 })
    const list = res.data?.records || []
    const normalizeComment = (item) => ({
      ...item,
      userAvatar: toDisplayUrl(item.userAvatar),
      replies: (item.replies || []).map((r) => ({ ...r, userAvatar: toDisplayUrl(r.userAvatar) }))
    })
    comments.value = list.map(normalizeComment)
  } catch (error) {
    console.error('评论加载失败:', error)
  }
}

const handleLike = async () => {
  if (!isLoggedIn.value) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }

  try {
    const res = await dreamApi.toggleLike(route.params.id)
    dream.value.isLiked = res.data
    dream.value.likeCount += res.data ? 1 : -1
    ElMessage.success(res.data ? '点赞成功' : '取消点赞')
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

const handleAnalyze = async () => {
  if (!dream.value?.id) return
  analysisLoading.value = true
  try {
    const res = await dreamApi.analyzeDream(dream.value.id)
    await loadDream()
    const roomId = res.data?.dream_room_id
    if (roomId) {
      ElMessage.success('分析完成，已生成辅导室入口')
    } else {
      ElMessage.success('分析完成')
    }
  } catch (error) {
    ElMessage.error('分析失败')
  } finally {
    analysisLoading.value = false
  }
}

const goDreamRoom = () => {
  router.push('/dream-room')
}

const maybePromptOther = () => {
  if (!shouldPromptOther.value || !dream.value?.id) return
  const key = `other_prompted_${dream.value.id}`
  if (localStorage.getItem(key) === '1') return
  otherDialogVisible.value = true
}

const submitOtherFeedback = () => {
  const s = sleepQuality.value || 3
  const p = stressLevel.value || 3
  otherEncourageText.value = buildEncourageText(s, p)
  if (dream.value?.id) {
    localStorage.setItem(`other_prompted_${dream.value.id}`, '1')
  }
  otherDialogVisible.value = false
}

const buildEncourageText = (sleepScore, stressScore) => {
  if (stressScore >= 4) {
    return '压力有点高，先给自己一点缓冲时间。试试深呼吸或短暂散步，你做得很好。'
  }
  if (stressScore <= 2 && sleepScore >= 4) {
    return '睡得不错且压力不高，状态很好，继续保持规律作息。'
  }
  if (sleepScore <= 2) {
    return '睡眠不太足时，情绪会更敏感。今晚尽量早点休息，给自己一点恢复空间。'
  }
  return '你的状态还算稳定，保持节奏就好。需要时也可以给自己一点小奖励。'
}

const handleComment = async () => {
  if (!commentContent.value.trim()) {
    ElMessage.warning('请输入评论内容')
    return
  }

  commentLoading.value = true
  try {
    await commentApi.createComment({
      dreamId: Number(route.params.id),
            content: commentContent.value,
      parentId: 0
    })

    ElMessage.success('评论发表成功')
    commentContent.value = ''
    if (dream.value) {
      dream.value.commentCount = (dream.value.commentCount || 0) + 1
    }
    await loadComments()
  } catch (error) {
    ElMessage.error('评论失败')
  } finally {
    commentLoading.value = false
  }
}

const toggleReply = (commentId) => {
  activeReplyId.value = activeReplyId.value === commentId ? null : commentId
  replyContent.value = ''
}

const cancelReply = () => {
  activeReplyId.value = null
  replyContent.value = ''
}

const submitReply = async (parentId) => {
  if (!replyContent.value.trim()) {
    ElMessage.warning('请输入回复内容')
    return
  }
  if (!isLoggedIn.value) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }

  replyLoading.value = true
  try {
    await commentApi.createComment({
      dreamId: Number(route.params.id),
            content: replyContent.value,
      parentId
    })
    ElMessage.success('回复成功')
    if (dream.value) {
      dream.value.commentCount = (dream.value.commentCount || 0) + 1
    }
    cancelReply()
    await loadComments()
  } catch (error) {
    ElMessage.error(error.message || '回复失败')
  } finally {
    replyLoading.value = false
  }
}

const handleEdit = () => {
  router.push(`/dream/edit/${dream.value.id}`)
}

const handleDelete = async () => {
  try {
    await ElMessageBox.confirm('确定要删除这个梦境吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await dreamApi.deleteDream(dream.value.id)
    ElMessage.success('删除成功')
    router.push(`/profile/${userStore.userId}`)
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const formatDate = (dateStr) => {
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric' })
}

const formatTime = (timeStr) => {
  const date = new Date(timeStr)
  const now = new Date()
  const diff = now - date

  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  if (diff < 2592000000) return `${Math.floor(diff / 86400000)}天前`

  return date.toLocaleDateString()
}

onMounted(() => {
  loadDream()
  loadComments()
})
</script>

<style scoped>
.dream-detail-container {
  max-width: 900px;
  margin: 0 auto;
  padding: 20px;
}

.dream-detail-card {
  margin-bottom: 20px;
}

.dream-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.14);
}

.user-info {
  display: flex;
  gap: 15px;
  align-items: center;
}

.user-meta {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.username {
  font-weight: 600;
  font-size: 18px;
  cursor: pointer;
  color: #eef3ff;
}

.username:hover {
  color: #00c8ff;
}

.meta-info {
  font-size: 13px;
  color: #becde5;
}

.dream-title {
  font-size: 36px;
  line-height: 1.3;
  font-weight: bold;
  color: #f7fbff;
  margin-bottom: 22px;
}

.mood-section {
  display: flex;
  align-items: center;
  margin-bottom: 24px;
}

.mood-label {
  font-weight: 600;
  margin-right: 10px;
  color: #c1cee3;
}

.dream-content {
  line-height: 2.05;
  font-size: 18px;
  color: #eef5ff;
  white-space: pre-wrap;
  word-break: break-word;
  margin-bottom: 28px;
  padding: 22px 24px;
  background: rgba(14, 20, 32, 0.88);
  border: 1px solid rgba(255, 255, 255, 0.14);
  border-radius: 12px;
}

.dream-images {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 10px;
  margin-bottom: 24px;
}

.dream-images img {
  width: 100%;
  height: 180px;
  object-fit: cover;
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.12);
}

.tags-section {
  margin-bottom: 30px;
}

.analysis-section {
  margin-bottom: 28px;
  padding: 18px 20px;
  border-radius: 14px;
  border: 1px solid rgba(96, 165, 250, 0.35);
  background: linear-gradient(135deg, rgba(15, 23, 42, 0.9), rgba(30, 64, 175, 0.2));
  box-shadow: 0 12px 26px rgba(15, 23, 42, 0.3);
}

.analysis-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.analysis-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.analysis-title {
  font-size: 16px;
  font-weight: 700;
  color: #e2e8f0;
  letter-spacing: 0.5px;
}

.analysis-time {
  font-size: 12px;
  color: #94a3b8;
}

.analysis-body {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.analysis-metrics {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.analysis-chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(37, 99, 235, 0.2);
  border: 1px solid rgba(96, 165, 250, 0.35);
  color: #e2e8f0;
  font-size: 13px;
}

.chip-label {
  color: #9fb1cb;
  font-weight: 600;
}

.chip-value {
  color: #eff6ff;
  font-weight: 700;
}

.analysis-feedback {
  color: #e8f1ff;
  line-height: 1.8;
  font-size: 15px;
  padding: 12px 14px;
  background: rgba(15, 23, 42, 0.55);
  border-radius: 10px;
  border: 1px solid rgba(148, 163, 184, 0.2);
}

.other-encourage {
  background: rgba(30, 64, 175, 0.35);
  border-color: rgba(96, 165, 250, 0.4);
}

.analysis-empty {
  color: #cbd5f5;
  font-size: 13px;
  padding: 10px 0;
}

.other-dialog-body {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.other-dialog-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.other-dialog-label {
  font-weight: 600;
  color: #e2e8f0;
}

.category-tag,
.dream-tag {
  background: linear-gradient(90deg, #1d4ed8, #2563eb) !important;
  border: 1px solid #60a5fa !important;
  color: #eff6ff !important;
  font-weight: 600;
}

.action-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 20px;
  border-top: 1px solid rgba(255, 255, 255, 0.14);
}

.stats {
  display: flex;
  gap: 20px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 5px;
  color: #c1d0e7;
  font-size: 14px;
}

.actions {
  display: flex;
  gap: 10px;
}

.like-btn {
  min-width: 96px;
  font-weight: 600;
  box-shadow: 0 8px 18px rgba(37, 99, 235, 0.26);
  background-image: none !important;
}

.like-btn.liked {
  background-image: none !important;
  border: none !important;
}

.comment-section {
  margin-top: 20px;
}

.comment-title {
  font-size: 21px;
  font-weight: bold;
  color: #f2f7ff;
}

.comment-form,
.login-tip {
  margin-bottom: 30px;
}

.comment-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.comment-item {
  display: flex;
  gap: 15px;
}

.comment-content-wrap {
  flex: 1;
  background: rgba(10, 16, 28, 0.72);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 12px;
  padding: 12px 14px;
}

.comment-user {
  font-weight: 600;
  margin-bottom: 6px;
  font-size: 15px;
  color: #eef3ff;
}

.comment-text {
  color: #e6eefc;
  line-height: 1.8;
  font-size: 15px;
  margin-bottom: 8px;
  word-break: break-word;
}

.comment-footer {
  display: flex;
  align-items: center;
  gap: 15px;
}

.comment-time {
  font-size: 13px;
  color: #b7c8e2;
}

.reply-editor {
  margin-top: 10px;
  background: rgba(12, 18, 29, 0.92);
  border: 1px solid rgba(255, 255, 255, 0.14);
  border-radius: 8px;
  padding: 10px;
}

.reply-actions {
  margin-top: 8px;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.reply-list {
  margin-top: 12px;
  padding-left: 10px;
  border-left: 2px solid rgba(143, 169, 216, 0.5);
}

.reply-item {
  padding: 8px 0;
  font-size: 14px;
  line-height: 1.6;
  color: #e0eaf9;
}

.reply-user {
  color: #eef3ff;
  font-weight: 600;
}

.reply-time {
  margin-left: 8px;
  color: #b2c4dd;
}

.mood-section :deep(.el-rate__text) {
  color: #d8e6fc;
  font-size: 14px;
}
</style>
