import request from '@/utils/request'

export const userApi = {
  login(data) {
    return request.post('/user/login', data)
  },
  register(data) {
    return request.post('/user/register', data)
  },
  getUserById(id) {
    return request.get(`/user/${id}`)
  },
  updateProfile(data) {
    return request.put('/user/profile', data)
  },
  // Backward compatibility for existing callers.
  updateUser(data) {
    return request.put('/user/profile', data)
  },
  uploadAvatar(formData) {
    return request.post('/user/avatar', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
  }
}

export const dreamApi = {
  getHotDreams(limit = 10) {
    return request.get('/dream/hot', { params: { limit } })
  },
  getPublicDreams(params) {
    return request.get('/dream/public', { params })
  },
  getDreamById(id, userId) {
    return request.get(`/dream/${id}`, {
      params: { userId: userId || undefined }
    })
  },
  createDream(data) {
    return request.post('/dream/create', data)
  },
  uploadDreamImage(formData) {
    return request.post('/dream/upload-image', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
  },
  updateDream(id, data) {
    return request.put(`/dream/${id}`, data)
  },
  deleteDream(id, userId) {
    return request.delete(`/dream/${id}`, { params: { userId } })
  },
  getUserDreams(userId, params = {}) {
    return request.get(`/dream/user/${userId}`, { params })
  },
  toggleLike(dreamId, userId) {
    return request.post(`/dream/${dreamId}/like`, null, { params: { userId } })
  },
  analyzeDream(dreamId) {
    return request.post(`/dream/${dreamId}/analyze`)
  }
}

export const categoryApi = {
  getAllCategories() {
    return request.get('/category/list')
  },
  getCategoriesWithCount() {
    return request.get('/category/list/count')
  },
  getCategoryById(id) {
    return request.get(`/category/${id}`)
  }
}

export const commentApi = {
  getComments(dreamId, params = {}) {
    return request.get(`/comment/dream/${dreamId}`, { params })
  },
  createComment(data) {
    return request.post('/comment/create', data)
  },
  deleteComment(id) {
    return request.delete(`/comment/${id}`)
  },
  getUserComments(userId) {
    return request.get(`/comment/user/${userId}`)
  }
}

export const adminApi = {
  getStats(adminId) {
    return request.get('/admin/stats', { params: { adminId } })
  },
  getUsers(adminId, params = {}) {
    return request.get('/admin/users', { params: { adminId, ...params } })
  },
  getDreams(adminId, params = {}) {
    return request.get('/admin/dreams', { params: { adminId, ...params } })
  },
  deleteDream(id, adminId) {
    return request.delete(`/admin/dream/${id}`, { params: { adminId } })
  },
  deleteUser(id, adminId) {
    return request.delete(`/admin/user/${id}`, { params: { adminId } })
  },
  updateUserRole(id, role, adminId) {
    return request.put(`/admin/user/${id}/role`, { role }, { params: { adminId } })
  },
  updateUserStatus(id, status, adminId) {
    return request.put(`/admin/user/${id}/status`, { status }, { params: { adminId } })
  },
  batchDeleteDreams(ids, adminId) {
    return request.post('/admin/dreams/batch-delete', { ids }, { params: { adminId } })
  }
}

export default { userApi, dreamApi, categoryApi, commentApi, adminApi }
