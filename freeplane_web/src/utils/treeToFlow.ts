import type { Node, Edge } from '@vue-flow/core'
import type { MindMapNode } from '@/types/mindmap'

export const treeToFlow = (root: MindMapNode) => {
  const nodes: Node[] = []
  const edges: Edge[] = []
  const LEVEL_GAP = 300
  const SIBLING_GAP = 90
  const NODE_HALF_HEIGHT = 32
  const ROOT_X = 0
  const ROOT_Y = 0
  const BRANCH_COLORS = ['#2f8f5b', '#446fd6', '#a85cc8', '#d4872f', '#2f9eb2', '#b75a75']

  const visibleChildren = (node: MindMapNode) => (node.folded ? [] : (node.children || []))

  const branchColor = (branchIndex: number) => BRANCH_COLORS[Math.abs(branchIndex) % BRANCH_COLORS.length]

  const alpha = (hex: string, value: string) => `${hex}${value}`

  const subtreeSpan = (node: MindMapNode): number => {
    const children = visibleChildren(node)
    if (children.length === 0) return SIBLING_GAP
    return Math.max(
      SIBLING_GAP,
      children.map(subtreeSpan).reduce((a, b) => a + b, 0)
    )
  }

  const pushNode = (
    node: MindMapNode,
    x: number,
    y: number,
    depth: number,
    branchIndex: number
  ) => {
    const isFolded = !!node.folded
    const hasChildren = node.children && node.children.length > 0
    const color = branchColor(branchIndex)
    const borderColor = isFolded ? '#95a5a6' : color
    const bg = isFolded ? '#f5f6f7' : alpha(color, depth === 0 ? '14' : '0D')

    nodes.push({
      id: node.id,
      type: 'default',
      position: { x, y },
      data: {
        label: node.text + (isFolded && hasChildren ? ' ▼' : ''),
        text: node.text,
        note: node.note,
        originalNode: node,
        folded: isFolded,
      },
      style: {
        width: depth === 0 ? 220 : 200,
        padding: '12px 16px',
        borderRadius: depth === 0 ? '16px' : '12px',
        border: `2px solid ${borderColor}`,
        background: bg,
        fontSize: depth <= 1 ? '15px' : '14px',
        fontWeight: depth === 0 ? '700' : '500',
        textAlign: 'center' as const,
        minHeight: '52px',
        color: '#1f2937',
      },
      connectable: true,
    })
  }

  const pushEdge = (
    sourceId: string,
    targetId: string,
    branchIndex: number
  ) => {
    const color = branchColor(branchIndex)
    edges.push({
      id: `${sourceId}-${targetId}`,
      source: sourceId,
      target: targetId,
      type: 'bezier',
      style: { stroke: alpha(color, 'AA'), strokeWidth: 2.2 },
    })
  }

  const layoutBranch = (
    node: MindMapNode,
    x: number,
    centerY: number,
    direction: 1 | -1,
    depth: number,
    branchIndex: number,
    parentId: string | null
  ) => {
    pushNode(node, x, centerY - NODE_HALF_HEIGHT, depth, branchIndex)
    if (parentId) pushEdge(parentId, node.id, branchIndex)

    const children = visibleChildren(node)
    if (children.length === 0) return

    const totalSpan = children.map(subtreeSpan).reduce((a, b) => a + b, 0)
    let cursorY = centerY - totalSpan / 2
    children.forEach((child) => {
      const span = subtreeSpan(child)
      const childCenterY = cursorY + span / 2
      layoutBranch(
        child,
        x + direction * LEVEL_GAP,
        childCenterY,
        direction,
        depth + 1,
        branchIndex,
        node.id
      )
      cursorY += span
    })
  }

  // 根节点始终居中
  pushNode(root, ROOT_X, ROOT_Y - NODE_HALF_HEIGHT, 0, 0)

  const rootChildren = visibleChildren(root)
  const leftChildren: MindMapNode[] = []
  const rightChildren: MindMapNode[] = []
  rootChildren.forEach((child, index) => {
    if (index % 2 === 0) rightChildren.push(child)
    else leftChildren.push(child)
  })

  const layoutSide = (
    sideChildren: MindMapNode[],
    direction: 1 | -1,
    sideIndexOffset: number
  ) => {
    if (sideChildren.length === 0) return
    const totalSpan = sideChildren.map(subtreeSpan).reduce((a, b) => a + b, 0)
    let cursorY = ROOT_Y - totalSpan / 2
    sideChildren.forEach((child, index) => {
      const span = subtreeSpan(child)
      const childCenterY = cursorY + span / 2
      layoutBranch(
        child,
        ROOT_X + direction * LEVEL_GAP,
        childCenterY,
        direction,
        1,
        sideIndexOffset + index,
        root.id
      )
      cursorY += span
    })
  }

  layoutSide(rightChildren, 1, 0)
  layoutSide(leftChildren, -1, rightChildren.length)

  return { nodes, edges }
}