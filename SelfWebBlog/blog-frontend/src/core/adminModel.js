function isoDay(value) {
  const date = value instanceof Date ? new Date(value) : new Date(`${value}T12:00:00`)
  if (Number.isNaN(date.getTime())) return ''
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const INTERACTION_TRANSITIONS = Object.freeze({
  PENDING: Object.freeze(['PUBLISHED', 'SPAM']),
  PUBLISHED: Object.freeze(['HIDDEN', 'SPAM']),
  HIDDEN: Object.freeze(['PUBLISHED']),
  SPAM: Object.freeze(['HIDDEN'])
})

export function createAdminRequestGate() {
  let current = 0
  return {
    begin() {
      current += 1
      return current
    },
    isCurrent(ticket) {
      return ticket === current
    },
    runIfCurrent(ticket, callback) {
      if (ticket !== current) return false
      callback()
      return true
    }
  }
}

export function mergeAssetReferenceDetails(asset = {}, details = {}) {
  return {
    ...asset,
    referenceStatus: details.referenceStatus ?? asset.referenceStatus,
    referenceCount: details.referenceCount ?? asset.referenceCount
  }
}

export function allowedInteractionTransitions(status) {
  return [...(INTERACTION_TRANSITIONS[status] || [])]
}

export function sharedInteractionTransitions(interactions = []) {
  if (!interactions.length) return []
  const [first, ...rest] = interactions
  return allowedInteractionTransitions(first?.status).filter((target) =>
    rest.every((item) => allowedInteractionTransitions(item?.status).includes(target))
  )
}

export function canPinInteraction(interaction) {
  return interaction?.status === 'PUBLISHED' && interaction?.rootId == null
}

export function canMovePinnedInteraction(interaction) {
  return canPinInteraction(interaction) && Boolean(interaction?.pinned)
}

export function canReplyToInteraction(interaction) {
  return interaction?.status === 'PUBLISHED'
}

export function pageAfterMutation(currentPage, records = []) {
  const page = Math.max(1, Number(currentPage) || 1)
  return records.length || page === 1 ? page : page - 1
}

export function normalizeAdminUserPage(value, requestedPage = 1, requestedSize = 20) {
  const source = value && !Array.isArray(value) ? value : {}
  const records = Array.isArray(value)
    ? value
    : Array.isArray(source.records)
      ? source.records
      : Array.isArray(source.users)
        ? source.users
        : []
  const size = Math.max(1, Number(source.size) || Number(requestedSize) || 20)
  const total = Math.max(0, Number(source.total) || records.length)
  return {
    records,
    current: Math.max(1, Number(source.current) || Number(requestedPage) || 1),
    pages: Math.max(1, Number(source.pages) || Math.ceil(total / size) || 1),
    total,
    size,
    duplicateIpCount: Math.max(0, Number(source.duplicateIpCount) || 0)
  }
}

export function syncSiteAvatar(store, avatarUrl) {
  store.site = { ...(store.site || {}), avatarUrl }
  return store.site
}

export function fillTrend(days = [], range = 14, endDate = isoDay(new Date())) {
  const length = Math.max(1, Number(range) || 14)
  const end = new Date(`${endDate}T12:00:00`)
  if (Number.isNaN(end.getTime())) return []
  const byDate = new Map(days.map((item) => [item.date, item]))
  return Array.from({ length }, (_, index) => {
    const date = new Date(end)
    date.setDate(end.getDate() - (length - index - 1))
    const key = isoDay(date)
    const item = byDate.get(key) || {}
    return {
      date: key,
      views: Math.max(0, Number(item.views) || 0),
      interactions: Math.max(0, Number(item.interactions) || 0),
      posts: Math.max(0, Number(item.posts) || 0)
    }
  })
}

export function chartGeometry(
  values = [],
  width = 720,
  height = 240,
  padding = 20,
  scaleMaximum = 0
) {
  const safeValues = values.map((value) => Math.max(0, Number(value) || 0))
  const usableWidth = Math.max(1, width - padding * 2)
  const usableHeight = Math.max(1, height - padding * 2)
  const maximum = Math.max(1, Number(scaleMaximum) || 0, ...safeValues)
  const denominator = Math.max(1, safeValues.length - 1)
  const points = safeValues.map((value, index) => ({
    x: padding + (index / denominator) * usableWidth,
    y: padding + usableHeight - (value / maximum) * usableHeight
  }))
  const path = points.length
    ? points
        .map((point, index) => `${index ? 'L' : 'M'} ${point.x.toFixed(2)} ${point.y.toFixed(2)}`)
        .join(' ')
    : ''
  return { points, path, maximum }
}

export function moveOrderedItem(items = [], id, direction) {
  const next = items.slice()
  const index = next.findIndex((item) => String(item.id) === String(id))
  if (index < 0) return next
  const target = direction === 'UP' ? index - 1 : direction === 'DOWN' ? index + 1 : index
  if (target < 0 || target >= next.length || target === index) return next
  ;[next[index], next[target]] = [next[target], next[index]]
  return next
}

export function formatBytes(value) {
  const bytes = Math.max(0, Number(value) || 0)
  if (bytes < 1024) return `${Math.round(bytes)} B`
  const units = ['KB', 'MB', 'GB']
  let amount = bytes / 1024
  let unit = 0
  while (amount >= 1024 && unit < units.length - 1) {
    amount /= 1024
    unit += 1
  }
  const precision = amount >= 10 || Number.isInteger(amount) ? 0 : 1
  return `${amount.toFixed(precision)} ${units[unit]}`
}
