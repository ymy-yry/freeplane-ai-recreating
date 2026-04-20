<template>
  <div class="ai-panel-container">
    <SemanticSearchBar />
    <AiChatPanel />
    <SummarizePanel />
    <ExpandNodeDialog @close="closeDialog" v-if="showExpandDialog" />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import AiChatPanel from './components/ai/AiChatPanel.vue'
import ExpandNodeDialog from './components/ai/ExpandNodeDialog.vue'
import SummarizePanel from './components/ai/SummarizePanel.vue'
import SemanticSearchBar from './components/ai/SemanticSearchBar.vue'

const showExpandDialog = ref(false)
const closeDialog = () => {
  showExpandDialog.value = false
}
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}
.ai-panel-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
}
</style>