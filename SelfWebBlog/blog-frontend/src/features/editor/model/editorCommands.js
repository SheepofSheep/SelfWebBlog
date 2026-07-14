const COMMANDS = {
  bold: ({ selected }) => ({ prefix: '**', content: selected || '粗体文字', suffix: '**' }),
  italic: ({ selected }) => ({ prefix: '*', content: selected || '斜体文字', suffix: '*' }),
  heading: ({ selected }) => ({ prefix: '## ', content: selected || '小标题', suffix: '' }),
  link: ({ selected }) => ({ prefix: '[', content: selected || '链接文字', suffix: '](https://)' }),
  codeBlock: ({ selected }) => ({
    prefix: '```\n',
    content: selected || '代码',
    suffix: '\n```'
  }),
  quote: ({ selected }) => ({ prefix: '> ', content: selected || '引用内容', suffix: '' }),
  task: () => ({ prefix: '', content: '- [ ] 待办事项', suffix: '' }),
  table: () => ({
    prefix: '',
    content: '| 列 1 | 列 2 |\n| --- | --- |\n| 内容 | 内容 |',
    suffix: ''
  })
}

export function applyEditorCommand(name, selection) {
  const command = COMMANDS[name]
  if (!command) throw new Error(`Unknown editor command: ${name}`)
  const from = Math.max(0, selection.from || 0)
  const to = Math.max(from, selection.to ?? from)
  const selected = selection.text.slice(from, to)
  const insertion = command({ selected })
  const replacement = insertion.prefix + insertion.content + insertion.suffix

  return {
    text: selection.text.slice(0, from) + replacement + selection.text.slice(to),
    selection: {
      from: from + insertion.prefix.length,
      to: from + insertion.prefix.length + insertion.content.length
    }
  }
}

export function uploadPlaceholder(taskId) {
  return `![正在上传](uploading:${taskId})`
}

export function replaceUploadPlaceholder(text, taskId, markdown) {
  return text.replace(uploadPlaceholder(taskId), markdown)
}
