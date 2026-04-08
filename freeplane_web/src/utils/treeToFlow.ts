import type { Node, Edge } from '@vue-flow/core'
import type { MindMapNode } from '@/types/mindmap'

export const treeToFlow = (root: MindMapNode) => {
  const nodes: Node[] = []
  const edges: Edge[] = []
  let yOffset = 0
  const LEVEL_GAP = 340
  const SIBLING_GAP = 160

  const traverse = (node: MindMapNode, level: number, parentId: string | null) => {
    const x = level * LEVEL_GAP
    const y = yOffset

    const isFolded = !!node.folded
    const hasChildren = node.children && node.children.length > 0

    nodes.push({
      id: node.id,
      type: 'default',
      position: { x, y },
      data: {
        label: node.text + (isFolded && hasChildren ? ' ▼' : ''),
        text: node.text,
        note: node.note,
        originalNode: node,   // 保留原始节点用于判断 folded
        folded: isFolded
      },
      style: {
        width: 200,
        padding: '14px 18px',
        borderRadius: '10px',
        border: isFolded ? '2px solid #999' : '2px solid #1890ff',
        background: isFolded ? '#f8f9fa' : '#ffffff',
        fontSize: '15px',
        textAlign: 'center' as const,
        minHeight: '52px',
        opacity: isFolded ? 0.9 : 1,
      },
      connectable: true,
    })

    if (parentId) {
      edges.push({
        id: `${parentId}-${node.id}`,
        source: parentId,
        target: node.id,
        type: 'bezier',
        style: { stroke: '#666', strokeWidth: 2.5 },
      })
    }

    // 只有未折叠时才渲染子节点
    if (!isFolded && hasChildren) {
      const startY = yOffset
      node.children.forEach((child) => {
        yOffset += SIBLING_GAP
        traverse(child, level + 1, node.id)
      })
      yOffset = startY + (node.children.length * SIBLING_GAP) / 2
    } else {
      yOffset += SIBLING_GAP
    }
  }

  traverse(root, 0, null)
  return { nodes, edges }
}