<template>
  <section ref="rootRef" class="section-block" id="portfolio">
    <h2 class="section-title"><el-icon><Picture /></el-icon>作品集</h2>
    <div class="portfolio-grid" :class="`count-${list.length}`">
      <div v-for="item in list" :key="item.id" class="portfolio-card">
        <div class="cover">
          <img
            v-if="item.cover"
            :src="item.cover"
            :alt="item.title"
            loading="lazy"
            decoding="async"
          />
          <div v-else class="cover-placeholder">
            <el-icon :size="42"><PictureFilled /></el-icon>
            <span class="placeholder-text">PROJECT</span>
          </div>
          <span class="cover-mask"></span>
        </div>
        <div class="card-body">
          <h3 class="title">{{ item.title }}</h3>
          <p class="desc" :data-id="item.id">{{ item.description }}</p>
          <!-- 描述被截断为 3 行时出现，弹窗内查看完整内容（无独立详情页） -->
          <button
            v-if="overflowIds.has(item.id)"
            type="button"
            class="expand-btn"
            @click="openDetail(item)"
          >
            展开全文<el-icon><ArrowDownBold /></el-icon>
          </button>
          <a
            v-if="item.url && item.url !== '#'"
            :href="item.url"
            target="_blank"
            rel="noopener"
            class="link"
          >
            查看项目<el-icon><TopRight /></el-icon>
          </a>
        </div>
      </div>
    </div>

    <!-- 完整内容弹窗：封面 + 完整描述 + 外链，不占用路由、不做详情页 -->
    <el-dialog
      v-model="detailVisible"
      :title="active?.title || '作品详情'"
      width="min(520px, 92vw)"
      append-to-body
    >
      <img
        v-if="active?.cover"
        :src="active.cover"
        :alt="active?.title"
        class="detail-cover"
      />
      <p v-if="active?.description" class="detail-desc">{{ active.description }}</p>
      <template #footer>
        <div class="detail-footer">
          <a
            v-if="active?.url && active.url !== '#'"
            :href="active.url"
            target="_blank"
            rel="noopener"
            class="link"
          >
            查看项目<el-icon><TopRight /></el-icon>
          </a>
          <el-button @click="detailVisible = false">关闭</el-button>
        </div>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { ref, nextTick, onMounted, onUnmounted, watch } from 'vue'

const props = defineProps({
  list: { type: Array, default: () => [] }
})

const rootRef = ref(null)
// 描述超过 3 行被截断的作品 id（弹窗入口只给真正溢出的卡片显示）
const overflowIds = ref(new Set())
const detailVisible = ref(false)
const active = ref(null)

function checkOverflow() {
  if (!rootRef.value) return
  const set = new Set()
  rootRef.value.querySelectorAll('.desc').forEach((el) => {
    if (el.scrollHeight > el.clientHeight + 1) {
      set.add(Number(el.dataset.id))
    }
  })
  overflowIds.value = set
}

let resizeTimer = null
function onResize() {
  clearTimeout(resizeTimer)
  resizeTimer = setTimeout(checkOverflow, 150)
}

function openDetail(item) {
  active.value = item
  detailVisible.value = true
}

onMounted(() => {
  nextTick(checkOverflow)
  // 中文字体异步加载完成后行高可能变化，再测一次
  document.fonts?.ready.then(() => checkOverflow())
  window.addEventListener('resize', onResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', onResize)
  clearTimeout(resizeTimer)
})

watch(
  () => props.list,
  () => nextTick(checkOverflow)
)
</script>

<style scoped>
/* 默认一排三个；2/4 个时一排两个，1 个时单独一排，5 个以上按三排流式 */
.portfolio-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.portfolio-grid.count-1 {
  grid-template-columns: minmax(0, 1fr);
}

.portfolio-grid.count-2,
.portfolio-grid.count-4 {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.portfolio-grid.count-1 .portfolio-card {
  max-width: 360px;
}

@media (max-width: 640px) {
  .portfolio-grid,
  .portfolio-grid.count-2,
  .portfolio-grid.count-4 {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 420px) {
  .portfolio-grid,
  .portfolio-grid.count-2,
  .portfolio-grid.count-4 {
    grid-template-columns: minmax(0, 1fr);
  }

  .portfolio-grid.count-1 .portfolio-card {
    max-width: none;
  }
}

.portfolio-card {
  border: 1px solid var(--color-border);
  border-radius: 12px;
  overflow: hidden;
  background: var(--color-surface);
  box-shadow: var(--shadow-card);
  transition: transform 0.28s ease, box-shadow 0.28s ease, border-color 0.28s ease;
}

.portfolio-card:hover {
  transform: translateY(-6px);
  box-shadow: var(--shadow-card-hover);
  border-color: color-mix(in srgb, var(--color-primary) 32%, var(--color-border));
}

.cover {
  position: relative;
  height: 108px;
  overflow: hidden;
  background: var(--color-bg-soft);
}

.cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.45s cubic-bezier(0.22, 0.61, 0.36, 1);
}

.portfolio-card:hover .cover img {
  transform: scale(1.07);
}

.cover-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 100%;
  color: #fff;
  background: linear-gradient(135deg, var(--color-primary-dark), var(--color-primary-light));
  opacity: 0.92;
}

.placeholder-text {
  font-size: 11px;
  letter-spacing: 4px;
  opacity: 0.75;
}

/* 封面底部渐隐，和卡片正文自然衔接 */
.cover-mask {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 32px;
  pointer-events: none;
  background: linear-gradient(180deg, transparent, color-mix(in srgb, var(--color-surface) 55%, transparent));
}

.card-body {
  padding: 11px 14px 13px;
}

.title {
  margin: 0 0 6px;
  font-size: 14.5px;
  font-weight: 700;
  color: var(--color-text);
}

.desc {
  margin: 0 0 10px;
  font-size: 12.5px;
  line-height: 1.65;
  color: var(--color-text-secondary);
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* “展开全文”文字按钮 */
.expand-btn {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding: 0 0 8px;
  border: none;
  background: none;
  font-size: 12px;
  font-weight: 600;
  color: var(--color-primary-dark);
  cursor: pointer;
}

.expand-btn .el-icon {
  font-size: 12px;
}

.expand-btn:hover {
  color: var(--color-primary);
}

.link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 12px;
  font-size: 12.5px;
  font-weight: 600;
  color: var(--color-primary-dark);
  background: var(--tag-bg);
  border-radius: 999px;
  transition: background-color 0.2s ease, color 0.2s ease, gap 0.2s ease;
}

.link:hover {
  gap: 7px;
  color: #fff;
  background: linear-gradient(135deg, var(--color-primary-dark), var(--color-primary-light));
}

/* —— 完整内容弹窗 —— */
.detail-cover {
  width: 100%;
  max-height: 280px;
  object-fit: cover;
  border-radius: 10px;
  margin-bottom: 14px;
}

.detail-desc {
  margin: 0;
  font-size: 14px;
  line-height: 1.9;
  color: var(--color-text-secondary);
  white-space: pre-wrap;
  word-break: break-word;
}

.detail-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
</style>
