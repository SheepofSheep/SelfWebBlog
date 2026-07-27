# Fluid Pointer Effects Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the discrete dot cursor trail with a theme-aware Canvas liquid ribbon, restrained hover response, and bounded click ripples that shut down cleanly on reduced-motion and touch environments.

**Architecture:** Keep `PointerTrail.vue` as the single global integration point, but replace its DOM-dot state with one Canvas renderer. Put deterministic policy and lifecycle math in `pointerEffects.js` so speed adaptation, gesture rejection, DPR limits, and ripple timing can be tested without mocking Canvas.

**Tech Stack:** Vue 3 Composition API, Canvas 2D, requestAnimationFrame, native Pointer Events, Node test runner, Playwright.

---

## File map

- Create `SelfWebBlog/blog-frontend/src/core/pointerEffects.js`: pure enablement, sizing, gesture, trail, and ripple helpers.
- Create `SelfWebBlog/blog-frontend/src/core/pointerEffects.test.js`: deterministic unit coverage for those helpers.
- Replace `SelfWebBlog/blog-frontend/src/components/PointerTrail.vue`: Canvas lifecycle, events, theme color, ribbon and ripple drawing.
- Modify `SelfWebBlog/blog-frontend/src/styles.css`: replace dot styles with the non-interactive Canvas layer and reduced-motion/coarse-pointer guards.
- Modify `SelfWebBlog/blog-frontend/src/styles-public-upgrade.css`: keep its pointer-effect reduced-motion selector aligned with the new Canvas class.
- Modify `SelfWebBlog/blog-frontend/e2e/core-flows.spec.js`: rendered Canvas, primary click, drag rejection, reduced motion, and click passthrough regression tests.

### Task 1: Pure pointer-effect model

**Files:**
- Create: `SelfWebBlog/blog-frontend/src/core/pointerEffects.js`
- Test: `SelfWebBlog/blog-frontend/src/core/pointerEffects.test.js`

- [ ] **Step 1: Write the failing model tests**

Cover these exact public contracts:

```js
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

test('only a short stationary primary mouse gesture creates a ripple', () => {
  assert.equal(isPrimaryTap({ button: 0, distance: 4, duration: 180, pointerType: 'mouse' }), true)
  assert.equal(isPrimaryTap({ button: 2, distance: 0, duration: 100, pointerType: 'mouse' }), false)
  assert.equal(isPrimaryTap({ button: 0, distance: 20, duration: 100, pointerType: 'mouse' }), false)
  assert.equal(isPrimaryTap({ button: 0, distance: 0, duration: 100, pointerType: 'touch' }), false)
})

test('ripples are capped and expire after the requested lifetime', () => {
  const ripples = Array.from({ length: 5 }, (_, index) => createRipple(index, index, index, index))
  assert.deepEqual(appendRipple(ripples.slice(0, 4), ripples[4]).map((item) => item.x), [1, 2, 3, 4])
  assert.equal(rippleFrame(ripples[0], RIPPLE_DURATION - 1).active, true)
  assert.equal(rippleFrame(ripples[0], RIPPLE_DURATION).active, false)
})

test('trail pruning and DPR limits keep work bounded', () => {
  const points = Array.from({ length: 40 }, (_, index) => ({ x: index, y: index, time: index * 10 }))
  assert.equal(trimTrailPoints(points, { now: 400, maxPoints: 32, maxAge: 260 }).length, 25)
  assert.ok(resolveCanvasDpr({ deviceDpr: 3, width: 3840, height: 2160 }) <= 1)
})
```

- [ ] **Step 2: Run the tests and verify RED**

Run: `pnpm test -- --test-name-pattern "pointer effects|ribbon|ripple|DPR"`

Expected: FAIL because `pointerEffects.js` does not exist.

- [ ] **Step 3: Implement the minimal pure model**

Use these constants and formulas so the component and tests share one policy:

```js
export const TRAIL_LIMIT = 32
export const TRAIL_MAX_AGE = 260
export const RIPPLE_LIMIT = 4
export const RIPPLE_DURATION = 520

const clamp = (value, min, max) => Math.min(max, Math.max(min, value))

export function canRenderPointerEffects(state) {
  return (
    !state.siteReduced &&
    !state.systemReduced &&
    state.finePointer &&
    state.hoverCapable &&
    state.visible &&
    state.focused
  )
}

export function trailPointLimit(speed = 0, interactive = false) {
  const speedRatio = clamp((speed - 0.05) / 1.45, 0, 1)
  return clamp(Math.round(10 + speedRatio * 22) - (interactive ? 4 : 0), 8, TRAIL_LIMIT)
}

export function isPrimaryTap({ button, distance, duration, pointerType, cancelled = false }) {
  return !cancelled && pointerType === 'mouse' && button === 0 && distance <= 7 && duration <= 650
}

export function trimTrailPoints(points, { now, maxPoints, maxAge = TRAIL_MAX_AGE }) {
  return points.filter((point) => now - point.time < maxAge).slice(-maxPoints)
}

export function createRipple(x, y, createdAt, phase = 0) {
  return { x, y, createdAt, phase }
}

export function appendRipple(ripples, ripple, limit = RIPPLE_LIMIT) {
  return [...ripples, ripple].slice(-limit)
}

export function rippleFrame(ripple, now) {
  const progress = clamp((now - ripple.createdAt) / RIPPLE_DURATION, 0, 1)
  const eased = 1 - Math.pow(1 - progress, 3)
  return {
    active: progress < 1,
    progress,
    radius: 10 + eased * 78,
    alpha: Math.pow(1 - progress, 1.7),
    lineWidth: 2.2 - progress * 1.35
  }
}

export function resolveCanvasDpr({ deviceDpr = 1, width, height, maxDpr = 1.75, maxPixels = 4_000_000 }) {
  const pixelCap = Math.sqrt(maxPixels / Math.max(1, width * height))
  return clamp(Math.min(deviceDpr, maxDpr, pixelCap), 0.75, maxDpr)
}
```

- [ ] **Step 4: Run the model tests and verify GREEN**

Run: `pnpm test -- --test-name-pattern "pointer effects|ribbon|ripple|DPR"`

Expected: all model tests pass.

### Task 2: Rendered behavior tests

**Files:**
- Modify: `SelfWebBlog/blog-frontend/e2e/core-flows.spec.js`

- [ ] **Step 1: Add a bounded Canvas pixel helper**

```js
async function canvasRegionHasInk(page, { x, y, width = 180, height = 180 }) {
  return page.locator('canvas.pointer-effects-canvas').evaluate((canvas, region) => {
    const scale = canvas.width / Math.max(1, canvas.clientWidth)
    const context = canvas.getContext('2d')
    const data = context.getImageData(
      Math.max(0, Math.floor(region.x * scale)),
      Math.max(0, Math.floor(region.y * scale)),
      Math.max(1, Math.floor(region.width * scale)),
      Math.max(1, Math.floor(region.height * scale))
    ).data
    for (let index = 3; index < data.length; index += 4) if (data[index] > 0) return true
    return false
  }, { x, y, width, height })
}
```

- [ ] **Step 2: Add failing desktop interaction coverage**

The test must assert one Canvas, no `<i>` nodes, `pointer-events: none`, visible ink after a stepped mouse move, automatic clearing, no ink after a right click, a visible ring after a left click, no ring after a drag once the trail has expired, and successful opening of the existing appearance dialog.

- [ ] **Step 3: Add failing reduced-motion coverage**

Before navigation call `page.emulateMedia({ reducedMotion: 'reduce' })`; after movement and primary click, assert the sampled Canvas region remains transparent.

- [ ] **Step 4: Run the focused E2E tests and verify RED**

Run: `pnpm exec playwright test e2e/core-flows.spec.js --project=desktop --grep "fluid pointer"`

Expected: FAIL because the current component renders multiple circular `<i>` nodes and has no Canvas.

### Task 3: Canvas component and styling

**Files:**
- Replace: `SelfWebBlog/blog-frontend/src/components/PointerTrail.vue`
- Modify: `SelfWebBlog/blog-frontend/src/styles.css`
- Modify: `SelfWebBlog/blog-frontend/src/styles-public-upgrade.css`

- [ ] **Step 1: Replace the template with one Canvas**

```vue
<template>
  <canvas ref="canvas" class="pointer-effects-canvas" aria-hidden="true" />
</template>
```

- [ ] **Step 2: Add bounded event and lifecycle state**

Import the Task 1 helpers. Maintain one target pointer, one spring head with velocity, at most 32 trail points, at most 4 ripples, one primary press record, one resize flag, and one animation-frame id. Register passive `pointermove`, `pointerdown`, `pointerup`, `pointercancel`, `dragstart`, `blur`, `focus`, `resize`, and `visibilitychange` listeners. Observe `data-motion`/`data-theme`, `(pointer: fine)`, `(hover: hover)`, and `(prefers-reduced-motion: reduce)`; disable and clear immediately whenever `canRenderPointerEffects` returns false.

- [ ] **Step 3: Implement the frame loop**

On each active frame: resize only if flagged; advance the spring using a capped delta; append a center point only after meaningful movement; prune with `trimTrailPoints` and `trailPointLimit`; clear the Canvas; draw the ribbon; draw active ripples from `rippleFrame`; request another frame only while the spring, trail, or ripples remain active.

- [ ] **Step 4: Draw one continuous ribbon**

For each center point compute a tangent from its neighbors and a perpendicular normal. Build left and right edge arrays using a tail-to-head width profile, close them as one quadratic Bézier path, and fill twice: a low-alpha blurred pass followed by a restrained crisp pass. Use a tail-transparent linear gradient and a short center highlight; never call `arc` for trail points and never create DOM trail children.

- [ ] **Step 5: Draw bounded liquid rings**

For each ripple draw 48 connected angular samples with a maximum 1.8% three-lobe sinusoidal radius variation. Stroke one primary ring and one 55ms delayed weaker ring; use the 520ms `rippleFrame` opacity and width, no fill, dots, sparks, or debris.

- [ ] **Step 6: Integrate theme and hover behavior**

Parse `--accent` into RGB channels on mount and theme mutation, falling back to `#d7a642`. Treat only links, enabled buttons, link/button roles, and post cards as interactive; exclude form controls, contenteditable, CodeMirror, `pre`, and `code`. Interactive state reduces point budget by four and increases ribbon alpha by no more than 0.08.

- [ ] **Step 7: Replace global dot CSS**

```css
.pointer-effects-canvas {
  position: fixed;
  z-index: 50;
  inset: 0;
  width: 100vw;
  height: 100vh;
  contain: strict;
  pointer-events: none;
}

@media (pointer: coarse), (hover: none), (prefers-reduced-motion: reduce) {
  .pointer-effects-canvas { display: none; }
}
```

Remove `.pointer-trail i` and update the reduced-motion selector in `styles-public-upgrade.css` to `.pointer-effects-canvas`.

- [ ] **Step 8: Run focused unit and E2E tests and verify GREEN**

Run:

```text
pnpm test
pnpm exec playwright test e2e/core-flows.spec.js --project=desktop --grep "fluid pointer"
```

Expected: all unit tests and fluid pointer tests pass with no console errors.

### Task 4: Full QA and delivery

**Files:**
- Verify all files above; do not stage `SelfWebBlog/src/main/java/org/example/selfwebblog/controller/AuthController.java` or unrelated theme-animation work.

- [ ] **Step 1: Run static and production checks**

Run: `pnpm check`

Expected: unit tests, ESLint, Prettier, and Vite production build pass.

- [ ] **Step 2: Run the complete browser matrix**

Run: `pnpm exec playwright test --workers=3`

Expected: all configured desktop/tablet/mobile tests pass; project-specific skips remain intentional.

- [ ] **Step 3: Validate the live Browser flow**

At `http://127.0.0.1:5174/`, verify URL/title, meaningful DOM, no framework overlay, clean console, a desktop screenshot during a curved move, left-click ripple, right-click/drag rejection, interaction passthrough, and reduced-motion shutdown. Temporarily emulate no-preference only if the Browser host reports system reduced motion, then restore the original media state.

- [ ] **Step 4: Review the scoped diff**

Confirm there are no discrete trail elements, all listeners/observers/frames clean up, Canvas DPR and arrays are bounded, and no unrelated work is staged.

- [ ] **Step 5: Commit the feature**

```text
git add -- SelfWebBlog/blog-frontend/src/core/pointerEffects.js SelfWebBlog/blog-frontend/src/core/pointerEffects.test.js SelfWebBlog/blog-frontend/src/components/PointerTrail.vue SelfWebBlog/blog-frontend/src/styles.css SelfWebBlog/blog-frontend/src/styles-public-upgrade.css SelfWebBlog/blog-frontend/e2e/core-flows.spec.js docs/superpowers/plans/2026-07-27-fluid-pointer-effects.md
git commit -m "feat: add fluid pointer interactions"
```
