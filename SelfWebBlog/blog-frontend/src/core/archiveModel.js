function postTime(post) {
  const value = new Date(post?.publishedTime || post?.createTime || 0).getTime()
  return Number.isNaN(value) ? 0 : value
}

export function archivePostDate(post) {
  return post?.publishedTime || post?.createTime || ''
}

export function groupArchive(posts = []) {
  const groups = new Map()
  for (const post of [...posts].sort((left, right) => postTime(right) - postTime(left))) {
    const publicationDate = archivePostDate(post)
    if (!publicationDate) continue
    const date = new Date(publicationDate)
    if (Number.isNaN(date.getTime())) continue
    const year = date.getFullYear()
    if (!groups.has(year)) groups.set(year, [])
    groups.get(year).push(post)
  }
  return [...groups.entries()]
    .sort(([left], [right]) => right - left)
    .map(([year, groupedPosts]) => ({ year, posts: groupedPosts }))
}

export function filterArchivePosts(posts = [], query = '') {
  const normalized = String(query).trim().toLocaleLowerCase()
  if (!normalized) return posts
  return posts.filter((post) => {
    const tags = Array.isArray(post?.tags) ? post.tags.join(' ') : post?.tags
    return [post?.title, post?.summary, post?.content, tags]
      .map((value) => String(value || '').toLocaleLowerCase())
      .some((value) => value.includes(normalized))
  })
}

export async function collectAllPosts(fetchPage, pageSize = 100) {
  const collected = []
  let page = 1
  let collecting = true
  while (collecting) {
    const result = (await fetchPage(page, pageSize)) || {}
    const records = Array.isArray(result.records) ? result.records : []
    collected.push(...records)

    const totalPages = Number(result.pages || 0)
    const total = Number(result.total)
    const hasMore = totalPages
      ? page < totalPages
      : Number.isFinite(total)
        ? collected.length < total
        : records.length === pageSize
    collecting = hasMore && records.length > 0
    if (collecting) page += 1
  }
  return collected
}
