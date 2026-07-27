export function filterArticles(posts = [], query = '') {
  const keyword = String(query).normalize('NFKC').trim().toLocaleLowerCase()
  if (!keyword) return posts
  return posts.filter((post) =>
    [post.title, post.summary, post.content, post.category, post.tags]
      .join(' ')
      .normalize('NFKC')
      .toLocaleLowerCase()
      .includes(keyword)
  )
}
