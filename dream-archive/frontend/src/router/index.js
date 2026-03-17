import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/Home.vue'),
    meta: { title: '首页' }
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Register.vue'),
    meta: { title: '注册' }
  },
  {
    path: '/dream/create',
    name: 'DreamCreate',
    component: () => import('@/views/DreamCreate.vue'),
    meta: { title: '记录梦境', requiresAuth: true }
  },
  {
    path: '/dream/edit/:id',
    name: 'DreamEdit',
    component: () => import('@/views/DreamCreate.vue'),
    meta: { title: '编辑梦境', requiresAuth: true }
  },
  {
    path: '/dream/:id',
    name: 'DreamDetail',
    component: () => import('@/views/DreamDetail.vue'),
    meta: { title: '梦境详情' }
  },
  {
    path: '/profile/:id',
    name: 'Profile',
    component: () => import('@/views/Profile.vue'),
    meta: { title: '个人主页' }
  },
  {
    path: '/explore',
    name: 'Explore',
    component: () => import('@/views/Explore.vue'),
    meta: { title: '探索梦境' }
  },
  {
    path: '/admin',
    name: 'Admin',
    component: () => import('@/views/Admin.vue'),
    meta: { title: '管理后台', requiresAuth: true, requiresAdmin: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  document.title = `${to.meta.title || '梦境档案馆'} - 梦境档案馆`

  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    next('/login')
  } else if (to.meta.requiresAdmin && !userStore.isAdmin) {
    next('/')
  } else {
    next()
  }
})

export default router
