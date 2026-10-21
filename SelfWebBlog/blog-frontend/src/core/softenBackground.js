import { DEFAULT_BACKGROUND_BLUR, backgroundBlurRadius } from './backgroundBlur.js'

/**
 * Produce a small, pre-blurred data URL for the site wallpaper.
 *
 * CSS filters on a fixed full-viewport layer are expensive to composite and can
 * stall capture/repaint on low-power GPUs. Rendering the blur once into a small
 * canvas keeps the visual effect without paying the cost on every frame.
 */
export function softenBackgroundImage(
  url,
  { width = 720, blur = backgroundBlurRadius(DEFAULT_BACKGROUND_BLUR) } = {}
) {
  return new Promise((resolve) => {
    if (!url || typeof document === 'undefined' || typeof Image === 'undefined') {
      resolve('')
      return
    }
    const image = new Image()
    image.decoding = 'async'
    image.onload = () => {
      try {
        const sourceWidth = image.naturalWidth || width
        const sourceHeight = image.naturalHeight || Math.round(width * 0.66)
        const canvas = document.createElement('canvas')
        canvas.width = width
        canvas.height = Math.max(1, Math.round((sourceHeight / sourceWidth) * width))
        const context = canvas.getContext('2d')
        if (!context) {
          resolve('')
          return
        }
        const pad = blur * 2
        context.filter = `blur(${blur}px) saturate(0.9)`
        context.drawImage(image, -pad, -pad, canvas.width + pad * 2, canvas.height + pad * 2)
        resolve(canvas.toDataURL('image/jpeg', 0.72))
      } catch {
        // Tainted canvas (remote image without CORS) or an unsupported encoder.
        resolve('')
      }
    }
    image.onerror = () => resolve('')
    image.src = url
  })
}
