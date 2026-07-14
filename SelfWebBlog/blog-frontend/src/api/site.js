import { api, unwrap } from './http'

export async function getContentCalendar(year, month) {
  const res = await api.get('/content/calendar', { params: { year, month } })
  return unwrap(res.data)
}

export async function getSiteSummary() {
  const res = await api.get('/site/summary')
  return unwrap(res.data)
}
