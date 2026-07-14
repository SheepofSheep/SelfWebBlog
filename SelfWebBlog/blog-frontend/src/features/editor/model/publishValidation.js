function issue(field, message) {
  return { field, message }
}

export function validatePost(post, status = 'PUBLISHED') {
  const issues = []
  const title = String(post.title || '').trim()
  const content = String(post.content || '').trim()

  if (status === 'DRAFT') {
    if (!title && !content) issues.push(issue('title', '草稿至少需要标题或正文。'))
    return { valid: issues.length === 0, issues }
  }

  if (!title) issues.push(issue('title', '请填写文章标题。'))
  if (!content) issues.push(issue('content', '请填写文章正文。'))
  if (!String(post.summary || '').trim()) issues.push(issue('summary', '请填写文章摘要。'))
  if (!String(post.coverUrl || '').trim()) issues.push(issue('coverUrl', '请选择文章封面。'))
  if (!String(post.category || '').trim()) issues.push(issue('category', '请选择文章分类。'))

  const tags = String(post.tags || '')
    .split(/[,，\n]+/)
    .map((tag) => tag.trim())
    .filter(Boolean)
  const normalized = tags.map((tag) => tag.toLocaleLowerCase())
  if (new Set(normalized).size !== normalized.length) {
    issues.push(issue('tags', '标签中有重复项。'))
  }
  if (/uploading:/.test(content)) issues.push(issue('content', '仍有图片正在上传。'))
  if (/upload-failed:/.test(content)) issues.push(issue('content', '有图片上传失败，请重试或移除。'))
  if (/!\[\]\([^)]+\)/.test(content)) issues.push(issue('content', '图片需要填写替代文本。'))

  return { valid: issues.length === 0, issues }
}
