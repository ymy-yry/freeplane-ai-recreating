import type { Node, Edge } from '@vue-flow/core'
import type { MindMapNode } from '@/types/mindmap'

export const treeToFlow = (root: MindMapNode) => {
  const nodes: Node[] = []
  const edges: Edge[] = []
  let yOffset = 0
  const LEVEL_GAP = 280
  const SIBLING_GAP = 110

  const traverse = (node: MindMapNode, level: number, parentId: string | null) => {
    const x = level * LEVEL_GAP
    const y = yOffset

    nodes.push({
      id: node.id,
      type: 'default',
      position: { x, y },
      data: { text: node.text, note: node.note, originalNode: node },
      style: { width: 180 }
    })

    if (parentId) {
      edges.push({ id: `${parentId}-${node.id}`, source: parentId, target: node.id, type: 'smoothstep' })
    }

    if (!node.folded && node.children?.length) {
      const startY = yOffset
      node.children.forEach(child => {
        yOffset += SIBLING_GAP
        traverse(child, level + 1, node.id)
      })
      yOffset = startY + (node.children.length * SIBLING_GAP) / 2
    }
  }

  traverse(root, 0, null)
  return { nodes, edges }
}