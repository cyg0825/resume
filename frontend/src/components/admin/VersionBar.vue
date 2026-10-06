<template>
  <el-card shadow="never" class="version-bar">
    <div class="bar-inner">
      <div class="bar-left">
        <el-icon><Files /></el-icon>
        <span class="label">当前编辑的简历版本：</span>
        <el-select
          :model-value="modelValue"
          placeholder="请选择版本"
          style="width: 240px"
          @change="onChange"
        >
          <el-option
            v-for="v in versions"
            :key="v.id"
            :value="v.id"
            :label="v.versionName + (v.isDefault === 1 ? '（默认）' : '')"
          />
        </el-select>
        <el-button link type="primary" @click="openPreview">
          <el-icon><View /></el-icon>&nbsp;预览该版本
        </el-button>
      </div>
      <slot />
    </div>
  </el-card>
</template>

<script setup>
const props = defineProps({
  modelValue: { type: [Number, String], default: null },
  versions: { type: Array, default: () => [] }
})
const emit = defineEmits(['update:modelValue'])

// 页面侧用 watch(currentVersionId) 重新拉列表，这里不再额外 emit change
function onChange(value) {
  emit('update:modelValue', value)
}

function openPreview() {
  if (props.modelValue) {
    window.open(`/#/?versionId=${props.modelValue}`, '_blank')
  }
}
</script>

<style scoped>
.version-bar {
  margin-bottom: 16px;
  border-radius: 10px;
}

.bar-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
}

.bar-left {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.label {
  font-size: 14px;
  color: #475569;
  font-weight: 500;
}
</style>
