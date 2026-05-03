<template>
	<div v-if="visible" class="edit-panel">
	  <div class="panel-content">
		<textarea
		  ref="textareaRef"
		  v-model="editText"
		  rows="3"
		  placeholder="输入节点文本..."
		  @keydown.enter.exact.prevent="handleSave"
		  @blur="handleSave"
		></textarea>
		<div class="actions">
		  <button @click="handleCancel" class="cancel-btn">取消</button>
		  <button @click="handleSave" class="save-btn">保存</button>
		</div>
	  </div>
	</div>
  </template>
  
  <script setup lang="ts">
  import { ref, onMounted, nextTick } from 'vue'
  
  const props = defineProps<{
	visible: boolean
	nodeId: string
	initialText: string
  }>()
  
  const emit = defineEmits<{
	(e: 'save', nodeId: string, text: string): void
	(e: 'cancel'): void
  }>()
  
  const editText = ref(props.initialText)
  const textareaRef = ref<HTMLTextAreaElement | null>(null)
  
  const handleSave = () => {
	const trimmed = editText.value.trim()
	if (trimmed) {
	  emit('save', props.nodeId, trimmed)
	} else {
	  emit('cancel')
	}
  }
  
  const handleCancel = () => {
	emit('cancel')
  }
  
  onMounted(() => {
	if (props.visible) {
	  nextTick(() => {
		textareaRef.value?.focus()
		textareaRef.value?.select()
	  })
	}
  })
  </script>
  
  <style scoped>
  .edit-panel {
	position: fixed;
	top: 50%;
	left: 50%;
	transform: translate(-50%, -50%);
	background: #fff;
	border: 1px solid #1890ff;
	border-radius: 8px;
	box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2);
	z-index: 10000;
	padding: 16px;
	width: 320px;
  }
  
  .panel-content {
	display: flex;
	flex-direction: column;
	gap: 12px;
  }
  
  textarea {
	width: 100%;
	padding: 10px;
	border: 1px solid #ddd;
	border-radius: 4px;
	font-size: 15px;
	resize: none;
	outline: none;
	line-height: 1.5;
  }
  
  .actions {
	display: flex;
	justify-content: flex-end;
	gap: 8px;
  }
  
  button {
	padding: 6px 16px;
	border-radius: 4px;
	cursor: pointer;
	font-size: 14px;
  }
  
  .cancel-btn {
	background: #f0f0f0;
	border: 1px solid #ddd;
  }
  
  .save-btn {
	background: #1890ff;
	color: white;
	border: none;
  }
  </style>