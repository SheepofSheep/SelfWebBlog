// Markdown stays the stored format. Only these presentation directives are supported.
export const presentationKinds = new Set([
  'align-left',
  'align-center',
  'align-right',
  'size-small',
  'size-normal',
  'size-large',
  'size-xlarge'
])

function existingPresentation(text, start, end, family) {
  const stack = []
  const matches = []
  let offset = 0
  let fence = null
  for (const line of text.split('\n')) {
    const codeFence = /^\s{0,3}(`{3,}|~{3,})/.exec(line)
    if (codeFence) {
      if (!fence) fence = codeFence[1]
      else if (codeFence[1][0] === fence[0] && codeFence[1].length >= fence.length) fence = null
    }
    if (!fence) {
      const opening = /^::: ([\w-]+)\s*$/.exec(line)
      if (opening && presentationKinds.has(opening[1]))
        stack.push({ kind: opening[1], from: offset, to: offset + line.length })
      else if (/^:::\s*$/.test(line) && stack.length) {
        const block = stack.pop()
        if (
          block.kind.startsWith(`${family}-`) &&
          ((block.from <= start && offset + line.length >= end) ||
            (block.from >= start && offset + line.length <= end))
        )
          matches.push(block)
      }
    }
    offset += line.length + 1
  }
  return matches.sort((a, b) => a.from - b.from)
}

export function formatMarkdown(source, from, to, action, value) {
  const text = String(source || '')
  const start = Math.max(0, Math.min(from, text.length))
  const end = Math.max(start, Math.min(to, text.length))
  const selected = text.slice(start, end)
  const inline = {
    bold: ['**', '**', '粗体文字'],
    italic: ['*', '*', '斜体文字'],
    strike: ['~~', '~~', '删除线文字'],
    code: ['`', '`', '代码'],
    link: ['[', '](https://example.com)', '链接文字']
  }
  if (inline[action]) {
    const [left, right, placeholder] = inline[action]
    if (
      action !== 'link' &&
      text.slice(start - left.length, start) === left &&
      text.slice(end, end + right.length) === right
    ) {
      return {
        from: start - left.length,
        to: end + right.length,
        insert: selected,
        anchor: start - left.length,
        head: end - left.length
      }
    }
    const body = selected || placeholder
    return {
      from: start,
      to: end,
      insert: left + body + right,
      anchor: start + left.length,
      head: start + left.length + body.length
    }
  }
  // Block commands always include whole lines, so they cannot split a Markdown token.
  const lineStart = text.lastIndexOf('\n', start - 1) + 1
  const nextLine = text.indexOf('\n', Math.max(start, end - (text[end - 1] === '\n' ? 1 : 0)))
  const lineEnd = nextLine < 0 ? text.length : nextLine
  const block = text.slice(lineStart, lineEnd) || '在这里输入文字'
  let insert
  if (action === 'align' || action === 'size') {
    const kind = `${action}-${value}`
    if (!presentationKinds.has(kind)) return null
    const existing = existingPresentation(text, start, end, action)
    if (existing.length) {
      const from = existing[0].from
      const to = existing.at(-1).to
      let insert = text.slice(from, to)
      const header = `::: ${kind}`
      for (const item of [...existing].reverse())
        insert = insert.slice(0, item.from - from) + header + insert.slice(item.to - from)
      const map = (position) =>
        position +
        existing.reduce(
          (delta, item) =>
            delta + (position >= item.to ? header.length - (item.to - item.from) : 0),
          0
        )
      return { from, to, insert, anchor: map(start), head: map(end) }
    }
    insert = `::: ${kind}\n${block}\n:::`
  } else if (action === 'heading') {
    const level = Number(value)
    if (!Number.isInteger(level) || level < 0 || level > 3) return null
    insert = block
      .split('\n')
      .map((line) => `${'#'.repeat(level)}${level ? ' ' : ''}${line.replace(/^#{1,6}\s+/, '')}`)
      .join('\n')
  } else if (action === 'quote')
    insert = block
      .split('\n')
      .map((line) => `> ${line}`)
      .join('\n')
  else if (action === 'bullet')
    insert = block
      .split('\n')
      .map((line) => `- ${line}`)
      .join('\n')
  else if (action === 'ordered')
    insert = block
      .split('\n')
      .map((line, i) => `${i + 1}. ${line}`)
      .join('\n')
  else if (action === 'task')
    insert = block
      .split('\n')
      .map((line) => `- [ ] ${line}`)
      .join('\n')
  else if (action === 'codeblock') insert = `\`\`\`text\n${block}\n\`\`\``
  else if (action === 'table')
    insert = `${block === '在这里输入文字' ? '' : block + '\n\n'}| 标题 | 内容 |\n| --- | --- |\n| 项目 | 说明 |`
  else if (action === 'divider') insert = `${block === '在这里输入文字' ? '' : block + '\n\n'}---\n`
  else return null
  // Blank lines ensure directives/tables after a paragraph form their own block.
  const prefix = lineStart > 0 && text[lineStart - 2] !== '\n' ? '\n' : ''
  return {
    from: lineStart,
    to: lineEnd,
    insert: prefix + insert,
    anchor: lineStart + prefix.length,
    head: lineStart + prefix.length + insert.length
  }
}
