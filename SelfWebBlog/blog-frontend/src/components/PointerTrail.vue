<script setup>
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
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
} from '@/core/pointerEffects.js'

const canvas = ref(null)
const route = useRoute()

const FALLBACK_COLOR = [215, 166, 66]
const INTERACTIVE_SELECTOR =
  'a[href], button:not(:disabled), [role="button"], [role="link"], .post-card'
const EXCLUDED_SELECTOR =
  'input, textarea, select, option, [contenteditable="true"], pre, code, .cm-editor'
const RIPPLE_ECHO_DELAY = 52

let context
let frameId = 0
let lastFrame = 0
let canvasDpr = 1
let enabled = false
let hasPointer = false
let interactive = false
let resizePending = true
let channels = FALLBACK_COLOR
let points = []
let ripples = []
let press = null
let reducedMotionMedia
let finePointerMedia
let hoverMedia
let rootObserver

const target = { x: 0, y: 0, time: 0, speed: 0 }
const head = { x: 0, y: 0, vx: 0, vy: 0 }

const clamp = (value, min, max) => Math.min(max, Math.max(min, value))
const rgba = (alpha) => `rgba(${channels[0]}, ${channels[1]}, ${channels[2]}, ${alpha})`

function readThemeColor() {
  const value = getComputedStyle(document.documentElement).getPropertyValue('--accent').trim()
  const hex = value.match(/^#([\da-f]{3}|[\da-f]{6})$/i)?.[1]

  if (hex) {
    const expanded = hex.length === 3 ? [...hex].map((part) => `${part}${part}`).join('') : hex
    channels = [0, 2, 4].map((offset) => Number.parseInt(expanded.slice(offset, offset + 2), 16))
    return
  }

  const rgb = value.match(/^rgba?\(\s*([\d.]+)[,\s]+([\d.]+)[,\s]+([\d.]+)/i)
  channels = rgb
    ? rgb.slice(1, 4).map((part) => clamp(Math.round(Number(part)), 0, 255))
    : FALLBACK_COLOR
}

function clearCanvas() {
  if (!context || !canvas.value) return
  context.clearRect(0, 0, window.innerWidth, window.innerHeight)
}

function stopFrame() {
  if (frameId) cancelAnimationFrame(frameId)
  frameId = 0
  lastFrame = 0
}

function resetEffects() {
  stopFrame()
  points = []
  ripples = []
  press = null
  hasPointer = false
  interactive = false
  head.vx = 0
  head.vy = 0
  clearCanvas()
}

watch(
  () => route.fullPath,
  () => resetEffects(),
  { flush: 'sync' }
)

function wake() {
  if (enabled && !frameId) frameId = requestAnimationFrame(renderFrame)
}

function resizeCanvas() {
  if (!canvas.value || !context) return

  const width = Math.max(1, window.innerWidth)
  const height = Math.max(1, window.innerHeight)
  canvasDpr = resolveCanvasDpr({
    deviceDpr: window.devicePixelRatio || 1,
    width,
    height
  })
  canvas.value.width = Math.max(1, Math.floor(width * canvasDpr))
  canvas.value.height = Math.max(1, Math.floor(height * canvasDpr))
  canvas.value.style.width = `${width}px`
  canvas.value.style.height = `${height}px`
  context.setTransform(canvasDpr, 0, 0, canvasDpr, 0, 0)
  resizePending = false
}

function syncAvailability() {
  const nextEnabled = canRenderPointerEffects({
    siteReduced: document.documentElement.dataset.motion === 'reduced',
    systemReduced: reducedMotionMedia?.matches ?? true,
    finePointer: finePointerMedia?.matches ?? false,
    hoverCapable: hoverMedia?.matches ?? false,
    visible: !document.hidden,
    focused: document.hasFocus()
  })

  if (!nextEnabled) {
    enabled = false
    resetEffects()
    return
  }

  if (!enabled) resizePending = true
  enabled = true
}

function isInteractiveTarget(eventTarget) {
  if (!(eventTarget instanceof Element) || eventTarget.closest(EXCLUDED_SELECTOR)) return false
  return Boolean(eventTarget.closest(INTERACTIVE_SELECTOR))
}

function recordPointerMove(event) {
  if (!enabled || event.pointerType !== 'mouse') return

  const now = performance.now()
  const isFirstPointer = !hasPointer
  const elapsed = Math.max(8, now - target.time)
  const distance = hasPointer ? Math.hypot(event.clientX - target.x, event.clientY - target.y) : 0

  target.x = event.clientX
  target.y = event.clientY
  target.time = now
  target.speed = distance / elapsed
  interactive = isInteractiveTarget(event.target)

  if (!hasPointer) {
    hasPointer = true
    head.x = target.x
    head.y = target.y
    head.vx = 0
    head.vy = 0
    points = [{ x: head.x, y: head.y, time: now }]
  }

  if (press && press.pointerId === event.pointerId) {
    press.distance = Math.max(
      press.distance,
      Math.hypot(event.clientX - press.x, event.clientY - press.y)
    )
  }

  if (isFirstPointer || distance > 0.05) wake()
}

function recordPointerDown(event) {
  if (!enabled || event.pointerType !== 'mouse') return
  if (event.button !== 0) {
    if (press?.pointerId === event.pointerId) press.cancelled = true
    return
  }
  press = {
    pointerId: event.pointerId,
    button: event.button,
    x: event.clientX,
    y: event.clientY,
    distance: 0,
    cancelled: false
  }
}

function recordPointerUp(event) {
  if (!press || press.pointerId !== event.pointerId) return
  if (event.button !== 0) {
    press.cancelled = true
    return
  }

  const now = performance.now()
  const distance = Math.max(
    press.distance,
    Math.hypot(event.clientX - press.x, event.clientY - press.y)
  )
  const shouldRipple =
    enabled &&
    isPrimaryTap({
      button: press.button,
      distance,
      pointerType: event.pointerType,
      cancelled: press.cancelled
    })

  press = null
  if (!shouldRipple) return

  const phase = (event.clientX + event.clientY + now) * 0.018
  ripples = appendRipple(ripples, createRipple(event.clientX, event.clientY, now, phase))
  wake()
}

function cancelPress() {
  press = null
}

function cancelDrag() {
  if (press) press.cancelled = true
}

function handleResize() {
  resizePending = true
  wake()
}

function traceSmoothOpenPath(pathPoints, reverse = false, connect = false) {
  const ordered = reverse ? [...pathPoints].reverse() : pathPoints
  if (!ordered.length) return

  if (connect) context.lineTo(ordered[0].x, ordered[0].y)
  else context.moveTo(ordered[0].x, ordered[0].y)
  for (let index = 1; index < ordered.length - 1; index += 1) {
    const point = ordered[index]
    const next = ordered[index + 1]
    context.quadraticCurveTo(point.x, point.y, (point.x + next.x) / 2, (point.y + next.y) / 2)
  }
  const last = ordered.at(-1)
  if (last) context.lineTo(last.x, last.y)
}

function ribbonEdges(now) {
  const lastIndex = points.length - 1
  const left = []
  const right = []

  points.forEach((point, index) => {
    const previous = points[Math.max(0, index - 1)]
    const next = points[Math.min(lastIndex, index + 1)]
    const dx = next.x - previous.x
    const dy = next.y - previous.y
    const length = Math.max(0.001, Math.hypot(dx, dy))
    const progress = lastIndex ? index / lastIndex : 1
    const age = clamp(1 - (now - point.time) / 260, 0, 1)
    const profile = Math.pow(progress, 0.72) * (1 - 0.34 * Math.pow(progress, 8))
    const baseWidth = interactive ? 5.5 : 6.3
    const sway = Math.sin(now * 0.008 + index * 0.82) * 0.42 * (1 - progress)
    const offset = Math.max(0, baseWidth * profile * age + sway)
    const nx = -dy / length
    const ny = dx / length

    left.push({ x: point.x + nx * offset, y: point.y + ny * offset })
    right.push({ x: point.x - nx * offset, y: point.y - ny * offset })
  })

  return { left, right }
}

function createRibbonPath(left, right) {
  context.beginPath()
  traceSmoothOpenPath(left)
  traceSmoothOpenPath(right, true, true)
  context.closePath()
}

function drawRibbon(now) {
  if (points.length < 2) return

  const { left, right } = ribbonEdges(now)
  const tail = points[0]
  const tip = points.at(-1)
  const gradient = context.createLinearGradient(tail.x, tail.y, tip.x, tip.y)
  const tipAlpha = interactive ? 0.43 : 0.34
  gradient.addColorStop(0, rgba(0))
  gradient.addColorStop(0.35, rgba(tipAlpha * 0.34))
  gradient.addColorStop(0.76, rgba(tipAlpha * 0.75))
  gradient.addColorStop(1, rgba(tipAlpha))

  context.save()
  createRibbonPath(left, right)
  context.fillStyle = gradient
  context.shadowColor = rgba(interactive ? 0.2 : 0.15)
  context.shadowBlur = interactive ? 8 : 10
  context.globalAlpha = 0.7
  context.fill()
  context.restore()

  context.save()
  createRibbonPath(left, right)
  context.fillStyle = gradient
  context.fill()

  const highlight = points.slice(Math.floor(points.length * 0.42))
  if (highlight.length > 1) {
    context.beginPath()
    traceSmoothOpenPath(highlight)
    context.strokeStyle = rgba(interactive ? 0.3 : 0.22)
    context.lineWidth = 0.8
    context.lineCap = 'round'
    context.lineJoin = 'round'
    context.stroke()
  }
  context.restore()
}

function ripplePath(ripple, radius, progress, phaseOffset) {
  const ring = []
  const segments = 48

  for (let index = 0; index <= segments; index += 1) {
    const angle = (index / segments) * Math.PI * 2
    const deformation =
      1 + 0.018 * Math.sin(angle * 3 + ripple.phase + progress * 2.4 + phaseOffset)
    ring.push({
      x: ripple.x + Math.cos(angle) * radius * deformation,
      y: ripple.y + Math.sin(angle) * radius * deformation
    })
  }

  context.beginPath()
  traceSmoothOpenPath(ring)
  context.closePath()
}

function drawRippleLayer(ripple, now, delay, echo = false) {
  const createdAt = ripple.createdAt + delay
  if (now < createdAt) return

  const state = rippleFrame({ ...ripple, createdAt }, now)
  if (!state.active) return

  context.save()
  ripplePath(ripple, state.radius, state.progress, echo ? 0.72 : 0)
  context.strokeStyle = rgba(state.alpha * (echo ? 0.2 : 0.38))
  context.lineWidth = state.lineWidth * (echo ? 0.72 : 1)
  context.lineCap = 'round'
  context.lineJoin = 'round'
  context.shadowColor = rgba(state.alpha * 0.12)
  context.shadowBlur = 4
  context.stroke()
  context.restore()
}

function drawRipples(now) {
  ripples.forEach((ripple) => {
    drawRippleLayer(ripple, now, 0)
    drawRippleLayer(ripple, now, RIPPLE_ECHO_DELAY, true)
  })
}

function renderFrame(now) {
  frameId = 0
  if (!enabled || !context) return
  if (resizePending) resizeCanvas()

  const delta = clamp(lastFrame ? now - lastFrame : 16.667, 1, 32)
  const step = delta / 16.667
  lastFrame = now

  if (hasPointer) {
    if (now - target.time > 300) {
      head.x = target.x
      head.y = target.y
      head.vx = 0
      head.vy = 0
    } else {
      head.vx = (head.vx + (target.x - head.x) * 0.12 * step) * Math.pow(0.72, step)
      head.vy = (head.vy + (target.y - head.y) * 0.12 * step) * Math.pow(0.72, step)
      head.x += head.vx * step
      head.y += head.vy * step
    }

    const lastPoint = points.at(-1)
    if (!lastPoint || Math.hypot(head.x - lastPoint.x, head.y - lastPoint.y) > 1.2) {
      points.push({ x: head.x, y: head.y, time: now })
    }
    points = trimTrailPoints(points, {
      now,
      maxPoints: trailPointLimit(target.speed, interactive)
    })
  }

  clearCanvas()
  drawRibbon(now)
  drawRipples(now)
  ripples = ripples.filter((ripple) => now - ripple.createdAt < RIPPLE_DURATION + RIPPLE_ECHO_DELAY)

  const headIsMoving = hasPointer && Math.hypot(target.x - head.x, target.y - head.y) > 0.45
  if (points.length > 1 || ripples.length || headIsMoving) {
    frameId = requestAnimationFrame(renderFrame)
  } else {
    points = []
    head.x = target.x
    head.y = target.y
    head.vx = 0
    head.vy = 0
    lastFrame = 0
  }
}

onMounted(() => {
  context = canvas.value?.getContext('2d', { alpha: true })
  if (!context) return

  reducedMotionMedia = matchMedia('(prefers-reduced-motion: reduce)')
  finePointerMedia = matchMedia('(pointer: fine)')
  hoverMedia = matchMedia('(hover: hover)')
  ;[reducedMotionMedia, finePointerMedia, hoverMedia].forEach((media) =>
    media.addEventListener('change', syncAvailability)
  )

  rootObserver = new MutationObserver((mutations) => {
    if (mutations.some((mutation) => mutation.attributeName === 'data-theme')) readThemeColor()
    if (mutations.some((mutation) => mutation.attributeName === 'data-motion')) syncAvailability()
  })
  rootObserver.observe(document.documentElement, {
    attributes: true,
    attributeFilter: ['data-theme', 'data-motion']
  })

  window.addEventListener('pointermove', recordPointerMove, { passive: true })
  window.addEventListener('pointerdown', recordPointerDown, { passive: true })
  window.addEventListener('pointerup', recordPointerUp, { passive: true })
  window.addEventListener('pointercancel', cancelPress, { passive: true })
  window.addEventListener('dragstart', cancelDrag, { passive: true })
  window.addEventListener('resize', handleResize, { passive: true })
  window.addEventListener('blur', syncAvailability)
  window.addEventListener('focus', syncAvailability)
  document.addEventListener('visibilitychange', syncAvailability)

  readThemeColor()
  syncAvailability()
})

onBeforeUnmount(() => {
  window.removeEventListener('pointermove', recordPointerMove)
  window.removeEventListener('pointerdown', recordPointerDown)
  window.removeEventListener('pointerup', recordPointerUp)
  window.removeEventListener('pointercancel', cancelPress)
  window.removeEventListener('dragstart', cancelDrag)
  window.removeEventListener('resize', handleResize)
  window.removeEventListener('blur', syncAvailability)
  window.removeEventListener('focus', syncAvailability)
  document.removeEventListener('visibilitychange', syncAvailability)
  ;[reducedMotionMedia, finePointerMedia, hoverMedia].forEach((media) =>
    media?.removeEventListener('change', syncAvailability)
  )
  rootObserver?.disconnect()
  enabled = false
  resetEffects()
  context = undefined
})
</script>

<template>
  <Teleport to="body">
    <canvas ref="canvas" class="pointer-effects-canvas" aria-hidden="true" />
  </Teleport>
</template>
