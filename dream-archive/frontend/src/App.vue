<template>
  <div id="app">
    <Header />
    <main class="main-content">
      <router-view />
    </main>
    <Footer />
  </div>
</template>

<script setup>
import Header from '@/components/Header.vue'
import Footer from '@/components/Footer.vue'
</script>

<style>
:root {
  --bg-main: #111820;
  --bg-panel: rgba(22, 28, 40, 0.86);
  --text-main: #eaf1ff;
  --text-muted: #97a4bd;
  --line-soft: rgba(255, 255, 255, 0.1);
  --accent-cyan: #00e5ff;
  --accent-blue: #2f6bff;
  --accent-pink: #ff3d81;
  --accent-green: #7dff7a;
}

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', 
               'Microsoft YaHei', Arial, sans-serif;
  color: var(--text-main);
  background:
    radial-gradient(850px 360px at 8% -10%, rgba(0, 229, 255, 0.15), transparent 58%),
    radial-gradient(760px 320px at 96% -8%, rgba(255, 160, 61, 0.15), transparent 62%),
    radial-gradient(900px 340px at 50% 108%, rgba(125, 255, 122, 0.1), transparent 66%),
    linear-gradient(135deg, #111820 0%, #161d2a 55%, #111821 100%);
  background-attachment: fixed;
  min-height: 100vh;
}

#app {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.main-content {
  flex: 1;
  max-width: 1200px;
  width: 100%;
  margin: 0 auto;
  padding: 20px;
}

/* Element Plus Global Tweaks */
.el-card {
  background: var(--bg-panel) !important;
  border: 1px solid var(--line-soft) !important;
  box-shadow: 0 14px 38px rgba(0, 0, 0, 0.35) !important;
}

.el-card,
.el-card * {
  color: inherit;
}

.el-button--primary {
  background: linear-gradient(90deg, var(--accent-blue), var(--accent-cyan)) !important;
  border: none !important;
  color: #fff !important;
}

.el-button--success {
  background: linear-gradient(90deg, #00c853, var(--accent-green)) !important;
  border: none !important;
  color: #09120c !important;
}

.el-button--danger {
  background: linear-gradient(90deg, #ff1744, #ff6a00) !important;
  border: none !important;
  color: #fff !important;
}

.el-input__wrapper,
.el-textarea__inner {
  background: rgba(6, 10, 18, 0.85) !important;
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.12) !important;
  color: #eaf1ff !important;
}

.el-select__wrapper {
  background: rgba(6, 10, 18, 0.85) !important;
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.12) !important;
}

.el-pagination {
  color: var(--text-main) !important;
}

.el-tabs__item {
  color: #aab7ce !important;
}

.el-tabs__item.is-active {
  color: #ffffff !important;
}

.el-tabs__active-bar {
  background: linear-gradient(90deg, #00e5ff, #2f6bff) !important;
}

.el-radio-button__inner {
  background: rgba(12, 18, 29, 0.95) !important;
  border-color: rgba(255, 255, 255, 0.14) !important;
  color: #d7e3f7 !important;
}

.el-radio-button.is-active .el-radio-button__inner {
  background: linear-gradient(90deg, rgba(47, 107, 255, 0.85), rgba(0, 200, 255, 0.85)) !important;
  border-color: transparent !important;
  color: #fff !important;
  box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.12) inset !important;
}

.el-table {
  --el-table-bg-color: transparent !important;
  --el-table-tr-bg-color: rgba(14, 20, 31, 0.65) !important;
  --el-table-row-hover-bg-color: rgba(0, 229, 255, 0.08) !important;
  --el-table-header-bg-color: rgba(18, 24, 36, 0.95) !important;
  --el-table-text-color: #dce7f8 !important;
  --el-table-header-text-color: #f4f8ff !important;
  --el-table-border-color: rgba(255, 255, 255, 0.12) !important;
}

.el-table th.el-table__cell,
.el-table td.el-table__cell {
  background: transparent !important;
}

.el-table__inner-wrapper::before {
  background-color: rgba(255, 255, 255, 0.12) !important;
}

.el-pager li {
  background: rgba(11, 16, 26, 0.9) !important;
  color: #c7d4ea !important;
}

.el-pager li.is-active {
  color: #fff !important;
  background: linear-gradient(90deg, #2f6bff, #00c8ff) !important;
}

/* Overlay Components: message box / dialog / message / dropdown / popover */
.el-overlay {
  background-color: rgba(4, 8, 14, 0.62) !important;
}

.el-message-box,
.el-dialog,
.el-notification,
.el-dropdown-menu,
.el-select-dropdown,
.el-popper,
.el-popover {
  background: linear-gradient(160deg, rgba(19, 26, 39, 0.98), rgba(14, 20, 32, 0.98)) !important;
  border: 1px solid rgba(255, 255, 255, 0.12) !important;
  color: #eaf1ff !important;
}

.el-message-box__title,
.el-message-box__content,
.el-message-box__message p,
.el-dialog__title,
.el-dialog__body,
.el-dialog__headerbtn .el-dialog__close,
.el-dropdown-menu__item,
.el-select-dropdown__item,
.el-notification__title,
.el-notification__content,
.el-popover__title {
  color: #eaf1ff !important;
}

.el-dropdown-menu__item:hover,
.el-select-dropdown__item.hover,
.el-select-dropdown__item:hover {
  background: rgba(0, 229, 255, 0.1) !important;
  color: #ffffff !important;
}

.el-message {
  background: rgba(16, 23, 36, 0.96) !important;
  border: 1px solid rgba(255, 255, 255, 0.12) !important;
}

.el-message .el-message__content {
  color: #f2f7ff !important;
}

.el-alert {
  border-color: rgba(255, 255, 255, 0.12) !important;
}

.el-alert__title,
.el-alert__description {
  color: #eaf1ff !important;
}

.el-tag,
.el-tag.el-tag--dark,
.el-tag.el-tag--info,
.el-tag.el-tag--success,
.el-tag.el-tag--warning,
.el-tag.el-tag--danger {
  background: linear-gradient(90deg, #1d4ed8, #2563eb) !important;
  border: 1px solid #60a5fa !important;
  color: #eff6ff !important;
  font-weight: 600 !important;
}
</style>
