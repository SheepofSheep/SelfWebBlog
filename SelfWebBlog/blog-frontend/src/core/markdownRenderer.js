import { Marked } from 'marked'
import { presentationKinds } from './markdownFormatting.js'

export const markdownParser = new Marked({ gfm: true, breaks: true })
markdownParser.use({
  extensions: [
    {
      name: 'presentation',
      level: 'block',
      start(source) {
        return source.match(/^::: /m)?.index
      },
      tokenizer(source) {
        const opening =
          /^::: (align-(?:left|center|right)|size-(?:small|normal|large|xlarge))\s*\n/.exec(source)
        if (!opening || !presentationKinds.has(opening[1])) return
        let depth = 1
        const lines = source.slice(opening[0].length).split('\n')
        let length = opening[0].length
        const body = []
        let fence = null
        for (const line of lines) {
          const codeFence = /^\s{0,3}(`{3,}|~{3,})/.exec(line)
          if (codeFence) {
            if (!fence) fence = codeFence[1]
            else if (codeFence[1][0] === fence[0] && codeFence[1].length >= fence.length)
              fence = null
          }
          if (!fence && /^::: \S+\s*$/.test(line)) depth++
          if (!fence && /^:::\s*$/.test(line) && --depth === 0) {
            const raw = source.slice(
              0,
              length + line.length + (source[length + line.length] === '\n' ? 1 : 0)
            )
            return {
              type: 'presentation',
              raw,
              kind: opening[1],
              tokens: this.lexer.blockTokens(body.join('\n'))
            }
          }
          body.push(line)
          length += line.length + 1
        }
      },
      renderer(token) {
        return `<div class="md-${token.kind}">${this.parser.parse(token.tokens)}</div>\n`
      }
    }
  ]
})

export const markdownSanitizeOptions = {
  ADD_TAGS: ['video', 'audio'],
  ADD_ATTR: ['controls', 'playsinline'],
  // Article HTML cannot style/position the application or embed active documents.
  FORBID_TAGS: ['style', 'form', 'iframe', 'object', 'embed'],
  FORBID_ATTR: ['style', 'id', 'name'],
  ALLOW_DATA_ATTR: false
}

export function cleanMarkdownTree(fragment) {
  fragment.querySelectorAll('[class]').forEach((element) => {
    const allowed = [...element.classList].filter(
      (name) =>
        /^md-(align-(left|center|right)|size-(small|normal|large|xlarge))$/.test(name) ||
        (element.tagName === 'CODE' && /^language-[\w-]+$/.test(name))
    )
    if (allowed.length) element.className = allowed.join(' ')
    else element.removeAttribute('class')
  })
  fragment.querySelectorAll('input').forEach((input) => {
    if (input.type === 'checkbox') input.disabled = true
    else input.remove()
  })
  fragment.querySelectorAll('a[href]').forEach((link) => {
    if (/^https?:\/\//i.test(link.getAttribute('href'))) {
      link.setAttribute('target', '_blank')
      link.setAttribute('rel', 'noopener noreferrer')
    } else link.removeAttribute('target')
  })
  fragment.querySelectorAll('img').forEach((image) => {
    image.loading = 'lazy'
    image.decoding = 'async'
  })
  return fragment
}
