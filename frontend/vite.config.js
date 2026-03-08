import { fileURLToPath } from 'node:url';
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

const projectRoot = fileURLToPath(new URL('.', import.meta.url));

export default defineConfig({
  root: projectRoot,
  resolve: {
    preserveSymlinks: true,
  },
  plugins: [react()],
  server: {
    host: '0.0.0.0',
    port: 5173,
    fs: {
      allow: [projectRoot],
    },
    proxy: {
      '/rest': {
        target: 'http://localhost:18080',
        changeOrigin: true,
      },
      '/common': {
        target: 'http://localhost:18080',
        changeOrigin: true,
      },
      '/mail': {
        target: 'http://localhost:18080',
        changeOrigin: true,
      },
      '/chat': {
        target: 'http://localhost:18080',
        changeOrigin: true,
      },
      '/starworks-groupware-websocket': {
        target: 'ws://localhost:18080',
        changeOrigin: true,
        ws: true,
      },
    },
  },
});
