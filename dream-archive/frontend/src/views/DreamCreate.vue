<template>
  <div class="dream-create-container">
    <div class="dream-create-card">
      <div class="create-header">
        <h2>{{ isEdit ? '编辑梦境' : '记录新梦境' }}</h2>
        <p>{{ isEdit ? '修改你的梦境记录' : '记录你的梦境并分享给大家' }}</p>
      </div>

      <el-form
        ref="dreamFormRef"
        :model="dreamForm"
        :rules="rules"
        label-width="100px"
        class="dream-form"
      >
        <el-form-item label="梦境标题" prop="title">
          <el-input
            v-model="dreamForm.title"
            placeholder="给你的梦境起个标题"
            maxlength="100"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="梦境分类" prop="categoryId">
          <el-select v-model="dreamForm.categoryId" placeholder="选择梦境类型" style="width: 100%">
            <el-option
              v-for="category in categories"
              :key="category.id"
              :label="`${category.icon || ''} ${category.name}`"
              :value="category.id"
            >
              <span>{{ category.icon }}</span>
              <span style="margin-left: 10px">{{ category.name }}</span>
              <span style="color: #a9b9d3; margin-left: 10px; font-size: 12px">{{ category.description }}</span>
            </el-option>
          </el-select>
        </el-form-item>

        <el-form-item label="梦境日期" prop="dreamDate">
          <el-date-picker
            v-model="dreamForm.dreamDate"
            type="date"
            placeholder="选择做梦日期"
            style="width: 100%"
            :disabled-date="disabledDate"
          />
        </el-form-item>

        <el-form-item label="梦境内容" prop="content">
          <el-input
            v-model="dreamForm.content"
            type="textarea"
            :rows="10"
            placeholder="详细描述你的梦境..."
            maxlength="5000"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="梦境图片">
          <el-upload
            :show-file-list="false"
            :auto-upload="false"
            :on-change="handleImageSelected"
            accept="image/*"
          >
            <el-button :loading="imageUploading">上传图片</el-button>
          </el-upload>
          <div class="image-preview-list" v-if="dreamForm.imageUrls.length">
            <div
              class="image-preview-item"
              v-for="(url, idx) in dreamForm.imageUrls"
              :key="`${url}-${idx}`"
            >
              <img :src="toDisplayUrl(url)" alt="dream image" />
              <el-button
                circle
                type="danger"
                size="small"
                class="remove-image"
                @click="removeImage(idx)"
              >
                x
              </el-button>
            </div>
          </div>
        </el-form-item>

        <el-form-item label="情绪评分" prop="moodScore">
          <el-rate v-model="dreamForm.moodScore" :texts="['很糟糕', '不太好', '一般', '不错', '很愉快']" show-text />
        </el-form-item>

        <el-form-item label="标签">
          <el-tag
            v-for="tag in dreamForm.tagList"
            :key="tag"
            closable
            @close="handleRemoveTag(tag)"
            style="margin-right: 10px"
          >
            {{ tag }}
          </el-tag>
          <el-input
            v-if="tagInputVisible"
            ref="tagInputRef"
            v-model="tagInputValue"
            size="small"
            style="width: 120px"
            @keyup.enter="handleAddTag"
            @blur="handleAddTag"
          />
          <el-button v-else size="small" @click="showTagInput">+ 添加标签</el-button>
        </el-form-item>

        <el-form-item label="公开设置" prop="isPublic">
          <el-radio-group v-model="dreamForm.isPublic">
            <el-radio :label="1">公开</el-radio>
            <el-radio :label="0">私密</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" size="large" :loading="loading" @click="handleSubmit">
            {{ loading ? '保存中...' : (isEdit ? '保存修改' : '发布梦境') }}
          </el-button>
          <el-button size="large" @click="handleCancel">取消</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { dreamApi, categoryApi } from '@/api'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const dreamFormRef = ref(null)
const tagInputRef = ref(null)
const loading = ref(false)
const imageUploading = ref(false)
const isEdit = ref(false)
const categories = ref([])

const dreamForm = reactive({
  title: '',
  categoryId: null,
  dreamDate: new Date(),
  content: '',
  moodScore: 3,
  tagList: [],
  imageUrls: [],
  isPublic: 1
})

const tagInputVisible = ref(false)
const tagInputValue = ref('')

const rules = {
  title: [
    { required: true, message: '请输入梦境标题', trigger: 'blur' },
    { min: 2, max: 100, message: '标题长度在 2 到 100 个字符', trigger: 'blur' }
  ],
  categoryId: [{ required: true, message: '请选择梦境分类', trigger: 'change' }],
  content: [
    { required: true, message: '请输入梦境内容', trigger: 'blur' },
    { min: 10, max: 5000, message: '内容长度在 10 到 5000 个字符', trigger: 'blur' }
  ],
  dreamDate: [{ required: true, message: '请选择做梦日期', trigger: 'change' }]
}

const disabledDate = (time) => time.getTime() > Date.now()

const loadCategories = async () => {
  try {
    const res = await categoryApi.getAllCategories()
    categories.value = res.data || []
  } catch (error) {
    console.error('加载分类失败:', error)
  }
}

const showTagInput = () => {
  tagInputVisible.value = true
  nextTick(() => tagInputRef.value?.focus())
}

const handleAddTag = () => {
  const tag = tagInputValue.value.trim()
  if (tag && !dreamForm.tagList.includes(tag)) {
    if (dreamForm.tagList.length >= 5) {
      ElMessage.warning('最多只能添加 5 个标签')
      return
    }
    dreamForm.tagList.push(tag)
  }
  tagInputVisible.value = false
  tagInputValue.value = ''
}

const handleRemoveTag = (tag) => {
  dreamForm.tagList = dreamForm.tagList.filter((t) => t !== tag)
}

const toDisplayUrl = (url) => {
  if (!url) return url
  if (url.startsWith('/uploads/')) return `/api${url}`
  return url
}

const normalizeStoreUrl = (url) => {
  if (!url) return url
  if (url.startsWith('/api/uploads/')) return url.replace('/api/uploads/', '/uploads/')
  return url
}

const handleImageSelected = async (uploadFile) => {
  if (!uploadFile?.raw) return
  imageUploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', uploadFile.raw)
    const res = await dreamApi.uploadDreamImage(formData)
    const imageUrl = normalizeStoreUrl(res.data)
    if (!dreamForm.imageUrls.includes(imageUrl)) {
      dreamForm.imageUrls.push(imageUrl)
    }
    ElMessage.success('图片上传成功')
  } catch (error) {
    ElMessage.error(error.message || '图片上传失败')
  } finally {
    imageUploading.value = false
  }
}

const removeImage = (idx) => {
  dreamForm.imageUrls.splice(idx, 1)
}

const handleSubmit = async () => {
  if (!dreamFormRef.value) return

  await dreamFormRef.value.validate(async (valid) => {
    if (!valid) return

    loading.value = true
    try {
      if (!userStore.userId) {
        ElMessage.error('请先登录后再操作')
        router.push('/login')
        return
      }

      const dreamDateValue =
        dreamForm.dreamDate instanceof Date
          ? dreamForm.dreamDate.toISOString().split('T')[0]
          : dreamForm.dreamDate

      const data = {
        ...dreamForm,
        userId: Number(userStore.userId),
        tags: dreamForm.tagList.join(','),
        imageUrls: dreamForm.imageUrls.map((u) => normalizeStoreUrl(u)),
        dreamDate: dreamDateValue
      }

      if (isEdit.value) {
        await dreamApi.updateDream(route.params.id, data)
        ElMessage.success('梦境修改成功')
      } else {
        await dreamApi.createDream(data)
        ElMessage.success('梦境发布成功')
      }

      router.push(`/profile/${userStore.userId}`)
    } catch (error) {
      ElMessage.error(error.message || '操作失败，请稍后重试')
    } finally {
      loading.value = false
    }
  })
}

const handleCancel = () => {
  router.back()
}

onMounted(() => {
  loadCategories()
  if (route.params.id) {
    isEdit.value = true
    dreamApi.getDreamById(route.params.id).then((res) => {
      const d = res.data
      dreamForm.title = d.title || ''
      dreamForm.categoryId = d.categoryId ?? null
      dreamForm.dreamDate = d.dreamDate ? new Date(d.dreamDate) : new Date()
      dreamForm.content = d.content || ''
      dreamForm.moodScore = d.moodScore || 3
      dreamForm.tagList = d.tags ? d.tags.split(',').filter(Boolean) : []
      dreamForm.imageUrls = d.imageUrls || []
      dreamForm.isPublic = d.isPublic ?? 1
    }).catch(() => {
      ElMessage.error('加载梦境详情失败')
      router.push('/')
    })
  }
})
</script>

<style scoped>
.dream-create-container {
  max-width: 900px;
  margin: 0 auto;
  padding: 20px;
}

.dream-create-card {
  background: linear-gradient(160deg, rgba(20, 27, 40, 0.92), rgba(15, 22, 34, 0.92));
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 20px;
  padding: 40px;
  box-shadow: 0 16px 40px rgba(0, 0, 0, 0.36);
}

.create-header {
  text-align: center;
  margin-bottom: 40px;
}

.create-header h2 {
  font-size: 32px;
  font-weight: bold;
  color: #eef4ff;
  margin-bottom: 10px;
}

.create-header p {
  color: #a4b5cf;
  font-size: 14px;
}

.dream-form {
  margin-top: 30px;
}

.image-preview-list {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 10px;
}

.image-preview-item {
  position: relative;
  width: 110px;
  height: 110px;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.12);
}

.image-preview-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.remove-image {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 22px;
  height: 22px;
  min-height: 22px;
  padding: 0;
}

:deep(.el-form-item__label) {
  font-weight: 600;
  color: #d3e0f4;
}

:deep(.el-textarea__inner) {
  font-family: inherit;
  line-height: 1.8;
}
</style>
