import { ref, onMounted } from 'vue'
import { getVersions } from '@/api'

/**
 * 后台各内容管理页复用的“版本作用域”：
 * 加载所有简历版本，默认选中默认版本。
 */
export function useVersionScope() {
  const versions = ref([])
  const currentVersionId = ref(null)

  async function loadVersions(selectId) {
    const res = await getVersions()
    versions.value = res.data || []
    if (selectId) {
      currentVersionId.value = selectId
    } else if (!currentVersionId.value || !versions.value.some((v) => v.id === currentVersionId.value)) {
      const def = versions.value.find((v) => v.isDefault === 1) || versions.value[0]
      currentVersionId.value = def ? def.id : null
    }
  }

  onMounted(() => loadVersions())

  return { versions, currentVersionId, loadVersions }
}
