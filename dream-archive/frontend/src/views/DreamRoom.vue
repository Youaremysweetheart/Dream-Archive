<template>
  <div class="kiki-room-page">
    <aside class="history-pane">
      <button class="brand-block" type="button" @click="router.push('/')">
        <img class="brand-logo" :src="somniumLogo" alt="Somnium" />
        <div class="brand-copy">
          <p class="brand-name">Somnium Dream Archive</p>
          <p class="brand-subtitle">Mental Assistant</p>
        </div>
      </button>

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
        <button class="side-action primary" type="button" @click="router.push('/dream/create')">记录梦境</button>
        <button class="side-action secondary" type="button" @click="router.push('/dream-room')">返回</button>
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

        <div v-if="showThinkingBubble" class="msg-row assistant opening-row">
          <div class="msg-side-icon thinking-icon">
            <span class="thinking-ring"></span>
          </div>
          <div class="msg-bubble opening thinking">
            <span class="typing-dot"></span>
            <span class="typing-dot"></span>
            <span class="typing-dot"></span>
            KiKi 正在思考你的这条消息...
          </div>
        </div>

        <el-empty
          v-if="visibleMessages.length === 0 && !showOpeningBubble && !showThinkingBubble"
          :description="searchKeyword ? '未找到匹配内容' : '还没有消息，试着和 KiKi 打个招呼吧'"
        />
      </main>

      <footer class="composer-wrap">
        <div ref="composerShellRef" class="composer-shell">
          <div v-if="emojiPanelVisible" class="emoji-panel">
            <button
              v-for="emoji in emojiOptions"
              :key="emoji"
              type="button"
              class="emoji-item"
              @click="appendEmoji(emoji)"
            >
              {{ emoji }}
            </button>
          </div>

          <div class="composer">
            <div class="composer-left-tools">
              <button
                class="emoji-trigger"
                type="button"
                :disabled="!canSend"
                aria-label="打开表情面板"
                @click="toggleEmojiPanel"
              >
                <svg class="emoji-trigger-icon" viewBox="0 0 24 24" aria-hidden="true">
                  <circle cx="12" cy="12" r="8.5"></circle>
                  <circle cx="9" cy="10" r="1.1" fill="currentColor" stroke="none"></circle>
                  <circle cx="15" cy="10" r="1.1" fill="currentColor" stroke="none"></circle>
                  <path d="M8.4 13.4C9.15 14.55 10.4 15.2 12 15.2C13.6 15.2 14.85 14.55 15.6 13.4"></path>
                </svg>
              </button>
            </div>

            <el-input
              ref="composerInputRef"
              v-model="inputText"
              type="textarea"
              :autosize="{ minRows: 1, maxRows: 4 }"
              maxlength="800"
              :disabled="!canSend"
              :placeholder="placeholderText"
              @keydown.enter.ctrl.prevent="sendMessage"
            />
          </div>

          <button class="send-btn outer-send" type="button" :disabled="!canSend || sending" @click="sendMessage">
            <el-icon><ArrowUpBold /></el-icon>
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowUpBold, MoreFilled, Search } from '@element-plus/icons-vue'
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
const hasPendingReply = ref(false)
const messageListRef = ref(null)
const composerShellRef = ref(null)
const composerInputRef = ref(null)
const pollTimer = ref(null)
const pollEpoch = ref(0)
const aiApiError = ref(false)
const violationDialogShownKey = ref('')

const historyLoading = ref(false)
const historyPosts = ref([])

const searchVisible = ref(false)
const searchKeyword = ref('')
const searchInputRef = ref(null)
const emojiPanelVisible = ref(false)

const detailVisible = ref(false)

const emojiOptions = ['🙂', '😊', '🥺', '😌', '😴', '😢', '😭', '😔', '😕', '😳', '😮', '😣', '😖', '😡', '🤍', '💙', '✨', '🌙', '🌧️', '🌊', '🫧', '🕯️', '🫂', '💭']

const canSend = computed(() => roomStatus.value === 2 && !aiApiError.value && !hasPendingReply.value)
const showOpeningBubble = computed(() => roomStatus.value === 1 && !messages.value.some((msg) => Number(msg.senderId) === 0))
const showThinkingBubble = computed(() => roomStatus.value === 2 && hasPendingReply.value)

const statusText = computed(() => {
  if (aiApiError.value) return 'ARCHIVE INACTIVE'
  if (hasPendingReply.value && roomStatus.value === 2) return 'KIKI IS THINKING'
  if (roomStatus.value === 1 || roomStatus.value === 2) return 'ARCHIVE ACTIVE'
  if (roomStatus.value === 3) return 'ARCHIVE DISABLED'
  return 'ARCHIVE INACTIVE'
})

const statusClass = computed(() => {
  if (aiApiError.value) return 'inactive'
  if (hasPendingReply.value && roomStatus.value === 2) return 'thinking'
  if (roomStatus.value === 1 || roomStatus.value === 2) return 'active'
  if (roomStatus.value === 3) return 'disabled'
  return 'inactive'
})

const placeholderText = computed(() => {
  if (aiApiError.value) return '智能体接口异常，请稍后重试'
  if (roomStatus.value === 3) return '当前辅导室不可用'
  if (roomStatus.value === 1) return 'KiKi 正在生成开场引导...'
  if (hasPendingReply.value) return 'KiKi 正在思考，请稍候...'
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

const shouldPoll = () => roomStatus.value === 1 || hasPendingReply.value

const stopPolling = () => {
  if (pollTimer.value) {
    clearTimeout(pollTimer.value)
    pollTimer.value = null
  }
}

const schedulePolling = () => {
  stopPolling()
  if (!shouldPoll()) return
  const currentEpoch = pollEpoch.value
  pollTimer.value = setTimeout(async () => {
    if (currentEpoch !== pollEpoch.value) return
    await poll()
    if (currentEpoch !== pollEpoch.value) return
    schedulePolling()
  }, 3000)
}

const focusComposer = async () => {
  await nextTick()
  composerInputRef.value?.focus?.()
}

const toggleEmojiPanel = async () => {
  emojiPanelVisible.value = !emojiPanelVisible.value
  if (!emojiPanelVisible.value) {
    await focusComposer()
  }
}

const appendEmoji = async (emoji) => {
  inputText.value = `${inputText.value || ''}${emoji}`
  emojiPanelVisible.value = false
  await focusComposer()
}

const handleDocumentClick = (event) => {
  if (!emojiPanelVisible.value) return
  const shell = composerShellRef.value
  if (!shell) return
  if (shell.contains(event.target)) return
  emojiPanelVisible.value = false
}

const enterRoom = async () => {
  const previousStatus = roomStatus.value
  const requestBody = { dream_post_id: dreamPostId.value || null }
  const res = dreamPostId.value > 0
    ? await dreamRoomApi.enterRoomByPost(requestBody)
    : await dreamRoomApi.enterRoom(requestBody)
  const resolvedPostId = Number(res.data?.dream_post_id || 0)
  if (resolvedPostId > 0) {
    dreamPostId.value = resolvedPostId
  }
  roomId.value = res.data?.dream_room_id || ''
  roomStatus.value = Number(res.data?.dream_room_status || 0)
  tip.value = res.data?.tip || ''
  hasPendingReply.value = Boolean(res.data?.has_pending_reply)
  aiApiError.value = false

  const currentDialogKey = `${roomId.value}_${roomStatus.value}_${tip.value || ''}`
  if (
    roomStatus.value === 3 &&
    tip.value &&
    (previousStatus !== 3 || violationDialogShownKey.value !== currentDialogKey)
  ) {
    violationDialogShownKey.value = currentDialogKey
    await ElMessageBox.alert(tip.value, '辅导室提示', {
      confirmButtonText: '我知道了',
      type: 'warning',
      customClass: 'dream-room-warning-dialog',
      autofocus: false
    })
    router.push({
      path: '/dream-room',
      query: {
        violation: '1',
        tip: tip.value,
        source: 'dify_violation'
      }
    })
  }
}

const loadMessages = async () => {
  if (!roomId.value) return
  const res = await dreamRoomApi.getMessages({
    dream_room_id: roomId.value,
    pageNum: 1,
    pageSize: 80
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
  pollEpoch.value += 1
  stopPolling()
  roomId.value = ''
  roomStatus.value = 0
  tip.value = ''
  messages.value = []
  inputText.value = ''
  hasPendingReply.value = false
  emojiPanelVisible.value = false
  violationDialogShownKey.value = ''
  searchKeyword.value = ''

  try {
    await enterRoom()
    await loadMessages()
    schedulePolling()
  } catch (error) {
    aiApiError.value = true
    ElMessage.error(error.message || '连接心理导师失败，请稍后重试')
  }
}

const poll = async () => {
  if (!shouldPoll()) {
    stopPolling()
    return
  }
  try {
    const previousStatus = roomStatus.value
    const previousPendingReply = hasPendingReply.value
    await enterRoom()
    const finishedWaiting = (previousStatus === 1 && roomStatus.value !== 1)
      || (previousPendingReply && !hasPendingReply.value)
      || roomStatus.value === 3

    if (finishedWaiting) {
      await loadMessages()
    }

    if (!shouldPoll()) {
      stopPolling()
    }
  } catch (error) {
    aiApiError.value = true
    stopPolling()
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
    hasPendingReply.value = true
    emojiPanelVisible.value = false
    messages.value.push({
      id: `local_${Date.now()}`,
      senderId: userStore.userId,
      messageText: text,
      createTime: new Date().toISOString()
    })
    await nextTick()
    scrollToBottom()
    schedulePolling()
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
  document.addEventListener('click', handleDocumentClick)
  await loadHistoryPosts()
  await resetConversation()
})

onBeforeUnmount(() => {
  document.removeEventListener('click', handleDocumentClick)
  stopPolling()
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
  width: 100%;
  border: none;
  background: transparent;
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 24px 18px 18px;
  cursor: pointer;
  text-align: left;
  transition: transform 0.2s ease, opacity 0.2s ease;
}

.brand-block:hover {
  transform: translateY(-1px);
}

.brand-block:focus-visible {
  outline: 1px solid rgba(143, 132, 255, 0.45);
  outline-offset: -1px;
  border-radius: 16px;
}

.brand-logo {
  width: 30px;
  height: 30px;
  flex-shrink: 0;
  margin-top: 4px;
  filter: drop-shadow(0 2px 10px rgba(140, 123, 255, 0.35));
}

.brand-copy {
  min-width: 0;
}

.brand-name {
  margin: 0;
  font-size: 19px;
  line-height: 1.05;
  font-weight: 700;
  letter-spacing: 0.15px;
  font-family: Georgia, 'Times New Roman', 'Noto Serif SC', serif;
  background: linear-gradient(92deg, #9af2ff 0%, #8f84ff 45%, #ff6bb1 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  text-shadow: 0 0 18px rgba(130, 120, 255, 0.16);
}

.brand-subtitle {
  margin: 4px 0 0;
  color: #d7e2f8;
  font-size: 12px;
  line-height: 1.3;
  letter-spacing: 1.2px;
  text-transform: uppercase;
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
  color: #d4e1f8;
  letter-spacing: 1px;
}

.history-tip {
  margin: 0 8px;
  color: #d9e6fb;
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
  color: #cddbf4;
  font-size: 12px;
}

.history-footer {
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  padding: 16px 14px 18px;
  display: flex;
  gap: 10px;
}

.side-action {
  flex: 1;
  border-radius: 10px;
  border: 1px solid rgba(158, 179, 232, 0.3);
  background: rgba(255, 255, 255, 0.03);
  color: #dce8ff;
  height: 42px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 0.4px;
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease, background 0.18s ease;
}

.side-action:hover {
  transform: translateY(-1px);
}

.side-action.primary {
  border-color: rgba(138, 164, 255, 0.38);
  background: linear-gradient(180deg, rgba(70, 84, 146, 0.9), rgba(51, 61, 111, 0.95));
  color: #f2f6ff;
  box-shadow: 0 10px 22px rgba(32, 44, 101, 0.24);
}

.side-action.secondary {
  border-color: rgba(163, 183, 240, 0.26);
  background: rgba(255, 255, 255, 0.02);
  color: #d6e3fb;
}

.side-action.secondary:hover {
  border-color: rgba(194, 209, 255, 0.38);
  background: rgba(255, 255, 255, 0.05);
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

.mentor-status.thinking {
  color: #8fb4ff;
}

.mentor-status.disabled {
  color: #ff8ca6;
}

.mentor-status.inactive {
  color: #c4d3ef;
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
  color: #d0def7;
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

.msg-side-icon.thinking-icon {
  position: relative;
  color: transparent;
}

.thinking-ring {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  border: 2px solid rgba(169, 191, 255, 0.28);
  border-top-color: #a9bfff;
  animation: spinRing 0.9s linear infinite;
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
  color: #d0ddf6;
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

.msg-bubble.thinking {
  color: #dfe8ff;
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
  padding: 14px 24px 18px;
  background: linear-gradient(180deg, rgba(3, 7, 28, 0) 0%, rgba(3, 7, 28, 0.9) 34%, rgba(3, 7, 28, 0.96) 100%);
}

.composer-shell {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
}

.emoji-panel {
  position: absolute;
  left: 0;
  bottom: calc(100% + 12px);
  width: 312px;
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 8px;
  padding: 12px;
  border-radius: 20px;
  border: 1px solid rgba(203, 214, 242, 0.14);
  background: linear-gradient(180deg, rgba(33, 38, 57, 0.98), rgba(26, 31, 48, 0.98));
  box-shadow: 0 18px 44px rgba(0, 0, 0, 0.34);
  z-index: 4;
}

.emoji-item {
  width: 100%;
  aspect-ratio: 1;
  border: none;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.04);
  color: #eff4ff;
  font-size: 20px;
  cursor: pointer;
  transition: transform 0.15s ease, background 0.15s ease;
}

.emoji-item:hover {
  background: rgba(255, 255, 255, 0.08);
  transform: translateY(-1px);
}

.composer {
  border-radius: 999px;
  border: 1px solid rgba(203, 214, 242, 0.16);
  background: linear-gradient(180deg, rgba(38, 43, 60, 0.95), rgba(32, 37, 56, 0.94));
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.06),
    0 12px 30px rgba(0, 0, 0, 0.22);
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 60px;
  padding: 0 12px;
}

.composer-left-tools,
.composer-right-tools {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
  height: 60px;
}

.composer-right-tools {
  display: none;
}

.emoji-trigger {
  width: 44px;
  height: 44px;
  border: none;
  border-radius: 50%;
  display: grid;
  place-items: center;
  background: transparent;
  color: rgba(205, 216, 243, 0.72);
  cursor: pointer;
  align-self: center;
  transition: background 0.15s ease, color 0.15s ease, transform 0.15s ease;
}

.emoji-trigger:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.06);
  color: #eff4ff;
  transform: translateY(-1px);
}

.emoji-trigger:disabled {
  cursor: default;
}

.emoji-trigger-face {
  display: none;
}

.emoji-trigger-icon {
  width: 24px;
  height: 24px;
  stroke: currentColor;
  stroke-width: 1.8;
  fill: none;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.composer :deep(.el-textarea) {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  height: 60px;
}

.composer :deep(.el-textarea__wrapper) {
  box-shadow: none !important;
  background: transparent !important;
  padding: 0 !important;
  display: flex !important;
  align-items: center !important;
  min-height: 60px !important;
}

.composer :deep(.el-textarea__inner) {
  background: transparent !important;
  box-shadow: none !important;
  border: none !important;
  color: #edf4ff !important;
  font-size: 16px;
  line-height: 1.45;
  min-height: 24px !important;
  padding: 18px 0 16px !important;
  resize: none !important;
}

.composer :deep(.el-textarea__inner::placeholder) {
  color: #b8c7e4 !important;
}

.send-btn {
  width: 46px;
  height: 46px;
  flex-shrink: 0;
  border-radius: 50%;
  border: none;
  background: linear-gradient(180deg, #ffd54f 0%, #ffbf0b 100%);
  color: #1d2030;
  cursor: pointer;
  display: grid;
  place-items: center;
  font-size: 18px;
  box-shadow:
    0 10px 22px rgba(255, 191, 11, 0.26),
    inset 0 1px 0 rgba(255, 255, 255, 0.42);
  transition: transform 0.18s ease, box-shadow 0.18s ease, opacity 0.18s ease;
}

.outer-send {
  align-self: center;
  margin-top: 0;
}

.send-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow:
    0 14px 28px rgba(255, 191, 11, 0.34),
    inset 0 1px 0 rgba(255, 255, 255, 0.48);
}

.send-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
  transform: none;
  box-shadow:
    0 8px 18px rgba(255, 191, 11, 0.12),
    inset 0 1px 0 rgba(255, 255, 255, 0.24);
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

.detail-row span {
  color: #d7e5ff;
}

.detail-row strong {
  color: #f4f8ff;
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

@keyframes spinRing {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
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

  .history-footer {
    flex-direction: column;
  }

  .history-list-wrap {
    max-height: 220px;
  }

  .msg-row {
    max-width: 92%;
  }
}
</style>

<style>
.dream-room-warning-dialog {
  border-radius: 20px;
  background: linear-gradient(180deg, #1b2038 0%, #141a31 100%);
  border: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow: 0 24px 60px rgba(0, 0, 0, 0.38);
}

.dream-room-warning-dialog .el-message-box__title,
.dream-room-warning-dialog .el-message-box__message {
  color: #eef4ff;
}

.dream-room-warning-dialog .el-message-box__headerbtn .el-message-box__close {
  color: rgba(219, 231, 255, 0.72);
}

.dream-room-warning-dialog .el-button--primary {
  border: none;
  border-radius: 12px;
  background: linear-gradient(180deg, #ffd54f 0%, #ffbf0b 100%);
  color: #1d2030;
}
</style>
