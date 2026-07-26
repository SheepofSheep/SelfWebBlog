const LEGACY_KEY = 'postDraft'

export const draftKey = (id) => (id ? `postDraft:${id}` : 'postDraft:new')

export function readDraft(storage, key) {
  try {
    const value = storage.getItem(key)
    return value ? JSON.parse(value) : null
  } catch {
    return null
  }
}

export function writeDraft(storage, key, value) {
  storage.setItem(key, JSON.stringify(value))
}

export function removeDraft(storage, key) {
  storage.removeItem(key)
  storage.removeItem(LEGACY_KEY)
}

export function migrateLegacyDraft(storage) {
  const legacy = readDraft(storage, LEGACY_KEY)
  if (!legacy) return
  writeDraft(storage, draftKey(legacy.editingId), legacy)
  storage.removeItem(LEGACY_KEY)
}

export function stripLocalMedia(content = '') {
  return String(content)
    .replace(/!\[[^\]]*]\(blob:[^)]+\)/gi, '')
    .replace(/<(?:img|video|audio)\b[^>]*\bsrc=["']blob:[^"']+["'][^>]*>/gi, '')
}

export function hasUsefulDraft(value) {
  return Boolean(
    value?.title?.trim() ||
    stripLocalMedia(value?.content).trim() ||
    value?.summary?.trim() ||
    value?.coverUrl?.trim() ||
    value?.category?.trim() ||
    value?.tags?.length
  )
}
