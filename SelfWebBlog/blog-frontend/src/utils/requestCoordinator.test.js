import assert from 'node:assert/strict'
import test from 'node:test'
import { createRequestCoordinator } from './requestCoordinator.js'

test('shares an in-flight read by key and releases it after completion', async () => {
  const coordinator = createRequestCoordinator()
  let calls = 0
  const request = () => new Promise((resolve) => setTimeout(() => resolve(++calls), 5))
  const first = coordinator.share('summary', request)
  const second = coordinator.share('summary', request)

  assert.equal(first, second)
  assert.equal(await first, 1)
  assert.equal(await coordinator.share('summary', request), 2)
})

test('replacing a request aborts the older operation', async () => {
  const coordinator = createRequestCoordinator()
  let firstSignal
  const first = coordinator.replace('search', async (signal) => {
    firstSignal = signal
    await new Promise((resolve) => setTimeout(resolve, 10))
    return 'old'
  })
  await Promise.resolve()
  const second = coordinator.replace('search', async () => 'new')

  assert.equal(firstSignal.aborted, true)
  assert.equal(await second, 'new')
  assert.equal(await first, 'old')
})

test('cancelAll aborts active latest requests', async () => {
  const coordinator = createRequestCoordinator()
  let signal
  const pending = coordinator.replace('page', async (nextSignal) => {
    signal = nextSignal
    await new Promise((resolve) => setTimeout(resolve, 5))
  })
  await Promise.resolve()
  coordinator.cancelAll()
  assert.equal(signal.aborted, true)
  await pending
})
