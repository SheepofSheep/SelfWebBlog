import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

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
      '@': path.resolve(__dirname, './src')
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
