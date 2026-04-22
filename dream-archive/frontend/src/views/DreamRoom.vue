<template>
  <div class="kiki-room-page">
    <aside class="history-pane">
      <div class="brand-block">
        <img class="brand-logo" :src="somniumLogo" alt="Somnium" />
        <p class="brand-name">Somnium Dream Archive · Mental Assistant</p>
      </div>

      <div class="history-list-wrap">
        <p class="history-heading">梦境历史</p>
        <div v-if="historyLoading" class="history-tip">正在加载历史梦境...</div>

        <button
          v-for="post in historyPosts"
          :key="post.id"
          type="button"
          class="history-item"
          :class="{ active: Number(post.id) === dreamPostId }"
          @click="switchConversation(post.id)"
        >
          <span class="history-dot"></span>
          <div class="history-text">
            <p>{{ post.title || `梦境帖子 #${post.id}` }}</p>
            <small>{{ post.dateLabel }}</small>
          </div>
        </button>

        <div v-if="!historyLoading && historyPosts.length === 0" class="history-tip">
          暂无梦境帖子，请先记录梦境。
        </div>
      </div>

      <div class="history-footer">
        <button class="side-action" type="button" @click="router.push('/dream/create')">记录梦境</button>
        <button class="side-action danger" type="button" @click="router.push('/')">返回首页</button>
      </div>
    </aside>

    <section class="chat-stage">
      <header class="chat-top">
        <div class="mentor-info">
          <div class="mentor-avatar">Ki</div>
          <div>
            <p class="mentor-name">梦境心灵导师 · KiKi</p>
            <p class="mentor-status" :class="statusClass">
              <span class="status-point"></span>
              {{ statusText }}
            </p>
          </div>
        </div>

        <div class="chat-top-actions">
          <button class="icon-btn" type="button" title="搜索本次对话" @click="toggleSearch">
            <el-icon><Search /></el-icon>
          </button>
          <button class="icon-btn" type="button" title="聊天室详情" @click="detailVisible = true">
            <el-icon><MoreFilled /></el-icon>
          </button>
        </div>
      </header>

      <div v-if="searchVisible" class="search-strip">
        <el-input
          ref="searchInputRef"
          v-model="searchKeyword"
          placeholder="搜索本次对话内容"
          clearable
          size="large"
        />
        <span class="search-count">{{ searchCount }} 条结果</span>
      </div>

      <main ref="messageListRef" class="message-stage">
        <template v-for="msg in visibleMessages" :key="msg.id">
          <div class="msg-row" :class="Number(msg.senderId) === 0 ? 'assistant' : 'user'">
            <div v-if="Number(msg.senderId) === 0" class="msg-side-icon">✦</div>
            <div class="msg-bubble">{{ msg.messageText }}</div>
          </div>
          <div class="msg-time" :class="Number(msg.senderId) === 0 ? 'assistant' : 'user'">{{ formatMinute(msg.createTime) }}</div>
        </template>

        <div v-if="showOpeningBubble" class="msg-row assistant opening-row">
          <div class="msg-side-icon">✦</div>
          <div class="msg-bubble opening">
            <span class="typing-dot"></span>
            <span class="typing-dot"></span>
            <span class="typing-dot"></span>
            正在整理你的梦境线索，请稍候...
          </div>
        </div>

        <el-empty
          v-if="visibleMessages.length === 0 && !showOpeningBubble"
          :description="searchKeyword ? '未找到匹配内容' : '还没有消息，试着和 KiKi 打个招呼吧'"
        />
      </main>

      <footer class="composer-wrap">
        <div class="composer">
          <div class="composer-mark">✦</div>
          <el-input
            v-model="inputText"
            type="textarea"
            :autosize="{ minRows: 1, maxRows: 4 }"
            maxlength="800"
            :disabled="!canSend"
            :placeholder="placeholderText"
            @keydown.enter.ctrl.prevent="sendMessage"
          />
          <button class="send-btn" type="button" :disabled="!canSend || sending" @click="sendMessage">
            <el-icon><Promotion /></el-icon>
          </button>
        </div>
      </footer>
    </section>

    <el-dialog v-model="detailVisible" title="聊天室详情" width="420px">
      <div class="detail-list">
        <div class="detail-row"><span>梦境帖子 ID</span><strong>{{ dreamPostId || '-' }}</strong></div>
        <div class="detail-row"><span>房间 ID</span><strong>{{ roomId || '-' }}</strong></div>
        <div class="detail-row"><span>消息数量</span><strong>{{ messages.length }}</strong></div>
        <div class="detail-row"><span>连接状态</span><strong>{{ statusText }}</strong></div>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { MoreFilled, Promotion, Search } from '@element-plus/icons-vue'
import somniumLogo from '@/assets/somnium-logo.svg'
import { dreamApi, dreamRoomApi } from '@/api'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const dreamPostId = ref(Number(route.params.dreamPostId || 0))
const roomId = ref('')
const roomStatus = ref(0)
const tip = ref('')
const messages = ref([])
const inputText = ref('')
const sending = ref(false)
const messageListRef = ref(null)
const pollTimer = ref(null)
const aiApiError = ref(false)

const historyLoading = ref(false)
const historyPosts = ref([])

const searchVisible = ref(false)
const searchKeyword = ref('')
const searchInputRef = ref(null)

const detailVisible = ref(false)

const canSend = computed(() => roomStatus.value === 2 && !aiApiError.value)
const showOpeningBubble = computed(() => roomStatus.value === 1 && !messages.value.some((msg) => Number(msg.senderId) === 0))

const statusText = computed(() => {
  if (aiApiError.value) return 'ARCHIVE INACTIVE'
  if (roomStatus.value === 1 || roomStatus.value === 2) return 'ARCHIVE ACTIVE'
  if (roomStatus.value === 3) return 'ARCHIVE DISABLED'
  return 'ARCHIVE INACTIVE'
})

const statusClass = computed(() => {
  if (aiApiError.value) return 'inactive'
  if (roomStatus.value === 1 || roomStatus.value === 2) return 'active'
  if (roomStatus.value === 3) return 'disabled'
  return 'inactive'
})

const placeholderText = computed(() => {
  if (aiApiError.value) return '智能体接口异常，请稍后重试'
  if (roomStatus.value === 3) return '当前辅导室不可用'
  if (roomStatus.value === 1) return '导师正在生成开场引导...'
  if (roomStatus.value === 0) return '你今天还没有发布梦境帖子，请先记录梦境'
  return '描述那个困扰你的梦境细节...（Ctrl + Enter 发送）'
})

const visibleMessages = computed(() => {
  const keyword = searchKeyword.value.trim().toLowerCase()
  if (!keyword) return messages.value
  return messages.value.filter((msg) => String(msg.messageText || '').toLowerCase().includes(keyword))
})

const searchCount = computed(() => visibleMessages.value.length)

const formatMinute = (timeStr) => {
  if (!timeStr) return '--:--'
  const d = new Date(timeStr)
  if (Number.isNaN(d.getTime())) return '--:--'
  return d.toLocaleTimeString('zh-CN', { hour12: false, hour: '2-digit', minute: '2-digit' })
}

const formatDateShort = (timeStr) => {
  const d = new Date(timeStr)
  if (Number.isNaN(d.getTime())) return '未知时间'
  return d.toLocaleDateString('zh-CN', { month: 'numeric', day: 'numeric' })
}

const scrollToBottom = () => {
  const dom = messageListRef.value
  if (!dom) return
  dom.scrollTop = dom.scrollHeight
}

const enterRoom = async () => {
  const res = await dreamRoomApi.enterRoom({ dream_post_id: dreamPostId.value || null })
  const resolvedPostId = Number(res.data?.dream_post_id || 0)
  if (resolvedPostId > 0) {
    dreamPostId.value = resolvedPostId
  }
  roomId.value = res.data?.dream_room_id || ''
  roomStatus.value = Number(res.data?.dream_room_status || 0)
  tip.value = res.data?.tip || ''
  aiApiError.value = false
}

const loadMessages = async () => {
  if (!roomId.value) return
  const res = await dreamRoomApi.getMessages({
    dream_room_id: roomId.value,
    pageNum: 1,
    pageSize: 200
  })
  messages.value = res.data?.records || []
  await nextTick()
  scrollToBottom()
}

const loadHistoryPosts = async () => {
  if (!userStore.userId) return
  historyLoading.value = true
  try {
    const res = await dreamApi.getUserDreams(userStore.userId, {
      pageNum: 1,
      pageSize: 120
    })
    const records = Array.isArray(res.data?.records) ? res.data.records : []
    historyPosts.value = [...records]
      .sort((a, b) => new Date(b.createTime) - new Date(a.createTime))
      .map((post) => ({
        id: Number(post.id),
        title: post.title || `梦境帖子 #${post.id}`,
        dateLabel: formatDateShort(post.createTime)
      }))
  } catch (error) {
    historyPosts.value = []
  } finally {
    historyLoading.value = false
  }
}

const resetConversation = async () => {
  roomId.value = ''
  roomStatus.value = 0
  tip.value = ''
  messages.value = []
  inputText.value = ''
  searchKeyword.value = ''

  try {
    await enterRoom()
    await loadMessages()
  } catch (error) {
    aiApiError.value = true
    ElMessage.error(error.message || '连接心理导师失败，请稍后重试')
  }
}

const poll = async () => {
  try {
    await enterRoom()
    await loadMessages()
  } catch (error) {
    aiApiError.value = true
  }
}

const sendMessage = async () => {
  const text = inputText.value.trim()
  if (!text) {
    ElMessage.warning('请输入消息内容')
    return
  }
  if (!roomId.value) {
    ElMessage.error('房间初始化失败，请稍后重试')
    return
  }

  sending.value = true
  try {
    await dreamRoomApi.sendMessage({
      dream_room_id: roomId.value,
      dream_post_id: dreamPostId.value,
      text,
      client_msg_id: `cmsg_${Date.now()}_${Math.random().toString(16).slice(2, 8)}`
    })
    inputText.value = ''
    await loadMessages()
  } catch (error) {
    ElMessage.error(error.message || '发送失败，请稍后重试')
  } finally {
    sending.value = false
  }
}

const switchConversation = (postId) => {
  if (!postId) return
  if (Number(postId) === dreamPostId.value) return
  router.push(`/dream-room/chat/${postId}`)
}

const toggleSearch = async () => {
  searchVisible.value = !searchVisible.value
  if (searchVisible.value) {
    await nextTick()
    searchInputRef.value?.focus?.()
  } else {
    searchKeyword.value = ''
  }
}

watch(
  () => route.params.dreamPostId,
  async (newVal) => {
    const nextId = Number(newVal || 0)
    if (!nextId || nextId === dreamPostId.value) return
    dreamPostId.value = nextId
    await resetConversation()
  }
)

onMounted(async () => {
  await loadHistoryPosts()
  await resetConversation()
  pollTimer.value = setInterval(poll, 2500)
})

onBeforeUnmount(() => {
  if (pollTimer.value) {
    clearInterval(pollTimer.value)
    pollTimer.value = null
  }
})
</script>

<style scoped>
.kiki-room-page {
  height: 100dvh;
  min-height: 100vh;
  display: grid;
  grid-template-columns: 286px minmax(0, 1fr);
  background:
    radial-gradient(36% 42% at 42% 72%, rgba(45, 62, 159, 0.16), transparent 70%),
    linear-gradient(135deg, #050928 0%, #030726 55%, #02041c 100%);
  overflow: hidden;
}

.history-pane {
  border-right: 1px solid rgba(255, 255, 255, 0.1);
  background: linear-gradient(180deg, rgba(22, 26, 52, 0.9), rgba(18, 23, 46, 0.92));
  display: flex;
  flex-direction: column;
  height: 100dvh;
  min-height: 100vh;
}

.brand-block {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 28px 20px 20px;
}

.brand-logo {
  width: 34px;
  height: 34px;
  flex-shrink: 0;
}

.brand-name {
  margin: 0;
  color: #eef5ff;
  font-size: 14px;
  letter-spacing: 0.4px;
  line-height: 1.4;
}

.history-list-wrap {
  flex: 1;
  padding: 12px 14px;
  overflow-y: auto;
  min-height: 0;
}

.history-heading {
  margin: 0 8px 12px;
  font-size: 12px;
  color: rgba(188, 201, 239, 0.7);
  letter-spacing: 1px;
}

.history-tip {
  margin: 0 8px;
  color: rgba(184, 198, 235, 0.72);
  font-size: 13px;
  line-height: 1.6;
}

.history-item {
  width: 100%;
  text-align: left;
  border: none;
  border-radius: 12px;
  background: transparent;
  color: #dbe6fd;
  display: flex;
  align-items: flex-start;
  gap: 9px;
  padding: 10px 8px;
  cursor: pointer;
  margin-bottom: 6px;
}

.history-item:hover,
.history-item.active {
  background: rgba(255, 255, 255, 0.08);
}

.history-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: rgba(160, 178, 231, 0.8);
  margin-top: 8px;
}

.history-text p {
  margin: 0;
  font-size: 15px;
  line-height: 1.42;
  color: #edf4ff;
}

.history-text small {
  color: rgba(180, 196, 237, 0.72);
  font-size: 12px;
}

.history-footer {
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  padding: 14px;
  display: grid;
  gap: 8px;
}

.side-action {
  border-radius: 10px;
  border: 1px solid rgba(158, 179, 232, 0.3);
  background: rgba(255, 255, 255, 0.03);
  color: #dce8ff;
  height: 38px;
  cursor: pointer;
}

.side-action.danger {
  border-color: rgba(244, 114, 182, 0.38);
  color: #ffbfd8;
}

.chat-stage {
  height: 100dvh;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.chat-top {
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  padding: 16px 26px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-shrink: 0;
}

.mentor-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.mentor-avatar {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, #3f4dd6, #667dff);
  color: #fff;
  font-weight: 700;
}

.mentor-name {
  margin: 0;
  color: #eff6ff;
  font-size: 16px;
  font-weight: 600;
}

.mentor-status {
  margin: 2px 0 0;
  font-size: 12px;
  letter-spacing: 0.8px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.mentor-status.active {
  color: #7dfaa2;
}

.mentor-status.disabled {
  color: #ff8ca6;
}

.mentor-status.inactive {
  color: #a3b2cf;
}

.status-point {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: currentColor;
}

.chat-top-actions {
  display: flex;
  gap: 8px;
}

.icon-btn {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  border: 1px solid rgba(172, 189, 235, 0.26);
  background: rgba(255, 255, 255, 0.03);
  color: #dce8ff;
  cursor: pointer;
}

.search-strip {
  padding: 10px 24px;
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.search-count {
  color: #a5b7d8;
  font-size: 12px;
}

.message-stage {
  flex: 1 1 auto;
  overflow-y: auto;
  padding: 26px 24px 12px;
  min-height: 0;
}

.msg-row {
  display: flex;
  gap: 10px;
  max-width: 76%;
  margin-bottom: 4px;
}

.msg-row.assistant {
  align-items: flex-start;
}

.msg-row.user {
  margin-left: auto;
  justify-content: flex-end;
}

.msg-side-icon {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  border: 1px solid rgba(166, 183, 231, 0.22);
  background: rgba(255, 255, 255, 0.05);
  display: grid;
  place-items: center;
  color: #c5d4fb;
  font-size: 13px;
  flex-shrink: 0;
}

.msg-bubble {
  position: relative;
  border-radius: 26px;
  padding: 14px 18px;
  font-size: 16px;
  line-height: 1.75;
  border: 1px solid rgba(190, 203, 242, 0.16);
  background: linear-gradient(160deg, rgba(34, 39, 72, 0.9), rgba(24, 29, 60, 0.9));
  color: #e8efff;
  white-space: pre-wrap;
}

.msg-row.user .msg-bubble {
  background: #f4f7fb;
  color: #111a38;
  border-color: rgba(255, 255, 255, 0.86);
}

.msg-row.assistant .msg-bubble::before {
  content: '';
  position: absolute;
  left: -10px;
  top: 16px;
  width: 12px;
  height: 14px;
  background: linear-gradient(160deg, rgba(34, 39, 72, 0.9), rgba(24, 29, 60, 0.9));
  clip-path: polygon(100% 0, 0 50%, 100% 100%);
  filter: drop-shadow(-1px 0 0 rgba(190, 203, 242, 0.16));
}

.msg-row.user .msg-bubble::after {
  content: '';
  position: absolute;
  right: -10px;
  top: 16px;
  width: 12px;
  height: 14px;
  background: #f4f7fb;
  clip-path: polygon(0 0, 100% 50%, 0 100%);
  filter: drop-shadow(1px 0 0 rgba(255, 255, 255, 0.86));
}

.msg-time {
  font-size: 12px;
  color: rgba(172, 188, 227, 0.72);
  margin: 0 0 16px 40px;
}

.msg-time.user {
  text-align: right;
  margin: 0 12px 16px auto;
}

.opening-row {
  margin-top: 8px;
}

.msg-bubble.opening {
  display: flex;
  align-items: center;
  gap: 6px;
}

.typing-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #8ea9ff;
  animation: dotPulse 1s infinite ease-in-out;
}

.typing-dot:nth-child(2) {
  animation-delay: 0.2s;
}

.typing-dot:nth-child(3) {
  animation-delay: 0.4s;
}

.composer-wrap {
  flex-shrink: 0;
  padding: 14px 24px 16px;
  background: linear-gradient(180deg, rgba(3, 7, 28, 0) 0%, rgba(3, 7, 28, 0.9) 34%, rgba(3, 7, 28, 0.96) 100%);
}

.composer {
  border-radius: 36px;
  border: 1px solid rgba(178, 196, 237, 0.2);
  background: linear-gradient(160deg, rgba(31, 36, 68, 0.92), rgba(22, 27, 58, 0.9));
  display: flex;
  align-items: flex-end;
  gap: 10px;
  padding: 10px 10px 10px 14px;
}

.composer-mark {
  color: #9cb2f0;
  margin-bottom: 8px;
}

.composer :deep(.el-textarea__inner) {
  background: transparent !important;
  box-shadow: none !important;
  border: none !important;
  color: #eaf2ff !important;
  font-size: 15px;
  line-height: 1.7;
  padding: 6px 2px;
}

.send-btn {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  border: 1px solid rgba(189, 204, 241, 0.28);
  background: rgba(255, 255, 255, 0.06);
  color: #e8f1ff;
  cursor: pointer;
  display: grid;
  place-items: center;
  font-size: 20px;
}

.send-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.detail-list {
  display: grid;
  gap: 10px;
}

.detail-row {
  display: flex;
  justify-content: space-between;
  padding: 10px 12px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
  color: #dbe7ff;
}

@keyframes dotPulse {
  0%,
  80%,
  100% {
    transform: scale(0.85);
    opacity: 0.72;
  }
  40% {
    transform: scale(1.2);
    opacity: 1;
  }
}

@media (max-width: 1100px) {
  .kiki-room-page {
    height: auto;
    min-height: 100vh;
    overflow: visible;
    grid-template-columns: 1fr;
  }

  .history-pane {
    height: auto;
    min-height: auto;
    border-right: none;
    border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  }

  .history-list-wrap {
    max-height: 220px;
  }

  .msg-row {
    max-width: 92%;
  }
}
</style>
