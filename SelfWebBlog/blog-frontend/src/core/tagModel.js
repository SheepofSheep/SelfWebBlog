export function normalizeTags(values = []) {
  const seen = new Set()
  return values.reduce((tags, value) => {
    const tag = String(value ?? '')
      .normalize('NFKC')
      .trim()
      .replace(/\s+/g, ' ')
    const key = tag.toLocaleLowerCase()
    if (tag && tag.length <= 30 && !seen.has(key)) {
      seen.add(key)
      tags.push(tag)
    }
    return tags
  }, [])
}

export function commitTag(tags, input) {
  return { tags: normalizeTags([...tags, input]), input: '' }
}

export function prepareTagCreation(tags = [], input = '') {
  const [normalized = ''] = normalizeTags([input])
  if (!normalized) return { name: '', existing: null, shouldCreate: false }
  const key = normalized.toLocaleLowerCase()
  const existing = tags.find((tag) => {
    const [name = ''] = normalizeTags([tag?.name])
    return name.toLocaleLowerCase() === key
  })
  return existing
    ? { name: existing.name, existing, shouldCreate: false }
    : { name: normalized, existing: null, shouldCreate: true }
}
