<template>
  <div class="model-selector">
    <span class="label">AI 模型：</span>
    <select v-model="aiStore.currentModel" @change="handleChange" class="model-select">
      <option value="">请选择模型</option>
      <option
        v-for="model in aiStore.modelList"
        :key="model.name"
        :value="model.name"
      >
        {{ model.label }}
      </option>
    </select>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
// 👇 就这两行是关键！
import { useAiStore } from '../../stores/aiStore'
import { getAiModels } from '../../api/aiApi'

const aiStore = useAiStore()

onMounted(async () => {
  try {
    const res = await getAiModels()
    aiStore.setModelList(res.data)
  } catch (err) {
    console.error('获取模型失败:', err)
  }
})

const handleChange = () => {
  aiStore.setCurrentModel(aiStore.currentModel)
}
</script>

<style scoped>
.model-selector {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-bottom: 1px solid #eee;
}
.label {
  font-size: 14px;
  color: #333;
}
.model-select {
  padding: 4px 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
}
</style>