export function buildMonthCells(year, month, days = []) {
  const offset = (new Date(year, month - 1, 1).getDay() + 6) % 7
  const total = new Date(year, month, 0).getDate()
  const byDay = new Map(days.map((item) => [Number(String(item.date).slice(-2)), item]))
  return [
    ...Array.from({ length: offset }, () => null),
    ...Array.from({ length: total }, (_, index) => ({
      day: index + 1,
      count: 0,
      posts: [],
      ...byDay.get(index + 1)
    }))
  ]
}
