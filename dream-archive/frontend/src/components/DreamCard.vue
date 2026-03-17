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
  padding: 20px;
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
  margin-bottom: 15px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.username {
  font-weight: 500;
  color: #eaf1ff;
}

.card-category-tag {
  background: linear-gradient(90deg, #1d4ed8, #2563eb) !important;
  border: 1px solid #60a5fa !important;
  color: #eff6ff !important;
  font-weight: 600;
}

.dream-title {
  font-size: 18px;
  font-weight: bold;
  margin-bottom: 10px;
  color: #f5f9ff;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dream-image-wrap {
  margin-bottom: 12px;
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
  color: #b7c3d9;
  line-height: 1.6;
  margin-bottom: 15px;
  height: 60px;
  overflow: hidden;
}

.dream-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 15px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}

.stats {
  display: flex;
  gap: 15px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 5px;
  color: #95a3bc;
  font-size: 14px;
}

.dream-time {
  color: #91a0b9;
  font-size: 12px;
}
</style>
