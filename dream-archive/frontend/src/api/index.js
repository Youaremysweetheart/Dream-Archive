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
  getDreamById(id) {
    return request.get(`/dream/${id}`)
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
  deleteDream(id) {
    return request.delete(`/dream/${id}`)
  },
  getUserDreams(userId, params = {}) {
    return request.get(`/dream/user/${userId}`, { params })
  },
  toggleLike(dreamId) {
    return request.post(`/dream/${dreamId}/like`)
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
  getStats() {
    return request.get('/admin/stats')
  },
  getUsers(params = {}) {
    return request.get('/admin/users', { params })
  },
  getDreams(params = {}) {
    return request.get('/admin/dreams', { params })
  },
  deleteDream(id) {
    return request.delete(`/admin/dream/${id}`)
  },
  deleteUser(id) {
    return request.delete(`/admin/user/${id}`)
  },
  updateUserRole(id, role) {
    return request.put(`/admin/user/${id}/role`, { role })
  },
  updateUserStatus(id, status) {
    return request.put(`/admin/user/${id}/status`, { status })
  },
  batchDeleteDreams(ids) {
    return request.post('/admin/dreams/batch-delete', { ids })
  }
}

export const dreamRoomApi = {
  enterRoom(data = {}) {
    return request.post('/dream-room/enter', data)
  },
  sendMessage(data) {
    return request.post('/dream-room/send', data)
  },
  getMessages(params) {
    return request.get('/dream-room/messages', { params })
  }
}

export default { userApi, dreamApi, categoryApi, commentApi, dreamRoomApi, adminApi }
