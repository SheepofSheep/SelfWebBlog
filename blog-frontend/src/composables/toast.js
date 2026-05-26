import { inject, provide, ref } from 'vue'

const KEY = Symbol('toast')

let _items = null

function getItems() {
  if (!_items) _items = ref([])
  return _items
}

function pushToast(message, type = 'success') {
  const items = getItems()
  const id = `${Date.now()}-${Math.random()}`
  items.value.push({ id, message, type })
  setTimeout(() => {
    items.value = items.value.filter((t) => t.id !== id)
  }, 3000)
}

export function provideToast() {
  const items = getItems()
  provide(KEY, { items, push: pushToast })
  return { items, push: pushToast }
}

export function useToast() {
  const api = inject(KEY)
  if (!api) {
    return { items: getItems(), push: pushToast }
  }
  return api
}

export function showToast(message, type = 'success') {
  pushToast(message, type)
}
