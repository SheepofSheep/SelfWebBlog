const legacyMeta = {
  github: { label: 'GitHub', iconKey: 'github', order: 0 },
  bilibili: { label: '哔哩哔哩', iconKey: 'bilibili', order: 1 },
  website: { label: '网站', iconKey: 'website', order: 2 },
  email: { label: '邮箱', iconKey: 'mail', order: 3 }
}

function safeUrl(value) {
  return /^(?:https:\/\/|mailto:)[^\s<>]+$/i.test(value)
}

export function publicSiteLinks(site = {}) {
  if (Array.isArray(site?.links)) {
    return site.links
      .filter((link) => link?.enabled !== false && safeUrl(String(link?.url || '')))
      .slice()
      .sort((left, right) => Number(left.sortOrder || 0) - Number(right.sortOrder || 0))
      .map((link) => ({ ...link, id: String(link.id) }))
  }

  return Object.entries(site?.socialLinks || {})
    .map(([key, rawUrl]) => {
      const meta = legacyMeta[key] || { label: key, iconKey: 'link', order: 99 }
      const text = String(rawUrl || '').trim()
      const url =
        key === 'email' && !text.toLowerCase().startsWith('mailto:') ? `mailto:${text}` : text
      return {
        id: `legacy-${key}`,
        key,
        label: meta.label,
        iconKey: meta.iconKey,
        order: meta.order,
        url,
        enabled: true
      }
    })
    .filter((link) => safeUrl(link.url))
    .sort((left, right) => left.order - right.order)
}
