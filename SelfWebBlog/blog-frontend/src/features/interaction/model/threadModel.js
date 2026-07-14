function cloneInteraction(item) {
  return {
    ...item,
    replies: Array.isArray(item.replies)
      ? item.replies.map((reply) => ({ ...reply, replies: [] }))
      : []
  }
}

export function normalizeThread(input) {
  const source = Array.isArray(input) ? input : input?.items || input?.content || []
  const roots = []
  const rootsById = new Map()
  const replies = []

  source.forEach((item) => {
    const normalized = cloneInteraction(item)
    if (item.rootId || item.parentId || item.replyToId) {
      replies.push(normalized)
      return
    }
    roots.push(normalized)
    rootsById.set(String(item.id), normalized)
  })

  replies.forEach((reply) => {
    const rootId = reply.rootId || reply.parentId || reply.replyToId
    const root = rootsById.get(String(rootId))
    if (root) root.replies.push(reply)
  })

  return roots
}

export function applyOptimisticLike(item, desired) {
  const rollback = { ...item }
  const liked = Boolean(item.liked)
  const delta = desired === liked ? 0 : desired ? 1 : -1

  return {
    next: {
      ...item,
      liked: desired,
      likeCount: Math.max(0, Number(item.likeCount || 0) + delta)
    },
    rollback
  }
}

export function insertReply(roots, rootId, reply) {
  return roots.map((root) =>
    String(root.id) === String(rootId)
      ? { ...root, replies: [...(root.replies || []), { ...reply, replies: [] }] }
      : root
  )
}
