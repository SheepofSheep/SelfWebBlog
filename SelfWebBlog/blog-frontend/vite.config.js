import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const root = fileURLToPath(new URL('.', import.meta.url))

const backendProxy = {
  '^/(api|uploads|actuator)': {
    target: 'http://localhost:8080',
    changeOrigin: true
  }
}

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': path.join(root, 'src')
    }
  },
  build: {
    rolldownOptions: {
      output: {
        codeSplitting: {
          groups: [
            { name: 'lezer-runtime', test: /node_modules[\\/]@lezer[\\/]/ },
            {
              name: 'codemirror-markdown',
              test: /node_modules[\\/]@codemirror[\\/]lang-markdown[\\/]/
            },
            {
              name: 'codemirror-language',
              test: /node_modules[\\/]@codemirror[\\/](language|autocomplete)[\\/]/
            },
            { name: 'codemirror-core', test: /node_modules[\\/]@codemirror[\\/]/ },
            { name: 'markdown-runtime', test: /node_modules[\\/](marked|dompurify)[\\/]/ },
            { name: 'qr-runtime', test: /node_modules[\\/]qrcode[\\/]/ }
          ]
        }
      }
    }
  },
  server: {
    port: 5174,
    proxy: backendProxy
  },
  preview: {
    port: 4174,
    proxy: backendProxy
  }
})
