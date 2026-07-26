export function legacyRouteTarget(locationValue) {
  if (typeof locationValue !== 'string') return null
  const hashIndex = locationValue.indexOf('#')
  if (hashIndex < 0) return null
  const hashRoute = locationValue.slice(hashIndex + 1)
  if (!hashRoute.startsWith('/')) return null

  const url = new URL(hashRoute, 'http://legacy.local')
  if (url.pathname === '/post') {
    const id = url.searchParams.get('id')
    if (/^\d+$/.test(id || '')) {
      url.searchParams.delete('id')
      const query = url.searchParams.toString()
      return `/post/${id}${query ? `?${query}` : ''}`
    }
  }
  return `${url.pathname}${url.search}`
}
