export function headingId(text, index) {
  const slug = String(text || '')
    .normalize('NFKC')
    .trim()
    .toLocaleLowerCase()
    .replace(/[^\p{L}\p{N}]+/gu, '-')
    .replace(/^-|-$/g, '')
  return `section-${index}-${slug || 'heading'}`
}
