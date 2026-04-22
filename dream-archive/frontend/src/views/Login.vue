<template>
  <div class="login-page">
    <section class="login-layout" :class="{ 'is-entering': enteringSystem }">
      <div class="brand-side" :class="{ leaving: enteringSystem }">
        <div class="globe" aria-hidden="true">
          <div class="globe-rings">
            <span class="ring ring-1"></span>
            <span class="ring ring-2"></span>
            <span class="ring ring-3"></span>
            <span class="ring ring-4"></span>
            <span class="ring ring-5"></span>
            <span class="ring ring-6"></span>
            <span class="line line-x"></span>
            <span class="line line-y"></span>
            <span v-for="i in 14" :key="i" class="star" :style="starStyle(i)"></span>
          </div>

          <div class="brand-text">
            <p class="brand-main">SOMNIUM</p>
            <p class="brand-sub">DREAM ARCHIVE</p>
          </div>
        </div>
      </div>

      <div class="panel-side" :class="{ leaving: enteringSystem }">
        <transition name="card-switch" mode="out-in">
          <div class="login-card" :key="authMode">
            <template v-if="authMode === 'login'">
              <div class="card-header">
                <h1>归档登记</h1>
                <p>欢迎回到梦境的港湾，这里记录着您的每一次心灵旅程。</p>
              </div>

              <el-form
                ref="loginFormRef"
                :model="loginForm"
                :rules="loginRules"
                class="login-form"
                @submit.prevent="handleLogin"
              >
                <el-form-item prop="username" class="field-item">
                  <label class="field-label">档案邮箱</label>
                  <el-input
                    v-model="loginForm.username"
                    :prefix-icon="Message"
                    placeholder="name@dream.archive"
                    size="large"
                    clearable
                  />
                </el-form-item>

                <el-form-item prop="password" class="field-item">
                  <label class="field-label">访问秘钥</label>
                  <el-input
                    v-model="loginForm.password"
                    :prefix-icon="Lock"
                    type="password"
                    placeholder="••••••••"
                    size="large"
                    show-password
                    @keyup.enter="handleLogin"
                  />
                </el-form-item>

                <div class="option-row">
                  <el-checkbox v-model="rememberMe">保持唤醒状态</el-checkbox>
                  <button type="button" class="ghost-link">遗忘秘钥?</button>
                </div>

                <el-form-item class="submit-item">
                  <el-button
                    class="submit-btn"
                    type="primary"
                    size="large"
                    :loading="loadingLogin"
                    @click="handleLogin"
                  >
                    {{ loadingLogin ? '连接中...' : '开启连接 →' }}
                  </el-button>
                </el-form-item>
              </el-form>

              <div class="register-tip">
                <span>还没有账号？</span>
                <el-link type="primary" @click="switchMode('register')">立刻创建新档案</el-link>
              </div>
            </template>

            <template v-else>
              <div class="card-header">
                <h1>创建档案</h1>
                <p>第一次来到梦境港湾？创建你的专属档案，开始记录每一次内心旅程。</p>
              </div>

              <el-form
                ref="registerFormRef"
                :model="registerForm"
                :rules="registerRules"
                class="login-form"
                @submit.prevent="handleRegister"
              >
                <el-form-item prop="username" class="field-item">
                  <label class="field-label">档案名称</label>
                  <el-input
                    v-model="registerForm.username"
                    :prefix-icon="User"
                    placeholder="设置用户名（可用昵称）"
                    size="large"
                    clearable
                  />
                </el-form-item>

                <el-form-item prop="password" class="field-item">
                  <label class="field-label">访问秘钥</label>
                  <el-input
                    v-model="registerForm.password"
                    :prefix-icon="Lock"
                    type="password"
                    placeholder="设置密码"
                    size="large"
                    show-password
                  />
                </el-form-item>

                <el-form-item prop="confirmPassword" class="field-item">
                  <label class="field-label">确认秘钥</label>
                  <el-input
                    v-model="registerForm.confirmPassword"
                    :prefix-icon="Lock"
                    type="password"
                    placeholder="再次输入密码"
                    size="large"
                    show-password
                  />
                </el-form-item>

                <el-form-item prop="email" class="field-item">
                  <label class="field-label">档案邮箱（选填）</label>
                  <el-input
                    v-model="registerForm.email"
                    :prefix-icon="Message"
                    placeholder="name@dream.archive"
                    size="large"
                    clearable
                  />
                </el-form-item>

                <el-form-item class="submit-item">
                  <el-button
                    class="submit-btn"
                    type="primary"
                    size="large"
                    :loading="loadingRegister"
                    @click="handleRegister"
                  >
                    {{ loadingRegister ? '创建中...' : '立刻创建新档案' }}
                  </el-button>
                </el-form-item>
              </el-form>

              <div class="register-tip">
                <span>已有账号？</span>
                <el-link type="primary" @click="switchMode('login')">返回登录</el-link>
              </div>
            </template>
          </div>
        </transition>
      </div>
    </section>

    <button class="help-btn" type="button" aria-label="help">?</button>

    <div class="entry-overlay" :class="{ active: enteringSystem }">
      <div class="entry-orb"></div>
      <p>正在进入 Somnium Dream Archive...</p>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Lock, Message, User } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const loginFormRef = ref(null)
const registerFormRef = ref(null)
const loadingLogin = ref(false)
const loadingRegister = ref(false)
const rememberMe = ref(false)
const enteringSystem = ref(false)
const authMode = ref('login')

const loginForm = reactive({
  username: '',
  password: ''
})

const registerForm = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  email: ''
})

const loginRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 64, message: '用户名长度在 3 到 64 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' }
  ]
}

const validateConfirmPassword = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请再次输入密码'))
    return
  }
  if (value !== registerForm.password) {
    callback(new Error('两次输入密码不一致'))
    return
  }
  callback()
}

const registerRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在 3 到 20 个字符', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_\u4e00-\u9fa5]+$/, message: '用户名只能包含字母、数字、下划线和中文', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ],
  email: [
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ]
}

const starPoints = [
  [24, 18, 0.7], [32, 26, 0.4], [40, 22, 0.5], [28, 40, 0.45], [52, 34, 0.65], [60, 46, 0.42],
  [38, 60, 0.52], [48, 56, 0.35], [30, 70, 0.38], [56, 68, 0.45], [42, 48, 0.5], [34, 52, 0.42],
  [50, 76, 0.4], [62, 58, 0.34]
]

const starStyle = (index) => {
  const [left, top, opacity] = starPoints[index - 1] || [50, 50, 0.4]
  return {
    left: `${left}%`,
    top: `${top}%`,
    opacity
  }
}

const syncModeByRoute = () => {
  authMode.value = route.path === '/register' ? 'register' : 'login'
}

watch(() => route.path, syncModeByRoute, { immediate: true })

const switchMode = (mode) => {
  authMode.value = mode
  const target = mode === 'register' ? '/register' : '/login'
  if (route.path !== target) {
    router.replace(target)
  }
}

const handleLogin = async () => {
  if (!loginFormRef.value || enteringSystem.value) return

  await loginFormRef.value.validate(async (valid) => {
    if (!valid) return

    loadingLogin.value = true
    try {
      await userStore.login(loginForm.username, loginForm.password)
      ElMessage.success('登录成功')
      enteringSystem.value = true
      sessionStorage.setItem('lumina-entry-sequence', '1')
      await new Promise((resolve) => setTimeout(resolve, 860))
      router.push('/')
    } catch (error) {
      ElMessage.error(error.message || '登录失败，请检查用户名和密码')
    } finally {
      loadingLogin.value = false
    }
  })
}

const handleRegister = async () => {
  if (!registerFormRef.value) return

  await registerFormRef.value.validate(async (valid) => {
    if (!valid) return

    loadingRegister.value = true
    try {
      await userStore.register(registerForm.username, registerForm.password, registerForm.email)
      ElMessage.success('创建成功，请登录')
      registerForm.password = ''
      registerForm.confirmPassword = ''
      switchMode('login')
    } catch (error) {
      ElMessage.error(error.message || '创建失败，请稍后重试')
    } finally {
      loadingRegister.value = false
    }
  })
}
</script>

<style scoped>
.login-page {
  position: relative;
  min-height: 100vh;
  overflow: hidden;
  background:
    radial-gradient(72% 80% at 30% 48%, rgba(41, 49, 112, 0.45), rgba(15, 20, 58, 0) 74%),
    linear-gradient(135deg, #020423 0%, #070b30 54%, #040624 100%);
}

.login-layout {
  max-width: 1520px;
  min-height: 100vh;
  margin: 0 auto;
  padding: 24px 40px;
  display: grid;
  grid-template-columns: minmax(620px, 1fr) minmax(360px, 450px);
  gap: 40px;
  align-items: center;
}

.login-layout.is-entering {
  pointer-events: none;
}

.brand-side {
  display: grid;
  place-items: center;
  transition: transform 0.66s cubic-bezier(0.2, 0.7, 0.2, 1), opacity 0.66s ease;
}

.brand-side.leaving {
  transform: scale(0.95);
  opacity: 0.24;
}

.globe {
  position: relative;
  width: clamp(320px, 34vw, 500px);
  aspect-ratio: 1;
}

.globe-rings {
  position: absolute;
  inset: 0;
  animation: globeSpin 30s linear infinite;
}

.ring {
  position: absolute;
  inset: 12%;
  border-radius: 50%;
  border: 1px solid rgba(234, 239, 255, 0.16);
}

.ring-1 {
  transform: rotate(0deg) scaleX(1);
}

.ring-2 {
  transform: rotateY(18deg) scaleX(0.98);
}

.ring-3 {
  transform: rotateY(40deg) scaleX(0.92);
}

.ring-4 {
  transform: rotateY(62deg) scaleX(0.84);
}

.ring-5 {
  transform: rotateY(84deg) scaleX(0.74);
}

.ring-6 {
  transform: rotateX(85deg) scaleY(0.9);
  opacity: 0.65;
}

.line {
  position: absolute;
  left: 20%;
  right: 20%;
  border-top: 1px solid rgba(234, 239, 255, 0.11);
}

.line-x {
  top: 50%;
}

.line-y {
  top: 68%;
  opacity: 0.45;
}

.star {
  position: absolute;
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: rgba(250, 253, 255, 0.85);
  box-shadow: 0 0 8px rgba(214, 224, 255, 0.42);
}

.brand-text {
  position: absolute;
  inset: 0;
  display: grid;
  place-content: center;
  text-align: center;
  letter-spacing: 2px;
}

.brand-main {
  margin: 0;
  color: #f4f7ff;
  font-family: Georgia, 'Times New Roman', 'Source Han Serif SC', serif;
  font-size: clamp(40px, 4.5vw, 64px);
  line-height: 1;
}

.brand-sub {
  margin: 12px 0 0;
  color: #dfe6f5;
  font-family: Georgia, 'Times New Roman', 'Source Han Serif SC', serif;
  font-size: clamp(28px, 3.2vw, 46px);
  line-height: 1;
  letter-spacing: 2px;
}

.panel-side {
  display: flex;
  justify-content: center;
  transition: transform 0.58s cubic-bezier(0.2, 0.7, 0.2, 1), opacity 0.58s ease, filter 0.58s ease;
}

.panel-side.leaving {
  transform: translateX(56px) scale(0.94);
  opacity: 0;
  filter: blur(8px);
}

.login-card {
  width: 100%;
  max-width: 430px;
  border-radius: 30px;
  border: 1px solid rgba(212, 222, 246, 0.15);
  background: linear-gradient(165deg, rgba(38, 43, 74, 0.88), rgba(30, 35, 66, 0.86));
  backdrop-filter: blur(12px);
  box-shadow: 0 18px 38px rgba(1, 3, 15, 0.55), inset 0 1px 0 rgba(255, 255, 255, 0.05);
  padding: 34px 30px 28px;
}

.card-switch-enter-active,
.card-switch-leave-active {
  transition: opacity 0.34s ease, transform 0.34s ease, filter 0.34s ease;
}

.card-switch-enter-from {
  opacity: 0;
  transform: translateY(10px) scale(0.985);
  filter: blur(4px);
}

.card-switch-leave-to {
  opacity: 0;
  transform: translateY(-10px) scale(0.985);
  filter: blur(4px);
}

.card-header h1 {
  margin: 0;
  font-family: 'Source Han Sans CN', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  color: #eaf0ff;
  font-size: 44px;
  font-weight: 500;
  line-height: 1.04;
}

.card-header p {
  margin: 14px 0 0;
  max-width: 320px;
  color: rgba(195, 205, 232, 0.78);
  font-size: 14px;
  line-height: 1.6;
}

.login-form {
  margin-top: 24px;
}

.field-item {
  margin-bottom: 16px;
}

.field-label {
  display: block;
  margin-bottom: 7px;
  color: rgba(181, 193, 224, 0.88);
  font-size: 13px;
  letter-spacing: 0.6px;
}

.login-form :deep(.el-form-item__content) {
  display: block;
}

.login-form :deep(.el-input__wrapper) {
  min-height: 50px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.04) !important;
  box-shadow: inset 0 0 0 1px rgba(185, 196, 227, 0.2) !important;
}

.login-form :deep(.el-input__inner) {
  color: #eef3ff !important;
  font-size: 14px;
}

.option-row {
  margin: 2px 2px 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.option-row :deep(.el-checkbox__label) {
  color: rgba(195, 205, 232, 0.9);
  font-size: 13px;
}

.ghost-link {
  border: none;
  background: transparent;
  color: rgba(195, 205, 232, 0.7);
  font-size: 12px;
  cursor: pointer;
}

.ghost-link:hover {
  color: #edf3ff;
}

.submit-item {
  margin-bottom: 0;
}

.submit-btn {
  width: 100%;
  min-height: 50px;
  border-radius: 16px;
  border: none;
  background: linear-gradient(180deg, #ffffff 0%, #f3f4f7 100%) !important;
  color: #101322 !important;
  font-size: 18px;
  font-weight: 500;
  box-shadow: 0 8px 24px rgba(197, 207, 255, 0.26);
}

.register-tip {
  margin-top: 16px;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: rgba(188, 200, 231, 0.86);
}

.register-tip :deep(.el-link) {
  font-size: 13px;
  font-weight: 500;
}

.entry-overlay {
  position: fixed;
  inset: 0;
  z-index: 30;
  display: grid;
  place-content: center;
  gap: 18px;
  background: radial-gradient(circle at 50% 50%, rgba(33, 52, 129, 0.32), rgba(5, 7, 22, 0.92));
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.45s ease;
}

.entry-overlay.active {
  opacity: 1;
}

.entry-orb {
  width: 130px;
  height: 130px;
  border-radius: 50%;
  border: 1px solid rgba(220, 230, 255, 0.32);
  box-shadow: 0 0 0 14px rgba(113, 136, 224, 0.08), 0 0 70px rgba(100, 130, 248, 0.4);
  animation: orbPulse 1.05s ease-in-out infinite;
}

.entry-overlay p {
  text-align: center;
  color: rgba(226, 235, 255, 0.88);
  letter-spacing: 0.8px;
  font-size: 14px;
}

.help-btn {
  position: absolute;
  right: 12px;
  bottom: 10px;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  border: 1px solid rgba(182, 195, 231, 0.35);
  background: rgba(255, 255, 255, 0.08);
  color: #d7e2ff;
  cursor: pointer;
}

@keyframes globeSpin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

@keyframes orbPulse {
  0%,
  100% {
    transform: scale(0.94);
    opacity: 0.82;
  }
  50% {
    transform: scale(1.03);
    opacity: 1;
  }
}

@media (max-width: 1360px) {
  .login-layout {
    grid-template-columns: minmax(540px, 1fr) minmax(340px, 430px);
    gap: 28px;
  }

  .card-header h1 {
    font-size: 40px;
  }
}

@media (max-width: 1100px) {
  .login-layout {
    grid-template-columns: 1fr;
    gap: 10px;
    padding: 18px;
  }

  .brand-side {
    min-height: 300px;
  }

  .globe {
    width: min(390px, 84vw);
  }

  .login-card {
    max-width: 460px;
    margin: 0 auto;
    border-radius: 30px;
    padding: 30px 24px;
  }
}

@media (max-width: 760px) {
  .brand-side {
    min-height: 220px;
  }

  .card-header h1 {
    font-size: 36px;
  }

  .card-header p {
    font-size: 14px;
    margin-top: 12px;
  }

  .submit-btn {
    font-size: 16px;
  }
}
</style>
