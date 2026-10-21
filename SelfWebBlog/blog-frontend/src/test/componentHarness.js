import { readFileSync } from 'node:fs'
import * as compiler from 'vue/compiler-sfc'
import * as vue from 'vue'

// Execute the real setup script with Vue reactivity and isolated browser/API boundaries.
export function componentHarness(url, bindings, names) {
  const { descriptor } = compiler.parse(readFileSync(url, 'utf8'), { filename: url.pathname })
  const compiled = compiler.compileScript(descriptor, { id: 'component-regression' })
  let body = descriptor.scriptSetup.content
  const imports = compiled.scriptSetupAst
    .filter((node) => node.type === 'ImportDeclaration')
    .sort((left, right) => right.start - left.start)
  for (const node of imports) body = body.slice(0, node.start) + body.slice(node.end)

  const mounted = []
  const cleanup = []
  const scope = vue.effectScope()
  const dependencies = {
    ...vue,
    ...bindings,
    onMounted: (callback) => mounted.push(callback),
    onBeforeUnmount: (callback) => cleanup.push(callback)
  }
  const keys = Object.keys(dependencies).filter((key) => /^[A-Za-z_$][\w$]*$/.test(key))
  const setup = new Function(...keys, `${body}\nreturn {${names.join(',')}}`)
  const state = scope.run(() => setup(...keys.map((key) => dependencies[key])))
  return {
    state,
    async mount() {
      for (const callback of mounted) await callback()
      await new Promise((resolve) => setImmediate(resolve))
    },
    unmount() {
      for (const callback of cleanup) callback()
      scope.stop()
    }
  }
}

export function memoryStorage() {
  const values = new Map()
  return {
    getItem: (key) => values.get(key) ?? null,
    setItem: (key, value) => values.set(key, value),
    removeItem: (key) => values.delete(key)
  }
}
