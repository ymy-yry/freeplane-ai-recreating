<template>
  <div class="summary-panel">
    <h3>分支摘要</h3>
    <div class="node-info" v-if="aiStore.selectedNode">
      当前分支根节点：{{ aiStore.selectedNode.text }}
    </div>

    <button @click="runSummary" :disabled="!aiStore.hasModel || loading">
      {{ loading ? '生成中...' : '生成分支摘要' }}
    </button>

    <div v-if="summaryHtml" class="summary" v-html="summaryHtml"></div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useAiStore } from '../../stores/aiStore'
import { summarizeBranch } from '../../api/aiApi'
import { marked } from 'marked'

const aiStore = useAiStore()
const loading = ref(false)
const summaryHtml = ref('')

const runSummary = async () => {
  if (!aiStore.selectedNode) return
  loading.value = true
  try {
    const res = await summarizeBranch({
      nodeId: aiStore.selectedNode.id,
      model: aiStore.currentModel
    })
    summaryHtml.value = await marked.parse(res.data)

    // ======================
    // ✅ 画布联动：高亮节点
    // ======================
    window.postMessage({
      type: 'ai:showSummary',
      nodeId: aiStore.selectedNode.id,
      summary: res.data
    }, '*')

  } catch (err) {
    console.error('摘要失败', err)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.summary-panel { padding: 16px; }
.summary { margin-top: 10px; padding: 10px; background: #f9f9f9; }
</style>