import test from 'node:test'
import assert from 'node:assert/strict'
import { reactive } from 'vue'
import { componentHarness } from '../test/componentHarness.js'

function coverHarness(upload) {
  const props = reactive({ title: '', summary: '', coverUrl: '/uploads/old.png', disabled: false })
  const events = []
  const revoked = []
  let blobId = 0
  const component = componentHarness(
    new URL('./ArticleDetails.vue', import.meta.url),
    {
      defineProps: () => props,
      defineEmits: () => (name, value) => {
        events.push([name, value])
        if (name === 'update:coverUrl') props.coverUrl = value
      },
      api: { upload },
      URL: {
        createObjectURL: () => `blob:cover-${++blobId}`,
        revokeObjectURL: (url) => revoked.push(url)
      }
    },
    [
      'uploadCover',
      'cancelUpload',
      'removeCover',
      'uploading',
      'error',
      'previewUrl',
      'selectCover'
    ]
  )
  return { ...component, props, events, revoked }
}
const file = { name: 'cover.png', type: 'image/png' }

test('cover upload uses the existing API and emits only the permanent URL', async () => {
  let finish
  const cover = coverHarness(() => new Promise((resolve) => (finish = resolve)))
  try {
    const pending = cover.state.uploadCover(file)
    assert.equal(cover.state.previewUrl.value, 'blob:cover-1')
    assert.equal(cover.props.coverUrl, '/uploads/old.png')
    assert.equal(cover.state.uploading.value, true)
    finish({ originalUrl: '/uploads/new.png' })
    await pending
    assert.equal(cover.props.coverUrl, '/uploads/new.png')
    assert.equal(cover.state.uploading.value, false)
    assert.deepEqual(
      cover.events.filter(([name]) => name === 'update:coverUrl'),
      [['update:coverUrl', '/uploads/new.png']]
    )
    assert.deepEqual(cover.revoked, ['blob:cover-1'])
  } finally {
    cover.unmount()
  }
})

test('failed cover replacement preserves the saved cover and resets the file input', async () => {
  const cover = coverHarness(async () => {
    throw new Error('图片过大')
  })
  try {
    const input = { files: [file], value: 'cover.png' }
    cover.state.selectCover({ target: input })
    await new Promise((resolve) => setImmediate(resolve))
    assert.equal(input.value, '')
    assert.equal(cover.props.coverUrl, '/uploads/old.png')
    assert.equal(cover.state.previewUrl.value, '/uploads/old.png')
    assert.equal(cover.state.error.value, '图片过大')
    assert.equal(cover.state.uploading.value, false)
    assert.deepEqual(cover.revoked, ['blob:cover-1'])
  } finally {
    cover.unmount()
  }
})

test('removing a cover ignores a late upload response and aborts the pending request', async () => {
  let finish
  let signal
  const cover = coverHarness((selected, options) => {
    signal = options.signal
    return new Promise((resolve) => (finish = resolve))
  })
  try {
    const pending = cover.state.uploadCover(file)
    cover.state.removeCover()
    assert.equal(signal.aborted, true)
    finish({ originalUrl: '/uploads/late.png' })
    await pending
    assert.equal(cover.props.coverUrl, '')
    assert.equal(cover.state.uploading.value, false)
    assert.deepEqual(cover.revoked, ['blob:cover-1'])
  } finally {
    cover.unmount()
  }
})

test('leaving the editor aborts cover upload and releases its local preview', async () => {
  let finish
  let signal
  const cover = coverHarness((selected, options) => {
    signal = options.signal
    return new Promise((resolve) => (finish = resolve))
  })
  const pending = cover.state.uploadCover(file)
  cover.unmount()
  assert.equal(signal.aborted, true)
  finish({ originalUrl: '/uploads/late.png' })
  await pending
  assert.equal(cover.props.coverUrl, '/uploads/old.png')
  assert.deepEqual(cover.revoked, ['blob:cover-1'])
})

test('non-image files are rejected without invoking the upload API', async () => {
  let calls = 0
  const cover = coverHarness(async () => calls++)
  try {
    await cover.state.uploadCover({ name: 'readme.txt', type: 'text/plain' })
    assert.equal(calls, 0)
    assert.match(cover.state.error.value, /JPG/)
    assert.equal(cover.props.coverUrl, '/uploads/old.png')
  } finally {
    cover.unmount()
  }
})
