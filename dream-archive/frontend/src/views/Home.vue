<template>
  <div class="home-page" :class="{ 'entry-sequence': playEntrySequence }">
    <section class="hero reveal-block" style="--delay: 0.06s">
      <div class="hero-overlay"></div>
      <div class="hero-content">
        <div class="hero-kicker">Dream Archive</div>
        <h1 class="hero-title">记录你的夜晚，读懂你的内心</h1>
        <p class="hero-subtitle">在这里整理梦境、标注情绪、分享感受，与同频的人交流解梦体验。</p>

        <div class="hero-actions">
          <el-button
            v-if="!isLoggedIn"
            type="primary"
            size="large"
            @click="router.push('/register')"
          >
            立即注册
          </el-button>
          <el-button
            v-else
            type="primary"
            size="large"
            @click="router.push('/dream/create')"
          >
            记录新梦境
          </el-button>
          <el-button size="large" plain @click="router.push('/explore')">探索梦境广场</el-button>
        </div>
      </div>

      <div class="hero-metrics">
        <div class="metric-card">
          <div class="metric-number">{{ categories.length }}</div>
          <div class="metric-label">梦境分类</div>
        </div>
        <div class="metric-card">
          <div class="metric-number">{{ hotDreams.length }}</div>
          <div class="metric-label">热门梦境</div>
        </div>
      </div>
    </section>

    <section class="section reveal-block" style="--delay: 0.2s">
      <div class="section-head">
        <h2>梦境分类</h2>
        <span>选择一个入口，开始探索相似梦境</span>
      </div>

      <div class="category-grid">
        <article
          v-for="(category, index) in categories"
          :key="category.id"
          class="category-card reveal-item"
          :style="{ '--item-delay': `${0.28 + index * 0.06}s` }"
          @click="exploreCategory(category.id)"
        >
          <div class="category-glow" :style="{ background: category.color || '#6b7280' }"></div>
          <div class="category-top">
            <span class="category-icon">{{ category.icon || '🌙' }}</span>
            <el-tag class="category-name-tag" size="small" effect="dark">{{ category.name }}</el-tag>
          </div>
          <div class="category-count">{{ category.dreamCount || 0 }} 条梦境</div>
          <div class="category-cta">点击查看 →</div>
        </article>
      </div>
    </section>

    <section class="section reveal-block" style="--delay: 0.34s">
      <div class="section-head">
        <h2>热门梦境</h2>
        <span>看看大家最近最有共鸣的梦</span>
      </div>

      <div class="dream-list">
        <div
          v-for="(dream, index) in hotDreams"
          :key="dream.id"
          class="dream-item reveal-item"
          :style="{ '--item-delay': `${0.44 + index * 0.08}s` }"
        >
          <DreamCard
            :dream="dream"
            @click="viewDream(dream.id)"
          />
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { categoryApi, dreamApi } from '@/api'
import { useUserStore } from '@/stores/user'
import DreamCard from '@/components/DreamCard.vue'

const router = useRouter()
const userStore = useUserStore()

const categories = ref([])
const hotDreams = ref([])
const playEntrySequence = ref(false)

const isLoggedIn = computed(() => userStore.isLoggedIn)

const loadCategories = async () => {
  try {
    const res = await categoryApi.getCategoriesWithCount()
    categories.value = Array.isArray(res.data) ? res.data : []
  } catch (error) {
    console.error('加载分类失败:', error)
  }
}

const loadHotDreams = async () => {
  try {
    const res = await dreamApi.getHotDreams(6)
    hotDreams.value = Array.isArray(res.data) ? res.data : []
  } catch (error) {
    console.error('加载热门梦境失败:', error)
  }
}

const exploreCategory = (categoryId) => {
  router.push({
    path: '/explore',
    query: { categoryId }
  })
}

const viewDream = (id) => {
  router.push(`/dream/${id}`)
}

onMounted(() => {
  if (sessionStorage.getItem('lumina-entry-sequence') === '1') {
    playEntrySequence.value = true
    sessionStorage.removeItem('lumina-entry-sequence')
  }
  loadCategories()
  loadHotDreams()
})
</script>

<style scoped>
.home-page {
  padding-bottom: 48px;
}

.entry-sequence .reveal-block,
.entry-sequence .reveal-item {
  opacity: 0;
  transform: translateY(22px) scale(0.985);
  filter: blur(3px);
  animation: revealCard 0.72s cubic-bezier(0.2, 0.72, 0.2, 1) forwards;
}

.entry-sequence .reveal-block {
  animation-delay: var(--delay, 0s);
}

.entry-sequence .reveal-item {
  animation-delay: var(--item-delay, 0s);
}

.hero {
  position: relative;
  border-radius: 24px;
  overflow: hidden;
  min-height: 300px;
  margin-bottom: 28px;
  background: radial-gradient(1200px 400px at -10% -20%, #99f6e4 0%, transparent 50%),
    radial-gradient(800px 300px at 110% 20%, #fbcfe8 0%, transparent 55%),
    linear-gradient(135deg, #0f172a 0%, #1e293b 40%, #0b3b66 100%);
}

.hero-overlay {
  position: absolute;
  inset: 0;
  background-image: linear-gradient(to right, rgba(255, 255, 255, 0.06) 1px, transparent 1px),
    linear-gradient(to bottom, rgba(255, 255, 255, 0.06) 1px, transparent 1px);
  background-size: 22px 22px;
  opacity: 0.25;
}

.hero-content {
  position: relative;
  z-index: 2;
  color: #f8fafc;
  padding: 44px 34px;
  max-width: 720px;
}

.hero-kicker {
  display: inline-block;
  font-size: 12px;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  background: rgba(255, 255, 255, 0.14);
  padding: 6px 10px;
  border-radius: 999px;
  margin-bottom: 14px;
}

.hero-title {
  font-size: 40px;
  line-height: 1.2;
  margin: 0 0 10px;
  font-weight: 800;
}

.hero-subtitle {
  margin: 0;
  line-height: 1.7;
  opacity: 0.92;
}

.hero-actions {
  margin-top: 24px;
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.hero-metrics {
  position: absolute;
  right: 24px;
  bottom: 24px;
  z-index: 2;
  display: grid;
  gap: 10px;
}

.metric-card {
  width: 120px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.14);
  border: 1px solid rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(5px);
  padding: 10px;
  text-align: center;
  color: #fff;
}

.metric-number {
  font-size: 28px;
  line-height: 1;
  font-weight: 800;
}

.metric-label {
  font-size: 12px;
  margin-top: 4px;
  opacity: 0.9;
}

.section {
  margin-bottom: 28px;
}

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 14px;
}

.section-head h2 {
  margin: 0;
  font-size: 24px;
  color: #eef3ff;
}

.section-head span {
  font-size: 13px;
  color: #9cb0cc;
}

.category-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(210px, 1fr));
  gap: 14px;
}

.category-card {
  position: relative;
  padding: 14px;
  border-radius: 14px;
  background: linear-gradient(160deg, rgba(21, 28, 41, 0.92), rgba(15, 22, 34, 0.92));
  border: 1px solid rgba(255, 255, 255, 0.1);
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  overflow: hidden;
}

.category-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 12px 25px rgba(0, 229, 255, 0.12);
}

.category-glow {
  position: absolute;
  width: 160px;
  height: 160px;
  border-radius: 50%;
  right: -60px;
  top: -70px;
  opacity: 0.16;
}

.category-top {
  position: relative;
  z-index: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.category-icon {
  font-size: 24px;
}

.category-count {
  position: relative;
  z-index: 1;
  color: #dce6f7;
  font-weight: 600;
}

.category-cta {
  position: relative;
  z-index: 1;
  color: #9eb2cf;
  margin-top: 8px;
  font-size: 13px;
}

.category-name-tag {
  background: linear-gradient(90deg, #1d4ed8, #2563eb) !important;
  border: 1px solid #60a5fa !important;
  color: #eff6ff !important;
  font-weight: 600;
}

.dream-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 18px;
}

.dream-item {
  min-width: 0;
}

@keyframes revealCard {
  0% {
    opacity: 0;
    transform: translateY(22px) scale(0.985);
    filter: blur(3px);
  }
  100% {
    opacity: 1;
    transform: translateY(0) scale(1);
    filter: blur(0);
  }
}

@media (max-width: 768px) {
  .hero {
    min-height: 340px;
  }

  .hero-title {
    font-size: 30px;
  }

  .hero-content {
    padding: 28px 20px;
  }

  .hero-metrics {
    position: static;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    padding: 0 20px 20px;
  }

  .metric-card {
    width: auto;
  }

  .section-head {
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
  }

  .dream-list {
    grid-template-columns: 1fr;
  }
}
</style>
