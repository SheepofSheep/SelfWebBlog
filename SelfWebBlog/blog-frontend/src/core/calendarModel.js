export function buildMonthCells(year, month, days = []) {
  const offset = (new Date(year, month - 1, 1).getDay() + 6) % 7
  const total = new Date(year, month, 0).getDate()
  const byDay = new Map(days.map((item) => [Number(String(item.date).slice(-2)), item]))
  return [
    ...Array.from({ length: offset }, () => null),
    ...Array.from({ length: total }, (_, index) => ({
      day: index + 1,
      date: `${year}-${String(month).padStart(2, '0')}-${String(index + 1).padStart(2, '0')}`,
      count: 0,
      posts: [],
      ...byDay.get(index + 1)
    }))
  ]
}

export function shiftCalendarMonth(year, month, delta) {
  const date = new Date(year, month - 1 + delta, 1)
  return { year: date.getFullYear(), month: date.getMonth() + 1 }
}

export function currentCalendarMonth(now = new Date()) {
  return { year: now.getFullYear(), month: now.getMonth() + 1 }
}

export function selectableCalendarDate(cell) {
  return cell && Number(cell.count) > 0 && Array.isArray(cell.posts) && cell.posts.length
    ? cell.date
    : ''
}

export function createCalendarRequestGate() {
  let current = 0
  return {
    begin() {
      current += 1
      return current
    },
    isCurrent(version) {
      return version === current
    }
  }
}
