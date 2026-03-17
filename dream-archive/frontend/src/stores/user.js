import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { userApi } from '@/api'

export const useUserStore = defineStore('user', () => {
  const user = ref(null)
  const token = ref('')

  const isLoggedIn = computed(() => !!user.value && !!token.value)
  const isAdmin = computed(() => String(user.value?.role || '').toUpperCase() === 'ADMIN')
  const userId = computed(() => user.value?.id)

  const normalizeAvatar = (avatar) => {
    if (!avatar) return avatar
    if (avatar.startsWith('/uploads/')) return `/api${avatar}`
    return avatar
  }

  const normalizeUser = (userData) => {
    if (!userData) return userData
    return { ...userData, avatar: normalizeAvatar(userData.avatar) }
  }

  const setUser = (userData) => {
    user.value = normalizeUser(userData)
    localStorage.setItem('user', JSON.stringify(user.value))
  }

  const setToken = (tokenValue) => {
    token.value = tokenValue
    localStorage.setItem('token', tokenValue)
  }

  const loginSuccess = (userData, tokenValue) => {
    setUser(userData)
    setToken(tokenValue)
  }

  // 支持两种调用方式：
  // 1) login(username, password)
  // 2) login(userData, token)
  const login = async (arg1, arg2) => {
    if (typeof arg1 === 'string') {
      const res = await userApi.login({ username: arg1, password: arg2 })
      const data = res.data || {}
      loginSuccess(data.user, data.token)
      return data
    }

    loginSuccess(arg1, arg2)
    return { user: arg1, token: arg2 }
  }

  const register = async (username, password, email) => {
    const res = await userApi.register({ username, password, email })
    return res.data
  }

  const logout = () => {
    user.value = null
    token.value = ''
    localStorage.removeItem('user')
    localStorage.removeItem('token')
  }

  const updateUser = (userData) => {
    user.value = normalizeUser({ ...user.value, ...userData })
    localStorage.setItem('user', JSON.stringify(user.value))
  }

  const restoreLoginState = () => {
    const savedUser = localStorage.getItem('user')
    const savedToken = localStorage.getItem('token')

    if (savedUser && savedToken) {
      try {
        user.value = JSON.parse(savedUser)
        user.value = normalizeUser(user.value)
        token.value = savedToken
      } catch (error) {
        console.error('恢复登录状态失败:', error)
        logout()
      }
    }
  }

  restoreLoginState()

  return {
    user,
    token,
    isLoggedIn,
    isAdmin,
    userId,
    setUser,
    setToken,
    login,
    register,
    logout,
    updateUser,
    restoreLoginState
  }
})
