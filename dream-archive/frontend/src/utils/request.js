import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

const UNAUTHORIZED_MESSAGE = '\u767b\u5f55\u72b6\u6001\u5df2\u5931\u6548\uff0c\u8bf7\u91cd\u65b0\u767b\u5f55'
let redirectingToLogin = false

const handleUnauthorized = (message = UNAUTHORIZED_MESSAGE) => {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  ElMessage.error(message)

  if (!redirectingToLogin && router.currentRoute.value.path !== '/login') {
    redirectingToLogin = true
    router.push('/login').finally(() => {
      redirectingToLogin = false
    })
  }
}

request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

request.interceptors.response.use(
  (response) => {
    const res = response.data

    if (res.code !== 200) {
      if (res.code === 401) {
        handleUnauthorized(res.message || UNAUTHORIZED_MESSAGE)
      } else {
        ElMessage.error(res.message || 'Request failed')
      }
      return Promise.reject(new Error(res.message || 'Request failed'))
    }

    return res
  },
  (error) => {
    if (error?.response?.status === 401) {
      handleUnauthorized()
      return Promise.reject(new Error(UNAUTHORIZED_MESSAGE))
    }

    ElMessage.error(error.message || 'Network error')
    return Promise.reject(error)
  }
)

export default request
