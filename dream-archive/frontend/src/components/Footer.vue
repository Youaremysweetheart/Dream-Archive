<template>
  <footer class="footer">
    <div class="footer-glow footer-glow-left"></div>
    <div class="footer-glow footer-glow-right"></div>

    <div class="footer-content">
      <div class="footer-section">
        <div class="footer-logo">
          <span class="logo-icon">🌙</span>
          <span class="logo-text">梦境档案馆</span>
        </div>
        <p class="footer-description">记录梦境，分享感受，探索内心世界。</p>
      </div>

      <div class="footer-section">
        <h3>快速链接</h3>
        <ul class="footer-links">
          <li><router-link to="/">首页</router-link></li>
          <li><router-link to="/explore">探索梦境</router-link></li>
          <li><router-link to="/dream/create">记录梦境</router-link></li>
        </ul>
      </div>

      <div class="footer-section">
        <h3>关于我们</h3>
        <ul class="footer-links">
          <li><a href="#about">项目介绍</a></li>
          <li><a href="#privacy">隐私政策</a></li>
          <li><a href="#terms">使用条款</a></li>
        </ul>
      </div>

      <div class="footer-section">
        <h3>联系方式</h3>
        <ul class="footer-links">
          <li>📧 邮箱: 待公布</li>
          <li>💬 反馈: 正在开发</li>
          <li>🛠 技术支持: 正在开发</li>
          <li>🌐 B站: <a href="https://space.bilibili.com/358312570" target="_blank">点击进入</a></li>
        </ul>
      </div>
    </div>

    <div class="footer-bottom">
      <p>&copy; 2026 梦境档案馆. All rights reserved.</p>
      <p>基于 Spring Boot + Vue 3 开发</p>
    </div>
  </footer>
</template>

<script setup>
</script>

<style scoped>
.footer {
  position: relative;
  overflow: hidden;
  margin-top: 60px;
  background: linear-gradient(160deg, rgba(20, 27, 40, 0.97), rgba(17, 24, 36, 0.97));
  color: #eaf1ff;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}

.footer-glow {
  position: absolute;
  width: 420px;
  height: 420px;
  border-radius: 50%;
  filter: blur(40px);
  opacity: 0.18;
  pointer-events: none;
}

.footer-glow-left {
  left: -150px;
  top: -160px;
  background: #00d8ff;
}

.footer-glow-right {
  right: -160px;
  top: -130px;
  background: #ff9e3d;
}

.footer-content {
  position: relative;
  z-index: 1;
  max-width: 1200px;
  margin: 0 auto;
  padding: 56px 20px 34px;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 32px;
}

.footer-section h3 {
  font-size: 18px;
  font-weight: 700;
  margin-bottom: 14px;
  color: #f5f9ff;
}

.footer-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 24px;
  font-weight: 700;
  margin-bottom: 12px;
}

.logo-icon {
  font-size: 32px;
}

.footer-description {
  color: #b7c5dd;
  line-height: 1.7;
}

.footer-links {
  list-style: none;
  padding: 0;
  margin: 0;
}

.footer-links li {
  margin-bottom: 10px;
  color: #c4d1e8;
}

.footer-links a {
  color: #cde7ff;
  text-decoration: none;
  transition: color 0.25s ease;
}

.footer-links a:hover {
  color: #00dbff;
}

.footer-bottom {
  position: relative;
  z-index: 1;
  border-top: 1px solid rgba(255, 255, 255, 0.09);
  padding: 22px 20px 26px;
  text-align: center;
  font-size: 14px;
  color: #a8b5cb;
}

.footer-bottom p {
  margin: 4px 0;
}

@media (max-width: 768px) {
  .footer-content {
    grid-template-columns: 1fr;
    gap: 24px;
    padding-top: 40px;
  }
}
</style>
