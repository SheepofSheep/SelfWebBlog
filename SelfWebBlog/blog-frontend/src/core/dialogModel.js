export function nextDialogFocusIndex(current, count, direction) {
  if (count <= 0) return -1
  const normalized = current < 0 ? (direction < 0 ? 0 : count - 1) : current
  return (normalized + direction + count) % count
}
