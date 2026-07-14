import { api, unwrap } from './http'

function makeFileForm(file) {
  const form = new FormData()
  form.append('file', file)
  return form
}

export async function uploadFile(path, file, options = {}) {
  const res = await api.post(path, makeFileForm(file), {
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress: options.onUploadProgress
  })
  return unwrap(res.data)
}

export async function uploadImage(file, options) {
  return uploadFile('/upload/image', file, options)
}

export async function listArticleAssets() {
  const res = await api.get('/upload/assets')
  return unwrap(res.data)
}

export async function uploadAvatar(file, options) {
  return uploadFile('/upload/avatar', file, options)
}

export async function uploadUserAvatar(file, options) {
  return uploadFile('/user/avatar', file, options)
}
