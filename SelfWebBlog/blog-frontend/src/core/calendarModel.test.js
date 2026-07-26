import test from 'node:test'
import assert from 'node:assert/strict'
import { buildMonthCells } from './calendarModel.js'

test('builds Monday-first month cells and attaches calendar posts', () => {
  const cells = buildMonthCells(2026, 7, [{ date: '2026-07-26', count: 2, posts: [{ id: '1' }] }])
  assert.equal(cells[0], null)
  assert.equal(cells[2].day, 1)
  assert.equal(cells.find((cell) => cell?.day === 26).count, 2)
})
