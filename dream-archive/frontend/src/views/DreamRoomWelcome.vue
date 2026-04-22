<template>
  <div class="counsel-page">
    <section class="intro-shell">
      <article class="intro-main">
        <p class="eyebrow">今日梦境 · 专属辅导空间</p>
        <h1>心理辅导室</h1>
        <p class="lead">
          先记录今天的梦境，再由心理导师智能体结合你的帖子生成开场引导。
          进入后可连续对话，陪你整理情绪、识别线索、复盘当下压力来源。
        </p>

        <div class="actions-row">
          <el-button class="enter-btn" type="primary" :loading="entering" @click="handleEnter">进入心理辅导室</el-button>
          <el-button class="record-btn" round @click="goCreateDream">先去记录梦境</el-button>
        </div>

        <div class="feature-grid">
          <article class="feature-card">
            <div class="feature-title">1. 自动检测</div>
            <p>点击进入后，系统会先检查你今天是否已发布梦境帖子。</p>
          </article>
          <article class="feature-card">
            <div class="feature-title">2. 自动开场</div>
            <p>检测到帖子后，心理导师会生成开场引导，再进入连续对话。</p>
          </article>
          <article class="feature-card">
            <div class="feature-title">3. 持续陪伴</div>
            <p>你可以围绕梦境细节追问，获得逐步深入的情绪梳理反馈。</p>
          </article>
          <article class="feature-card">
            <div class="feature-title">4. 安全守护</div>
            <p>对话过程启用内容安全策略，异常内容将触发保护机制。</p>
          </article>
        </div>
      </article>

      <aside class="intro-side">
        <h3>进入流程</h3>
        <div class="step-list">
          <div class="step-item">
            <span class="step-dot"></span>
            <span>点击“进入心理辅导室”</span>
          </div>
          <div class="step-item">
            <span class="step-dot"></span>
            <span>自动检查今天是否有梦境帖子</span>
          </div>
          <div class="step-item">
            <span class="step-dot"></span>
            <span>进入与今日帖子对应的辅导房间</span>
          </div>
        </div>

        <div class="status-card">
          <div class="status-head">
            <span>当前状态</span>
            <el-tag :type="tagType" effect="dark" round>{{ statusText }}</el-tag>
          </div>
          <p class="status-desc">{{ tip || '准备就绪，点击下方按钮即可开始。' }}</p>
        </div>

        <el-alert
          v-if="tip"
          :title="tip"
          :type="alertType"
          :closable="false"
          class="tip"
        />
      </aside>
    </section>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { dreamRoomApi } from '@/api'
import { ElMessage } from 'element-plus'

const router = useRouter()
const entering = ref(false)
const roomStatus = ref(null)
const tip = ref('')

const alertType = computed(() => {
  if (roomStatus.value === 3) return 'error'
  if (roomStatus.value === 0) return 'warning'
  return 'info'
})

const statusText = computed(() => {
  if (roomStatus.value === 3) return '服务受限'
  if (roomStatus.value === 0) return '缺少今日梦境'
  if (roomStatus.value === 1) return '正在初始化'
  if (roomStatus.value === 2) return '可进入对话'
  return '等待进入'
})

const tagType = computed(() => {
  if (roomStatus.value === 3) return 'danger'
  if (roomStatus.value === 0) return 'warning'
  if (roomStatus.value === 2) return 'success'
  return 'info'
})

const handleEnter = async () => {
  entering.value = true
  try {
    const res = await dreamRoomApi.enterRoom({})
    const data = res.data || {}
    roomStatus.value = Number(data.dream_room_status ?? 0)
    tip.value = data.tip || ''

    if (roomStatus.value === 0) {
      ElMessage.warning(data.tip || '你今天还没有发布梦境帖子，请先记录梦境')
      return
    }

    const dreamPostId = data.dream_post_id
    if (!dreamPostId) {
      ElMessage.error('未找到今日梦境帖子，请先发布后再进入')
      return
    }

    router.push(`/dream-room/chat/${dreamPostId}`)
  } catch (error) {
    ElMessage.error(error.message || '进入辅导室失败，请稍后重试')
  } finally {
    entering.value = false
  }
}

const goCreateDream = () => {
  router.push('/dream/create')
}
</script>

<style scoped>
.counsel-page {
  max-width: 1180px;
  margin: 0 auto;
  padding: 26px 0 34px;
  min-height: calc(100vh - 180px);
  display: flex;
  align-items: center;
}

.intro-shell {
  width: 100%;
  border-radius: 24px;
  border: 1px solid rgba(148, 163, 184, 0.2);
  background:
    radial-gradient(circle at 8% 4%, rgba(56, 189, 248, 0.13), transparent 34%),
    radial-gradient(circle at 96% 96%, rgba(96, 165, 250, 0.09), transparent 36%),
    linear-gradient(145deg, rgba(11, 18, 35, 0.96), rgba(14, 24, 46, 0.95));
  box-shadow: 0 22px 48px rgba(2, 8, 23, 0.34);
  backdrop-filter: blur(4px);
  display: grid;
  grid-template-columns: 1.3fr 0.7fr;
  overflow: hidden;
}

.intro-main {
  padding: 34px 34px 30px;
}

.eyebrow {
  display: inline-block;
  padding: 5px 13px;
  border-radius: 999px;
  font-size: 11px;
  color: #c8e9ff;
  letter-spacing: 0.6px;
  border: 1px solid rgba(103, 232, 249, 0.32);
  background: rgba(8, 47, 73, 0.28);
}

.intro-main h1 {
  margin: 12px 0 10px;
  color: #f4f8ff;
  font-size: 56px;
  line-height: 1.08;
  letter-spacing: 0.5px;
}

.lead {
  margin: 0;
  color: #ccd9ef;
  font-size: 16px;
  line-height: 1.9;
  max-width: 720px;
}

.actions-row {
  margin-top: 24px;
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.enter-btn {
  min-width: 208px;
  font-weight: 700;
  border-radius: 999px;
  --el-button-bg-color: #f8fbff;
  --el-button-border-color: #f8fbff;
  --el-button-text-color: #0e1834;
  --el-button-hover-bg-color: #ecf3ff;
  --el-button-hover-border-color: #ecf3ff;
  --el-button-active-bg-color: #e4edff;
  --el-button-active-border-color: #e4edff;
  box-shadow: 0 10px 24px rgba(194, 208, 255, 0.25);
}

.record-btn {
  border-color: rgba(125, 211, 252, 0.35);
  color: #d4e6ff;
  background: rgba(15, 23, 42, 0.46);
}

.feature-grid {
  margin-top: 18px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 11px;
}

.feature-card {
  border-radius: 12px;
  border: 1px solid rgba(148, 163, 184, 0.2);
  background: rgba(15, 23, 42, 0.42);
  padding: 13px 13px 12px;
  min-height: 116px;
  transition: border-color 0.2s ease, transform 0.2s ease;
}

.feature-card:hover {
  border-color: rgba(125, 211, 252, 0.4);
  transform: translateY(-2px);
}

.feature-title {
  color: #eaf2ff;
  font-size: 15px;
  font-weight: 700;
}

.feature-card p {
  margin: 7px 0 0;
  color: #becee8;
  font-size: 13px;
  line-height: 1.75;
}

.intro-side {
  border-left: 1px solid rgba(148, 163, 184, 0.16);
  background: linear-gradient(180deg, rgba(14, 22, 42, 0.88), rgba(20, 34, 64, 0.86));
  padding: 28px 22px;
}

.intro-side h3 {
  margin: 0;
  color: #f2f8ff;
  font-size: 26px;
  line-height: 1;
}

.step-list {
  margin-top: 14px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.step-item {
  border-radius: 11px;
  border: 1px solid rgba(148, 163, 184, 0.22);
  background: rgba(10, 18, 35, 0.56);
  padding: 10px 12px;
  display: flex;
  align-items: center;
  gap: 10px;
  color: #d8e6fd;
  font-size: 13px;
  line-height: 1.5;
}

.step-dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: linear-gradient(180deg, #67e8f9, #38bdf8);
  box-shadow: 0 0 12px rgba(56, 189, 248, 0.65);
}

.status-card {
  margin-top: 16px;
  border-radius: 12px;
  border: 1px solid rgba(148, 163, 184, 0.24);
  background: rgba(8, 15, 30, 0.6);
  padding: 12px;
}

.status-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.status-head span {
  color: #e8f1ff;
  font-size: 14px;
  font-weight: 600;
}

.status-desc {
  margin: 9px 0 0;
  color: #c2d2ec;
  font-size: 12px;
  line-height: 1.75;
}

.tip {
  margin-top: 12px;
}

@media (max-width: 980px) {
  .counsel-page {
    min-height: unset;
  }

  .intro-shell {
    grid-template-columns: 1fr;
  }

  .intro-side {
    border-left: none;
    border-top: 1px solid rgba(148, 163, 184, 0.2);
  }

  .feature-grid {
    grid-template-columns: 1fr;
  }

  .intro-main h1 {
    font-size: 44px;
  }

  .lead {
    font-size: 16px;
  }
}

@media (max-width: 640px) {
  .counsel-page {
    padding: 10px 0 16px;
    min-height: unset;
  }

  .intro-main {
    padding: 22px 18px;
  }

  .intro-side {
    padding: 20px 16px;
  }

  .intro-main h1 {
    font-size: 36px;
  }

  .actions-row {
    flex-direction: column;
    align-items: stretch;
  }

  .enter-btn,
  .record-btn {
    width: 100%;
  }
}
</style>
