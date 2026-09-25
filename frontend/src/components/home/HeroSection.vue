<template>
  <!-- 左侧信息栏的竖向个人信息卡（网页 / PDF 均为竖向） -->
  <div class="hero">
    <span class="hero-dots"></span>
    <div class="hero-inner">
      <div class="avatar-wrap">
        <img
          v-if="profile.avatar"
          :src="profile.avatar"
          :alt="profile.name || '头像'"
          class="avatar-img"
          fetchpriority="high"
        />
        <div v-else class="avatar-img avatar-empty">
          <el-icon :size="32"><UserFilled /></el-icon>
        </div>
      </div>

      <div class="hero-info">
        <h1 class="name">{{ profile.name || '暂未设置姓名' }}</h1>
        <p class="job-title">
          <el-icon><Aim /></el-icon>{{ profile.jobTitle || '欢迎来到我的个人主页' }}
        </p>
        <p v-if="profile.slogan" class="slogan">{{ profile.slogan }}</p>
        <div class="info-list">
          <a v-if="profile.email" :href="`mailto:${profile.email}`" class="info-item" title="邮箱">
            <el-icon><Message /></el-icon><span>{{ profile.email }}</span>
          </a>
          <span v-if="profile.address" class="info-item" title="所在地">
            <el-icon><Location /></el-icon><span>{{ profile.address }}</span>
          </span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  profile: { type: Object, required: true }
})
</script>

<style scoped>
/* ========== 网页端：左侧栏竖向信息卡 ========== */
.hero {
  position: relative;
  overflow: hidden;
  background: var(--hero-gradient);
  border-radius: 14px;
  padding: 16px 14px 15px;
  color: #fff;
  box-shadow: var(--shadow-card-hover);
  transition: background 0.5s ease;
}

/* 细点阵纹理，低调点缀 */
.hero-dots {
  position: absolute;
  inset: 0;
  pointer-events: none;
  opacity: 0.4;
  background-image: radial-gradient(rgba(255, 255, 255, 0.14) 1px, transparent 1px);
  background-size: 20px 20px;
  -webkit-mask-image: linear-gradient(150deg, rgba(0, 0, 0, 0.7), transparent 60%);
  mask-image: linear-gradient(150deg, rgba(0, 0, 0, 0.7), transparent 60%);
}

.hero-inner {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.avatar-wrap {
  flex-shrink: 0;
}

/* 长方形证件照：3:4 竖版 */
.avatar-img {
  display: block;
  width: 82px;
  aspect-ratio: 3 / 4;
  object-fit: cover;
  border-radius: 10px;
  border: 3px solid rgba(255, 255, 255, 0.85);
  box-shadow: 0 8px 22px rgba(0, 0, 0, 0.26);
  background: rgba(255, 255, 255, 0.18);
}

.avatar-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(255, 255, 255, 0.75);
}

.hero-info {
  width: 100%;
  text-align: center;
}

.name {
  margin: 0 0 6px;
  font-size: 20px;
  font-weight: 800;
  letter-spacing: 2px;
  text-shadow: 0 2px 10px rgba(0, 0, 0, 0.18);
}

.job-title {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  margin: 0 0 6px;
  padding: 3px 11px;
  font-size: 12.5px;
  font-weight: 600;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.16);
  border: 1px solid rgba(255, 255, 255, 0.28);
}

.slogan {
  margin: 0 0 8px;
  font-size: 12px;
  line-height: 1.6;
  opacity: 0.85;
}

.info-list {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 5px;
  text-align: left;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #fff;
  font-size: 12px;
  line-height: 1.4;
  padding: 5px 10px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.18);
  word-break: break-all;
  transition: background 0.2s ease;
}

.info-item:hover {
  background: rgba(255, 255, 255, 0.24);
}

.info-item .el-icon {
  flex-shrink: 0;
  font-size: 13px;
}

/* ========== PDF 导出：保持竖向信息卡，尺寸微调 ========== */
.pdf-exporting .hero {
  padding: 16px 14px;
}

.pdf-exporting .avatar-img {
  width: 86px;
}

.pdf-exporting .name {
  font-size: 21px;
}

/* ========== 窄屏：卡片全宽，头像稍大 ========== */
@media (max-width: 768px) {
  .hero {
    padding: 20px 18px 17px;
  }

  .avatar-img {
    width: 92px;
  }

  .name {
    font-size: 23px;
  }

  .info-list {
    max-width: 320px;
    margin: 0 auto;
  }
}
</style>
