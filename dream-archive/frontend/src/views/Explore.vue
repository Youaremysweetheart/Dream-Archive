<template>
  <div class="explore-container">
    <el-card class="filter-card">
      <div class="filter-content">
        <div class="filter-item">
          <span class="filter-label">分类：</span>
          <el-radio-group v-model="filters.categoryId" @change="handleFilter">
            <el-radio-button :label="null">全部</el-radio-button>
            <el-radio-button
              v-for="category in categories"
              :key="category.id"
              :label="category.id"
            >
              {{ category.icon }} {{ category.name }}
            </el-radio-button>
          </el-radio-group>
        </div>

        <div class="filter-item">
          <el-input
            v-model="filters.keyword"
            placeholder="搜索梦境标题或内容..."
            clearable
            style="width: 320px"
            @clear="handleFilter"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
            <template #append>
              <el-button @click="handleFilter">
                <el-icon><Search /></el-icon>
              </el-button>
            </template>
          </el-input>
        </div>
      </div>
    </el-card>

    <div class="dream-section">
      <div class="section-header">
        <h2>探索梦境世界</h2>
        <span class="dream-count">共 {{ total }} 个梦境</span>
      </div>

      <el-skeleton v-if="loading" :rows="3" animated />

      <div v-else class="dream-list">
        <DreamCard
          v-for="dream in dreams"
          :key="dream.id"
          :dream="dream"
          @click="router.push(`/dream/${dream.id}`)"
        />

        <el-empty v-if="dreams.length === 0" description="暂无梦境，快来记录第一个吧">
          <el-button type="primary" @click="router.push('/dream/create')">记录梦境</el-button>
        </el-empty>
      </div>

      <el-pagination
        v-if="total > 0"
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 30, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handlePageSizeChange"
        @current-change="handlePageChange"
        class="pagination"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { dreamApi, categoryApi } from '@/api'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import DreamCard from '@/components/DreamCard.vue'

const router = useRouter()
const route = useRoute()

const loading = ref(false)
const dreams = ref([])
const categories = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

const filters = reactive({
  categoryId: null,
  keyword: ''
})

const loadCategories = async () => {
  try {
    const res = await categoryApi.getCategoriesWithCount()
    categories.value = res.data
  } catch (error) {
    console.error('加载分类失败:', error)
  }
}

const loadDreams = async () => {
  loading.value = true
  try {
    const res = await dreamApi.getPublicDreams({
      categoryId: filters.categoryId,
      keyword: filters.keyword,
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })

    dreams.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (error) {
    ElMessage.error('加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const handleFilter = () => {
  pageNum.value = 1
  loadDreams()
}

const handlePageChange = (page) => {
  pageNum.value = page
  loadDreams()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const handlePageSizeChange = (size) => {
  pageSize.value = size
  pageNum.value = 1
  loadDreams()
}

onMounted(() => {
  loadCategories()

  if (route.query.categoryId) {
    filters.categoryId = Number(route.query.categoryId)
  }

  loadDreams()
})
</script>

<style scoped>
.explore-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.filter-card {
  margin-bottom: 20px;
  border-radius: 15px;
}

.filter-content {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.filter-item {
  display: flex;
  align-items: center;
  gap: 14px;
}

.filter-label {
  font-weight: 600;
  color: #d8e2f3;
  white-space: nowrap;
}

.dream-section {
  background: rgba(22, 28, 40, 0.82);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 15px;
  padding: 30px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.35);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
  padding-bottom: 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.section-header h2 {
  font-size: 38px;
  font-weight: 800;
  color: #eef3ff;
}

.dream-count {
  color: #9eb0cc;
  font-size: 14px;
}

.dream-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 20px;
  min-height: 200px;
}

.pagination {
  margin-top: 30px;
  display: flex;
  justify-content: center;
}

@media (max-width: 768px) {
  .filter-item {
    flex-direction: column;
    align-items: flex-start;
  }

  .dream-list {
    grid-template-columns: 1fr;
  }

  .section-header h2 {
    font-size: 30px;
  }
}
</style>
