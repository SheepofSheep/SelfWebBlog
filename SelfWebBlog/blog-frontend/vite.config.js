import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src')
    }
  },
  server: {
    port: 5174,
    proxy: {
      '^/(uploads|profile|posts|comments|upload|auth|admin|user|tags)': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
