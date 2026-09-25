<template>
  <section class="section-block" id="contact">
    <h2 class="section-title"><el-icon><Promotion /></el-icon>联系我</h2>
    <div v-if="hasContact" class="contact-grid">
      <a v-if="profile.github" :href="profile.github" target="_blank" rel="noopener" class="contact-card">
        <span class="contact-icon"><el-icon><Link /></el-icon></span>
        <span class="contact-name">GitHub</span>
        <el-icon class="contact-arrow"><TopRight /></el-icon>
      </a>
      <a v-if="profile.gitee" :href="profile.gitee" target="_blank" rel="noopener" class="contact-card">
        <span class="contact-icon"><el-icon><Connection /></el-icon></span>
        <span class="contact-name">Gitee</span>
        <el-icon class="contact-arrow"><TopRight /></el-icon>
      </a>
      <a v-if="profile.csdn" :href="profile.csdn" target="_blank" rel="noopener" class="contact-card">
        <span class="contact-icon"><el-icon><Notebook /></el-icon></span>
        <span class="contact-name">CSDN 博客</span>
        <el-icon class="contact-arrow"><TopRight /></el-icon>
      </a>
    </div>
  </section>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  profile: { type: Object, required: true }
})

const hasContact = computed(() => {
  const p = props.profile || {}
  return Boolean(p.github || p.gitee || p.csdn)
})
</script>

<style scoped>
.contact-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 12px;
}

.contact-card {
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 13px 15px;
  background: linear-gradient(180deg, var(--color-surface), var(--color-bg-soft));
  border: 1px solid var(--color-border);
  border-radius: 12px;
  color: var(--color-text);
  transition: transform 0.25s ease, box-shadow 0.25s ease, border-color 0.25s ease;
}

a.contact-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-card-hover);
  border-color: color-mix(in srgb, var(--color-primary) 35%, var(--color-border));
}

.contact-icon {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  border-radius: 11px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 18px;
  background: linear-gradient(135deg, var(--color-primary-dark), var(--color-primary-light));
  box-shadow: 0 6px 14px color-mix(in srgb, var(--color-primary) 30%, transparent);
}

.contact-name {
  flex: 1;
  font-size: 14px;
  font-weight: 700;
  color: var(--color-text);
}

.contact-arrow {
  flex-shrink: 0;
  color: var(--color-text-secondary);
  opacity: 0;
  transform: translateX(-4px);
  transition: opacity 0.25s ease, transform 0.25s ease, color 0.25s ease;
}

.contact-card:hover .contact-arrow {
  opacity: 1;
  transform: translateX(0);
  color: var(--color-primary);
}
</style>
