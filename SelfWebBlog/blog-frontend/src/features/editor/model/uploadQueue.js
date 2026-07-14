const ALLOWED = {
  queued: new Set(['uploading', 'cancelled']),
  uploading: new Set(['completed', 'failed', 'cancelled']),
  failed: new Set(['queued', 'cancelled']),
  completed: new Set(),
  cancelled: new Set()
}

export function createUploadQueue() {
  const tasks = new Map()

  function add(file, id = crypto.randomUUID()) {
    const task = { id, file, status: 'queued', progress: 0, error: '', markdown: '' }
    tasks.set(id, task)
    return { ...task }
  }

  function transition(id, status, patch = {}) {
    const current = tasks.get(id)
    if (!current) throw new Error('Upload task not found')
    if (!ALLOWED[current.status]?.has(status)) {
      throw new Error(`Invalid upload transition: ${current.status} -> ${status}`)
    }
    const next = { ...current, ...patch, status }
    tasks.set(id, next)
    return { ...next }
  }

  function retry(id) {
    return transition(id, 'queued', { progress: 0, error: '' })
  }

  function updateProgress(id, progress) {
    const current = tasks.get(id)
    if (!current || current.status !== 'uploading') return null
    const next = { ...current, progress: Math.min(100, Math.max(0, Math.round(progress))) }
    tasks.set(id, next)
    return { ...next }
  }

  return {
    add,
    transition,
    retry,
    updateProgress,
    get: (id) => (tasks.has(id) ? { ...tasks.get(id) } : null),
    list: () => [...tasks.values()].map((task) => ({ ...task }))
  }
}
