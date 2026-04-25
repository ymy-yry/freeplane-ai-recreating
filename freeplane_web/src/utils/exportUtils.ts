/**
 * 思维导图导出工具
 * 支持导出为 Freeplane .mm 格式和 JSON 格式
 */

import type { MindMapData, MindMapNode } from '@/types/mindmap'

/**
 * 导出为 Freeplane .mm 格式（XML）
 */
export function exportToMM(mindmap: MindMapData, filename?: string): void {
  const xmlContent = buildMMXML(mindmap)
  downloadFile(xmlContent, filename || `${mindmap.title}.mm`, 'application/xml')
}

/**
 * 导出为 JSON 格式
 */
export function exportToJSON(mindmap: MindMapData, filename?: string): void {
  const jsonContent = JSON.stringify(mindmap, null, 2)
  downloadFile(jsonContent, filename || `${mindmap.title}.json`, 'application/json')
}

/**
 * 构建 Freeplane .mm 格式的 XML 内容
 */
function buildMMXML(mindmap: MindMapData): string {
  const xmlDeclaration = '<?xml version="1.0" encoding="UTF-8"?>'
  const mapOpen = `<map version="1.0.1">`
  const mapClose = `</map>`
  
  const rootXML = nodeToXML(mindmap.root)
  
  return `${xmlDeclaration}
${mapOpen}
${rootXML}
${mapClose}`
}

/**
 * 递归将节点转换为 XML
 */
function nodeToXML(node: MindMapNode, indent: string = '  '): string {
  const attributes = [
    `ID="${node.id}"`,
    `TEXT="${escapeXML(node.text)}"`
  ].join(' ')
  
  let xml = `${indent}<node ${attributes}>`
  
  // 如果有备注
  if (node.note) {
    xml += `
${indent}  <richcontent TYPE="NOTE">
${indent}    <html>
${indent}      <body>
${indent}        <p>${escapeXML(node.note)}</p>
${indent}      </body>
${indent}    </html>
${indent}  </richcontent>`
  }
  
  // 如果有属性
  if (node.attributes && node.attributes.length > 0) {
    xml += `
${indent}  <attribute_registry>`
    for (const attr of node.attributes) {
      xml += `
${indent}    <attribute NAME="${escapeXML(attr.key)}" VALUE="${escapeXML(attr.value)}"/>`
    }
    xml += `
${indent}  </attribute_registry>`
  }
  
  // 递归处理子节点
  if (node.children && node.children.length > 0) {
    for (const child of node.children) {
      xml += '\n' + nodeToXML(child, indent + '  ')
    }
  }
  
  xml += `
${indent}</node>`
  
  return xml
}

/**
 * 转义 XML 特殊字符
 */
function escapeXML(text: string): string {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&apos;')
}

/**
 * 下载文件到本地
 */
function downloadFile(content: string, filename: string, mimeType: string): void {
  const blob = new Blob([content], { type: `${mimeType};charset=utf-8` })
  const url = URL.createObjectURL(blob)
  
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  link.style.display = 'none'
  
  document.body.appendChild(link)
  link.click()
  
  // 清理
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

/**
 * 显示导出对话框（让用户选择格式）
 */
export function showExportDialog(mindmap: MindMapData): void {
  const format = prompt(
    '选择导出格式：\n\n1 - Freeplane .mm 格式（推荐）\n2 - JSON 备份格式\n\n请输入数字（1 或 2）：',
    '1'
  )
  
  if (format === '1') {
    exportToMM(mindmap)
  } else if (format === '2') {
    exportToJSON(mindmap)
  }
}
