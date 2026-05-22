import { marked } from 'marked'
import DOMPurify from 'dompurify'

marked.setOptions({
  breaks: true,
  gfm: true
})

export function renderMarkdown(content) {
  if (!content) return ''
  const raw = marked.parse(content)
  return DOMPurify.sanitize(raw, {
    ALLOWED_TAGS: [
      'h1', 'h2', 'h3', 'h4', 'h5', 'h6',
      'p', 'br', 'hr',
      'strong', 'em', 'del',
      'a', 'img',
      'ul', 'ol', 'li',
      'code', 'pre',
      'blockquote',
      'table', 'thead', 'tbody', 'tr', 'th', 'td',
      'sup', 'sub',
      'span', 'div'
    ],
    ALLOWED_ATTR: [
      'href', 'src', 'alt', 'title', 'class', 'id',
      'target', 'rel', 'width', 'height'
    ]
  })
}
