export interface MindMapNode {
	id: string
	text: string
	parentId: string | null
	folded: boolean
	note: string
	attributes?: { key: string; value: string }[]
	children: MindMapNode[]
  }
  
  export interface MindMapData {
	mapId: string
	title: string
	root: MindMapNode
  }