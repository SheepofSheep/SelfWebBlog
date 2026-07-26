export const visibleReplies = (replies = [], expanded) => (expanded ? replies : [])

export function toggleReplyGroup(open, id) {
  const next = new Set(open)
  next.has(id) ? next.delete(id) : next.add(id)
  return next
}
