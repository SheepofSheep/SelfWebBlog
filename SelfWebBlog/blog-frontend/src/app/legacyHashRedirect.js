export function legacyHashTarget(locationValue) {
  if (typeof locationValue !== 'string') return null

  const hashIndex = locationValue.indexOf('#')
  if (hashIndex < 0) return null

  const hashRoute = locationValue.slice(hashIndex + 1)
  if (!hashRoute.startsWith('/')) return null

  const url = new URL(hashRoute, 'http://localhost')
  if (url.pathname === '/post') {
    const postId = url.searchParams.get('id')
    if (/^\d+$/.test(postId || '')) {
      url.searchParams.delete('id')
      const query = url.searchParams.toString()
      return `/post/${postId}${query ? `?${query}` : ''}`
    }
  }

  return `${url.pathname}${url.search}${url.hash}`
}
