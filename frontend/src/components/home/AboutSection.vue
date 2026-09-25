<template>
  <section class="section-block" id="about">
    <h2 class="section-title"><el-icon><User /></el-icon>个人评价</h2>
    <p class="about-text">{{ profile.about || '这个人很懒，还没有填写个人简介。' }}</p>
  </section>
</template>

<script setup>
defineProps({
  profile: { type: Object, required: true }
})
</script>

<style scoped>
.about-text {
  position: relative;
  margin: 0;
  padding: 14px 16px;
  line-height: 1.9;
  font-size: 13.5px;
  color: var(--color-text);
  white-space: pre-wrap;
  background: linear-gradient(180deg, var(--color-surface), var(--color-bg-soft));
  border: 1px solid var(--color-border);
  border-radius: 12px;
  border-left: 4px solid var(--color-primary);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.35);
}

/* 右下角引号水印 */
.about-text::after {
  content: '”';
  position: absolute;
  right: 12px;
  bottom: -20px;
  font-size: 80px;
  line-height: 1;
  font-family: Georgia, 'Times New Roman', serif;
  color: color-mix(in srgb, var(--color-primary) 10%, transparent);
  pointer-events: none;
}
</style>
