import test from 'node:test'
import assert from 'node:assert/strict'
import { formatMarkdown } from './markdownFormatting.js'
import { markdownParser } from './markdownRenderer.js'

const apply = (text, result) => text.slice(0, result.from) + result.insert + text.slice(result.to)

test('inline formatting retains the selected words and can toggle back', () => {
  const first = formatMarkdown('前文正文后文', 2, 4, 'bold')
  const text = apply('前文正文后文', first)
  assert.equal(text, '前文**正文**后文')
  assert.equal(text.slice(first.anchor, first.head), '正文')
  assert.equal(apply(text, formatMarkdown(text, first.anchor, first.head, 'bold')), '前文正文后文')
  assert.equal(apply('正文', formatMarkdown('正文', 0, 2, 'italic')), '*正文*')
})
test('a command without selection inserts editable placeholder text', () => {
  const result = formatMarkdown('', 0, 0, 'bold')
  assert.equal(result.insert.slice(result.anchor, result.head), '粗体文字')
})
test('block commands expand to full selected lines and retain surrounding text', () => {
  const source = '开头\n第一行\n第二行\n结尾'
  assert.equal(
    apply(source, formatMarkdown(source, 4, 10, 'bullet')),
    '开头\n\n- 第一行\n- 第二行\n结尾'
  )
  assert.equal(formatMarkdown('正文', 0, 2, 'size', '999px'), null)
})
test('font size and alignment keep Markdown inside nested safe directives', () => {
  const first = formatMarkdown('**正文**', 0, 6, 'align', 'center')
  const text = apply('**正文**', first)
  const second = apply(text, formatMarkdown(text, 0, text.length, 'size', 'large'))
  const html = markdownParser.parse(second)
  assert.match(html, /md-size-large/)
  assert.match(html, /md-align-center/)
  assert.match(html, /<strong>正文<\/strong>/)
})
test('changing alignment or size updates the existing directive instead of nesting conflicting styles', () => {
  const text = '::: align-center\n正文\n:::'
  const result = apply(text, formatMarkdown(text, 17, 19, 'align', 'right'))
  assert.equal(result, '::: align-right\n正文\n:::')
  const sized = '::: size-large\n正文\n:::'
  assert.equal(
    apply(sized, formatMarkdown(sized, 0, sized.length, 'size', 'normal')),
    '::: size-normal\n正文\n:::'
  )
})
test('unclosed or unsupported presentation directives do not become HTML attributes', () => {
  assert.doesNotMatch(markdownParser.parse('::: size-999\n文字\n:::'), /class="md-/)
  assert.doesNotMatch(markdownParser.parse('::: align-center\n未结束'), /class="md-/)
})

test('alignment is replaced inside a selected font block and across several selected blocks', () => {
  const nested = '::: size-large\n::: align-center\n文字\n:::\n:::'
  const updated = apply(nested, formatMarkdown(nested, 0, nested.length, 'align', 'right'))
  assert.doesNotMatch(updated, /align-center/)
  assert.match(updated, /size-large\n::: align-right/)
  const multiple = '::: align-center\n甲\n:::\n\n::: align-left\n乙\n:::'
  assert.equal(
    (
      apply(multiple, formatMarkdown(multiple, 0, multiple.length, 'align', 'right')).match(
        /align-right/g
      ) || []
    ).length,
    2
  )
})
test('fenced code can contain literal directive delimiters without closing the outer block', () => {
  const html = markdownParser.parse('::: align-center\n```text\n:::\n```\n\n**文字**\n:::')
  assert.match(html, /<code class="language-text">:::/)
  assert.match(html, /<strong>文字<\/strong>/)
})
test('ordinary GFM tables, lists and code remain supported', () => {
  assert.match(markdownParser.parse('| A | B |\n| --- | --- |\n| 1 | 2 |'), /<table>/)
  assert.match(markdownParser.parse('- [x] 完成\n- [ ] 待办'), /type="checkbox"/)
  assert.match(markdownParser.parse('```js\nconst value = 1\n```'), /language-js/)
})
