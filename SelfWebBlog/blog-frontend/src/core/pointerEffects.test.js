import test from 'node:test'
import assert from 'node:assert/strict'
import {
  RIPPLE_DURATION,
  appendRipple,
  canRenderPointerEffects,
  createRipple,
  isPrimaryTap,
  resolveCanvasDpr,
  rippleFrame,
  trailPointLimit,
  trimTrailPoints
} from './pointerEffects.js'

test('pointer effects require a focused fine mouse environment with motion enabled', () => {
  const ready = {
    siteReduced: false,
    systemReduced: false,
    finePointer: true,
    hoverCapable: true,
    visible: true,
    focused: true
  }
  assert.equal(canRenderPointerEffects(ready), true)
  const disabledStates = [
    { siteReduced: true },
    { systemReduced: true },
    { finePointer: false },
    { hoverCapable: false },
    { visible: false },
    { focused: false }
  ]
  disabledStates.forEach((state) => {
    assert.equal(canRenderPointerEffects({ ...ready, ...state }), false)
  })
})

test('speed lengthens the ribbon while interactive hover keeps it tighter', () => {
  assert.ok(trailPointLimit(1.5, false) > trailPointLimit(0.05, false))
  assert.ok(trailPointLimit(1.5, true) < trailPointLimit(1.5, false))
})

test('only a stationary primary mouse gesture creates a ripple', () => {
  assert.equal(isPrimaryTap({ button: 0, distance: 4, duration: 180, pointerType: 'mouse' }), true)
  assert.equal(isPrimaryTap({ button: 2, distance: 0, duration: 100, pointerType: 'mouse' }), false)
  assert.equal(
    isPrimaryTap({ button: 0, distance: 20, duration: 100, pointerType: 'mouse' }),
    false
  )
  assert.equal(isPrimaryTap({ button: 0, distance: 0, duration: 100, pointerType: 'touch' }), false)
  assert.equal(
    isPrimaryTap({ button: 0, distance: 0, duration: 2_000, pointerType: 'mouse' }),
    true
  )
  assert.equal(
    isPrimaryTap({
      button: 0,
      distance: 0,
      duration: 100,
      pointerType: 'mouse',
      cancelled: true
    }),
    false
  )
})

test('ripples are capped and expire after the requested lifetime', () => {
  const ripples = Array.from({ length: 5 }, (_, index) => createRipple(index, index, index, index))
  assert.deepEqual(
    appendRipple(ripples.slice(0, 4), ripples[4]).map((item) => item.x),
    [1, 2, 3, 4]
  )
  assert.equal(rippleFrame(ripples[0], RIPPLE_DURATION - 1).active, true)
  assert.equal(rippleFrame(ripples[0], RIPPLE_DURATION).active, false)
  assert.equal(rippleFrame(createRipple(0, 0, 100), 99).active, false)
})

test('trail pruning and DPR limits keep work bounded', () => {
  const points = Array.from({ length: 40 }, (_, index) => ({
    x: index,
    y: index,
    time: index * 10
  }))
  assert.equal(trimTrailPoints(points, { now: 400, maxPoints: 32, maxAge: 260 }).length, 25)
  const width = 3840
  const height = 2160
  const dpr = resolveCanvasDpr({ deviceDpr: 3, width, height })
  assert.ok(dpr <= 1)
  assert.ok(width * height * dpr * dpr <= 4_000_000)
})
