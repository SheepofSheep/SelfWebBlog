export const visibleReplies = (replies = [], expanded) => (expanded ? replies : [])

export const scrollBehaviorForMotion = (reducedMotion) => (reducedMotion ? 'auto' : 'smooth')

export function createLatestLikeQueue(initial = {}, handlers = {}) {
  let confirmedLiked = Boolean(initial.liked)
  let confirmedCount = Math.max(0, Number(initial.count) || 0)
  let desiredLiked = confirmedLiked
  let pending = false
  let running = null

  function snapshot() {
    const optimisticDelta = desiredLiked === confirmedLiked ? 0 : desiredLiked ? 1 : -1
    return {
      liked: desiredLiked,
      count: Math.max(0, confirmedCount + optimisticDelta),
      pending
    }
  }

  function publish() {
    handlers.onChange?.(snapshot())
  }

  async function drain() {
    while (desiredLiked !== confirmedLiked) {
      const requestedLiked = desiredLiked
      try {
        const response = (await handlers.request?.(requestedLiked)) || {}
        const responseLiked = Boolean(response.liked ?? requestedLiked)
        const responseCount = Number(response.count ?? response.likeCount)
        confirmedLiked = responseLiked
        if (Number.isFinite(responseCount)) confirmedCount = Math.max(0, responseCount)
        if (responseLiked !== requestedLiked && desiredLiked === requestedLiked) {
          desiredLiked = responseLiked
        }
      } catch (error) {
        if (desiredLiked === requestedLiked) desiredLiked = confirmedLiked
        handlers.onError?.(error)
      }
      publish()
    }
  }

  function toggle() {
    desiredLiked = !desiredLiked
    if (!pending) {
      pending = true
      running = drain().finally(() => {
        pending = false
        running = null
        publish()
      })
    }
    publish()
    return running
  }

  return {
    toggle,
    snapshot,
    waitForIdle: () => running || Promise.resolve(snapshot())
  }
}

export function toggleReplyGroup(open, id) {
  const next = new Set(open)
  next.has(id) ? next.delete(id) : next.add(id)
  return next
}

function timestamp(value) {
  const time = new Date(value || 0).getTime()
  return Number.isNaN(time) ? 0 : time
}

export function sortPinned(items = []) {
  return [...items].sort((left, right) => {
    if (Boolean(left.pinned) !== Boolean(right.pinned)) return left.pinned ? -1 : 1
    if (left.pinned && right.pinned) {
      const leftOrder = Number.isFinite(Number(left.pinOrder)) ? Number(left.pinOrder) : Infinity
      const rightOrder = Number.isFinite(Number(right.pinOrder)) ? Number(right.pinOrder) : Infinity
      if (leftOrder !== rightOrder) return leftOrder - rightOrder
    }
    return timestamp(right.createTime) - timestamp(left.createTime)
  })
}

export function mergeThreadRoots(current = [], incoming = []) {
  const byId = new Map(current.map((item) => [String(item.id), item]))
  incoming.forEach((item) => {
    const id = String(item.id)
    byId.set(id, { ...byId.get(id), ...item })
  })
  return sortPinned([...byId.values()])
}

export function appendThreadReply(items = [], rootId, reply) {
  const targetRootId = String(rootId)
  return items.map((root) => {
    if (String(root.id) !== targetRootId) return root
    const replies = new Map((root.replies || []).map((item) => [String(item.id), item]))
    const replyId = String(reply.id)
    replies.set(replyId, { ...replies.get(replyId), ...reply })
    return { ...root, replies: [...replies.values()] }
  })
}

export function replyTargetName(root, reply, replies = []) {
  if (reply?.replyToNickname) return reply.replyToNickname
  const targetId = String(reply?.replyToId || '')
  if (!targetId) return ''
  if (String(root?.id) === targetId) return root.nickname || root.guestName || '访客'
  const target = replies.find((item) => String(item.id) === targetId)
  return target?.nickname || target?.guestName || ''
}

export function patchThreadItem(items = [], id, update) {
  const targetId = String(id)
  return items.map((root) => {
    if (String(root.id) === targetId) return update(root)
    const replies = (root.replies || []).map((reply) =>
      String(reply.id) === targetId ? update(reply) : reply
    )
    return replies.some((reply, index) => reply !== (root.replies || [])[index])
      ? { ...root, replies }
      : root
  })
}
