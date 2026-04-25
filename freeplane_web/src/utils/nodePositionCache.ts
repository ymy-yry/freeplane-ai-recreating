type NodePosition = {
  x: number
  y: number
}

type NodePositionMap = Record<string, NodePosition>

const STORAGE_PREFIX = 'freeplane_web_node_positions_v1'

const storageKey = (mapId: string) => `${STORAGE_PREFIX}:${mapId}`

const parsePositionMap = (value: string | null): NodePositionMap => {
  if (!value) return {}
  try {
    const parsed = JSON.parse(value) as unknown
    if (!parsed || typeof parsed !== 'object') return {}
    const entries = Object.entries(parsed as Record<string, unknown>)
    const validEntries = entries.filter(([, pos]) => {
      if (!pos || typeof pos !== 'object') return false
      const p = pos as Record<string, unknown>
      return typeof p.x === 'number' && typeof p.y === 'number'
    })
    return Object.fromEntries(validEntries) as NodePositionMap
  } catch {
    return {}
  }
}

export const loadNodePositions = (mapId: string): NodePositionMap => {
  return parsePositionMap(localStorage.getItem(storageKey(mapId)))
}

export const hasNodePositions = (mapId: string): boolean => {
  return Object.keys(loadNodePositions(mapId)).length > 0
}

export const saveNodePosition = (
  mapId: string,
  nodeId: string,
  position: NodePosition
): void => {
  const current = loadNodePositions(mapId)
  current[nodeId] = { x: position.x, y: position.y }
  localStorage.setItem(storageKey(mapId), JSON.stringify(current))
}

export const applyNodePositions = <
  T extends { id: string; position: NodePosition }
>(
  mapId: string,
  nodes: T[]
): T[] => {
  const cache = loadNodePositions(mapId)
  if (Object.keys(cache).length === 0) return nodes
  return nodes.map((node) => {
    const position = cache[node.id]
    if (!position) return node
    return { ...node, position: { ...position } }
  })
}
