<template>
  <div class="expand-dialog">
    <div class="header">
      <h3>AI 节点展开</h3>
      <button @click="$emit('close')">×</button>
    </div>
    <div class="body">
      <div v-if="aiStore.selectedNode">
        当前节点：{{ aiStore.selectedNode.text }}
      </div>
      <button @click="runExpand" :disabled="!aiStore.hasModel || loading">
        {{ loading ? '生成中...' : '生成子节点' }}
      </button>
      <div v-if="result" class="result">{{ result }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useAiStore } from '../../stores/aiStore'
import { expandNode } from '../../api/aiApi'

const aiStore = useAiStore()
const loading = ref(false)
const result = ref('')

const runExpand = async () => {
  if (!aiStore.selectedNode) return
  loading.value = true
  try {
    const res = await expandNode({
      nodeId: aiStore.selectedNode.id,
      model: aiStore.currentModel
    })
    result.value = res.data
  } catch (err) {
    console.error(err)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.expand-dialog { padding: 20px; }
.header { display: flex; justify-content: space-between; }
.body { margin-top: 10px; }
button { padding: 8px 12px; margin-top: 10px; }
.result { margin-top: 10px; padding: 10px; background: #f5f5f5; }
</style>