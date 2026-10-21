import test from 'node:test'
import assert from 'node:assert/strict'
import { nextTick } from 'vue'
import * as drafts from '../core/draftStorage.js'
import * as backgrounds from '../core/postBackground.js'
import * as format from '../core/format.js'
import * as publish from '../core/publishPolicy.js'
import { componentHarness, memoryStorage } from '../test/componentHarness.js'

test('publishing a new article leaves no published content in the next new-article draft', async () => {
  const storage = memoryStorage()
  const navigations = []
  const editor = componentHarness(
    new URL('./WritePage.vue', import.meta.url),
    {
      ...drafts,
      ...backgrounds,
      ...format,
      ...publish,
      useRoute: () => ({ params: {}, fullPath: '/write' }),
      useRouter: () => ({ replace() {}, push: (path) => navigations.push(path) }),
      localStorage: storage,
      window: { addEventListener() {}, removeEventListener() {} },
      appStore: { ready: true, user: { role: 'ADMIN' } },
      api: { savePost: async () => '添加成功' },
      notify() {}
    },
    ['post', 'save']
  )
  try {
    await editor.mount()
    editor.state.post.title = '已发布文章'
    editor.state.post.content = '已经提交的正文'
    await nextTick()
    await editor.state.save('PUBLISHED')
    await nextTick()
    assert.deepEqual(navigations, ['/admin?tab=content'])
  } finally {
    editor.unmount()
  }
  assert.equal(drafts.readDraft(storage, drafts.draftKey()), null)
})

test('local storage failures keep the editor usable and do not misreport a successful server save', async () => {
  const messages = []
  const editor = componentHarness(
    new URL('./WritePage.vue', import.meta.url),
    {
      ...drafts,
      ...backgrounds,
      ...format,
      ...publish,
      useRoute: () => ({ params: { id: '7' }, fullPath: '/write/7' }),
      useRouter: () => ({ replace() {}, push() {} }),
      localStorage: {
        getItem: () => null,
        setItem() {
          throw new Error('quota')
        },
        removeItem() {
          throw new Error('denied')
        }
      },
      window: { addEventListener() {}, removeEventListener() {} },
      appStore: { ready: true, user: { role: 'ADMIN' } },
      api: {
        post: async () => ({ id: '7', content: '正文', status: 'DRAFT' }),
        savePost: async () => '成功'
      },
      notify: (message) => messages.push(message)
    },
    ['post', 'save', 'persistLocalDraft', 'draftStorageError', 'draftDirty']
  )
  try {
    await editor.mount()
    editor.state.post.content = '修改后的正文'
    await nextTick()
    editor.state.persistLocalDraft()
    assert.equal(editor.state.draftDirty.value, true)
    assert.match(editor.state.draftStorageError.value, /本地草稿保存失败/)
    await editor.state.save('DRAFT')
    assert.equal(messages.includes('草稿已保存'), true)
    assert.equal(messages.includes('denied'), false)
    assert.equal(editor.state.draftDirty.value, false)
  } finally {
    editor.unmount()
  }
})

test('edits made while a save is in flight remain recoverable after leaving', async () => {
  const storage = memoryStorage()
  let completeSave
  const pending = new Promise((resolve) => {
    completeSave = resolve
  })
  const editor = componentHarness(
    new URL('./WritePage.vue', import.meta.url),
    {
      ...drafts,
      ...backgrounds,
      ...format,
      ...publish,
      useRoute: () => ({ params: { id: '7' }, fullPath: '/write/7' }),
      useRouter: () => ({ replace() {}, push() {} }),
      localStorage: storage,
      window: { addEventListener() {}, removeEventListener() {} },
      appStore: { ready: true, user: { role: 'ADMIN' } },
      api: {
        post: async () => ({ id: '7', title: '文章', content: '原文', status: 'PUBLISHED' }),
        savePost: () => pending
      },
      notify() {}
    },
    ['post', 'save']
  )
  try {
    await editor.mount()
    editor.state.post.content = '提交内容'
    await nextTick()
    const saving = editor.state.save('PUBLISHED')
    editor.state.post.content = '尚未提交的新修改'
    await nextTick()
    completeSave('修改成功')
    await saving
  } finally {
    editor.unmount()
  }
  assert.equal(drafts.readDraft(storage, drafts.draftKey('7')).content, '尚未提交的新修改')
})

test('cover uploads block saves, then the permanent cover URL is submitted and recoverable', async () => {
  const storage = memoryStorage()
  const saved = []
  const editor = componentHarness(
    new URL('./WritePage.vue', import.meta.url),
    {
      ...drafts,
      ...backgrounds,
      ...format,
      ...publish,
      useRoute: () => ({ params: { id: '7' }, fullPath: '/write/7' }),
      useRouter: () => ({ replace() {}, push() {} }),
      localStorage: storage,
      window: { addEventListener() {}, removeEventListener() {} },
      appStore: { ready: true, user: { role: 'ADMIN' } },
      api: {
        post: async () => ({ id: '7', content: '正文', status: 'DRAFT' }),
        savePost: async (payload) => saved.push(payload)
      },
      notify() {}
    },
    ['post', 'save', 'coverUploading', 'pendingUploads', 'publishable']
  )
  try {
    await editor.mount()
    editor.state.coverUploading.value = true
    assert.equal(editor.state.pendingUploads.value, true)
    assert.equal(editor.state.publishable.value, false)
    await editor.state.save('DRAFT')
    await editor.state.save('PUBLISHED')
    assert.equal(saved.length, 0)
    editor.state.coverUploading.value = false
    editor.state.post.coverUrl = '/uploads/cover.png'
    await editor.state.save('DRAFT')
    assert.equal(saved[0].coverUrl, '/uploads/cover.png')
    editor.state.post.coverUrl = '/uploads/replacement.png'
    await nextTick()
  } finally {
    editor.unmount()
  }
  assert.equal(drafts.readDraft(storage, drafts.draftKey('7')).coverUrl, '/uploads/replacement.png')
})
