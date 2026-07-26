import test from 'node:test'
import assert from 'node:assert/strict'
import {
  buildMonthCells,
  createCalendarRequestGate,
  currentCalendarMonth,
  selectableCalendarDate,
  shiftCalendarMonth
} from './calendarModel.js'

test('only real dates containing articles can open calendar details', () => {
  assert.equal(
    selectableCalendarDate({ date: '2026-07-26', count: 1, posts: [{ id: 'post-1' }] }),
    '2026-07-26'
  )
  assert.equal(selectableCalendarDate({ date: '2026-07-25', count: 0, posts: [] }), '')
  assert.equal(selectableCalendarDate(null), '')
})

test('current month control derives the visible year and month at click time', () => {
  assert.deepEqual(currentCalendarMonth(new Date(2027, 0, 1)), {
    year: 2027,
    month: 1
  })
})

test('builds Monday-first month cells and attaches calendar posts', () => {
  const cells = buildMonthCells(2026, 7, [{ date: '2026-07-26', count: 2, posts: [{ id: '1' }] }])
  assert.equal(cells[0], null)
  assert.equal(cells[2].day, 1)
  assert.equal(cells.find((cell) => cell?.day === 26).count, 2)
  assert.equal(cells.find((cell) => cell?.day === 26).date, '2026-07-26')
})

test('calendar month navigation crosses year boundaries', () => {
  assert.deepEqual(shiftCalendarMonth(2026, 1, -1), { year: 2025, month: 12 })
  assert.deepEqual(shiftCalendarMonth(2026, 12, 1), { year: 2027, month: 1 })
})

test('calendar request gate accepts only the latest month response', () => {
  const gate = createCalendarRequestGate()
  const july = gate.begin()
  const august = gate.begin()

  assert.equal(gate.isCurrent(july), false)
  assert.equal(gate.isCurrent(august), true)
})
