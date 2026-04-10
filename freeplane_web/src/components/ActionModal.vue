<template>
  <div v-if="visible" class="modal-overlay" @click="handleOverlayClick">
    <div class="modal-content" @click.stop>
      <h3>{{ title }}</h3>
      
      <div v-if="mode === 'choose'" class="action-buttons">
        <button @click="$emit('newChild')">新建子节点</button>
        <button @click="$emit('fold')">折叠/展开</button>
        <button @click="$emit('delete')" class="delete">删除节点</button>
        <button @click="$emit('cancel')" class="cancel">取消</button>
      </div>
      
      <div v-else-if="mode === 'input'" class="input-section">
        <input 
          v-model="inputText" 
          type="text" 
          placeholder="输入节点内容"
          @keyup.enter="handleConfirm"
          ref="inputRef"
          autofocus
        />
        <div class="input-actions">
          <button @click="handleConfirm" :disabled="!inputText.trim()">确认</button>
          <button @click="handleCancel" class="cancel">取消</button>
        </div>
      </div>
      
      <div v-else-if="mode === 'delete'" class="delete-section">
        <p>确定要删除这个节点吗？此操作不可撤销。</p>
        <div class="delete-actions">
          <button @click="$emit('delete')" class="delete">确认删除</button>
          <button @click="$emit('cancel')" class="cancel">取消</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, nextTick } from 'vue'

const props = defineProps<{
  visible: boolean
  title: string
  mode: 'choose' | 'input' | 'delete'
}>()

const emit = defineEmits<{
  newChild: []
  fold: []
  delete: []
  confirmInput: [text: string]
  cancel: []
}>()

const inputText = ref('')
const inputRef = ref<HTMLInputElement>()

const handleConfirm = () => {
  if (inputText.value.trim()) {
    emit('confirmInput', inputText.value.trim())
    // 自动清理输入内容
    inputText.value = ''
  }
}

const handleCancel = () => {
  // 清理输入内容
  inputText.value = ''
  emit('cancel')
}

const handleOverlayClick = () => {
  handleCancel()
}

// 监听modal显示状态，自动清理上一次的输入内容
watch(() => props.visible, (newVal, oldVal) => {
  if (newVal && !oldVal) {
    // 当modal显示时，清理上一次的输入内容
    inputText.value = ''
    nextTick(() => {
      if (props.mode === 'input' && inputRef.value) {
        inputRef.value.focus()
      }
    })
  }
  
  if (!newVal && oldVal) {
    // 当modal隐藏时，确保清理输入内容
    inputText.value = ''
  }
})

// 监听mode变化，清理输入内容
watch(() => props.mode, (newMode, oldMode) => {
  if (newMode === 'input' && oldMode !== 'input') {
    inputText.value = ''
    nextTick(() => {
      if (inputRef.value) {
        inputRef.value.focus()
      }
    })
  }
})
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1002;
}

.modal-content {
  background: white;
  border-radius: 8px;
  padding: 24px;
  min-width: 300px;
  max-width: 500px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
}

.modal-content h3 {
  margin: 0 0 16px 0;
  font-size: 18px;
  color: #333;
}

.action-buttons {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.action-buttons button {
  padding: 10px 16px;
  border: 1px solid #ddd;
  border-radius: 6px;
  background: white;
  cursor: pointer;
  font-size: 14px;
  text-align: left;
}

.action-buttons button:hover {
  background: #f5f5f5;
  border-color: #1890ff;
}

.action-buttons button.delete {
  color: #ff4d4f;
  border-color: #ff4d4f;
}

.action-buttons button.delete:hover {
  background: #fff2f0;
}

.action-buttons button.cancel {
  margin-top: 8px;
  border-color: #d9d9d9;
}

.input-section input {
  width: 100%;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 6px;
  margin-bottom: 16px;
  font-size: 14px;
}

.input-section input:focus {
  border-color: #1890ff;
  outline: none;
}

.input-actions, .delete-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}

.input-actions button, .delete-actions button {
  padding: 8px 16px;
  border: 1px solid #ddd;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
}

.input-actions button:not(.cancel), .delete-actions button:not(.cancel) {
  background: #1890ff;
  color: white;
  border-color: #1890ff;
}

.input-actions button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.delete-actions button.delete {
  background: #ff4d4f;
  color: white;
  border-color: #ff4d4f;
}

button.cancel {
  background: #f5f5f5;
  border-color: #d9d9d9;
}
</style>