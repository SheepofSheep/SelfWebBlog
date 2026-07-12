export const LEGACY_DRAFT_KEY = 'postDraft'
export const NEW_DRAFT_KEY = 'postDraft:new'

export function getDraftKey(id) {
  return id ? `postDraft:${id}` : NEW_DRAFT_KEY
}

export function readDraft(storage, key) {
  const raw = storage.getItem(key)
  if (!raw) return null
  try {
    return JSON.parse(raw)
  } catch {
    return null
  }
}

export function writeDraft(storage, key, draft) {
  storage.setItem(key, JSON.stringify(draft))
}

export function removeDraft(storage, key) {
  storage.removeItem(key)
  storage.removeItem(LEGACY_DRAFT_KEY)
}

export function migrateLegacyDraft(storage) {
  const legacy = readDraft(storage, LEGACY_DRAFT_KEY)
  if (!legacy) return
  writeDraft(storage, getDraftKey(legacy.editingId || null), legacy)
  storage.removeItem(LEGACY_DRAFT_KEY)
}

export function hasUsefulDraft(draft) {
  return Boolean(
    draft?.title ||
    draft?.content ||
    draft?.summary ||
    draft?.coverUrl ||
    draft?.category ||
    (Array.isArray(draft?.tags) ? draft.tags.length : draft?.tags)
  )
}
