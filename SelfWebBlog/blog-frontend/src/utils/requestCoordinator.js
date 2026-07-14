export function createRequestCoordinator() {
  const shared = new Map()
  const latest = new Map()

  function share(key, request) {
    if (shared.has(key)) return shared.get(key)
    const promise = Promise.resolve().then(request)
    shared.set(key, promise)
    promise.finally(() => {
      if (shared.get(key) === promise) shared.delete(key)
    })
    return promise
  }

  function replace(key, request) {
    latest.get(key)?.abort()
    const controller = new AbortController()
    latest.set(key, controller)
    return Promise.resolve()
      .then(() => request(controller.signal))
      .finally(() => {
        if (latest.get(key) === controller) latest.delete(key)
      })
  }

  function cancel(key) {
    latest.get(key)?.abort()
    latest.delete(key)
  }

  function cancelAll() {
    latest.forEach((controller) => controller.abort())
    latest.clear()
  }

  return { share, replace, cancel, cancelAll }
}
