import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
  },
  test: {
    globals: true,           // Enable global test functions (describe, it, expect)
    environment: 'jsdom',    // Use jsdom for browser environment
    setupFiles: './src/test/setup.ts', // Setup file (we'll create this)
    css: true,               // Process CSS files
  },
})