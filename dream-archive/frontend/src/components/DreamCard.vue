<template>
  <div class="dream-card" @click="$emit('click')">
    <div class="dream-header">
      <div class="user-info">
        <el-avatar :src="avatarUrl" :size="32">
          {{ dream.username }}
        </el-avatar>
        <span class="username">{{ dream.username }}</span>
      </div>
      <el-tag class="card-category-tag" size="small">
        {{ dream.categoryName }}
      </el-tag>
    </div>

    <h3 class="dream-title">{{ dream.title }}</h3>

    <div v-if="coverImage" class="dream-image-wrap">
      <img class="dream-image" :src="toDisplayUrl(coverImage)" alt="dream cover" />
    </div>

    <p class="dream-content">{{ truncateContent(dream.content) }}</p>

    <div class="dream-footer">
      <div class="stats">
        <span class="stat-item">
          <el-icon><View /></el-icon>
          {{ dream.viewCount }}
        </span>
        <span class="stat-item">
          <el-icon><Star /></el-icon>
          {{ dream.likeCount }}
        </span>
        <span class="stat-item">
          <el-icon><ChatDotRound /></el-icon>
          {{ getCommentCount(dream) }}
        </span>
      </div>
      <div class="dream-time">
        {{ formatTime(dream.createTime) }}
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { View, Star, ChatDotRound } from '@element-plus/icons-vue'

const props = defineProps({
  dream: {
    type: Object,
    required: true
  }
})

defineEmits(['click'])

const truncateContent = (content) => {
  if (!content) return ''
  return content.length > 100 ? content.substring(0, 100) + '...' : content
}

const toDisplayUrl = (url) => {
  if (!url) return ''
  if (url.startsWith('/uploads/')) return `/api${url}`
  return url
}

const parseImages = (images) => {
  if (!images || typeof images !== 'string') return []
  let raw = images.trim()
  if (!raw) return []
  if (raw.startsWith('[') && raw.endsWith(']')) {
    raw = raw.slice(1, -1)
  }

  return raw
    .split(',')
    .map((s) => s.trim().replace(/^"/, '').replace(/"$/, ''))
    .filter(Boolean)
}

const coverImage = computed(() => {
  const list = Array.isArray(props.dream?.imageUrls) ? props.dream.imageUrls : parseImages(props.dream?.images)
  return list.length ? list[0] : ''
})

const avatarUrl = computed(() => {
  const raw = props.dream?.userAvatar ?? props.dream?.user_avatar ?? ''
  return toDisplayUrl(raw)
})

const getCommentCount = (item) => {
  if (!item) return 0
  return Number(item.commentCount ?? item.comment_count ?? 0)
}

const formatTime = (time) => {
  const date = new Date(time)
  const now = new Date()
  const diff = now - date

  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  if (diff < 2592000000) return `${Math.floor(diff / 86400000)}天前`

  return date.toLocaleDateString()
}
</script>

<style scoped>
.dream-card {
  background: linear-gradient(160deg, rgba(16, 22, 36, 0.95), rgba(10, 14, 24, 0.95));
  border-radius: 15px;
  padding: 22px;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 10px 28px rgba(0, 0, 0, 0.4);
  border: 1px solid rgba(255, 255, 255, 0.09);
}

.dream-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 14px 30px rgba(0, 229, 255, 0.12);
  border-color: rgba(0, 229, 255, 0.4);
}

.dream-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.username {
  font-size: 15px;
  font-weight: 600;
  color: #f0f6ff;
}

.card-category-tag {
  background: linear-gradient(90deg, #1d4ed8, #2563eb) !important;
  border: 1px solid #60a5fa !important;
  color: #eff6ff !important;
  font-weight: 600;
  font-size: 12px;
}

.dream-title {
  font-size: 22px;
  line-height: 1.35;
  font-weight: 700;
  margin-bottom: 12px;
  color: #f7fbff;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  min-height: 58px;
}

.dream-image-wrap {
  margin-bottom: 14px;
  border-radius: 10px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.dream-image {
  width: 100%;
  height: 180px;
  object-fit: cover;
  display: block;
}

.dream-content {
  color: #d5e1f3;
  font-size: 15px;
  line-height: 1.8;
  margin-bottom: 16px;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  line-clamp: 3;
  -webkit-box-orient: vertical;
  min-height: 82px;
  word-break: break-word;
}

.dream-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 14px;
  border-top: 1px solid rgba(255, 255, 255, 0.12);
}

.stats {
  display: flex;
  gap: 15px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 5px;
  color: #b8c8e3;
  font-size: 13px;
  font-weight: 500;
}

.dream-time {
  color: #b2c1da;
  font-size: 13px;
}
</style>
