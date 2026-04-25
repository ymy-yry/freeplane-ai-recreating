<template>
  <div class="search-bar">
    <input v-model="kw" placeholder="语义搜索" @keyup.enter="doSearch" />
    <button @click="doSearch">搜索</button>

    <div class="result-list">
      <div
        v-for="item in searchResult"
        :key="item.id"
        class="item"
        @click="jumpToNode(item.id)"
      >
        {{ item.text }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useAiStore } from '../../stores/aiStore'
import { semanticSearch } from '../../api/aiApi'

interface NodeItem { id: string; text: string }
const aiStore = useAiStore()
const kw = ref('')
const searchResult = ref<NodeItem[]>([])

const doSearch = async () => {
  if (!kw.value || !aiStore.currentModel) return
  try {
    const res = await semanticSearch({
      keyword: kw.value,
      model: aiStore.currentModel
    })
    searchResult.value = res.data
  } catch (err) {
    console.error('搜索失败', err)
  }
}

// ======================
// ✅ 画布联动：跳转到节点
// ======================
const jumpToNode = (nodeId: string) => {
  window.postMessage({
    type: 'ai:jumpToNode',
    nodeId: nodeId
  }, '*')
}
</script>

<style scoped>
.search-bar { padding: 8px; }
input { padding: 6px; width: 200px; margin-right: 6px; }
.item { padding: 4px; cursor: pointer; }
.item:hover { background: #eee; }
</style>