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
