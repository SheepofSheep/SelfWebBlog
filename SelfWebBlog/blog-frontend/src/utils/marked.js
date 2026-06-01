import { marked } from 'marked'
import DOMPurify from 'dompurify'

marked.setOptions({
  breaks: true,
  gfm: true
})

const ARTICLE_TAGS = [
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
]

const COMMENT_TAGS = [
  'p', 'br',
  'strong', 'em', 'del',
  'a', 'img',
  'ul', 'ol', 'li',
  'code', 'pre',
  'blockquote'
]

const ARTICLE_ATTR = [
  'href', 'src', 'alt', 'title', 'class', 'id',
  'target', 'rel', 'width', 'height'
]

const COMMENT_ATTR = ['href', 'src', 'alt', 'title', 'target', 'rel']

const ARTICLE_POLICY = {
  stripClassAndId: false,
  allowRelativeLinks: true,
  allowRelativeImages: true,
  allowHttpImages: false
}

const COMMENT_POLICY = {
  stripClassAndId: true,
  allowRelativeLinks: false,
  allowRelativeImages: false,
  allowHttpImages: false,
  allowCommentImagesOnly: true
}

function getBaseUrl() {
  return typeof window === 'undefined' ? 'http://localhost' : window.location.origin
}

function toUrl(value) {
  try {
    return new URL(value, getBaseUrl())
  } catch {
    return null
  }
}

function isSafeLink(value, policy) {
  if (!value) return false
  const url = toUrl(value)
  if (!url) return false
  if (url.protocol === 'http:' || url.protocol === 'https:' || url.protocol === 'mailto:') return true
  if (policy.allowRelativeLinks && value.startsWith('/') && !value.startsWith('//')) return true
  return false
}

function isTrustedCommentImage(value) {
  if (!value) return false
  if (value.startsWith('/uploads/') || value.startsWith('/images/emojis/')) return true
  const url = toUrl(value)
  return url?.protocol === 'https:'
}

function isSafeImage(value, policy) {
  if (!value) return false
  if (policy.allowCommentImagesOnly) return isTrustedCommentImage(value)
  if (policy.allowRelativeImages && value.startsWith('/') && !value.startsWith('//')) return true
  const url = toUrl(value)
  if (!url) return false
  if (url.protocol === 'https:') return true
  return policy.allowHttpImages && url.protocol === 'http:'
}

function enforceAttributePolicy(policy) {
  return node => {
    if (policy.stripClassAndId) {
      node.removeAttribute('class')
      node.removeAttribute('id')
    }

    if (node.nodeName === 'A') {
      const href = node.getAttribute('href')
      if (!isSafeLink(href, policy)) {
        node.removeAttribute('href')
        node.removeAttribute('target')
      } else {
        const url = toUrl(href)
        if (url?.protocol === 'http:' || url?.protocol === 'https:') {
          node.setAttribute('target', '_blank')
          node.setAttribute('rel', 'noopener noreferrer')
        }
      }
    }

    if (node.nodeName === 'IMG') {
      const src = node.getAttribute('src')
      if (!isSafeImage(src, policy)) {
        node.remove()
      } else {
        node.setAttribute('loading', 'lazy')
      }
    }
  }
}

function sanitizeMarkdown(content, config, policy) {
  if (!content) return ''
  const raw = marked.parse(content)
  DOMPurify.addHook('afterSanitizeAttributes', enforceAttributePolicy(policy))
  try {
    return DOMPurify.sanitize(raw, config)
  } finally {
    DOMPurify.removeHook('afterSanitizeAttributes')
  }
}

export function renderArticleMarkdown(content) {
  return sanitizeMarkdown(content, {
    ALLOWED_TAGS: ARTICLE_TAGS,
    ALLOWED_ATTR: ARTICLE_ATTR
  }, ARTICLE_POLICY)
}

export function renderCommentMarkdown(content) {
  return sanitizeMarkdown(content, {
    ALLOWED_TAGS: COMMENT_TAGS,
    ALLOWED_ATTR: COMMENT_ATTR
  }, COMMENT_POLICY)
}

export function renderMarkdown(content) {
  return renderArticleMarkdown(content)
}
