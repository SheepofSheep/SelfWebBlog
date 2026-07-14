export function shouldEnablePointerTrail({ motion, finePointer, documentVisible = true }) {
  return motion === 'full' && finePointer && documentVisible
}
