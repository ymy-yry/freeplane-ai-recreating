<template>
  <div class="map-manager">
    <button @click="showMapList = !showMapList" class="map-manager-toggle">
      📁 {{ currentMap?.title || '选择导图' }}
    </button>
    
    <div v-if="showMapList" class="map-list">
      <div class="map-list-header">
        <h3>思维导图管理</h3>
        <button @click="showCreateForm = true" class="create-map-btn">+ 新建导图</button>
      </div>
      
      <div v-if="showCreateForm" class="create-map-form">
        <input 
          v-model="newMapTitle" 
          type="text" 
          placeholder="输入导图标题"
          @keyup.enter="handleCreateMap"
          ref="titleInput"
        />
        <div class="form-actions">
          <button @click="handleCreateMap" :disabled="!newMapTitle.trim()">创建</button>
          <button @click="cancelCreate" class="cancel">取消</button>
        </div>
      </div>
      
      <div class="maps">
        <div 
          v-for="map in allMaps" 
          :key="map.mapId"
          :class="['map-item', { active: currentMap?.mapId === map.mapId }]"
          @click="handleSwitchMap(map.mapId)"
        >
          <span class="map-title">{{ map.title }}</span>
          <span class="map-status" v-if="currentMap?.mapId === map.mapId">当前</span>
        </div>
        
        <div v-if="allMaps.length === 0" class="no-maps">
          <p>暂无导图</p>
          <p class="hint">请先打开Freeplane桌面端并加载.mm文件</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, nextTick, onMounted } from 'vue'
import { useMapStore } from '@/stores/mapStore'

const store = useMapStore()
const showMapList = ref(false)
const showCreateForm = ref(false)
const newMapTitle = ref('')
const titleInput = ref<HTMLInputElement>()

const currentMap = store.currentMap
const allMaps = store.allMaps

const handleSwitchMap = async (mapId: string) => {
  try {
    await store.switchMap(mapId)
    showMapList.value = false
  } catch (e) {
    console.error('切换导图失败', e)
    alert('切换导图失败，请确保Freeplane桌面端已打开对应文件')
  }
}

const handleCreateMap = async () => {
  if (!newMapTitle.value.trim()) return
  
  try {
    await store.createNewMap(newMapTitle.value.trim())
    newMapTitle.value = ''
    showCreateForm.value = false
    showMapList.value = false
  } catch (e) {
    console.error('创建导图失败', e)
    alert('创建导图失败，请检查后端API是否可用')
  }
}

const cancelCreate = () => {
  showCreateForm.value = false
  newMapTitle.value = ''
}

watch(showCreateForm, (newVal) => {
  if (newVal) {
    nextTick(() => {
      titleInput.value?.focus()
    })
  }
})

// 组件挂载时加载导图列表
onMounted(() => {
  store.loadAllMaps()
})
</script>

<style scoped>
.map-manager {
  position: relative;
  display: inline-block;
}

.map-manager-toggle {
  padding: 8px 16px;
  border: 1px solid #ddd;
  border-radius: 6px;
  background: white;
  cursor: pointer;
  font-size: 14px;
}

.map-manager-toggle:hover {
  background: #f5f5f5;
}

.map-list {
  position: absolute;
  top: 100%;
  left: 0;
  background: white;
  border: 1px solid #ddd;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  min-width: 300px;
  z-index: 1001;
  margin-top: 8px;
}

.map-list-header {
  padding: 16px;
  border-bottom: 1px solid #eee;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.map-list-header h3 {
  margin: 0;
  font-size: 16px;
}

.create-map-btn {
  padding: 4px 8px;
  border: 1px solid #1890ff;
  border-radius: 4px;
  background: #1890ff;
  color: white;
  font-size: 12px;
  cursor: pointer;
}

.create-map-form {
  padding: 16px;
  border-bottom: 1px solid #eee;
}

.create-map-form input {
  width: 100%;
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
  margin-bottom: 8px;
}

.form-actions {
  display: flex;
  gap: 8px;
}

.form-actions button {
  padding: 6px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  cursor: pointer;
}

.form-actions button:not(.cancel) {
  background: #1890ff;
  color: white;
  border-color: #1890ff;
}

.form-actions button.cancel {
  background: #f5f5f5;
}

.maps {
  max-height: 300px;
  overflow-y: auto;
}

.map-item {
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.map-item:hover {
  background: #f8f9fa;
}

.map-item.active {
  background: #e6f7ff;
  border-left: 3px solid #1890ff;
}

.map-title {
  font-weight: 500;
}

.map-status {
  font-size: 12px;
  color: #52c41a;
  background: #f6ffed;
  padding: 2px 6px;
  border-radius: 3px;
}

.no-maps {
  padding: 20px;
  text-align: center;
  color: #999;
}

.hint {
  font-size: 12px;
  margin-top: 8px;
}
</style>