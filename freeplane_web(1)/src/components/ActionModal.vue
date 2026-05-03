<template>
	<Teleport to="body">
	  <Transition name="modal">
		<div v-if="visible" class="modal-overlay" @click.self="handleCancel">
		  <div class="modal-content" @click.stop>
			<h3 class="modal-title">{{ title }}</h3>
			
			<!-- 选择操作模式 -->
			<div v-if="mode === 'choose'" class="action-buttons">
			  <button @click="handleNewChild" class="btn primary">➕ 新建子节点</button>
			  <button @click="handleFold" class="btn secondary">▼ 折叠节点</button>
			  <button @click="handleDelete" class="btn danger">🗑️ 删除节点</button>
			</div>
  
			<!-- 输入文本模式 -->
			<div v-else-if="mode === 'input'" class="input-mode">
			  <textarea
				v-model="inputText"
				rows="3"
				placeholder="请输入节点文本..."
				class="modal-textarea"
				autofocus
				@keydown.enter.exact.prevent="handleConfirmInput"
			  ></textarea>
			  <div class="modal-actions">
				<button @click="handleCancel" class="btn cancel">取消</button>
				<button @click="handleConfirmInput" class="btn primary">确定</button>
			  </div>
			</div>
  
			<!-- 删除确认模式（直接触发删除） -->
			<div v-else-if="mode === 'delete'" class="delete-mode">
			  <p class="delete-warning">确定要删除该节点及其所有子节点吗？</p>
			  <p class="delete-tip">此操作不可撤销</p>
			  <div class="modal-actions">
				<button @click="handleCancel" class="btn cancel">取消</button>
				<button @click="confirmDelete" class="btn danger">确认删除</button>
			  </div>
			</div>
		  </div>
		</div>
	  </Transition>
	</Teleport>
  </template>
  
  <script setup lang="ts">
  import { ref, watch } from 'vue'
  
  const props = defineProps<{
	visible: boolean
	title: string
	mode: 'choose' | 'input' | 'delete'
  }>()
  
  const emit = defineEmits<{
	(e: 'cancel'): void
	(e: 'new-child'): void
	(e: 'fold'): void
	(e: 'delete'): void
	(e: 'confirm-input', text: string): void
  }>()
  
  const inputText = ref('')
  
  const handleNewChild = () => emit('new-child')
  const handleFold = () => emit('fold')
  const handleDelete = () => emit('delete')
  const handleConfirmInput = () => {
	if (inputText.value.trim()) emit('confirm-input', inputText.value.trim())
  }
  const confirmDelete = () => emit('delete')   // 直接触发 delete 事件执行删除
  const handleCancel = () => emit('cancel')
  
  watch(() => props.visible, (val) => {
	if (val && props.mode === 'input') inputText.value = ''
  })
  </script>
  
  <style scoped>
  /* 样式保持不变 */
  .modal-overlay {
	position: fixed; top: 0; left: 0; right: 0; bottom: 0;
	background: rgba(0, 0, 0, 0.65); display: flex; align-items: center; justify-content: center; z-index: 10000;
  }
  
  .modal-content {
	background: white; border-radius: 12px; width: 400px; padding: 24px;
	box-shadow: 0 12px 40px rgba(0, 0, 0, 0.35); animation: modalPop 0.25s ease;
  }
  
  .modal-title { margin: 0 0 20px 0; font-size: 18px; text-align: center; color: #333; }
  
  .action-buttons { display: flex; flex-direction: column; gap: 12px; }
  
  .btn {
	padding: 14px 20px; border: none; border-radius: 8px; font-size: 16px; cursor: pointer; transition: all 0.2s;
  }
  
  .primary { background: #1890ff; color: white; }
  .primary:hover { background: #1677ff; }
  
  .secondary { background: #f0f0f0; color: #333; }
  .secondary:hover { background: #e0e0e0; }
  
  .danger { background: #ff4d4f; color: white; }
  .danger:hover { background: #ff3739; }
  
  .delete-mode { text-align: center; }
  .delete-warning { color: #d32f2f; font-size: 16px; margin: 12px 0; }
  .delete-tip { color: #666; font-size: 14px; }
  
  .modal-textarea { width: 100%; padding: 12px; border: 1px solid #ddd; border-radius: 8px; font-size: 15px; resize: none; min-height: 80px; }
  
  .modal-actions { display: flex; justify-content: flex-end; gap: 10px; }
  
  @keyframes modalPop { from { transform: scale(0.92); opacity: 0; } to { transform: scale(1); opacity: 1; } }
  </style>