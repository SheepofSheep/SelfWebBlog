import test from 'node:test'
import assert from 'node:assert/strict'
import {
  allowedInteractionTransitions,
  canMovePinnedInteraction,
  canPinInteraction,
  canReplyToInteraction,
  chartGeometry,
  createAdminRequestGate,
  fillTrend,
  formatBytes,
  mergeAssetReferenceDetails,
  moveOrderedItem,
  normalizeAdminUserPage,
  pageAfterMutation,
  syncSiteAvatar,
  sharedInteractionTransitions
} from './adminModel.js'

test('admin request gate accepts only the newest overlapping request', () => {
  const gate = createAdminRequestGate()
  const first = gate.begin()
  const second = gate.begin()

  assert.equal(second, first + 1)
  assert.equal(gate.isCurrent(first), false)
  assert.equal(gate.isCurrent(second), true)
})

test('admin request gate prevents stale completion from clearing current loading state', () => {
  const gate = createAdminRequestGate()
  const first = gate.begin()
  const second = gate.begin()
  let loading = true

  assert.equal(
    gate.runIfCurrent(first, () => {
      loading = false
    }),
    false
  )
  assert.equal(loading, true)
  assert.equal(
    gate.runIfCurrent(second, () => {
      loading = false
    }),
    true
  )
  assert.equal(loading, false)
})

test('media reference details replace stale status and count without dropping asset metadata', () => {
  const asset = {
    id: 'asset-1',
    originalUrl: '/uploads/asset-1.webp',
    referenceStatus: 'ACTIVE',
    referenceCount: 3
  }

  assert.deepEqual(
    mergeAssetReferenceDetails(asset, { referenceStatus: 'ORPHANED', referenceCount: 0 }),
    {
      id: 'asset-1',
      originalUrl: '/uploads/asset-1.webp',
      referenceStatus: 'ORPHANED',
      referenceCount: 0
    }
  )
})

test('fillTrend returns a stable daily series and preserves backend values', () => {
  const series = fillTrend(
    [
      { date: '2026-07-24', views: 4, interactions: 2, posts: 1 },
      { date: '2026-07-26', views: 8, interactions: 1, posts: 0 }
    ],
    3,
    '2026-07-26'
  )

  assert.deepEqual(series, [
    { date: '2026-07-24', views: 4, interactions: 2, posts: 1 },
    { date: '2026-07-25', views: 0, interactions: 0, posts: 0 },
    { date: '2026-07-26', views: 8, interactions: 1, posts: 0 }
  ])
})

test('chart geometry remains finite for an all-zero series', () => {
  const geometry = chartGeometry([0, 0, 0], 600, 220)
  assert.equal(geometry.points.length, 3)
  assert.ok(geometry.points.every((point) => Number.isFinite(point.x) && Number.isFinite(point.y)))
  assert.match(geometry.path, /^M /)
})

test('chart geometry can share one scale across unlike trend series', () => {
  const views = chartGeometry([0, 100], 600, 220, 20, 100)
  const interactions = chartGeometry([0, 1], 600, 220, 20, 100)

  assert.equal(views.maximum, interactions.maximum)
  assert.ok(views.points[1].y < interactions.points[1].y)
  assert.ok(interactions.points[1].y > 190)
})

test('ordered rows move one position without mutating input', () => {
  const rows = [{ id: '1' }, { id: '2' }, { id: '3' }]
  assert.deepEqual(
    moveOrderedItem(rows, '3', 'UP').map((row) => row.id),
    ['1', '3', '2']
  )
  assert.deepEqual(
    rows.map((row) => row.id),
    ['1', '2', '3']
  )
})

test('formatBytes produces compact readable sizes', () => {
  assert.equal(formatBytes(0), '0 B')
  assert.equal(formatBytes(1536), '1.5 KB')
})

test('interaction moderation actions mirror the backend transition policy', () => {
  assert.deepEqual(allowedInteractionTransitions('PENDING'), ['PUBLISHED', 'SPAM'])
  assert.deepEqual(allowedInteractionTransitions('PUBLISHED'), ['HIDDEN', 'SPAM'])
  assert.deepEqual(allowedInteractionTransitions('HIDDEN'), ['PUBLISHED'])
  assert.deepEqual(allowedInteractionTransitions('SPAM'), ['HIDDEN'])
  assert.deepEqual(allowedInteractionTransitions('DELETED'), [])
})

test('batch moderation exposes only transitions shared by every selected row', () => {
  assert.deepEqual(sharedInteractionTransitions([{ status: 'PENDING' }, { status: 'PUBLISHED' }]), [
    'SPAM'
  ])
  assert.deepEqual(sharedInteractionTransitions([{ status: 'PUBLISHED' }, { status: 'SPAM' }]), [
    'HIDDEN'
  ])
  assert.deepEqual(sharedInteractionTransitions([{ status: 'PENDING' }, { status: 'DELETED' }]), [])
  assert.deepEqual(sharedInteractionTransitions([]), [])
})

test('only published root interactions can be pinned or reordered', () => {
  assert.equal(canPinInteraction({ status: 'PUBLISHED', rootId: null }), true)
  assert.equal(canPinInteraction({ status: 'PENDING', rootId: null }), false)
  assert.equal(canPinInteraction({ status: 'PUBLISHED', rootId: 12 }), false)
  assert.equal(canMovePinnedInteraction({ status: 'PUBLISHED', rootId: null, pinned: true }), true)
  assert.equal(
    canMovePinnedInteraction({ status: 'PUBLISHED', rootId: null, pinned: false }),
    false
  )
})

test('moderators can reply only after an interaction is publicly visible', () => {
  assert.equal(canReplyToInteraction({ status: 'PUBLISHED' }), true)
  assert.equal(canReplyToInteraction({ status: 'PENDING' }), false)
  assert.equal(canReplyToInteraction({ status: 'HIDDEN' }), false)
  assert.equal(canReplyToInteraction({ status: 'SPAM' }), false)
  assert.equal(canReplyToInteraction({ status: 'DELETED' }), false)
})

test('mutation falls back one page when the current moderation page becomes empty', () => {
  assert.equal(pageAfterMutation(3, []), 2)
  assert.equal(pageAfterMutation(1, []), 1)
  assert.equal(pageAfterMutation(3, [{ id: 1 }]), 3)
})

test('admin user pages normalize the paged contract and retain the legacy users field', () => {
  const record = { id: '9223372036854775000', username: 'visitor' }

  assert.deepEqual(
    normalizeAdminUserPage({ users: [record], current: 2, pages: 3, total: 21, size: 10 }, 2, 10),
    { records: [record], current: 2, pages: 3, total: 21, size: 10, duplicateIpCount: 0 }
  )
  assert.deepEqual(normalizeAdminUserPage([record], 1, 20).records, [record])
})

test('site avatar synchronization leaves the signed-in account store unchanged', () => {
  const user = { id: '1', role: 'ADMIN', avatarUrl: '/uploads/avatars/account.webp' }
  const store = { user, site: { nickname: '博主', avatarUrl: '/uploads/avatars/site-old.webp' } }

  syncSiteAvatar(store, '/uploads/avatars/site-new.webp')

  assert.deepEqual(store.site, {
    nickname: '博主',
    avatarUrl: '/uploads/avatars/site-new.webp'
  })
  assert.equal(store.user, user)
  assert.equal(store.user.avatarUrl, '/uploads/avatars/account.webp')
})
